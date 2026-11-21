# План: атомарные тесты дружбы на двух сессиях

Целевой файл после одобрения: `docs/plans/friendship-session-tests.md` (первый шаг выполнения, плана-режим разрешает писать только в этот файл). Сейчас `docs/` в `.gitignore`, файл останется вне git, пока вы не решите иначе (как и `docs/plans/user-session.md`).

## Контекст

Один большой тест `FriendshipFlowTest.shouldBuildAndBreakFriendshipBetweenTwoUsers` проверяет сразу пять сценариев (заявка, Accept, удаление) цепочкой через интерфейс. При падении непонятно, какое действие сломано, а состояние для следующего шага готовится через UI. Нужны атомарные тесты: один сценарий на тест, предусловие готовит БД через `@User`, а не интерфейс, обе стороны дружбы проверяются каждая в своём браузерном контексте. Сценарии: отправка заявки, Accept, Decline, удаление инициатором, удаление другой стороной. Большой тест удаляется после того, как все пять новых тестов прошли минимум три прогона подряд.

## Опора на существующее (новых extension не нужно)

| Что | Где | Как используется |
|---|---|---|
| `@User(friends, incomeInvitations, outcomeInvitations)` | `jupiter/annotation/User.java`, `UserExtension` | создаёт пользователя и партнёров с дружбой в БД до теста (`UserRequest.fixture()`, `UserWithRelationsFixture`, `Partners`); партнёр — отдельный пользователь с логином и паролем в `TestUser.friends()/incomeInvitations()/outcomeInvitations()` |
| `Relation` | `data/Relation.java` | `INCOME_INVITATION`: партнёр прислал заявку пользователю; `OUTCOME_INVITATION`: пользователь прислал партнёру; `FRIEND`: принятая дружба |
| `@User UserSession` | `SessionExtension`, `SignIn` | второй авторизованный контекст для пользователя без отношений (сценарий «заявка») |
| `GuestSession` | `SessionExtension` | второй чистый контекст; вход партнёра выполняет тест через `WelcomePage → LoginPage` по данным из `TestUser` партнёра |
| `BasePeopleTest`, `BaseWebTest` | `test/people`, `test` | `UserExtension` и `SessionExtension` подключены, таймауты через `ContextTimeouts`, трейс главного и дополнительных контекстов (`trace.zip`, `trace-session-N.zip`) |
| Page Objects | `ui/page/PeoplePage`, `ui/component/PeopleRow`, `PeopleTable` | `openAllPeople`, `openFriends`, `openIncomeInvitations`, `search`, `clickAdd`, `clickAccept`, `clickRemove`, `shouldHaveUsers`, `shouldHaveButtons`, `shouldHaveInvitationSent`, `shouldHaveNotification` |
| `Notification`, `PeopleButton` | `ui/component` | `INVITATION_SENT/ACCEPTED/DECLINED`, `FRIEND_DELETED`; `ADD/ACCEPT/DECLINE/REMOVE` (правило 16) |

Дружба в БД удаляется вместе с пользователем (`sql/api/delete_friendships.sql`), поэтому упавший тест ничего не оставляет.

## Принципы тестов

- Каждый тест: `@UiTest`, наследует `BasePeopleTest`, `@Epic("People")`, `@Feature("Friendship")`, `@DisplayName` на английском, без Javadoc и без комментариев (правило проекта для `test/`).
- Сначала действует одна сторона, потом вторая заходит и проверяет итог: вторая сторона входит после действия, поэтому устаревший кэш Apollo (`cache-and-network`) не мешает.
- Итог проверяется положительными проверками (кнопка `Add` у бывшей пары, кнопки `Remove`, пользователь в списке), а не пустым списком, который до загрузки совпадает с пустым состоянием.
- Нотификация проверяется сразу после клика: снекбар исчезает через 3 секунды.
- Партнёра в `GuestSession` ведёт вспомогательный метод базового класса, а не копия шагов в каждом тесте.

## Тесты

Все в `rfr-e2e/src/test/java/io/student/rangiffler/test/people/`.

| Класс и метод | Предусловие (параметры) | Действие и проверки |
|---|---|---|
| `SendFriendRequestTest.shouldSendFriendRequest` | `@User TestUser sender`, `@User UserSession receiver` (между ними нет отношений) | `sender` на основной странице: All People, поиск, `clickAdd`, `INVITATION_SENT`, «Waiting...»; затем `receiver.page()`: Income invitations содержит `sender` с кнопками Accept и Decline |
| `AcceptFriendRequestTest.shouldAcceptFriendRequest` | `@User(incomeInvitations = 1) TestUser receiver`, `GuestSession inviterSession` | `receiver`: Income invitations, `clickAccept(inviter)`, `INVITATION_ACCEPTED`, Friends содержит `inviter` с `Remove`; затем `inviter` входит во втором контексте: Friends содержит `receiver` с `Remove` |
| `DeclineFriendRequestTest.shouldDeclineFriendRequest` | то же | `receiver`: `clickDecline(inviter)`, `INVITATION_DECLINED`; All People по имени `inviter` показывает `Add`; затем `inviter` входит: All People по имени `receiver` показывает `Add` (заявка снята) |
| `RemoveFriendTest.shouldRemoveFriendWhenInitiatorRemoves` | `@User(friends = 1) TestUser user`, `GuestSession friendSession` | `user`: Friends, `clickRemove(friend)`, `FRIEND_DELETED`; затем `friend` входит во втором контексте: All People по имени `user` показывает `Add` |
| `RemoveFriendTest.shouldRemoveFriendWhenFriendRemoves` | те же параметры | `friend` входит во втором контексте и на Friends нажимает `Remove` для `user`, `FRIEND_DELETED`; затем `user` на основной странице: All People по имени `friend` показывает `Add` |

