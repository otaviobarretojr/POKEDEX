package com.otaviobarreto.pokedex.data

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollectionPersistenceInstrumentedTest {
    @Test fun snapshotRoundTrip_preservesCapturedContextAndBox(){
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        CollectionStore.initialize(context)

        val before=CollectionStore.exportSnapshot()
        try{
            val source="scarlet-violet"
            CollectionStore.setCapturedIn(source,25,true)
            val box=CollectionStore.ensureBox("CI Persistence")!!
            assertTrue(CollectionStore.addToBox(box,25))

            val snapshot=CollectionStore.exportSnapshot()
            CollectionStore.setCaptured(25,false)
            assertTrue(CollectionStore.importSnapshot(snapshot))

            assertTrue(CollectionStore.isCaptured(25))
            assertTrue(CollectionStore.isCapturedIn(source,25))
            assertTrue(25 in CollectionStore.boxes[box].orEmpty())
        }finally{
            assertTrue(CollectionStore.importSnapshot(before))
        }
    }

    @Test fun snapshotImport_rejectsOutOfRangePokemonIds(){
        val context=ApplicationProvider.getApplicationContext<android.content.Context>()
        CollectionStore.initialize(context)
        val before=CollectionStore.exportSnapshot()
        try{
            val invalid=org.json.JSONObject()
                .put("captured",org.json.JSONArray(listOf(-1,0,999999)))
                .put("boxes",org.json.JSONArray())
            assertTrue(CollectionStore.importSnapshot(invalid))
            assertEquals(emptySet<Int>(),CollectionStore.capturedIds)
        }finally{
            assertTrue(CollectionStore.importSnapshot(before))
        }
    }
}
