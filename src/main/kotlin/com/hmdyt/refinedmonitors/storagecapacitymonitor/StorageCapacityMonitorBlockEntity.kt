package com.hmdyt.refinedmonitors.storagecapacitymonitor

import com.google.common.util.concurrent.RateLimiter
import com.hmdyt.refinedmonitors.RefinedMonitorsMod
import com.refinedmods.refinedstorage.api.network.Network
import com.refinedmods.refinedstorage.api.network.impl.node.SimpleNetworkNode
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent
import com.refinedmods.refinedstorage.api.storage.root.RootStorage
import com.refinedmods.refinedstorage.common.Platform
import com.refinedmods.refinedstorage.common.support.containermenu.NetworkNodeExtendedMenuProvider
import com.refinedmods.refinedstorage.common.support.network.AbstractBaseNetworkNodeContainerBlockEntity
import com.refinedmods.refinedstorage.common.support.resource.ResourceContainerData
import com.refinedmods.refinedstorage.common.util.PlatformUtil
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.codec.StreamEncoder
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.level.block.state.BlockState
import org.slf4j.LoggerFactory

class StorageCapacityMonitorBlockEntity(
    pos: BlockPos,
    state: BlockState,
) : AbstractBaseNetworkNodeContainerBlockEntity<SimpleNetworkNode>(
        RefinedMonitorsMod.STORAGE_CAPACITY_MONITOR_BLOCK_ENTITY.get(),
        pos,
        state,
        SimpleNetworkNode(Platform.INSTANCE.config.storageMonitor.energyUsage),
    ),
    NetworkNodeExtendedMenuProvider<ResourceContainerData> {
    companion object {
        private const val TAG_CLIENT_USED_CAPACITY = "cuc"
        private const val TAG_CLIENT_TOTAL_CAPACITY = "ctc"
        private const val TAG_CLIENT_ACTIVE = "cac"
        private val LOGGER = LoggerFactory.getLogger(StorageCapacityMonitorBlockEntity::class.java)
    }

    private val displayUpdateRateLimiter = RateLimiter.create(0.25)

    private var currentUsedCapacity: Long = 0
    private var currentTotalCapacity: Long = 0
    private var currentlyActive: Boolean = false

    override fun onLoad() {
        super.onLoad()
        if (level != null && !level!!.isClientSide) {
            sendDisplayUpdate()
        }
    }

    override fun doWork() {
        super.doWork()
        if (level == null) {
            return
        }
        trySendDisplayUpdate(level!!)
    }

    private fun trySendDisplayUpdate(level: net.minecraft.world.level.Level) {
        val capacityInfo = getCapacityInfo()
        val active = mainNetworkNode.isActive

        if ((
                capacityInfo.usedCapacity != currentUsedCapacity ||
                    capacityInfo.totalCapacity != currentTotalCapacity ||
                    active != currentlyActive
            ) && displayUpdateRateLimiter.tryAcquire()
        ) {
            sendDisplayUpdate(level, capacityInfo.usedCapacity, capacityInfo.totalCapacity, active)
        }
    }

    private fun getCapacityInfo(): CapacityInfo {
        val network = mainNetworkNode.network ?: return CapacityInfo(0L, 0L)
        return calculateNetworkCapacity(network)
    }

    private fun calculateNetworkCapacity(network: Network): CapacityInfo {
        val storageComponent = network.getComponent(StorageNetworkComponent::class.java)
        val rootStorage: RootStorage = storageComponent

        var totalUsed = 0L
        var totalCapacity = 0L

        val allResources = rootStorage.all
        for (resource in allResources) {
            totalUsed += resource.amount
        }

        return CapacityInfo(totalUsed, Long.MAX_VALUE)
    }

    private fun sendDisplayUpdate() {
        if (level == null) {
            return
        }
        val capacityInfo = getCapacityInfo()
        sendDisplayUpdate(level!!, capacityInfo.usedCapacity, capacityInfo.totalCapacity, mainNetworkNode.isActive)
    }

    private fun sendDisplayUpdate(
        level: net.minecraft.world.level.Level,
        usedCapacity: Long,
        totalCapacity: Long,
        active: Boolean,
    ) {
        currentUsedCapacity = usedCapacity
        currentTotalCapacity = totalCapacity
        currentlyActive = active

        LOGGER.debug(
            "Sending display update for storage capacity monitor {} with used {} / total {} and active {}",
            worldPosition,
            usedCapacity,
            totalCapacity,
            active,
        )
        PlatformUtil.sendBlockUpdateToClient(level, worldPosition)
    }

    override fun writeConfiguration(
        tag: CompoundTag,
        provider: HolderLookup.Provider,
    ) {
        super.writeConfiguration(tag, provider)
    }

    override fun readConfiguration(
        tag: CompoundTag,
        provider: HolderLookup.Provider,
    ) {
        super.readConfiguration(tag, provider)
    }

    override fun loadAdditional(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ) {
        super.loadAdditional(tag, registries)
        if (tag.contains(TAG_CLIENT_USED_CAPACITY) &&
            tag.contains(TAG_CLIENT_TOTAL_CAPACITY) &&
            tag.contains(TAG_CLIENT_ACTIVE)
        ) {
            currentUsedCapacity = tag.getLong(TAG_CLIENT_USED_CAPACITY)
            currentTotalCapacity = tag.getLong(TAG_CLIENT_TOTAL_CAPACITY)
            currentlyActive = tag.getBoolean(TAG_CLIENT_ACTIVE)
        }
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = CompoundTag()
        tag.putLong(TAG_CLIENT_USED_CAPACITY, currentUsedCapacity)
        tag.putLong(TAG_CLIENT_TOTAL_CAPACITY, currentTotalCapacity)
        tag.putBoolean(TAG_CLIENT_ACTIVE, currentlyActive)
        return tag
    }

    override fun getMenuData(): ResourceContainerData {
        return ResourceContainerData.of(com.refinedmods.refinedstorage.common.support.resource.ResourceContainerImpl.createForFilter(0))
    }

    override fun getMenuCodec(): StreamEncoder<RegistryFriendlyByteBuf, ResourceContainerData> {
        return ResourceContainerData.STREAM_CODEC
    }

    override fun getName(): Component {
        return Component.translatable("block.refinedmonitors.storage_capacity_monitor")
    }

    override fun createMenu(
        containerId: Int,
        playerInventory: Inventory,
        player: Player,
    ): AbstractContainerMenu {
        return StorageCapacityMonitorContainerMenu(containerId, player, this)
    }

    fun isCurrentlyActive(): Boolean {
        val isClientSide = level?.isClientSide ?: false
        return if (isClientSide) {
            currentlyActive
        } else {
            mainNetworkNode.isActive
        }
    }

    fun getCurrentUsedCapacity(): Long {
        return currentUsedCapacity
    }

    fun getCurrentTotalCapacity(): Long {
        return currentTotalCapacity
    }

    fun getUsagePercentage(): Double {
        return if (currentTotalCapacity > 0) {
            (currentUsedCapacity.toDouble() / currentTotalCapacity.toDouble()) * 100.0
        } else {
            0.0
        }
    }

    override fun doesBlockStateChangeWarrantNetworkNodeUpdate(
        oldBlockState: BlockState,
        newBlockState: BlockState,
    ): Boolean {
        return com.refinedmods.refinedstorage.common.support.AbstractDirectionalBlock.didDirectionChange(
            oldBlockState,
            newBlockState,
        )
    }

    data class CapacityInfo(
        val usedCapacity: Long,
        val totalCapacity: Long,
    )
}
