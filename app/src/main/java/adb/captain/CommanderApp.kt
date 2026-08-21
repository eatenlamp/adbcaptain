package adb.captain

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Основной класс приложения для инициализации Hilt и других глобальных компонентов.
 */
@HiltAndroidApp
class CommanderApp : Application()
