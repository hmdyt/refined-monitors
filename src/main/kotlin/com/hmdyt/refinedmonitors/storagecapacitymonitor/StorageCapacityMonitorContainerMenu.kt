package com.hmdyt.refinedmonitors.storagecapacitymonitor

import com.hmdyt.refinedmonitors.RefinedMonitorsMod
import com.refinedmods.refinedstorage.common.support.RedstoneMode
import com.refinedmods.refinedstorage.common.support.containermenu.AbstractResourceContainerMenu
import com.refinedmods.refinedstorage.common.support.containermenu.ClientProperty
import com.refinedmods.refinedstorage.common.support.containermenu.PropertyTypes
import com.refinedmods.refinedstorage.common.support.containermenu.ServerProperty
import com.refinedmods.refinedstorage.common.support.resource.ResourceContainerData
import com.refinedmods.refinedstorage.common.support.resource.ResourceContainerImpl
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player

class StorageCapacityMonitorContainerMenu : AbstractResourceContainerMenu {
    private var storageCapacityMonitor: StorageCapacityMonitorBlockEntity? = null
    val pos: net.minecraft.core.BlockPos?
        get() = storageCapacityMonitor?.blockPos

    constructor(
        syncId: Int,
        playerInventory: Inventory,
        resourceContainerData: ResourceContainerData,
    ) : super(RefinedMonitorsMod.STORAGE_CAPACITY_MONITOR_MENU_TYPE.get(), syncId) {
        registerProperty(ClientProperty(PropertyTypes.REDSTONE_MODE, RedstoneMode.IGNORE))
        addSlots(playerInventory, ResourceContainerImpl.createForFilter(resourceContainerData))
    }

    constructor(
        syncId: Int,
        player: Player,
        storageCapacityMonitor: StorageCapacityMonitorBlockEntity,
    ) : super(RefinedMonitorsMod.STORAGE_CAPACITY_MONITOR_MENU_TYPE.get(), syncId, player) {
        this.storageCapacityMonitor = storageCapacityMonitor
        registerProperty(
            ServerProperty(
                PropertyTypes.REDSTONE_MODE,
                storageCapacityMonitor::getRedstoneMode,
                storageCapacityMonitor::setRedstoneMode,
            ),
        )
        addSlots(player.inventory, ResourceContainerImpl.createForFilter(0))
    }

    private fun addSlots(
        playerInventory: Inventory,
        resourceContainer: com.refinedmods.refinedstorage.common.api.support.resource.ResourceContainer,
    ) {
        addPlayerInventory(playerInventory, 8, 55)
    }
}
