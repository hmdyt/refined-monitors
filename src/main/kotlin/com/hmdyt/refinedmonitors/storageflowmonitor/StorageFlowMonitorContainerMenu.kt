package com.hmdyt.refinedmonitors.storageflowmonitor

import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack

class StorageFlowMonitorContainerMenu(
    id: Int,
    playerInventory: Inventory,
    private val storageFlowMonitor: StorageFlowMonitorBlockEntity? = null,
) : AbstractContainerMenu(MenuType.GENERIC_9x1, id) {
    override fun quickMoveStack(
        player: Player,
        index: Int,
    ): ItemStack {
        return ItemStack.EMPTY
    }

    override fun stillValid(player: Player): Boolean {
        return storageFlowMonitor?.level?.let { level ->
            player.distanceToSqr(
                storageFlowMonitor.blockPos.x.toDouble(),
                storageFlowMonitor.blockPos.y.toDouble(),
                storageFlowMonitor.blockPos.z.toDouble(),
            ) <= 64.0 && level.getBlockEntity(storageFlowMonitor.blockPos) === storageFlowMonitor
        } ?: true
    }
}
