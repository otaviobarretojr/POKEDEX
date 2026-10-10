package com.otaviobarreto.pokedex.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startupAndPrimarySurface() = rule.collect(
        packageName = "com.otaviobarreto.pokedex",
        includeInStartupProfile = true
    ) {
        pressHome()
        startActivityAndWait()
    }
}
