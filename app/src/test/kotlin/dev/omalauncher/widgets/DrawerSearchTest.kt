package dev.omalauncher.widgets

import dev.omalauncher.widgets.lang.Budget
import dev.omalauncher.widgets.lang.Compiler
import dev.omalauncher.widgets.lang.Env
import dev.omalauncher.widgets.lang.Global
import dev.omalauncher.widgets.lang.Host
import dev.omalauncher.widgets.lang.Lst
import dev.omalauncher.widgets.lang.Null
import dev.omalauncher.widgets.lang.Obj
import dev.omalauncher.widgets.lang.Str
import dev.omalauncher.widgets.lang.Value
import dev.omalauncher.widgets.lang.WNode
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/** Exercises the shipped drawer, including selection when there are no matching apps. */
class DrawerSearchTest {
    private class Drawer(val query: String, val apps: List<Value> = emptyList()) : Host {
        val program = Compiler.compile(File("src/main/assets/widgets/drawer.owl").readText())
        override val budget = Budget()
        private val slots = Array<Value>(program.globals.size) { Null }
        val actions = mutableListOf<String>()
        private val env get() = Env.root(this)

        init {
            program.globals.forEachIndexed { i, g ->
                if (g is Global.State) slots[i] = g.init(env)
            }
        }

        override fun read(slot: Int): Value = when (val g = program.globals[slot]) {
            is Global.Let -> g.value(env)
            is Global.Source -> if (g.kind == "android.drawer") Obj(mapOf("apps" to Lst(apps))) else Null
            else -> slots[slot]
        }
        override fun write(slot: Int, value: Value) { slots[slot] = value }
        override fun tile(name: String): Value = if (name == "query") Str(query) else Null
        override fun theme(name: String): Int? = null
        override fun time() = 0.0
        override fun act(name: String, args: List<Value>) { actions += "$name ${args.joinToString()}" }
        fun key(key: String) = program.onKey!!.run(env, listOf(Str(key)))
        fun submit() = program.onSubmit!!.run(env, listOf(Str(query)))
        fun hint() = program.enter!!(env).toString()
        fun searchRow() = ((program.screen!!.single() as WNode.Element).children.takeLast(2).first() as WNode.Element)
        fun tapSearch() = searchRow().handlers["onTap"]!!.run(env, emptyList())
        fun searchVisible() = searchRow().props["visible"]!!(env).truthy
    }

    @Test fun searchIsReachableWithoutAppsLinuxOrAgent() {
        val drawer = Drawer("a query with no app")
        assertEquals("⏎ ask", drawer.hint())
        drawer.key("up")
        assertEquals("⏎ search", drawer.hint())
        drawer.key("up") // Clamp at search when there are no app hits.
        assertEquals("⏎ search", drawer.hint())
        drawer.submit()
        assertEquals(listOf("launcher search"), drawer.actions)
        drawer.key("down")
        assertEquals("⏎ ask", drawer.hint())
    }

    @Test fun matchingAppRemainsDefaultAndTabReachesSearch() {
        val drawer = Drawer("Camera", listOf(Obj(mapOf("name" to Str("Camera"), "key" to Str("a:camera")))))
        assertEquals("▸ open", drawer.hint())
        drawer.submit()
        assertEquals(listOf("open a:camera"), drawer.actions)
        drawer.key("shift-tab")
        assertEquals("⏎ search", drawer.hint())
        drawer.key("shift-tab")
        assertEquals("⏎ ask", drawer.hint())
        drawer.key("tab")
        assertEquals("⏎ search", drawer.hint())
        drawer.key("tab")
        assertEquals("▸ open", drawer.hint())
    }

    @Test fun tappingSearchDoesNotDependOnSelection() {
        val drawer = Drawer("something to search")
        assertEquals("⏎ ask", drawer.hint())
        assertEquals(true, drawer.searchVisible())
        drawer.tapSearch()
        assertEquals(listOf("launcher search"), drawer.actions)
    }

    @Test fun blankQueryDoesNotSearch() {
        val drawer = Drawer("   ")
        drawer.key("up")
        assertEquals(false, drawer.searchVisible())
        assertEquals("", drawer.hint())
        drawer.submit()
        assertEquals(emptyList<String>(), drawer.actions)
    }
}
