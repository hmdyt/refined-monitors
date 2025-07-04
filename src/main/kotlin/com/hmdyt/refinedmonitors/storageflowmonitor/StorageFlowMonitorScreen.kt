package com.hmdyt.refinedmonitors.storageflowmonitor

import com.refinedmods.refinedstorage.common.support.AbstractBaseScreen
import com.refinedmods.refinedstorage.common.support.containermenu.PropertyTypes
import com.refinedmods.refinedstorage.common.support.widget.FuzzyModeSideButtonWidget
import com.refinedmods.refinedstorage.common.support.widget.RedstoneModeSideButtonWidget
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory

class StorageFlowMonitorScreen(
    menu: StorageFlowMonitorContainerMenu,
    playerInventory: Inventory,
    title: Component,
) : AbstractBaseScreen<StorageFlowMonitorContainerMenu>(menu, playerInventory, title) {
    companion object {
        private val TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                "refinedmonitors",
                "textures/gui/storage_flow_monitor.png",
            )
    }

    init {
        inventoryLabelY = 43
        imageWidth = 211
        imageHeight = 137
    }

    override fun init() {
        super.init()
        addSideButton(RedstoneModeSideButtonWidget(menu.getProperty(PropertyTypes.REDSTONE_MODE)))
        addSideButton(
            FuzzyModeSideButtonWidget(
                menu.getProperty(PropertyTypes.FUZZY_MODE),
            ) { FuzzyModeSideButtonWidget.Type.GENERIC },
        )
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
            val amount = blockEntity.getCurrentAmount()
            val flowRate = blockEntity.getCurrentFlowRate()
            val flowRateText = blockEntity.getFlowRateDisplayText()

            val amountText = formatAmount(amount)
            val y = 35

            guiGraphics.drawString(font, amountText, 10, y, 4210752, false)

            val flowColor =
                when {
                    flowRate > 0 -> 0x55FF55
                    flowRate < 0 -> 0xFF5555
                    else -> 4210752
                }
            guiGraphics.drawString(font, flowRateText, 120, y, flowColor, false)
        }
    }

    private fun formatAmount(amount: Long): String {
        return when {
            amount >= 1000000000 -> "${amount / 1000000000}B"
            amount >= 1000000 -> "${amount / 1000000}M"
            amount >= 1000 -> "${amount / 1000}K"
            else -> amount.toString()
        }
    }

    private fun getBlockEntity(): StorageFlowMonitorBlockEntity? {
        val level = minecraft?.level ?: return null
        val pos = menu.pos ?: return null
        return level.getBlockEntity(pos) as? StorageFlowMonitorBlockEntity
    }
}