Во всех тестах, кроме первого, партнёры (`inviter`, `friend`) берутся как `receiver.incomeInvitations().getFirst()` и `user.friends().getFirst()` (логин и пароль есть в `TestUser`).

## Изменения в коде

Новые публичные методы: по рабочему соглашению перед написанием перечисляю и жду OK.
1. `PeopleRow.clickDecline()`: нажимает `Decline`, ждёт исчезновения этой кнопки (как `clickAccept`), `@Step`.
2. `PeoplePage.clickDecline(String username)`: делегирует `table.row(username).clickDecline()`.

Приватно-пакетные изменения (без новых публичных методов):
3. `BasePeopleTest`: перегрузка `PeoplePage openPeople(Page page, TestUser user)`, текущий `openPeople(TestUser)` делегирует ей с полем `page`.

Новые тестовые классы: четыре (`SendFriendRequestTest`, `AcceptFriendRequestTest`, `DeclineFriendRequestTest`, `RemoveFriendTest`).

Ничего не меняется в `UserExtension`, `SessionExtension`, `SessionFixture`, `Notification`, `PeopleButton`: всё нужное уже есть.

## Порядок выполнения

0. Скопировать этот план в `docs/plans/friendship-session-tests.md`.
1. Получить OK на методы `clickDecline`; написать их и перегрузку `openPeople`; компиляция.
2. По одному тесту, в порядке таблицы: написать класс, компиляция, тройной прогон (ниже), только потом следующий.
3. После пяти тестов: прогон пакета `io.student.rangiffler.test.people.*` один раз целиком.
4. Убрать `FriendshipFlowTest.java`: не `rm`, а `mv` в `.FriendshipFlowTest.java.<метка времени>.bak` рядом (в коммит не добавлять), проверить компиляцию и повторный прогон пакета.
5. `test-reviewer` на итоговый diff; исправить высокие и средние замечания.
6. Коммиты только по вашей просьбе; новые коммиты, без `Co-authored-by`, через `courier:commit-changes-to-git`.

## Проверка: минимум три прогона каждого теста

Задачи `e2e` и `test` всегда выполняются заново (`outputs.upToDateWhen { false }` в `rfr-e2e/build.gradle`), поэтому повторный запуск не берёт результат из кэша. Для каждого нового класса три запуска подряд, один за другим:

```bash
./gradlew :rfr-e2e:e2e --tests '*SendFriendRequestTest'
./gradlew :rfr-e2e:e2e --tests '*SendFriendRequestTest'
./gradlew :rfr-e2e:e2e --tests '*SendFriendRequestTest'
```

То же для `AcceptFriendRequestTest`, `DeclineFriendRequestTest`, `RemoveFriendTest` (оба метода каждый раз). Правила:
- Тесты запускаются только после того, как я назову их вам и получу разрешение; если запуск заблокирован классификатором, команды выполняете вы.
- Тест считается проверенным, если все три прогона подряд зелёные. Любое падение: открыть `trace.zip` или `trace-session-N.zip` (`./gradlew :rfr-e2e:showTrace -Ptrace=rfr-e2e/test-results/<класс>/<метод>/trace.zip`), найти причину, исправить, счёт прогонов начать заново с нуля.
- Параллельный запуск (`junit.jupiter.execution.parallel.enabled=false`) не включаем; пользователи случайные, поиск по полному имени, так что конфликтов при включении не ожидается, но это отдельная проверка.

## Критичные файлы

- Создать: `test/people/SendFriendRequestTest.java`, `AcceptFriendRequestTest.java`, `DeclineFriendRequestTest.java`, `RemoveFriendTest.java`.
- Изменить: `ui/component/PeopleRow.java`, `ui/page/PeoplePage.java`, `test/people/BasePeopleTest.java`.
- Убрать (в `.bak`): `test/people/FriendshipFlowTest.java`.
- Опираться (не менять): `jupiter/extension/UserExtension.java`, `SessionExtension.java`, `data/fixture/UserWithRelationsFixture.java`, `Partners.java`, `data/Relation.java`, `ui/component/Notification.java`, `PeopleButton.java`.

## Правила, которые проверить на итоговом diff

- `rfr-test-review`: 5 (позиции `@User`; `GuestSession` без `@User`), 6, 7, 14, 16.
- `rfr-page-objects`: 3, 5, 8, 11, 12, 13 (только видимые результаты, без `nth/first/last`, без сети).
- Проект: тесты без Javadoc и комментариев, `@DisplayName` на английском, 4 пробела, без `Thread.sleep`.
