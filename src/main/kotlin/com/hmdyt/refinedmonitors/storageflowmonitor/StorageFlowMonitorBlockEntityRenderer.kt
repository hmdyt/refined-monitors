package com.hmdyt.refinedmonitors.storageflowmonitor

import com.mojang.blaze3d.vertex.PoseStack
import com.refinedmods.refinedstorage.common.api.RefinedStorageClientApi
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey
import com.refinedmods.refinedstorage.common.support.resource.ItemResource
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.util.Mth
import net.minecraft.world.level.Level
import org.joml.Quaternionf
import org.slf4j.LoggerFactory

class StorageFlowMonitorBlockEntityRenderer : BlockEntityRenderer<StorageFlowMonitorBlockEntity> {
    companion object {
        private val ROTATE_TO_FRONT = Quaternionf().rotationY(Mth.DEG_TO_RAD * 180)
        private const val FONT_SPACING = -0.23f
        private val LOGGER = LoggerFactory.getLogger(StorageFlowMonitorBlockEntityRenderer::class.java)
    }

    override fun render(
        blockEntity: StorageFlowMonitorBlockEntity,
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
        blockEntity: StorageFlowMonitorBlockEntity,
        level: Level,
    ): com.refinedmods.refinedstorage.common.support.direction.BiDirection? {
        val state = level.getBlockState(blockEntity.blockPos)
        return if (state.block is StorageFlowMonitorBlock) {
            state.getValue(com.refinedmods.refinedstorage.common.support.direction.BiDirectionType.INSTANCE.property)
        } else {
            null
        }
    }

    private fun doRender(
        level: Level,
        poseStack: PoseStack,
        vertexConsumers: MultiBufferSource,
        direction: com.refinedmods.refinedstorage.common.support.direction.BiDirection,
        resource: com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey,
        amount: Long,
    ) {
        val resourceClass =
            when (resource) {
                is ItemResource -> ItemResource::class.java
                else -> resource.javaClass
            }

        val resourceRendering = RefinedStorageClientApi.INSTANCE.getResourceRendering(resourceClass)

        if (resourceRendering != null) {
            doRender(
                poseStack,
                vertexConsumers,
                direction.quaternion,
                resourceRendering.formatAmount(amount),
                level,
                resourceRendering,
                resource,
            )
        } else {
            LOGGER.error("ResourceRendering is null for resource class: {}, resource: {}", resourceClass, resource)
        }
    }

    private fun doRender(
        poseStack: PoseStack,
        renderTypeBuffer: MultiBufferSource,
        rotation: Quaternionf,
        amount: String,
        level: Level,
        resourceRendering: com.refinedmods.refinedstorage.common.api.support.resource.ResourceRendering,
        resource: com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey,
    ) {
        poseStack.pushPose()

        poseStack.translate(0.5, 0.5, 0.5)
        poseStack.mulPose(rotation)
        poseStack.mulPose(ROTATE_TO_FRONT)
        poseStack.translate(0.0, 0.05, 0.5)

        poseStack.pushPose()
        renderAmount(poseStack, renderTypeBuffer, amount)
        poseStack.popPose()

        poseStack.pushPose()
        poseStack.translate(0.0, 0.0, 0.01)
        resourceRendering.render(resource, poseStack, renderTypeBuffer, LightTexture.FULL_BRIGHT, level)
        poseStack.popPose()

        poseStack.popPose()
    }

    private fun renderAmount(
        poseStack: PoseStack,
        renderTypeBuffer: MultiBufferSource,
        amount: String,
    ) {
        val font = Minecraft.getInstance().font
        val width = font.width(amount)
        poseStack.translate(0.0, FONT_SPACING.toDouble(), 0.02)
        poseStack.scale(1.0f / 62.0f, -1.0f / 62.0f, 1.0f / 62.0f)
        poseStack.scale(0.5f, 0.5f, 0f)
        poseStack.translate(-0.5 * width, 0.0, 0.5)
        font.drawInBatch(
            amount,
            0f,
            0f,
            0xFFFFFF,
            false,
            poseStack.last().pose(),
            renderTypeBuffer,
            Font.DisplayMode.NORMAL,
            0,
            LightTexture.FULL_BRIGHT,
        )
    }
}
