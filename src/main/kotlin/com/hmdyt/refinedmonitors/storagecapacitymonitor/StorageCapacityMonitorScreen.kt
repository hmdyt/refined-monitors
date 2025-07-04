package com.hmdyt.refinedmonitors.storagecapacitymonitor

import com.refinedmods.refinedstorage.common.support.AbstractBaseScreen
import com.refinedmods.refinedstorage.common.support.containermenu.PropertyTypes
import com.refinedmods.refinedstorage.common.support.widget.RedstoneModeSideButtonWidget
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

class StorageCapacityMonitorScreen(
    menu: StorageCapacityMonitorContainerMenu,
    playerInventory: Inventory,
    title: Component,
) : AbstractBaseScreen<StorageCapacityMonitorContainerMenu>(menu, playerInventory, title) {
    companion object {
        private val TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                "refinedmonitors",
                "textures/gui/storage_capacity_monitor.png",
            )
    }

    init {
        inventoryLabelY = 43
        imageWidth = 176
        imageHeight = 137
    }

    override fun init() {
        super.init()
        addSideButton(RedstoneModeSideButtonWidget(menu.getProperty(PropertyTypes.REDSTONE_MODE)))
    }

    override fun getTexture(): ResourceLocation {
        return TEXTURE
    }

    override fun renderBg(
        guiGraphics: GuiGraphics,
        partialTick: Float,
        mouseX: Int,
        mouseY: Int,
    ) {
        super.renderBg(guiGraphics, partialTick, mouseX, mouseY)
    }

    override fun renderLabels(
        guiGraphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
    ) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 4210752, false)
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 4210752, false)

        val blockEntity = getBlockEntity()
        if (blockEntity != null) {
            val usedCapacity = blockEntity.getCurrentUsedCapacity()
            val totalCapacity = blockEntity.getCurrentTotalCapacity()
            val percentage = blockEntity.getUsagePercentage()

            val usedText = formatCapacity(usedCapacity)
            val totalText = formatCapacity(totalCapacity)
            val percentageText = String.format("%.1f%%", percentage)

            val capacityText = "$usedText / $totalText"
            val y = 25

            guiGraphics.drawString(font, "使用容量:", 10, y, 4210752, false)
            guiGraphics.drawString(font, capacityText, 10, y + 12, 4210752, false)

            val percentageColor =
                when {
                    percentage >= 90.0 -> 0xFF5555
                    percentage >= 75.0 -> 0xFFAA00
                    else -> 0x55FF55
                }
            guiGraphics.drawString(font, percentageText, 10, y + 24, percentageColor, false)

            drawCapacityBar(guiGraphics, 90, y + 5, 70, 8, percentage)
        }
    }

    private fun drawCapacityBar(
        guiGraphics: GuiGraphics,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        percentage: Double,
    ) {
        val barColor =
            when {
                percentage >= 90.0 -> 0xFFFF5555.toInt()
                percentage >= 75.0 -> 0xFFFFAA00.toInt()
                else -> 0xFF55FF55.toInt()
            }

        guiGraphics.fill(x, y, x + width, y + height, 0xFF333333.toInt())

        val fillWidth = (width * (percentage / 100.0)).toInt()
        if (fillWidth > 0) {
            guiGraphics.fill(x, y, x + fillWidth, y + height, barColor)
        }

        guiGraphics.fill(x, y, x + width, y + 1, 0xFF000000.toInt())
        guiGraphics.fill(x, y, x + 1, y + height, 0xFF000000.toInt())
        guiGraphics.fill(x + width - 1, y, x + width, y + height, 0xFF000000.toInt())
        guiGraphics.fill(x, y + height - 1, x + width, y + height, 0xFF000000.toInt())
    }

    private fun formatCapacity(capacity: Long): String {
        return when {
            capacity >= 1000000000000L -> String.format("%.1fT", capacity / 1000000000000.0)
            capacity >= 1000000000L -> String.format("%.1fB", capacity / 1000000000.0)
            capacity >= 1000000L -> String.format("%.1fM", capacity / 1000000.0)
            capacity >= 1000L -> String.format("%.1fK", capacity / 1000.0)
            else -> capacity.toString()
        }
    }

    private fun getBlockEntity(): StorageCapacityMonitorBlockEntity? {
        val level = minecraft?.level ?: return null
        val pos = menu.pos ?: return null
        return level.getBlockEntity(pos) as? StorageCapacityMonitorBlockEntity
    }
}
