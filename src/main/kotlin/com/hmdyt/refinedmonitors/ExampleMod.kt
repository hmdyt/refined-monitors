package com.hmdyt.refinedmonitors

import com.hmdyt.refinedmonitors.storagemonitor.StorageMonitorBlockEntity
import com.hmdyt.refinedmonitors.storagemonitor.StorageMonitorBlockEntityRenderer
import com.hmdyt.refinedmonitors.storagemonitor.StorageMonitorBlocks
import com.hmdyt.refinedmonitors.storagemonitor.StorageMonitorContainerMenu
import com.mojang.logging.LogUtils
import net.minecraft.client.Minecraft
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.food.FoodProperties
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.MapColor
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.IEventBus
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.fml.config.ModConfig
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.neoforge.client.event.EntityRenderersEvent
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import net.neoforged.neoforge.event.server.ServerStartingEvent
import net.neoforged.neoforge.registries.DeferredBlock
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredItem
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Consumer
import java.util.function.Supplier

@Mod(ExampleMod.MODID)
class ExampleMod {
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

        val STORAGE_MONITOR: DeferredBlock<Block> =
            BLOCKS.register("storage_monitor", Supplier { StorageMonitorBlocks.STORAGE_MONITOR })

        val STORAGE_MONITOR_BLOCK_ENTITY: DeferredHolder<BlockEntityType<*>, BlockEntityType<*>> =
            BLOCK_ENTITIES.register(
                "storage_monitor",
                Supplier {
                    BlockEntityType.Builder.of(
                        { pos, state -> StorageMonitorBlocks.STORAGE_MONITOR.newBlockEntity(pos, state) },
                        StorageMonitorBlocks.STORAGE_MONITOR,
                    ).build(null)
                },
            )

        val STORAGE_MONITOR_ITEM: DeferredItem<BlockItem> =
            ITEMS.registerSimpleBlockItem("storage_monitor", STORAGE_MONITOR)

        val STORAGE_MONITOR_MENU_TYPE: DeferredHolder<MenuType<*>, MenuType<StorageMonitorContainerMenu>> =
            MENU_TYPES.register(
                "storage_monitor",
                Supplier {
                    MenuType(
                        { id: Int, inventory: Inventory ->
                            StorageMonitorContainerMenu(id, inventory)
                        },
                        net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS,
                    )
                },
            )

        val EXAMPLE_BLOCK: DeferredBlock<Block> =
            BLOCKS.registerSimpleBlock("example_block", BlockBehaviour.Properties.of().mapColor(MapColor.STONE))

        val EXAMPLE_BLOCK_ITEM: DeferredItem<BlockItem> = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK)

        val EXAMPLE_ITEM: DeferredItem<Item> =
            ITEMS.registerSimpleItem(
                "example_item",
                Item.Properties().food(
                    FoodProperties.Builder()
                        .alwaysEdible().nutrition(1).saturationModifier(2f).build(),
                ),
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
                        .icon { STORAGE_MONITOR_ITEM.get().defaultInstance }
                        .displayItems { parameters: ItemDisplayParameters?, output: CreativeModeTab.Output ->
                            output.accept(STORAGE_MONITOR_ITEM.get())
                            output.accept(EXAMPLE_ITEM.get())
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
                    STORAGE_MONITOR_BLOCK_ENTITY.get() as BlockEntityType<StorageMonitorBlockEntity>,
                ) { context ->
                    StorageMonitorBlockEntityRenderer()
                }
            }
        }
    }

    constructor(modEventBus: IEventBus, modContainer: ModContainer) {
        modEventBus.addListener(::commonSetup)

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

        Config.items.forEach(
            Consumer { item: Item ->
                LOGGER.info(
                    "ITEM >> {}",
                    item.toString(),
                )
            },
        )
    }

    private fun addCreative(event: BuildCreativeModeTabContentsEvent) {
        if (event.tabKey === CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(STORAGE_MONITOR_ITEM)
            event.accept(EXAMPLE_BLOCK_ITEM)
        }
    }

    @SubscribeEvent
    fun onServerStarting(event: ServerStartingEvent) {
        LOGGER.info("HELLO from server starting")
    }
}
