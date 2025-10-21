package com.lohith.scrollsense.util

import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class EnhancedCategoryClassifierInstrumentedTest {

    @Test
    fun adultVetoTriggersCategoryAdult() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val classifier = EnhancedCategoryClassifier(context)
        val result = classifier.classifyContent(
            screenText = "Checkout onlyfans premium content",
            packageName = "com.android.chrome",
            previousCategory = null
        )
        assertEquals("adult", result.category)
    }

    @Test
    fun chromeAdultPageClassifiesAdult() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val classifier = EnhancedCategoryClassifier(context)
        val result = classifier.classifyContent(
            screenText = "watch xvideos now",
            packageName = "org.chromium.chrome",
            previousCategory = null
        )
        assertEquals("adult", result.category)
    }
}

