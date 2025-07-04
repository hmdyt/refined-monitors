package com.hmdyt.refinedmonitors.storageflowmonitor

import com.google.common.util.concurrent.RateLimiter
import com.hmdyt.refinedmonitors.RefinedMonitorsMod
import com.refinedmods.refinedstorage.api.network.Network
import com.refinedmods.refinedstorage.api.network.impl.node.SimpleNetworkNode
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent
import com.refinedmods.refinedstorage.api.storage.root.RootStorage
import com.refinedmods.refinedstorage.common.Platform
import com.refinedmods.refinedstorage.common.api.storage.root.FuzzyRootStorage
import com.refinedmods.refinedstorage.common.api.support.resource.PlatformResourceKey
import com.refinedmods.refinedstorage.common.support.FilterWithFuzzyMode
import com.refinedmods.refinedstorage.common.support.containermenu.NetworkNodeExtendedMenuProvider
import com.refinedmods.refinedstorage.common.support.network.AbstractBaseNetworkNodeContainerBlockEntity
import com.refinedmods.refinedstorage.common.support.resource.ResourceContainerData
import com.refinedmods.refinedstorage.common.support.resource.ResourceContainerImpl
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

class StorageFlowMonitorBlockEntity(
    pos: BlockPos,
    state: BlockState,
) : AbstractBaseNetworkNodeContainerBlockEntity<SimpleNetworkNode>(
        RefinedMonitorsMod.STORAGE_FLOW_MONITOR_BLOCK_ENTITY.get(),
        pos,
        state,
        SimpleNetworkNode(Platform.INSTANCE.config.storageMonitor.energyUsage),
    ),
    NetworkNodeExtendedMenuProvider<ResourceContainerData> {
    companion object {
        private const val TAG_CLIENT_FILTER = "cf"
        private const val TAG_CLIENT_AMOUNT = "ca"
        private const val TAG_CLIENT_ACTIVE = "cac"
        private val LOGGER = LoggerFactory.getLogger(StorageFlowMonitorBlockEntity::class.java)
    }

    private val filter: FilterWithFuzzyMode
    private val displayUpdateRateLimiter = RateLimiter.create(0.25)

    private var currentAmount: Long = 0
    private var currentlyActive: Boolean = false
    private val flowRateHistory = mutableListOf<Long>()
    private val flowRateTimestamps = mutableListOf<Long>()
    private var currentFlowRate: Double = 0.0
    private val maxHistorySize = 60

    init {
        val resourceContainer = ResourceContainerImpl.createForFilter(1)
        this.filter =
            FilterWithFuzzyMode.create(resourceContainer) {
                setChanged()
                sendDisplayUpdate()
            }
    }

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
        val amount = getAmount()
        val active = mainNetworkNode.isActive
        if ((amount != currentAmount || active != currentlyActive) && displayUpdateRateLimiter.tryAcquire()) {
            updateFlowRate(amount)
            sendDisplayUpdate(level, amount, active)
        }
    }

    private fun getAmount(): Long {
        val configuredResource = getConfiguredResource() ?: return 0L
        val network = mainNetworkNode.network ?: return 0L
        return getAmount(network, configuredResource)
    }

    private fun getAmount(
        network: Network,
        configuredResource: PlatformResourceKey,
    ): Long {
        val rootStorage: RootStorage = network.getComponent(StorageNetworkComponent::class.java)
        if (!filter.isFuzzyMode || rootStorage !is FuzzyRootStorage) {
            return rootStorage.get(configuredResource)
        }
        return rootStorage.getFuzzy(configuredResource)
            .stream()
            .mapToLong { rootStorage.get(it) }
            .sum()
    }

    private fun isActive(): Boolean {
        return mainNetworkNode.isActive
    }

    private fun sendDisplayUpdate() {
        if (level == null) {
            return
        }
        sendDisplayUpdate(level!!, getAmount(), mainNetworkNode.isActive)
    }

    private fun sendDisplayUpdate(
        level: net.minecraft.world.level.Level,
        amount: Long,
        active: Boolean,
    ) {
        currentAmount = amount
        currentlyActive = active
        LOGGER.debug("Sending display update for storage flow monitor {} with amount {} and active {}", worldPosition, amount, active)
        PlatformUtil.sendBlockUpdateToClient(level, worldPosition)
    }

    override fun writeConfiguration(
        tag: CompoundTag,
        provider: HolderLookup.Provider,
    ) {
        super.writeConfiguration(tag, provider)
        filter.save(tag, provider)
    }

    override fun readConfiguration(
        tag: CompoundTag,
        provider: HolderLookup.Provider,
    ) {
        super.readConfiguration(tag, provider)
        filter.load(tag, provider)
    }

    override fun loadAdditional(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ) {
        super.loadAdditional(tag, registries)
        if (tag.contains(TAG_CLIENT_FILTER) && tag.contains(TAG_CLIENT_AMOUNT) && tag.contains(TAG_CLIENT_ACTIVE)) {
            filter.filterContainer.fromTag(tag.getCompound(TAG_CLIENT_FILTER), registries)
            currentAmount = tag.getLong(TAG_CLIENT_AMOUNT)
            currentlyActive = tag.getBoolean(TAG_CLIENT_ACTIVE)
        }
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = CompoundTag()
        tag.put(TAG_CLIENT_FILTER, filter.filterContainer.toTag(registries))
        tag.putLong(TAG_CLIENT_AMOUNT, currentAmount)
        tag.putBoolean(TAG_CLIENT_ACTIVE, currentlyActive)
        return tag
    }

    override fun getMenuData(): ResourceContainerData {
        return ResourceContainerData.of(filter.filterContainer)
    }

    override fun getMenuCodec(): StreamEncoder<RegistryFriendlyByteBuf, ResourceContainerData> {
        return ResourceContainerData.STREAM_CODEC
    }

    override fun getName(): Component {
        return Component.translatable("block.refinedmonitors.storage_flow_monitor")
    }

    override fun createMenu(
        containerId: Int,
        playerInventory: Inventory,
        player: Player,
    ): AbstractContainerMenu {
        return StorageFlowMonitorContainerMenu(containerId, player, this, filter.filterContainer)
    }

    fun isCurrentlyActive(): Boolean {
        val isClientSide = level?.isClientSide ?: false
        val result =
            if (isClientSide) {
                currentlyActive
            } else {
                mainNetworkNode.isActive
            }

        return result
    }

    fun getConfiguredResource(): PlatformResourceKey? {
        return filter.filterContainer.getResource(0)
    }

    fun getCurrentAmount(): Long {
        return currentAmount
    }

    fun isFuzzyMode(): Boolean {
        return filter.isFuzzyMode
    }

    fun setFuzzyMode(fuzzyMode: Boolean) {
        filter.isFuzzyMode = fuzzyMode
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

    fun getFilter(): FilterWithFuzzyMode {
        return filter
    }

    fun updateFlowRate(newAmount: Long) {
        val currentTime = System.currentTimeMillis()

        flowRateHistory.add(newAmount)
        flowRateTimestamps.add(currentTime)

        if (flowRateHistory.size > maxHistorySize) {
            flowRateHistory.removeAt(0)
            flowRateTimestamps.removeAt(0)
        }

        calculateFlowRate()
    }

    private fun calculateFlowRate() {
        if (flowRateHistory.size < 2) {
            currentFlowRate = 0.0
            return
        }

        val recentHistory = flowRateHistory.takeLast(10)
        val recentTimestamps = flowRateTimestamps.takeLast(10)

        if (recentHistory.size < 2) {
            currentFlowRate = 0.0
            return
        }

        val timeDiff = (recentTimestamps.last() - recentTimestamps.first()) / 1000.0
        val amountDiff = recentHistory.last() - recentHistory.first()

        currentFlowRate = if (timeDiff > 0) amountDiff / timeDiff else 0.0
    }

    fun getCurrentFlowRate(): Double {
        return currentFlowRate
    }

    fun getFlowRateDisplayText(): String {
        val rate = getCurrentFlowRate()
        return when {
            rate == 0.0 -> "0/s"
            rate > 0 -> "+${formatFlowRate(rate)}/s"
            else -> "${formatFlowRate(rate)}/s"
        }
    }

    private fun formatFlowRate(rate: Double): String {
        val absRate = kotlin.math.abs(rate)
        return when {
            absRate >= 1000000 -> String.format("%.1fM", rate / 1000000)
            absRate >= 1000 -> String.format("%.1fK", rate / 1000)
            absRate >= 1 -> String.format("%.1f", rate)
            else -> String.format("%.2f", rate)
        }
    }
}
