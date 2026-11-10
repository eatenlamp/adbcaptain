/*
 * SPDX-FileCopyrightText: 2026 eatenlamp <https://github.com/eatenlamp>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * ADB Captain. This ownership notice is part of the source and may not be
 * removed, hidden or replaced without written permission from eatenlamp.
 */

package adb.captain.domain.usecase

import adb.captain.data.local.dao.MacroDao
import adb.captain.data.local.entity.MacroEntity
import adb.captain.data.local.entity.MacroStepEntity
import adb.captain.data.local.entity.MacroWithSteps
import adb.captain.domain.model.Device
import adb.captain.domain.model.Macro
import adb.captain.domain.model.MacroStep
import adb.captain.domain.model.StepCondition
import adb.captain.domain.model.StepFailure
import adb.captain.domain.repository.AdbRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class MacroUseCaseTest {

    @Test
    fun runsEveryStepInOrder() = runTest {
        val repository = FakeRepository(mapOf("one" to "first", "two" to "second"))
        val useCase = MacroUseCase(FakeMacroDao(), repository.repository)

        val result = useCase.run(
            Macro(name = "m", steps = listOf(MacroStep("one"), MacroStep("two")))
        )

        assertTrue(result.success)
        assertEquals(listOf("one", "two"), repository.executed)
        assertTrue(result.output.contains("first"))
        assertTrue(result.output.contains("second"))
    }

    @Test
    fun stopsOnErrorByDefault() = runTest {
        val repository = FakeRepository(
            mapOf("one" to "ok", "two" to "Error: nope", "three" to "never")
        )
        val useCase = MacroUseCase(FakeMacroDao(), repository.repository)

        val result = useCase.run(
            Macro(
                name = "m",
                steps = listOf(MacroStep("one"), MacroStep("two"), MacroStep("three"))
            )
        )

        assertFalse(result.success)
        assertEquals("two", result.failedCommand)
        assertEquals(listOf("one", "two"), repository.executed)
        assertFalse(result.output.contains("never"))
    }

    @Test
    fun continuesAfterErrorWhenAsked() = runTest {
        val repository = FakeRepository(
            mapOf("one" to "Error: nope", "two" to "ok")
        )
        val useCase = MacroUseCase(FakeMacroDao(), repository.repository)

        val result = useCase.run(
            Macro(
                name = "m",
                steps = listOf(
                    MacroStep("one", onError = StepFailure.CONTINUE),
                    MacroStep("two")
                )
            )
        )

        assertTrue(result.success)
        assertEquals(listOf("one", "two"), repository.executed)
    }

    @Test
    fun substitutesVariablesAndSerial() = runTest {
        val repository = FakeRepository(mapOf("echo com.example SER123" to "ok"))
        val useCase = MacroUseCase(FakeMacroDao(), repository.repository)

        val result = useCase.run(
            Macro(
                name = "m",
                steps = listOf(MacroStep("echo {{package}} {{serial}}"))
            ),
            variables = mapOf("package" to "com.example")
        )

        assertTrue(result.success)
        assertEquals(listOf("echo com.example SER123"), repository.executed)
    }

    @Test
    fun conditionsJudgeThePreviousOutput() = runTest {
        val repository = FakeRepository(mapOf("one" to "hello", "two" to "world"))
        val useCase = MacroUseCase(FakeMacroDao(), repository.repository)

        val result = useCase.run(
            Macro(
                name = "m",
                steps = listOf(
                    MacroStep("one"),
                    MacroStep(
                        "two",
                        condition = StepCondition.OUTPUT_CONTAINS,
                        conditionValue = "ell"
                    ),
                    MacroStep("three", condition = StepCondition.OUTPUT_EMPTY)
                )
            )
        )

        assertEquals(listOf("one", "two"), repository.executed)
        assertEquals(1, result.skipped)
    }

    @Test
    fun waitsBeforeTheStep() = runTest {
        val repository = FakeRepository(emptyMap())
        val useCase = MacroUseCase(FakeMacroDao(), repository.repository)

        useCase.run(
            Macro(
                name = "m",
                steps = listOf(MacroStep("one", waitMs = 2_500L), MacroStep("two"))
            )
        )

        assertEquals(2_500L, currentTime)
    }

    @Test
    fun saveKeepsStepsAndLegacyCommandColumn() = runTest {
        val dao = FakeMacroDao()
        val useCase = MacroUseCase(dao, FakeRepository(emptyMap()).repository)

        val id = useCase.save(
            Macro(
                name = "  Install  ",
                steps = listOf(
                    MacroStep("pm install"),
                    MacroStep("am start", condition = StepCondition.OUTPUT_NOT_EMPTY)
                )
            )
        )

        assertTrue(id > 0)
        val saved = useCase.find(id)
        assertEquals("Install", saved?.name)
        assertEquals(2, saved?.steps?.size)
        assertEquals(StepCondition.OUTPUT_NOT_EMPTY, saved?.steps?.get(1)?.condition)
        assertEquals("pm install\nam start", dao.macros[id]?.commands)
    }

    @Test
    fun deleteRemovesTheMacroAndItsSteps() = runTest {
        val dao = FakeMacroDao()
        val useCase = MacroUseCase(dao, FakeRepository(emptyMap()).repository)
        val id = useCase.save(Macro(name = "m", steps = listOf(MacroStep("id"))))

        useCase.delete(useCase.find(id)!!)

        assertEquals(null, useCase.find(id))
        assertTrue(dao.steps.isEmpty())
    }
}

