package com.hmdyt.refinedmonitors.storagemonitor

import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack

class StorageMonitorContainerMenu(
    id: Int,
    playerInventory: Inventory,
    private val storageMonitor: StorageMonitorBlockEntity? = null,
) : AbstractContainerMenu(MenuType.GENERIC_9x1, id) {
    override fun quickMoveStack(
        player: Player,
        index: Int,
    ): ItemStack {
        return ItemStack.EMPTY
    }

    override fun stillValid(player: Player): Boolean {
        return storageMonitor?.level?.let { level ->
            player.distanceToSqr(
                storageMonitor.blockPos.x.toDouble(),
                storageMonitor.blockPos.y.toDouble(),
                storageMonitor.blockPos.z.toDouble(),
            ) <= 64.0 && level.getBlockEntity(storageMonitor.blockPos) === storageMonitor
        } ?: true
    }
}
