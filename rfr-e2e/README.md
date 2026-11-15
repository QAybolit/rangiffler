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

Тесты, которым нужен браузер и стенд, помечены @Tag("ui") и запускаются задачей e2e. Задача test исключает тег ui и обходится без стенда. Обе задачи запускают тесты при каждом вызове, --rerun-tasks не нужен. Перед тестами e2e вызывает waitForStand (scripts/wait-for-stand.sh): он ждёт MySQL, rfr-auth, rfr-api и фронт не дольше WAIT_TIMEOUT секунд (по умолчанию 180).

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

## Статические пользователи

Тест, которому нельзя создать пользователя на лету, получает параметр `@StaticUser TestUser`. UsersQueueExtension выдаёт ему одного из пользователей пула static-user-1, static-user-2, static-user-3 (пароль static-secret) и возвращает его после теста. Пул общий для JVM, ожидание свободного пользователя ограничено rfr.staticUserTimeout. Код этих пользователей не создаёт, а статические тесты должны только читать их данные: пользователи общие для всех тестов и запусков.

Один раз создайте пользователей пула в обеих схемах (`rangiffler-auth` и `rangiffler-api`); скрипт можно запускать повторно:

```bash
docker exec -i rfr-mysql mysql -uroot -psecret < scripts/static-users.sql
```

Без пользователей тесты с @StaticUser падают на логине с «Bad credentials»; waitForStand их наличие не проверяет. Имена и пароль записаны и в StaticUserFixture, меняйте их в двух местах сразу. @StaticUser ставится только на параметр тестового метода, один раз на тест, а в каждый тестовый класс с ним добавляется @ExtendWith(UsersQueueExtension.class).

## Архитектура

Один source set src/test, пакет io.student.rangiffler:

| Пакет | Что лежит |
|---|---|
| config | TestConfig — единственная точка чтения настроек |
| jupiter | всё про JUnit: annotation (@User с friends, incomeInvitations, outcomeInvitations; @StaticUser), extension (UserExtension создаёт фикстуру в beforeEach и закрывает в afterEach; UsersQueueExtension выдаёт пользователя из пула для @StaticUser и возвращает его после теста; UserExtensionTest и UsersQueueExtensionTest проверяют их через EngineTestKit на классах-мишенях, которые выполняются только при параметре rfr.testkit=true) |
| data | тестовые данные и их владельцы: TestUser, UserFixture с реализацией BaseUserFixture, декоратором UserWithRelationsFixture и перечислением Relation и StaticUserFixture (пул статических пользователей) (общую работу с партнёрами делает Partners); UserRequest получает от UserExtension счётчики из @User и собирает цепочку фикстур методом fixture(); db — Databases, Jdbc, репозитории ApiUserRepository и AuthUserRepository (SQL лежит в resources/sql), Friendship и FriendshipStatus |
| utils | статические утилиты без состояния: RandomDataUtils — случайные username/password; сюда же будущие генераторы (описания фото и т.п.) |
| ui | page — Page Objects, component — общие части страниц |
| test | BaseWebTest и тесты по фичам: auth, profile, people (вкладки Friends, Income invitations, Outcome invitations) |

Правила: в jupiter нет SQL и локаторов; в data нет JUnit; в test нет локаторов и SQL; ThreadLocal для состояния теста не используется — состояние живёт в Store расширения.

## Агент

Page Objects пишет Claude Code по skill rfr-page-objects; UI исследуется через Playwright MCP (.mcp.json).

Доказательство корректности теста - запуск gradle таски.
