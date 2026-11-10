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

## License

Code is MIT. See [LICENSE](LICENSE).

Manrope by Mikhail Sharanda is used under the SIL Open Font License 1.1. See [LICENSES/OFL-Manrope.txt](LICENSES/OFL-Manrope.txt).
