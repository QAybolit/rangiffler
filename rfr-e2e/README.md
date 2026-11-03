# rfr-e2e

E2E-тесты Rangiffler: JUnit 6.1.3, Playwright Java 1.63 (@UsePlaywright), Allure 3, JdbcClient для фикстур.

## Запуск

```bash
./gradlew :rfr-e2e:installPlaywright                      # один раз: браузер Chromium
./gradlew e2e                                             # все UI-тесты (тег ui)
./gradlew :rfr-e2e:e2e --tests '*LoginTest'            # один тест
./gradlew :rfr-e2e:test                                   # всё без тега ui, стенд не нужен; с --tests тег ui не отсекается (запуск из IDE)
./gradlew :rfr-e2e:allureReport                           # rfr-e2e/build/reports/allure-report/allureReport/index.html
```

Стенд: MySQL :3306, rfr-auth :9001, rfr-api :8081, фронт :3001 (см. корневой README).

Тесты, которым нужен браузер и стенд, помечены @Tag("ui") и запускаются задачей e2e. Задача test исключает тег ui и обходится без стенда. Обе задачи запускают тесты при каждом вызове, --rerun-tasks не нужен. Стенд ждёт StandReadinessExtension перед первым классом с тегом ui, поэтому его ждут и e2e, и UI-тест, запущенный из IDE; отдельной задачи ожидания нет.

## Конфигурация

TestConfig читает -Drfr.<ключ> → RFR_<КЛЮЧ> → значение по умолчанию. Gradle передаёт все -Drfr.* в тестовую JVM.

| Ключ | По умолчанию |
|---|---|
| rfr.frontUrl | http://localhost:3001 |
| rfr.authUrl | http://localhost:9001 |
| rfr.apiUrl | http://localhost:8081 |
| rfr.dbUrl | jdbc:mysql://localhost:3306 (схема добавляется кодом) |
| rfr.dbUser / rfr.dbPassword | root / secret |
| rfr.browser / rfr.headless | chromium / true |
| rfr.browserLocale | en-US |
| rfr.staticUserTimeout | 30 (секунд ожидания свободного пользователя из пула) |
| rfr.standTimeout | 180 (секунд ожидания стенда; каждый запрос ограничен 5 секундами) |
| rfr.standCheck | true (false выключает ожидание стенда совсем) |
| rfr.migrationCheck | false (true включает проверку миграций перед всеми тестами; задача e2e включает её сама) |
| rfr.authMigrations / rfr.apiMigrations | ../rfr-auth/... и ../rfr-api/... (каталоги миграций бэкендов; Gradle передаёт абсолютные пути) |

## Статические пользователи

Тест, которому нельзя создать пользователя на лету, получает параметр `@StaticUser TestUser`. UsersQueueExtension выдаёт ему одного из пользователей пула static-user-1, static-user-2, static-user-3 (пароль static-secret) и возвращает его после теста. Пул общий для JVM, ожидание свободного пользователя ограничено rfr.staticUserTimeout. Код этих пользователей не создаёт, а статические тесты должны только читать их данные: пользователи общие для всех тестов и запусков.

Один раз создайте пользователей пула в обеих схемах (`rangiffler-auth` и `rangiffler-api`); скрипт можно запускать повторно:

```bash
docker exec -i rfr-mysql mysql -uroot -psecret < scripts/static-users.sql
```

Без пользователей тесты с @StaticUser падают на логине с «Bad credentials»; ожидание стенда их наличие не проверяет. Имена и пароль записаны и в StaticUserFixture, меняйте их в двух местах сразу. @StaticUser ставится только на параметр тестового метода, один раз на тест, а в каждый тестовый класс с ним добавляется @ExtendWith(UsersQueueExtension.class).

## Расширения уровня прогона

SuiteExtension — интерфейс расширения, которое срабатывает один раз на весь прогон: beforeSuite(ExtensionContext) вызывается перед первым тестовым классом, afterSuite(ExtensionContext) после последнего, сколько бы классов расширение ни обслуживало. Оба метода получают корневой контекст. Состояние прогона хранит SuiteRun в корневом Store: его кладут туда до вызова beforeSuite, поэтому закрытие происходит в любом случае. Если beforeSuite упал, он не повторяется, каждый класс падает с той же причиной, afterSuite не вызывается, а само исключение выбрасывается ещё раз при закрытии прогона. Если несколько расширений, они стартуют в порядке регистрации и закрываются в обратном.

Реализации SuiteExtension регистрируются глобально через ServiceLoader: класс перечисляется в `src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension`, автообнаружение включено в junit-platform.properties. Через @ExtendWith такое расширение не подключают. Расширение должно безопасно вызываться для любого теста проекта.

SuiteExtensionTest проверяет это через EngineTestKit, не опираясь на реализацию интерфейса (тег extension, MySQL не нужен).

### Ожидание стенда

