package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.BengaliKnowledgeBase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
        assertEquals("Next Offline Chat AI", appName)
    }

    @Test
    fun `test offline math solver`() {
        val result = BengaliKnowledgeBase.trySolveMath("২৫ + ৭৫ হিসাব কর")
        assertNotNull(result)
        val text = result ?: ""
        assertTrue(text.contains("100") || text.contains("১০০"))
    }

    @Test
    fun `test bengali language detection`() {
        assertTrue(BengaliKnowledgeBase.isBengali("আসসালামু আলাইকুম"))
    }
}
