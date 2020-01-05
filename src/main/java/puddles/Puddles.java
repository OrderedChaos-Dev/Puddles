package puddles;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.color.ItemColors;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.PotionUtils;
import net.minecraft.potion.Potions;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeColors;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.Heightmap.Type;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(Puddles.MOD_ID)
@EventBusSubscriber(modid = Puddles.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class Puddles {
	
	public static final String MOD_ID = "puddles";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
	
	public static Block puddle;
	
	public Puddles() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PuddlesConfig.COMMON_CONFIG);
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::loadComplete);
		MinecraftForge.EVENT_BUS.register(this);
		
		PuddlesConfig.loadConfig(PuddlesConfig.COMMON_CONFIG, FMLPaths.CONFIGDIR.get().resolve("puddles-common.toml"));
	}
	
	@SubscribeEvent
	public static void registerBlocks(RegistryEvent.Register<Block> event) {
		puddle = new PuddleBlock().setRegistryName(new ResourceLocation(MOD_ID, "puddle"));
		event.getRegistry().register(puddle);
		
		Item.Properties prop = new Item.Properties().group(ItemGroup.MISC);
		BlockItem item = new BlockItem(puddle, prop);
		item.setRegistryName(new ResourceLocation(MOD_ID, "puddle"));
		ForgeRegistries.ITEMS.register(item);
		
		LOGGER.info("Loaded puddles.");
		
		if(FMLEnvironment.dist == Dist.CLIENT) {
			RenderTypeLookup.setRenderLayer(puddle, RenderType.func_228641_d_());
		}
	}
	
	private void loadComplete(FMLLoadCompleteEvent event) {
		BlockColors blockColors = Minecraft.getInstance().getBlockColors();
		ItemColors itemColors = Minecraft.getInstance().getItemColors();
		
		//4159204 is the forest water color
		blockColors.register((state, world, pos, tintIndex) -> (world != null && pos != null)
				? BiomeColors.func_228363_c_(world, pos) : 4159204, puddle);
		
		itemColors.register((itemstack, tintIndex) -> 4159204, puddle);
	}
	
	@SubscribeEvent
	public void placePuddles(TickEvent.ServerTickEvent event) {
		try {
			ServerWorld world = Minecraft.getInstance().getIntegratedServer().getWorld(DimensionType.OVERWORLD);
			if(world.getGameTime() % 20 == 0) {
				Class<?> clazz = world.getChunkProvider().chunkManager.getClass();
				Method getLoadedChunks = clazz.getDeclaredMethod("func_223491_f");
				getLoadedChunks.setAccessible(true);
				Iterable<ChunkHolder> iterator = (Iterable<ChunkHolder>) getLoadedChunks.invoke(world.getChunkProvider().chunkManager);
				iterator.forEach((chunk) -> {
		            Optional<Chunk> optional = chunk.func_219297_b().getNow(ChunkHolder.UNLOADED_CHUNK).left();
		            if (optional.isPresent()) {
						ChunkPos chunkPos = chunk.getPosition();
						Random random = world.rand;

						int x = random.nextInt(16);
						int z = random.nextInt(16);
						BlockPos pos = chunkPos.getBlock(x, 0, z);

						int y = world.getHeight(Type.MOTION_BLOCKING, pos.getX(), pos.getZ());
						
						BlockPos puddlePos = pos.add(0, y - 1, 0);
						
						if (canSpawnPuddle(world, puddlePos)) {
							if ((random.nextFloat() * 1200) <  PuddlesConfig.puddleRate.get()) {
								world.setBlockState(puddlePos.up(), puddle.getDefaultState(), 2);
								System.out.println(puddlePos.up().toString());
							}
						}
		            }
		         });
			}
		} catch(Exception e) {
			//lol
		}
	}
	
	public boolean canSpawnPuddle(World world, BlockPos pos) {
		if(!world.getBlockState(pos).isSolid())
			return false;
		if(!world.isAirBlock(pos.up()))
			return false;
		if(!world.isRaining())
			return false;
		
		Biome biome = world.func_226691_t_(pos);
		if(PuddlesConfig.biomeBlacklist.get().contains(biome.getRegistryName().toString()))
			return false;
		
		if (!biome.doesSnowGenerate(world, pos)) {
			for (int y = pos.getY() + 1; y < world.getActualHeight(); y++) {
				BlockPos up = new BlockPos(pos.getX(), y, pos.getZ());
				if(!world.isAirBlock(up))
					return false;
			}
			return true;
		}
		
		return false;
	}
	
	@SubscribeEvent
	public void puddleInteract(PlayerInteractEvent.RightClickBlock event) {
		ItemStack stack = event.getItemStack();
		World world = event.getWorld();
		BlockPos pos = event.getPos().up();
		PlayerEntity player = event.getPlayer();
		if (world.getBlockState(pos).getBlock() == puddle) {
			if (stack.getItem() == Items.GLASS_BOTTLE) {
				if (event.getFace() == Direction.UP) {
					if (!world.isRemote) {
						stack.shrink(1);
						if (!player.inventory.addItemStackToInventory(
								PotionUtils.addPotionToItemStack(new ItemStack(Items.POTION), Potions.WATER))) {
		                    player.dropItem(PotionUtils.addPotionToItemStack(new ItemStack(Items.POTION), Potions.WATER), false);
		                }
						world.removeBlock(pos, false);
					} else {
						world.playSound(player, player.func_226277_ct_(), player.func_226278_cu_(), player.func_226281_cx_(), SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.NEUTRAL, 1.0F, 1.0F);
					}
				}
			}
		}
	}
	
	@SubscribeEvent
	public void makeBigSplash(LivingFallEvent event) {
		Entity entity = event.getEntity();
		BlockPos pos = entity.getPosition();
		World world = entity.getEntityWorld();

		if (!world.isRemote) {
			if (world.getBlockState(pos).getBlock() == Puddles.puddle) {
				float distance = event.getDistance();
				if(distance < 3.0F)
					((ServerWorld)world).spawnParticle(ParticleTypes.SPLASH, entity.func_226277_ct_(), entity.func_226278_cu_(), entity.func_226281_cx_(), 15, 0.0D, 0.0D, 0.0D, 0.13D);
				else
				{
		            float f = (float)MathHelper.ceil(distance - 3.0F);

	                double d0 = Math.min((double)(0.2F + f / 15.0F), 2.5D);
	                int i = (int)(200.0D * d0);
	                
					for (int a = 0; a < 20; a++) {
	                	double x = 0.8 * (world.rand.nextDouble() - world.rand.nextDouble());
	                	double z = 0.8 * (world.rand.nextDouble() - world.rand.nextDouble());
		                ((ServerWorld)world).spawnParticle(ParticleTypes.SPLASH, entity.func_226277_ct_() + x, entity.func_226278_cu_(), entity.func_226281_cx_() + z, i / 2, 0.0D, 0.0D, 0.0D, 0.25D);	
	                };
	                world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_SPLASH, SoundCategory.NEUTRAL, 1.0F, 1.0F);
				}	
			}	
		}
	}
}
