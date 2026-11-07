package com.animeav.tv.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test

class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() = baselineProfileRule.collect(
        packageName = "com.animeav.tv.debug",
    ) {
        startActivityAndWait()
        device.wait(Until.hasObject(By.text("One Piece")), 20_000)

        repeat(3) {
            device.pressDPadRight()
            device.pressDPadDown()
            device.pressDPadLeft()
            device.pressDPadUp()
        }

        device.pressDPadDown()
        device.pressDPadDown()
        device.pressEnter()
        device.waitForIdle()
        repeat(4) {
            device.pressDPadRight()
        }
        device.pressEnter()
        device.waitForIdle()
    }
}
