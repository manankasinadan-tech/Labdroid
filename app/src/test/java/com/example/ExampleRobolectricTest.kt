package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("LABDROID", appName)
  }

  @Test
  fun `theme preferences persist properly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.data.service.ThemePreferences.setThemeMode(context, com.example.ui.theme.AppThemeMode.DARK)
    assertEquals(com.example.ui.theme.AppThemeMode.DARK, com.example.data.service.ThemePreferences.getThemeMode(context))

    com.example.data.service.ThemePreferences.setThemeMode(context, com.example.ui.theme.AppThemeMode.LIGHT)
    assertEquals(com.example.ui.theme.AppThemeMode.LIGHT, com.example.data.service.ThemePreferences.getThemeMode(context))
  }
}

