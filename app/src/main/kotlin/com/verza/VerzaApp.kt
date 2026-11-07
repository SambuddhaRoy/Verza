package com.verza

import android.app.Application

class VerzaApp : Application() {
    lateinit var graph: Graph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = Graph(this)
    }
}

/** The app's [Graph], from any context. */
val android.content.Context.graph: Graph get() = (applicationContext as VerzaApp).graph
