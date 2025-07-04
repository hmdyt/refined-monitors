package com.hmdyt.refinedmonitors

import com.hmdyt.refinedmonitors.storageflowmonitor.StorageFlowMonitorBlockEntity
import com.hmdyt.refinedmonitors.storageflowmonitor.StorageFlowMonitorBlockEntityRenderer
import com.hmdyt.refinedmonitors.storageflowmonitor.StorageFlowMonitorBlocks
import com.hmdyt.refinedmonitors.storageflowmonitor.StorageFlowMonitorContainerMenu
import com.hmdyt.refinedmonitors.storageflowmonitor.StorageFlowMonitorScreen
import com.mojang.logging.LogUtils
import com.refinedmods.refinedstorage.neoforge.api.RefinedStorageNeoForgeApi
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntityType
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import net.neoforged.neoforge.client.event.EntityRenderersEvent
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import net.neoforged.neoforge.event.server.ServerStartingEvent
import net.neoforged.neoforge.registries.DeferredBlock
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredItem
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

@Mod(RefinedMonitorsMod.MODID)
class RefinedMonitorsMod {
    companion object {
        const val MODID = "refinedmonitors"

        private val LOGGER = LogUtils.getLogger()

        val BLOCKS: DeferredRegister.Blocks =
            DeferredRegister.createBlocks(
                MODID,
            )

        val ITEMS: DeferredRegister.Items =
            DeferredRegister.createItems(
                MODID,
            )

        val CREATIVE_MODE_TABS: DeferredRegister<CreativeModeTab> =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID)

        val BLOCK_ENTITIES: DeferredRegister<BlockEntityType<*>> =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID)

        val MENU_TYPES: DeferredRegister<MenuType<*>> =
            DeferredRegister.create(Registries.MENU, MODID)

        val STORAGE_FLOW_MONITOR: DeferredBlock<Block> =
            BLOCKS.register("storage_flow_monitor", Supplier { StorageFlowMonitorBlocks.STORAGE_FLOW_MONITOR })

        val STORAGE_FLOW_MONITOR_BLOCK_ENTITY: DeferredHolder<BlockEntityType<*>, BlockEntityType<StorageFlowMonitorBlockEntity>> =
            BLOCK_ENTITIES.register(
                "storage_flow_monitor",
                Supplier {
                    BlockEntityType.Builder.of(
                        { pos, state -> StorageFlowMonitorBlockEntity(pos, state) },
                        StorageFlowMonitorBlocks.STORAGE_FLOW_MONITOR,
                    ).build(null)
                },
            )

        val STORAGE_FLOW_MONITOR_ITEM: DeferredItem<BlockItem> =
            ITEMS.registerSimpleBlockItem("storage_flow_monitor", STORAGE_FLOW_MONITOR)

        val STORAGE_FLOW_MONITOR_MENU_TYPE: DeferredHolder<MenuType<*>, MenuType<StorageFlowMonitorContainerMenu>> =
            MENU_TYPES.register(
                "storage_flow_monitor",
                Supplier {
                    net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create { syncId, inventory, buf ->
                        val data = com.refinedmods.refinedstorage.common.support.resource.ResourceContainerData.STREAM_CODEC.decode(buf)
                        StorageFlowMonitorContainerMenu(syncId, inventory, data)
                    }
                },
            )

        val REFINED_MONITORS_TAB: DeferredHolder<CreativeModeTab, CreativeModeTab> =
            CREATIVE_MODE_TABS.register(
                "refined_monitors_tab",
                Supplier {
                    CreativeModeTab.builder()
                        .title(
                            Component.translatable("itemGroup.refinedmonitors"),
                        )
                        .withTabsBefore(CreativeModeTabs.COMBAT)
                        .icon { STORAGE_FLOW_MONITOR_ITEM.get().defaultInstance }
                        .displayItems { parameters: ItemDisplayParameters?, output: CreativeModeTab.Output ->
                            output.accept(STORAGE_FLOW_MONITOR_ITEM.get())
                        }.build()
                },
            )

        @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
        object ClientModEvents {
            @SubscribeEvent
            fun onClientSetup(event: FMLClientSetupEvent?) {
                LOGGER.info("HELLO FROM CLIENT SETUP")
                LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().user.name)
            }

            @SubscribeEvent
            fun onRegisterRenderers(event: EntityRenderersEvent.RegisterRenderers) {
                event.registerBlockEntityRenderer(
                    STORAGE_FLOW_MONITOR_BLOCK_ENTITY.get(),
                ) { context ->
                    StorageFlowMonitorBlockEntityRenderer()
                }
            }

            @SubscribeEvent
            fun onRegisterMenuScreens(event: RegisterMenuScreensEvent) {
                event.register(
                    STORAGE_FLOW_MONITOR_MENU_TYPE.get() as MenuType<StorageFlowMonitorContainerMenu>,
                    ::StorageFlowMonitorScreen,
                )
            }
        }
    }

    constructor(modEventBus: IEventBus, modContainer: ModContainer) {
        modEventBus.addListener(::commonSetup)
        modEventBus.addListener(::registerCapabilities)

        BLOCKS.register(modEventBus)
        ITEMS.register(modEventBus)
        BLOCK_ENTITIES.register(modEventBus)
        MENU_TYPES.register(modEventBus)
        CREATIVE_MODE_TABS.register(modEventBus)

        NeoForge.EVENT_BUS.register(this)

        modEventBus.addListener(::addCreative)

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC)
    }

    private fun commonSetup(event: FMLCommonSetupEvent) {
        LOGGER.info("HELLO FROM COMMON SETUP")

        if (Config.logDirtBlock) LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT))

        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber)
    }

    private fun addCreative(event: BuildCreativeModeTabContentsEvent) {
        if (event.tabKey === CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(STORAGE_FLOW_MONITOR_ITEM)
        }
    }

    private fun registerCapabilities(event: RegisterCapabilitiesEvent) {
        event.registerBlockEntity(
            RefinedStorageNeoForgeApi.INSTANCE.networkNodeContainerProviderCapability,
            STORAGE_FLOW_MONITOR_BLOCK_ENTITY.get(),
        ) { be, _ ->
            if (be is StorageFlowMonitorBlockEntity) {
                be.containerProvider
            } else {
                null
            }
        }
    }

    @SubscribeEvent
    fun onServerStarting(event: ServerStartingEvent) {
        LOGGER.info("HELLO from server starting")
    }
}
