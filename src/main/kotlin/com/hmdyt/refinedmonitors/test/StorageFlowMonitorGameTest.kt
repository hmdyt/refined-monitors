package com.hmdyt.refinedmonitors.test

import com.hmdyt.refinedmonitors.RefinedMonitorsMod
import com.hmdyt.refinedmonitors.storageflowmonitor.StorageFlowMonitorBlockEntity
import com.refinedmods.refinedstorage.api.resource.ResourceAmount
import com.refinedmods.refinedstorage.common.grid.AbstractGridBlockEntity
import com.refinedmods.refinedstorage.common.iface.InterfaceBlockEntity
import com.refinedmods.refinedstorage.common.importer.AbstractImporterBlockEntity
import com.refinedmods.refinedstorage.common.storage.ItemStorageVariant
import com.refinedmods.refinedstorage.common.storage.diskdrive.AbstractDiskDriveBlockEntity
import com.refinedmods.refinedstorage.common.support.RedstoneMode
import com.refinedmods.refinedstorage.common.support.resource.ItemResource
import net.minecraft.core.BlockPos
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.neoforged.neoforge.gametest.GameTestHolder
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate
import com.refinedmods.refinedstorage.common.content.Blocks as RSBlocks
import com.refinedmods.refinedstorage.common.content.Items as RSItems

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

    @GameTest(template = "empty", timeoutTicks = 400)
    @JvmStatic
    fun testBasicNetworkSetup(helper: GameTestHelper) {
        with(helper) {
            val controllerPos = BlockPos(1, 1, 1)
            val drivePos = BlockPos(2, 1, 1)
            val gridPos = BlockPos(3, 1, 1)
            val monitorPos = BlockPos(1, 1, 2)
            val cablePos1 = BlockPos(1, 1, 3)

            setBlock(controllerPos, RSBlocks.INSTANCE.creativeController.get(DyeColor.LIGHT_BLUE))
            setBlock(drivePos, RSBlocks.INSTANCE.diskDrive)
            setBlock(gridPos, RSBlocks.INSTANCE.grid.get(DyeColor.LIGHT_BLUE))
            setBlock(monitorPos, RefinedMonitorsMod.STORAGE_FLOW_MONITOR.get())
            setBlock(cablePos1, RSBlocks.INSTANCE.cable.get(DyeColor.GRAY))

            assertBlockPresent(RSBlocks.INSTANCE.creativeController.get(DyeColor.LIGHT_BLUE), controllerPos)
            assertBlockPresent(RSBlocks.INSTANCE.diskDrive, drivePos)
            assertBlockPresent(RSBlocks.INSTANCE.grid.get(DyeColor.LIGHT_BLUE), gridPos)
            assertBlockPresent(RefinedMonitorsMod.STORAGE_FLOW_MONITOR.get(), monitorPos)

            runAfterDelay(60) {
                val driveBlockEntity = getBlockEntity<AbstractDiskDriveBlockEntity>(drivePos)
                assertTrue(driveBlockEntity != null, "Drive BlockEntity should not be null")

                val storageDisk = ItemStack(RSItems.INSTANCE.getItemStorageDisk(ItemStorageVariant.ONE_K), 1)
                driveBlockEntity.diskInventory.setItem(0, storageDisk)
                driveBlockEntity.setChanged()

                runAfterDelay(120) {
                    val monitorBlockEntity = getBlockEntity<StorageFlowMonitorBlockEntity>(monitorPos)
                    val gridBlockEntity = getBlockEntity<AbstractGridBlockEntity>(gridPos)

                    assertTrue(monitorBlockEntity != null, "Monitor BlockEntity should not be null")
                    assertTrue(gridBlockEntity != null, "Grid BlockEntity should not be null")

                    runAfterDelay(20) {
                        assertTrue(monitorBlockEntity.isCurrentlyActive(), "Monitor should be active in network")
                        assertTrue(gridBlockEntity.isGridActive(), "Grid should be active in network")

                        val diamondResource = ItemResource(Items.DIAMOND)
                        val resourceAmount = ResourceAmount(diamondResource, 1)
                        monitorBlockEntity.getFilter().filterContainer.set(0, resourceAmount)

                        assertValueEqual(monitorBlockEntity.getCurrentAmount(), 0L, "initial amount should be 0")
                        assertValueEqual(monitorBlockEntity.getCurrentFlowRate(), 0.0, "initial flow rate should be 0")

                        val displayText = monitorBlockEntity.getFlowRateDisplayText()
                        assertTrue(displayText.isNotEmpty(), "Flow rate display text should not be empty")
                        assertTrue(displayText.contains("/s"), "Display text should contain per-second unit")

                        succeed()
                    }
                }
            }
        }
    }

    @GameTest(template = "empty", timeoutTicks = 600)
    @JvmStatic
    fun testInterfaceNetworkItemInsertionAndFlowRate(helper: GameTestHelper) {
        with(helper) {
            val controllerPos = BlockPos(1, 1, 1)
            val drivePos = BlockPos(2, 1, 1)
            val interfacePos = BlockPos(3, 1, 1)
            val monitorPos = BlockPos(1, 1, 2)
            val cablePos1 = BlockPos(1, 1, 3)

            setBlock(controllerPos, RSBlocks.INSTANCE.creativeController.get(DyeColor.LIGHT_BLUE))
            setBlock(drivePos, RSBlocks.INSTANCE.diskDrive)
            setBlock(interfacePos, RSBlocks.INSTANCE.getInterface())
            setBlock(monitorPos, RefinedMonitorsMod.STORAGE_FLOW_MONITOR.get())
            setBlock(cablePos1, RSBlocks.INSTANCE.cable.get(DyeColor.GRAY))

            runAfterDelay(60) {
                val driveBlockEntity = getBlockEntity<AbstractDiskDriveBlockEntity>(drivePos)
                assertTrue(driveBlockEntity != null, "Drive BlockEntity should not be null")

                val storageDisk = ItemStack(RSItems.INSTANCE.getItemStorageDisk(ItemStorageVariant.ONE_K), 1)
                driveBlockEntity.diskInventory.setItem(0, storageDisk)
                driveBlockEntity.setChanged()

                runAfterDelay(120) {
                    val monitorBlockEntity = getBlockEntity<StorageFlowMonitorBlockEntity>(monitorPos)
                    val interfaceBlockEntity = getBlockEntity<InterfaceBlockEntity>(interfacePos)

                    assertTrue(monitorBlockEntity != null, "Monitor BlockEntity should not be null")
                    assertTrue(interfaceBlockEntity != null, "Interface BlockEntity should not be null")

                    runAfterDelay(20) {
                        assertTrue(monitorBlockEntity.isCurrentlyActive(), "Monitor should be active in network")

                        val diamondResource = ItemResource(Items.DIAMOND)
                        val resourceAmount = ResourceAmount(diamondResource, 1)
                        monitorBlockEntity.getFilter().filterContainer.set(0, resourceAmount)

                        assertValueEqual(monitorBlockEntity.getCurrentAmount(), 0L, "initial amount should be 0")
                        assertValueEqual(monitorBlockEntity.getCurrentFlowRate(), 0.0, "initial flow rate should be 0")

                        val exportedContainer = interfaceBlockEntity.getExportedResourcesAsContainer()
                        exportedContainer.setItem(0, ItemStack(Items.DIAMOND, 64))
                        interfaceBlockEntity.setChanged()

                        runAfterDelay(100) {
                            assertTrue(monitorBlockEntity.getCurrentAmount() > 0, "Monitor should detect diamonds from interface")
                            val flowRate = monitorBlockEntity.getCurrentFlowRate()
                            assertTrue(flowRate > 0, "Flow rate should be positive after interface insertion")

                            val displayText = monitorBlockEntity.getFlowRateDisplayText()
                            assertTrue(displayText.contains("+"), "Positive flow should show + sign")
                            assertTrue(displayText.contains("/s"), "Display should show per-second unit")

                            exportedContainer.setItem(1, ItemStack(Items.DIAMOND, 32))
                            interfaceBlockEntity.setChanged()

                            runAfterDelay(100) {
                                val finalAmount = monitorBlockEntity.getCurrentAmount()
                                assertTrue(finalAmount > 64, "Should have more than 64 diamonds after second insertion")
                                val finalFlowRate = monitorBlockEntity.getCurrentFlowRate()
                                assertTrue(finalFlowRate > 0, "Flow rate should remain positive after second insertion")
                                succeed()
                            }
                        }
                    }
                }
            }
        }
    }

    @GameTest(template = "empty", timeoutTicks = 700)
    @JvmStatic
    fun testImporterNetworkItemInsertionAndFlowRate(helper: GameTestHelper) {
        with(helper) {
            val controllerPos = BlockPos(1, 1, 1)
            val drivePos = BlockPos(2, 1, 1)
            val importerPos = BlockPos(3, 1, 1)
            val chestPos = BlockPos(4, 1, 1)
            val monitorPos = BlockPos(1, 1, 2)
            val cablePos1 = BlockPos(1, 1, 3)

            setBlock(controllerPos, RSBlocks.INSTANCE.creativeController.get(DyeColor.LIGHT_BLUE))
            setBlock(drivePos, RSBlocks.INSTANCE.diskDrive)
            setBlock(importerPos, RSBlocks.INSTANCE.importer.get(DyeColor.LIGHT_BLUE))
            setBlock(chestPos, Blocks.CHEST)
            setBlock(monitorPos, RefinedMonitorsMod.STORAGE_FLOW_MONITOR.get())
            setBlock(cablePos1, RSBlocks.INSTANCE.cable.get(DyeColor.GRAY))

            runAfterDelay(60) {
                val driveBlockEntity = getBlockEntity<AbstractDiskDriveBlockEntity>(drivePos)
                assertTrue(driveBlockEntity != null, "Drive BlockEntity should not be null")

                val storageDisk = ItemStack(RSItems.INSTANCE.getItemStorageDisk(ItemStorageVariant.ONE_K), 1)
                driveBlockEntity.diskInventory.setItem(0, storageDisk)
                driveBlockEntity.setChanged()

                runAfterDelay(120) {
                    val monitorBlockEntity = getBlockEntity<StorageFlowMonitorBlockEntity>(monitorPos)
                    val importerBlockEntity = getBlockEntity<AbstractImporterBlockEntity>(importerPos)
                    val chestBlockEntity = getBlockEntity<ChestBlockEntity>(chestPos)

                    assertTrue(monitorBlockEntity != null, "Monitor BlockEntity should not be null")
                    assertTrue(importerBlockEntity != null, "Importer BlockEntity should not be null")
                    assertTrue(chestBlockEntity != null, "Chest BlockEntity should not be null")

                    runAfterDelay(20) {
                        assertTrue(monitorBlockEntity.isCurrentlyActive(), "Monitor should be active in network")

                        val diamondResource = ItemResource(Items.DIAMOND)
                        val resourceAmount = ResourceAmount(diamondResource, 1)
                        monitorBlockEntity.getFilter().filterContainer.set(0, resourceAmount)

                        assertValueEqual(monitorBlockEntity.getCurrentAmount(), 0L, "initial amount should be 0")
                        assertValueEqual(monitorBlockEntity.getCurrentFlowRate(), 0.0, "initial flow rate should be 0")

                        chestBlockEntity.setItem(0, ItemStack(Items.DIAMOND, 64))
                        chestBlockEntity.setChanged()

                        runAfterDelay(120) {
                            assertTrue(monitorBlockEntity.getCurrentAmount() > 0, "Monitor should detect diamonds imported from chest")
                            val flowRate = monitorBlockEntity.getCurrentFlowRate()
                            assertTrue(flowRate > 0, "Flow rate should be positive after importer processing")

                            val displayText = monitorBlockEntity.getFlowRateDisplayText()
                            assertTrue(displayText.contains("+"), "Positive flow should show + sign")
                            assertTrue(displayText.contains("/s"), "Display should show per-second unit")

                            chestBlockEntity.setItem(1, ItemStack(Items.DIAMOND, 32))
                            chestBlockEntity.setChanged()

                            runAfterDelay(120) {
                                val finalAmount = monitorBlockEntity.getCurrentAmount()
                                assertTrue(finalAmount > 64, "Should have more than 64 diamonds after second import")
                                val finalFlowRate = monitorBlockEntity.getCurrentFlowRate()
                                assertTrue(finalFlowRate > 0, "Flow rate should remain positive after second import")
                                succeed()
                            }
                        }
                    }
                }
            }
        }
    }
}
