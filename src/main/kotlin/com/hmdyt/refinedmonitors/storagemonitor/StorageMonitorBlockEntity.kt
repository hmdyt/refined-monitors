package com.hmdyt.refinedmonitors.storagemonitor

import com.hmdyt.refinedmonitors.RefinedMonitorsMod
import com.refinedmods.refinedstorage.api.network.Network
import com.refinedmods.refinedstorage.api.network.impl.node.SimpleNetworkNode
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent
import com.refinedmods.refinedstorage.api.resource.ResourceKey
import com.refinedmods.refinedstorage.api.storage.root.RootStorage
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi
import com.refinedmods.refinedstorage.common.api.storage.PlayerActor
import com.refinedmods.refinedstorage.common.support.network.AbstractBaseNetworkNodeContainerBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.level.block.state.BlockState
import org.slf4j.LoggerFactory

class StorageMonitorBlockEntity(
    pos: BlockPos,
    state: BlockState,
) : AbstractBaseNetworkNodeContainerBlockEntity<SimpleNetworkNode>(
        RefinedMonitorsMod.STORAGE_MONITOR_BLOCK_ENTITY.get(),
        pos,
        state,
        SimpleNetworkNode(25L),
    ),
    MenuProvider {
    companion object {
        private val LOGGER = LoggerFactory.getLogger(StorageMonitorBlockEntity::class.java)

        private const val TAG_CLIENT_FILTER = "cf"
        private const val TAG_CLIENT_AMOUNT = "ca"
        private const val TAG_CLIENT_ACTIVE = "cac"
    }

    // フィルタリングされたリソース
    private var configuredResource: ResourceKey? = null

    // 現在の表示状態
    private var currentAmount: Long = 0
    private var currentlyActive: Boolean = false
    private var lastExtractTime: Long = 0

    fun tick() {
        if (level?.isClientSide == true) {
            return
        }

        doWork()
        trySendDisplayUpdate()
    }

    override fun doWork() {
        super.doWork()
        if (level == null) {
            return
        }
        trySendDisplayUpdate()
    }

    private fun trySendDisplayUpdate() {
        // TODO: 表示更新のレート制限とネットワーク同期
        // 一旦ダミー実装
        val amount = getAmount()
        val active = isActive()

        if (amount != currentAmount || active != currentlyActive) {
            sendDisplayUpdate(amount, active)
        }
    }

    private fun getAmount(): Long {
        val resource = configuredResource ?: return 0L
        val network = mainNetworkNode.network ?: return 0L
        return getAmount(network, resource)
    }

    private fun getAmount(
        network: Network,
        configuredResource: ResourceKey,
    ): Long {
        val rootStorage: RootStorage = network.getComponent(StorageNetworkComponent::class.java)
        return rootStorage.get(configuredResource)
    }

    private fun isActive(): Boolean {
        return mainNetworkNode.isActive
    }

    private fun sendDisplayUpdate(
        amount: Long,
        active: Boolean,
    ) {
        currentAmount = amount
        currentlyActive = active

        // クライアント同期
        level?.sendBlockUpdated(blockPos, blockState, blockState, 3)
    }

    fun insert(
        player: Player,
        hand: InteractionHand,
    ) {
        if (level == null) {
            return
        }

        LOGGER.info("StorageMonitor insert called by player: ${player.name.string}")

        // TODO: RS2ネットワークへの挿入処理を実装
        if (doInsert(player, hand)) {
            sendDisplayUpdate(getAmount(), isActive())
        }
    }

    private fun doInsert(
        player: Player,
        hand: InteractionHand,
    ): Boolean {
        val network = mainNetworkNode.network ?: return false
        val resource = configuredResource ?: return false
        val stack = player.getItemInHand(hand)

        if (stack.isEmpty) return false

        val result =
            RefinedStorageApi.INSTANCE.storageMonitorInsertionStrategy.insert(
                resource,
                stack,
                PlayerActor(player),
                network,
            )

        if (result.isPresent) {
            player.setItemInHand(hand, result.get())
            return true
        }

        return false
    }

    fun extract(player: ServerPlayer) {
        if (level == null) {
            return
        }

        LOGGER.info("StorageMonitor extract called by player: ${player.name.string}")

        // TODO: RS2ネットワークからの抽出処理を実装
        val extracted = doExtract(player)
        if (extracted) {
            lastExtractTime = System.currentTimeMillis()
            sendDisplayUpdate(getAmount(), isActive())
        }
    }

    private fun doExtract(player: ServerPlayer): Boolean {
        val network = mainNetworkNode.network ?: return false
        val resource = configuredResource ?: return false

        return RefinedStorageApi.INSTANCE.storageMonitorExtractionStrategy.extract(
            resource,
            !player.isShiftKeyDown,
            player,
            PlayerActor(player),
            network,
        )
    }

    override fun saveAdditional(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ) {
        super.saveAdditional(tag, registries)

        // TODO: フィルタリソースとその他の状態を保存
        tag.putLong(TAG_CLIENT_AMOUNT, currentAmount)
        tag.putBoolean(TAG_CLIENT_ACTIVE, currentlyActive)
    }

    override fun loadAdditional(
        tag: CompoundTag,
        registries: HolderLookup.Provider,
    ) {
        super.loadAdditional(tag, registries)

        // TODO: フィルタリソースとその他の状態をロード
        currentAmount = tag.getLong(TAG_CLIENT_AMOUNT)
        currentlyActive = tag.getBoolean(TAG_CLIENT_ACTIVE)
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val tag = CompoundTag()
        saveAdditional(tag, registries)
        return tag
    }

    override fun getName(): Component {
        return Component.translatable("block.refinedmonitors.storage_monitor")
    }

    override fun createMenu(
        containerId: Int,
        playerInventory: Inventory,
        player: Player,
    ): AbstractContainerMenu {
        return StorageMonitorContainerMenu(containerId, playerInventory, this)
    }

    fun isCurrentlyActive(): Boolean {
        return currentlyActive
    }

    fun getConfiguredResource(): ResourceKey? {
        return configuredResource
    }

    fun getCurrentAmount(): Long {
        return currentAmount
    }
}
