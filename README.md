# Kwadra

Три фигуры. Поле восемь на восемь. Полный ряд или столбец исчезает.

Kwadra — головоломка для телефона. Внизу лотка всегда до трёх фигур. Фигуру перетаскивают на поле. Клетка, куда она не встаёт, подсвечивается отказом. Несколько линий подряд поднимают комбо, пустое поле после хода даёт надбавку.

Цвет берётся из акцента телефона. Тема — как в системе, светлая или тёмная. Одиннадцать языков: русский, украинский, английский, белорусский, эстонский, японский, французский, немецкий, китайский, корейский, вьетнамский. Звук и вибрация выключаются. Сеть, аккаунт и реклама не используются. Незаконченная партия остаётся на устройстве.

## Сборка

Нужны JDK 17, Android SDK с платформой 36 и Gradle 8.14.3, который уже лежит в `gradlew`.

```bat
set JAVA_HOME=C:\path\to\jdk-17
set ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk
gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

Отладочный пакет: `app\build\outputs\apk\debug\app-debug.apk`.

Релизная сборка без подписи, её ждёт F-Droid:

```bat
gradlew.bat :app:assembleRelease
```

Файл: `app\build\outputs\apk\release\app-release-unsigned.apk`.

## F-Droid

В репозитории есть то, что просит [инструкция F-Droid](https://f-droid.org/docs/Submitting_to_F-Droid_Quick_Start_Guide/): файл `LICENSE` (MIT), рецепт `.fdroid.yml`, описания `fastlane/metadata/android/en-US` и `ru-RU`. Зависимости — AndroidX, Jetpack Compose и JUnit. Все они со свободными лицензиями и лежат в Google Maven или Maven Central. Шрифт Manrope — OFL 1.1, текст лицензии в `LICENSES/OFL-Manrope.txt`.

Сразу в каталог это не попадёт. Политика включения просит, чтобы приложение развивалось хотя бы три месяца, либо у него уже были пользователи, либо автор уже ведёт другие приложения в F-Droid. Скриншотов телефона в метаданных нет: без них заявку тоже возвращают. Идентификатор `app.kwadra` не привязан к домену автора, а F-Droid это советует.

Заявка подаётся в [fdroiddata](https://gitlab.com/fdroid/fdroiddata): issue с ссылкой на этот репозиторий и тег версии, например `v1.0`.

## License

Code is MIT. See [LICENSE](LICENSE).

Manrope by Mikhail Sharanda is used under the SIL Open Font License 1.1. See [LICENSES/OFL-Manrope.txt](LICENSES/OFL-Manrope.txt).
