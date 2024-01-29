package com.mituuz.fuzzier.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import java.awt.GraphicsDevice
import java.awt.GraphicsEnvironment
import java.awt.Point

@State(
    name = "com.mituuz.fuzzier.FuzzierSettings",
    storages = [Storage("FuzzierSettings.xml")]
)
class FuzzierSettingsService : PersistentStateComponent<FuzzierSettingsService.State> {
    class State {
        var splitPosition: Int = 300
        var exclusionList: List<String> = listOf("/.idea/", "/.git/", "/target/", "/build/", "/.gradle/", "/.run/")
        var newTab: Boolean = false
        var debouncePeriod: Int = 150
        var posX: Int = -1
        var posY: Int = -1
        var graphicsDevices: Array<out GraphicsDevice>? = GraphicsEnvironment.getLocalGraphicsEnvironment().screenDevices
    }

    fun setPosition(p: Point) {
        state.posX = p.x
        state.posY = p.y
    }

    fun getPosition(): Point {
        return Point(state.posX, state.posY)
    }

    private var state = State()

    override fun getState(): State {
        return this.state
    }

    override fun loadState(p0: State) {
        this.state = p0
    }
}