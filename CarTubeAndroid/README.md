# CarTube 0.1
Прототип неофициального приложения в стиле YouTube на Android.
Главная, поиск, Shorts, подписки, избранное через мобильный сайт YouTube в WebView.
Для Android Automotive OS встроена проверка ограничений CarUxRestrictionsManager:
при ограничении UX воспроизведение ставится на паузу и интерфейс блокируется.

**Неофициальное приложение**. Google sign-in и YouTube WebView playback
могут не работать. Наличие APK не означает, что видео станет доступно на обычном
Android Auto. Просмотр видео допустим только на стоянке на совместимых экранах.

Сборка: Gradle 8.11.1, JDK 17, Android SDK 35, `gradle :app:assembleDebug`.
Установочный файл: `app/build/outputs/apk/debug/app-debug.apk`.
Автоматическая сборка: workflow `.github/workflows/cartube-apk.yml` в корне репозитория.
