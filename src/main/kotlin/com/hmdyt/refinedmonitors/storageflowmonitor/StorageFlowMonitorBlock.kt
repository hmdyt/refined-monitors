package com.hmdyt.refinedmonitors.storageflowmonitor

import com.hmdyt.refinedmonitors.RefinedMonitorsMod
import com.refinedmods.refinedstorage.common.content.BlockConstants
import com.refinedmods.refinedstorage.common.support.AbstractBlockEntityTicker
import com.refinedmods.refinedstorage.common.support.AbstractDirectionalBlock
import com.refinedmods.refinedstorage.common.support.direction.BiDirection
import com.refinedmods.refinedstorage.common.support.direction.BiDirectionType
import com.refinedmods.refinedstorage.common.support.direction.DirectionType
import com.refinedmods.refinedstorage.common.support.network.NetworkNodeBlockEntityTicker
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

class StorageFlowMonitorBlock : AbstractDirectionalBlock<BiDirection>(BlockConstants.PROPERTIES), EntityBlock {
    companion object {
        private val HELP = Component.translatable("item.refinedmonitors.storage_flow_monitor.help")
        private val TICKER: AbstractBlockEntityTicker<StorageFlowMonitorBlockEntity> =
            NetworkNodeBlockEntityTicker {
                RefinedMonitorsMod.STORAGE_FLOW_MONITOR_BLOCK_ENTITY.get() as BlockEntityType<StorageFlowMonitorBlockEntity>
            }
    }

    override fun getDirectionType(): DirectionType<BiDirection> {
        return BiDirectionType.INSTANCE
    }

    override fun newBlockEntity(
        pos: BlockPos,
        state: BlockState,
    ): BlockEntity {
        return StorageFlowMonitorBlockEntity(pos, state)
    }

    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>,
    ): BlockEntityTicker<T>? {
        return TICKER.get(level, blockEntityType)
    }

    override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hitResult: BlockHitResult,
    ): ItemInteractionResult {
        if (player.isCrouching) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION
        }

        if (!level.isClientSide) {
            val blockEntity = level.getBlockEntity(pos)
            if (blockEntity is StorageFlowMonitorBlockEntity && player is ServerPlayer) {
                com.refinedmods.refinedstorage.common.Platform.INSTANCE.menuOpener.openMenu(player, blockEntity)
            }
        }

        return ItemInteractionResult.SUCCESS
    }
}
