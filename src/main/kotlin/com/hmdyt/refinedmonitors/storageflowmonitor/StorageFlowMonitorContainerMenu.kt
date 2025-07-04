package com.hmdyt.refinedmonitors.storageflowmonitor

import com.hmdyt.refinedmonitors.RefinedMonitorsMod
import com.refinedmods.refinedstorage.common.api.support.resource.ResourceContainer
import com.refinedmods.refinedstorage.common.support.RedstoneMode
import com.refinedmods.refinedstorage.common.support.containermenu.AbstractResourceContainerMenu
import com.refinedmods.refinedstorage.common.support.containermenu.ClientProperty
import com.refinedmods.refinedstorage.common.support.containermenu.PropertyTypes
import com.refinedmods.refinedstorage.common.support.containermenu.ResourceSlot
import com.refinedmods.refinedstorage.common.support.containermenu.ResourceSlotType
import com.refinedmods.refinedstorage.common.support.containermenu.ServerProperty
import com.refinedmods.refinedstorage.common.support.resource.ResourceContainerData
import com.refinedmods.refinedstorage.common.support.resource.ResourceContainerImpl
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player

class StorageFlowMonitorContainerMenu : AbstractResourceContainerMenu {
    companion object {
        private val FILTER_HELP = Component.translatable("gui.refinedmonitors.storage_flow_monitor.filter_help")
    }

    private var storageFlowMonitor: StorageFlowMonitorBlockEntity? = null

    constructor(
        syncId: Int,
        playerInventory: Inventory,
        resourceContainerData: ResourceContainerData,
    ) : super(RefinedMonitorsMod.STORAGE_FLOW_MONITOR_MENU_TYPE.get(), syncId) {
        registerProperty(ClientProperty(PropertyTypes.FUZZY_MODE, false))
        registerProperty(ClientProperty(PropertyTypes.REDSTONE_MODE, RedstoneMode.IGNORE))
        addSlots(playerInventory, ResourceContainerImpl.createForFilter(resourceContainerData))
    }

    constructor(
        syncId: Int,
        player: Player,
        storageFlowMonitor: StorageFlowMonitorBlockEntity,
        resourceContainer: ResourceContainer,
    ) : super(RefinedMonitorsMod.STORAGE_FLOW_MONITOR_MENU_TYPE.get(), syncId, player) {
        this.storageFlowMonitor = storageFlowMonitor
        registerProperty(
            ServerProperty(
                PropertyTypes.FUZZY_MODE,
                storageFlowMonitor::isFuzzyMode,
                storageFlowMonitor::setFuzzyMode,
            ),
        )
        registerProperty(
            ServerProperty(
                PropertyTypes.REDSTONE_MODE,
                storageFlowMonitor::getRedstoneMode,
                storageFlowMonitor::setRedstoneMode,
            ),
        )
        addSlots(player.inventory, resourceContainer)
    }

    private fun addSlots(
        playerInventory: Inventory,
        resourceContainer: ResourceContainer,
    ) {
        val filterSlot = ResourceSlot(resourceContainer, 0, FILTER_HELP, 80, 20, ResourceSlotType.FILTER)
        addSlot(filterSlot)
        addPlayerInventory(playerInventory, 8, 55)
        transferManager.addFilterTransfer(playerInventory)
    }

    override fun handleResourceFilterSlotUpdate(
        slotIndex: Int,
        resource: com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey,
    ) {
        super.handleResourceFilterSlotUpdate(slotIndex, resource)
        storageFlowMonitor?.let {
            it.getFilter().filterContainer.set(slotIndex, com.refinedmods.refinedstorage.api.resource.ResourceAmount(resource, 1))
            it.setChanged()
        }
    }
}