StandReadinessExtension (SuiteExtension) перед первым классом с тегом ui опрашивает MySQL (TCP), rfr-auth (GET /.well-known/openid-configuration, статус меньше 400), rfr-api (POST /graphql, любой ответ) и фронт (GET /, статус меньше 400) по адресам из TestConfig. Опрос повторяется каждые 2 секунды, пока все четыре не ответят или не истечёт rfr.standTimeout. Каждый запрос ограничен своим таймаутом, бесконечного ожидания нет. Если время вышло, ошибка называет не ответившие сервисы и команды запуска: `docker compose up -d --wait`, `./gradlew runAll`, `cd rfr-gql-client && npm run dev`. Тесты без тега ui ожидание не запускают, в том числе пробы, которым нужна только MySQL. Метод appliesTo интерфейса SuiteExtension позволяет расширению стартовать на первом подходящем классе, а не на первом классе прогона.

Расширение стоит в META-INF/services выше MigrationCheckExtension. Порядок гарантирован: ServiceLoader перебирает провайдеров в порядке строк файла, Jupiter регистрирует автообнаруженные расширения в этом порядке и вызывает BeforeAllCallback в порядке регистрации, а SuiteRun.start() синхронизирован, поэтому сверка миграций не начнётся, пока ожидание не вернулось. Если ожидание упало, beforeAll класса прерывается, и сверка не запускается. ServicesOrderTest проверяет порядок на реальном ServiceLoader, StandReadinessExtensionTest проверяет поведение на подставном HTTP-сервере.

### Проверка миграций

MigrationCheckExtension перед всеми тестами сравнивает миграции, применённые к локальной БД, с файлами бэкендов rfr-auth и rfr-api через Flyway (SchemaMigrations), ничего не изменяя в схеме. Она нужна, когда ветку бэкенда переключили, а БД не пересоздавали. Проверка срабатывает только при rfr.migrationCheck=true, задача e2e включает её сама. Она находит версии, применённые к БД, но отсутствующие в коде, версии в коде, не применённые к БД, и расхождения контрольных сумм. При расхождении прогон падает с перечнем различий и подсказкой: переключить бэкенды на ветку, которая мигрировала БД, или пересоздать БД (`docker compose down -v`, затем `bash localenv.sh`) и перезапустить rfr-auth и rfr-api. Проверка сравнивает БД с файлами чекаута, а не с запущенным процессом, поэтому после смены ветки бэкенды нужно перезапустить.

## Демонстрация @ExtendWith

В пакете test/demo лежат два демонстрационных теста: DemoClassLevelTest и DemoExtendWithTest. Они показывают все места, где можно поставить @ExtendWith, на примере DemoExtension (реализует все callbacks и ParameterResolver, печатает порядок вызовов и идентификатор экземпляра расширения) и составной аннотации @Demo. Для каждого места по два теста, по выводу видно, делят ли тесты один экземпляр расширения.

## Архитектура

Один source set src/test, пакет io.student.rangiffler:

| Пакет | Что лежит |
|---|---|
| config | TestConfig — единственная точка чтения настроек |
| jupiter | всё про JUnit: annotation (@User с friends, incomeInvitations, outcomeInvitations; @StaticUser), extension (UserExtension создаёт фикстуру в beforeEach и закрывает в afterEach; UsersQueueExtension выдаёт пользователя из пула для @StaticUser и возвращает его после теста; SuiteExtension с SuiteRun и MigrationCheckExtension работают на весь прогон; DemoExtension с DemoValue и аннотация Demo нужны демонстрационным тестам), probe (UserExtensionTest, UsersQueueExtensionTest и SuiteExtensionTest проверяют расширения через EngineTestKit на классах-мишенях, которые выполняются только при параметре rfr.testkit=true) |
| data | тестовые данные и их владельцы: TestUser, UserFixture с реализацией BaseUserFixture, декоратором UserWithRelationsFixture и перечислением Relation и StaticUserFixture (пул статических пользователей) (общую работу с партнёрами делает Partners); UserRequest получает от UserExtension счётчики из @User и собирает цепочку фикстур методом fixture(); db — Databases, Jdbc, SchemaMigrations (сравнение миграций БД с файлами бэкенда), репозитории ApiUserRepository и AuthUserRepository (SQL лежит в resources/sql), Friendship и FriendshipStatus |
| stand | ожидание стенда без JUnit: StandService с реализациями TcpService и HttpService, StandReadiness; проверяют StandReadinessTest, TcpServiceTest и HttpServiceTest на подставных сервисах |
| utils | статические утилиты без состояния: RandomDataUtils — случайные username/password; сюда же будущие генераторы (описания фото и т.п.) |
| ui | page — Page Objects, component — общие части страниц |
| test | BaseWebTest и тесты по фичам: auth, profile, people (вкладки Friends, Income invitations, Outcome invitations), demo (демонстрация @ExtendWith) |

Правила: в jupiter нет SQL и локаторов; в data нет JUnit; в test нет локаторов и SQL; ThreadLocal для состояния теста не используется — состояние живёт в Store расширения.

## Агент

Page Objects пишет Claude Code по skill rfr-page-objects; UI исследуется через Playwright MCP (.mcp.json).

Доказательство корректности теста - запуск gradle таски.
