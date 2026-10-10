package com.otaviobarreto.pokedex.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UpgradePersistenceInstrumentedTest {
    @Test
    fun v20CollectionAndBoxState_survivesUpgrade() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        CollectionStore.initialize(context)

        assertTrue("Pikachu capture must survive upgrade",25 in CollectionStore.capturedIds)
        assertTrue("Eevee capture must survive upgrade",133 in CollectionStore.capturedIds)
        assertTrue(
            "Pokémon HOME Box placement must survive upgrade",
            25 in CollectionStore.boxes["Pokémon HOME"].orEmpty()
        )
    }
}