/** In-memory stand-in for the Room DAO; keeps the run tests free of Android. */
private class FakeMacroDao : MacroDao {
    private var nextId = 1L
    val macros = mutableMapOf<Long, MacroEntity>()
    val steps = mutableMapOf<Long, List<MacroStepEntity>>()
    private val state = MutableStateFlow<List<MacroWithSteps>>(emptyList())

    private fun publish() {
        state.value = macros.values
            .sortedBy { it.name.lowercase() }
            .map { MacroWithSteps(it, steps[it.id].orEmpty().sortedBy { step -> step.position }) }
    }

    override fun observeAll(): Flow<List<MacroWithSteps>> = state

    override suspend fun getAll(): List<MacroWithSteps> = state.value

    override suspend fun getById(id: Long): MacroWithSteps? =
        state.value.firstOrNull { it.macro.id == id }

    override suspend fun insertMacro(macro: MacroEntity): Long {
        val id = if (macro.id > 0L) macro.id else nextId++
        macros[id] = macro.copy(id = id)
        publish()
        return id
    }

    override suspend fun insertSteps(steps: List<MacroStepEntity>) {
        steps.groupBy { it.macroId }.forEach { (macroId, rows) ->
            this.steps[macroId] = this.steps[macroId].orEmpty() + rows
        }
        publish()
    }

    override suspend fun deleteSteps(macroId: Long) {
        steps.remove(macroId)
        publish()
    }

    override suspend fun deleteById(id: Long) {
        macros.remove(id)
        steps.remove(id)
        publish()
    }
}

/**
 * Dynamic proxy so the huge repository interface does not need a handwritten
 * fake. Only the two calls the macro runner makes are answered.
 */
private class FakeRepository(
    private val outputs: Map<String, String>
) : InvocationHandler {
    val executed = mutableListOf<String>()

    fun asRepository(): AdbRepository =
        Proxy.newProxyInstance(
            AdbRepository::class.java.classLoader,
            arrayOf(AdbRepository::class.java),
            this
        ) as AdbRepository

    override fun invoke(proxy: Any?, method: Method, args: Array<out Any?>?): Any? =
        when (method.name) {
            "executeCommand" -> {
                val command = args?.firstOrNull() as? String ?: ""
                executed.add(command)
                outputs[command] ?: ""
            }
            "getDevices" -> listOf(Device(serial = "SER123"))
            "toString" -> "FakeRepository"
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === args?.firstOrNull()
            else -> defaultValue(method.returnType)
        }

    private fun defaultValue(type: Class<*>): Any? = when (type) {
        java.lang.Boolean.TYPE -> false
        java.lang.Byte.TYPE -> 0.toByte()
        java.lang.Character.TYPE -> '\u0000'
        java.lang.Short.TYPE -> 0.toShort()
        java.lang.Integer.TYPE -> 0
        java.lang.Long.TYPE -> 0L
        java.lang.Float.TYPE -> 0f
        java.lang.Double.TYPE -> 0.0
        else -> null
    }
}

private val FakeRepository.repository: AdbRepository get() = asRepository()
