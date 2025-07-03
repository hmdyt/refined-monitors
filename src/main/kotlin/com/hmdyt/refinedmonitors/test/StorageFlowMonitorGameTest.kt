package com.hmdyt.refinedmonitors.test

import com.hmdyt.refinedmonitors.RefinedMonitorsMod
import com.hmdyt.refinedmonitors.storageflowmonitor.StorageFlowMonitorBlockEntity
import com.refinedmods.refinedstorage.api.resource.ResourceAmount
import com.refinedmods.refinedstorage.common.support.RedstoneMode
import com.refinedmods.refinedstorage.common.support.resource.ItemResource
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.Items
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
    fun testBasicConfiguration(helper: GameTestHelper) {
        with(helper) {
            val pos = BlockPos(1, 1, 1)
            setBlock(pos, RefinedMonitorsMod.STORAGE_FLOW_MONITOR.get())
            val blockEntity = getBlockEntity<StorageFlowMonitorBlockEntity>(pos)
            assertTrue(blockEntity != null, "BlockEntity should not be null")

            assertValueEqual(blockEntity.getCurrentAmount(), 0L, "current amount")
            assertFalse(blockEntity.isFuzzyMode(), "Initial fuzzy mode should be false")
            assertValueEqual(
                blockEntity.redstoneMode,
                RedstoneMode.IGNORE,
                "initial redstone mode",
            )

            val diamondResource = ItemResource(Items.DIAMOND)
            val resourceAmount = ResourceAmount(diamondResource, 1)
            blockEntity.getFilter().filterContainer.set(0, resourceAmount)

            assertTrue(blockEntity.getConfiguredResource() != null, "Resource should be configured")
            succeed()
        }
    }

    @GameTest(template = "empty")
    @JvmStatic
    fun testNetworkActivationWithController(helper: GameTestHelper) {
        with(helper) {
            val monitorPos = BlockPos(1, 1, 1)
            val controllerPos = BlockPos(2, 1, 1)

            setBlock(monitorPos, RefinedMonitorsMod.STORAGE_FLOW_MONITOR.get())
            val blockEntity = getBlockEntity<StorageFlowMonitorBlockEntity>(monitorPos)
            assertTrue(blockEntity != null, "BlockEntity should not be null")

            assertFalse(blockEntity.isCurrentlyActive(), "Should not be active without controller")

            setBlock(
                controllerPos,
                com.refinedmods.refinedstorage.common.content.Blocks.INSTANCE.creativeController.get(DyeColor.LIGHT_BLUE),
            )

            runAfterDelay(10) {
                assertTrue(blockEntity.isCurrentlyActive(), "Should be active with controller")

                val diamondResource = ItemResource(Items.DIAMOND)
                val resourceAmount = ResourceAmount(diamondResource, 64)
                blockEntity.getFilter().filterContainer.set(0, resourceAmount)

                assertTrue(blockEntity.getConfiguredResource() != null, "Resource should be configured")
                assertTrue(blockEntity.getConfiguredResource() is ItemResource, "Should be ItemResource")

                succeed()
            }
        }
    }
}
