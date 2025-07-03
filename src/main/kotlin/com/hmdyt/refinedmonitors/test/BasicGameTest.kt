package com.hmdyt.refinedmonitors.test

import com.hmdyt.refinedmonitors.RefinedMonitorsMod
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.neoforged.neoforge.gametest.GameTestHolder
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate

@GameTestHolder(RefinedMonitorsMod.MODID)
@PrefixGameTestTemplate(false)
object BasicGameTest {
    @GameTest(template = "empty")
    @JvmStatic
    fun testStorageFlowMonitorPlacement(helper: GameTestHelper) {
        helper.succeed()
    }
}
