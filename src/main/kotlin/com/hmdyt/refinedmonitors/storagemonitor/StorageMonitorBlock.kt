package com.hmdyt.refinedmonitors.storagemonitor

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.EntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.phys.BlockHitResult

class StorageMonitorBlock :
    Block(
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(2.0f)
            .requiresCorrectToolForDrops(),
    ),
    EntityBlock {
    companion object {
        val FACING: DirectionProperty = DirectionProperty.create("facing", Direction.values().toList())

        private val HELP = Component.translatable("item.refinedmonitors.storage_monitor.help")
    }

    init {
        registerDefaultState(
            stateDefinition.any()
                .setValue(FACING, Direction.NORTH),
        )
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    override fun newBlockEntity(
        pos: BlockPos,
        state: BlockState,
    ): BlockEntity {
        return StorageMonitorBlockEntity(pos, state)
    }

    override fun getRenderShape(state: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    override fun <T : BlockEntity> getTicker(
        level: Level,
        state: BlockState,
        blockEntityType: BlockEntityType<T>,
    ): BlockEntityTicker<T>? {
        if (level.isClientSide) {
            return null
        }

        return BlockEntityTicker { tickLevel, tickPos, tickState, tickBlockEntity ->
            if (tickBlockEntity is StorageMonitorBlockEntity) {
                tickBlockEntity.tick()
            }
        }
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
            if (!level.isClientSide) {
                val blockEntity = level.getBlockEntity(pos)
                if (blockEntity is StorageMonitorBlockEntity) {
                    player.openMenu(blockEntity)
                }
            }
            return ItemInteractionResult.SUCCESS
        }

        if (!level.isClientSide) {
            val blockEntity = level.getBlockEntity(pos)
            if (blockEntity is StorageMonitorBlockEntity) {
                blockEntity.insert(player, hand)
            }
        }

        return ItemInteractionResult.SUCCESS
    }

    override fun attack(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
    ) {
        super.attack(state, level, pos, player)

        if (level.isClientSide) {
            return
        }

        val blockEntity = level.getBlockEntity(pos)
        if (blockEntity !is StorageMonitorBlockEntity) {
            return
        }

        val direction = state.getValue(FACING)
        val hitDirection = getHitDirection(level, player)

        if (hitDirection != direction) {
            return
        }

        blockEntity.extract(player as ServerPlayer)
    }

    private fun getHitDirection(
        level: Level,
        player: Player,
    ): Direction {
        val base = player.getEyePosition(1.0f)
        val look = player.lookAngle
        val target = base.add(look.x * 20, look.y * 20, look.z * 20)

        return level.clip(
            ClipContext(
                base,
                target,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player,
            ),
        ).direction
    }
}
