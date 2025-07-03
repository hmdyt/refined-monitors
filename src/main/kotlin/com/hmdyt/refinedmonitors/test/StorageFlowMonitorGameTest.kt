package com.hmdyt.refinedmonitors.test

import com.hmdyt.refinedmonitors.RefinedMonitorsMod
import com.hmdyt.refinedmonitors.storageflowmonitor.StorageFlowMonitorBlockEntity
import com.refinedmods.refinedstorage.common.support.RedstoneMode
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.level.block.Blocks
import net.neoforged.neoforge.gametest.GameTestHolder
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate

@GameTestHolder(RefinedMonitorsMod.MODID)
@PrefixGameTestTemplate(false)
object StorageFlowMonitorGameTest {
    @GameTest(template = "empty")
    @JvmStatic
    fun testBlockPlacement(helper: GameTestHelper) {
        with(helper) {
            val pos = BlockPos(1, 1, 1)
            assertBlockPresent(Blocks.AIR, pos)
            setBlock(pos, RefinedMonitorsMod.STORAGE_FLOW_MONITOR.get())
            assertBlockPresent(RefinedMonitorsMod.STORAGE_FLOW_MONITOR.get(), pos)
            succeed()
        }
    }

    @GameTest(template = "empty")
    @JvmStatic
    fun testBlockEntityCreation(helper: GameTestHelper) {
        with(helper) {
            val pos = BlockPos(1, 1, 1)
            setBlock(pos, RefinedMonitorsMod.STORAGE_FLOW_MONITOR.get())
            val blockEntity = getBlockEntity<StorageFlowMonitorBlockEntity>(pos)
            assertTrue(blockEntity != null, "BlockEntity should not be null")
            assertTrue(
                blockEntity is StorageFlowMonitorBlockEntity,
                "BlockEntity should be StorageFlowMonitorBlockEntity",
            )
            succeed()
        }
    }

    @GameTest(template = "empty")
    @JvmStatic
    fun testBlockEntityBasicProperties(helper: GameTestHelper) {
        with(helper) {
            val pos = BlockPos(1, 1, 1)
            setBlock(pos, RefinedMonitorsMod.STORAGE_FLOW_MONITOR.get())
            val blockEntity = getBlockEntity<StorageFlowMonitorBlockEntity>(pos)
            assertTrue(blockEntity != null, "BlockEntity should not be null")
            assertValueEqual(blockEntity.getCurrentAmount(), 0L, "current amount")
            assertFalse(blockEntity.isFuzzyMode(), "Initial fuzzy mode should be false")
            succeed()
        }
    }

    @GameTest(template = "empty")
    @JvmStatic
    fun testFilterConfiguration(helper: GameTestHelper) {
        with(helper) {
            val pos = BlockPos(1, 1, 1)
            setBlock(pos, RefinedMonitorsMod.STORAGE_FLOW_MONITOR.get())
            val blockEntity = getBlockEntity<StorageFlowMonitorBlockEntity>(pos)
            assertTrue(blockEntity != null, "BlockEntity should not be null")
            val filter = blockEntity.getFilter()
            assertValueEqual(filter.filterContainer.size(), 1, "filter container size")
            assertTrue(blockEntity.getConfiguredResource() == null, "Configured resource should be null initially")
            succeed()
        }
    }

    @GameTest(template = "empty")
    @JvmStatic
    fun testFuzzyModeToggle(helper: GameTestHelper) {
        with(helper) {
            val pos = BlockPos(1, 1, 1)
            setBlock(pos, RefinedMonitorsMod.STORAGE_FLOW_MONITOR.get())
            val blockEntity = getBlockEntity<StorageFlowMonitorBlockEntity>(pos)
            assertTrue(blockEntity != null, "BlockEntity should not be null")
            assertFalse(blockEntity.isFuzzyMode(), "Initial fuzzy mode should be false")
            blockEntity.setFuzzyMode(true)
            assertTrue(blockEntity.isFuzzyMode(), "Fuzzy mode should be true after setting")
            blockEntity.setFuzzyMode(false)
            assertFalse(blockEntity.isFuzzyMode(), "Fuzzy mode should be false after resetting")
            succeed()
        }
    }

    @GameTest(template = "empty")
    @JvmStatic
    fun testRedstoneMode(helper: GameTestHelper) {
        with(helper) {
            val pos = BlockPos(1, 1, 1)
            setBlock(pos, RefinedMonitorsMod.STORAGE_FLOW_MONITOR.get())
            val blockEntity = getBlockEntity<StorageFlowMonitorBlockEntity>(pos)
            assertTrue(blockEntity != null, "BlockEntity should not be null")
            assertValueEqual(
                blockEntity.redstoneMode,
                RedstoneMode.IGNORE,
                "initial redstone mode",
            )
            blockEntity.redstoneMode = RedstoneMode.HIGH
            assertValueEqual(
                blockEntity.redstoneMode,
                RedstoneMode.HIGH,
                "redstone mode after setting HIGH",
            )
            blockEntity.redstoneMode = RedstoneMode.LOW
            assertValueEqual(
                blockEntity.redstoneMode,
                RedstoneMode.LOW,
                "redstone mode after setting LOW",
            )
            succeed()
        }
    }

    @GameTest(template = "empty")
    @JvmStatic
    fun testNetworkConnection(helper: GameTestHelper) {
        with(helper) {
            val pos = BlockPos(1, 1, 1)
            setBlock(pos, RefinedMonitorsMod.STORAGE_FLOW_MONITOR.get())
            val blockEntity = getBlockEntity<StorageFlowMonitorBlockEntity>(pos)
            assertTrue(blockEntity != null, "BlockEntity should not be null")
            assertFalse(
                blockEntity.isCurrentlyActive(),
                "Monitor should not be active without network connection",
            )
            succeed()
        }
    }

    @GameTest(template = "empty")
    @JvmStatic
    fun testResourceAmountDetection(helper: GameTestHelper) {
        with(helper) {
            val pos = BlockPos(1, 1, 1)
            setBlock(pos, RefinedMonitorsMod.STORAGE_FLOW_MONITOR.get())
            val blockEntity = helper.getBlockEntity<StorageFlowMonitorBlockEntity>(pos)
            assertTrue(blockEntity != null, "BlockEntity should not be null")
            assertValueEqual(
                blockEntity.getCurrentAmount(),
                0L,
                "current amount",
            )
            assertTrue(blockEntity.getConfiguredResource() == null, "Configured resource should be null initially")
            succeed()
        }
    }
}
