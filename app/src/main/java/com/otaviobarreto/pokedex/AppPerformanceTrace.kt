package com.otaviobarreto.pokedex

import android.os.Trace

object AppPerformanceTrace {
    inline fun <T> section(name:String, block:()->T):T {
        Trace.beginSection(name)
        return try { block() } finally { Trace.endSection() }
    }
}
