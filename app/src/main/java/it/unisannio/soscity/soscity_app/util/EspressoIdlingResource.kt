package it.unisannio.soscity.soscity_app.util

import androidx.test.espresso.idling.CountingIdlingResource

object EspressoIdlingResource {

    private const val RESOURCE = "SOSCITY_GLOBAL_IDLING_RESOURCE"

    @JvmField
    val countingIdlingResource = CountingIdlingResource(RESOURCE)

    var isTestEnvironment = false

    fun increment() {
        if (!isTestEnvironment) {
            countingIdlingResource.increment()
        }
    }

    fun decrement() {
        if (!isTestEnvironment) {
            if (!countingIdlingResource.isIdleNow) {
                countingIdlingResource.decrement()
            }
        }
    }
}