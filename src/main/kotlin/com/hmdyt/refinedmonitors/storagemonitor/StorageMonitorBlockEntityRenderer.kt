package com.hmdyt.refinedmonitors.storagemonitor

import com.mojang.blaze3d.vertex.PoseStack
import com.refinedmods.refinedstorage.api.resource.ResourceKey
import com.refinedmods.refinedstorage.common.api.RefinedStorageClientApi
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.level.Level
import org.joml.Quaternionf

class StorageMonitorBlockEntityRenderer : BlockEntityRenderer<StorageMonitorBlockEntity> {
    companion object {
        private val ROTATE_TO_FRONT = Quaternionf().rotationY(Mth.DEG_TO_RAD * 180)
        private const val FONT_SPACING = -0.23f
    }

    override fun render(
        blockEntity: StorageMonitorBlockEntity,
        tickDelta: Float,
        poseStack: PoseStack,
        vertexConsumers: MultiBufferSource,
        light: Int,
        overlay: Int,
    ) {
        val level = blockEntity.level ?: return
        val direction = getDirection(blockEntity, level) ?: return

        if (!blockEntity.isCurrentlyActive()) {
            return
        }

        val resource = blockEntity.getConfiguredResource() ?: return

        doRender(
            level,
            poseStack,
            vertexConsumers,
            direction,
            resource,
            blockEntity.getCurrentAmount(),
        )
    }

    private fun getDirection(
        blockEntity: StorageMonitorBlockEntity,
        level: Level,
    ): Direction? {
        val state = level.getBlockState(blockEntity.blockPos)
        return if (state.block is StorageMonitorBlock) {
            state.getValue(StorageMonitorBlock.FACING)
        } else {
            null
        }
    }

    private fun doRender(
        level: Level,
        poseStack: PoseStack,
        vertexConsumers: MultiBufferSource,
        direction: Direction,
        resource: ResourceKey,
        amount: Long,
    ) {
        poseStack.pushPose()

        poseStack.translate(0.5, 0.5, 0.5)

        when (direction) {
            Direction.NORTH -> {}
            Direction.SOUTH -> poseStack.mulPose(Quaternionf().rotationY(Mth.DEG_TO_RAD * 180))
            Direction.WEST -> poseStack.mulPose(Quaternionf().rotationY(Mth.DEG_TO_RAD * 90))
            Direction.EAST -> poseStack.mulPose(Quaternionf().rotationY(Mth.DEG_TO_RAD * -90))
            Direction.UP -> poseStack.mulPose(Quaternionf().rotationX(Mth.DEG_TO_RAD * -90))
            Direction.DOWN -> poseStack.mulPose(Quaternionf().rotationX(Mth.DEG_TO_RAD * 90))
        }

        poseStack.translate(0.0, 0.0, -0.501)

        renderResource(level, poseStack, vertexConsumers, resource, amount)

        poseStack.popPose()
    }

    private fun renderResource(
        level: Level,
        poseStack: PoseStack,
        vertexConsumers: MultiBufferSource,
        resource: ResourceKey,
        amount: Long,
    ) {
        val resourceRendering = RefinedStorageClientApi.INSTANCE.getResourceRendering(resource::class.java)
        if (resourceRendering != null) {
            poseStack.pushPose()

            poseStack.scale(0.5f, 0.5f, 0.01f)
            poseStack.translate(0.0, 0.5, 0.0)

            resourceRendering.render(resource, poseStack, vertexConsumers, LightTexture.FULL_BRIGHT, level)

            poseStack.popPose()

            renderAmount(poseStack, vertexConsumers, amount)
        }
    }

    private fun renderAmount(
        poseStack: PoseStack,
        vertexConsumers: MultiBufferSource,
        amount: Long,
    ) {
        val minecraft = Minecraft.getInstance()
        val font = minecraft.font
        val text = formatAmount(amount)

        poseStack.pushPose()
        poseStack.translate(0.0, FONT_SPACING.toDouble(), 0.001)
        poseStack.scale(0.02f, -0.02f, 0.02f)
        poseStack.mulPose(ROTATE_TO_FRONT)

        val textWidth = font.width(text)
        font.drawInBatch(
            text,
            -textWidth / 2f,
            0f,
            0xFFFFFF,
            false,
            poseStack.last().pose(),
            vertexConsumers,
            Font.DisplayMode.NORMAL,
            0,
            LightTexture.FULL_BRIGHT,
        )

        poseStack.popPose()
    }

    private fun formatAmount(amount: Long): String {
        return when {
            amount >= 1000000000 -> "${amount / 1000000000}B"
            amount >= 1000000 -> "${amount / 1000000}M"
            amount >= 1000 -> "${amount / 1000}K"
            else -> amount.toString()
        }
    }
}
