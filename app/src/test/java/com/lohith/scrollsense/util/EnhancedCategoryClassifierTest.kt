package com.lohith.scrollsense.util

import org.junit.Assert.assertEquals
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RuntimeEnvironment
import kotlinx.coroutines.runBlocking

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EnhancedCategoryClassifierTest {

    @Test
    fun adultVetoTriggersCategoryAdult() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
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
        val context = RuntimeEnvironment.getApplication()
        val classifier = EnhancedCategoryClassifier(context)
        val result = classifier.classifyContent(
            screenText = "watch xvideos now",
            packageName = "org.chromium.chrome",
            previousCategory = null
        )
        assertEquals("adult", result.category)
    }
}

