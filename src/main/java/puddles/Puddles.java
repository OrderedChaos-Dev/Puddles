package puddles;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Plane;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import puddles.block.PuddleBlock;

@Mod(Puddles.MOD_ID)
public class Puddles
{
	public static final String MOD_ID = "puddles";
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<Block> BLOCK_REGISTER = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    
    public static final RegistryObject<Block> PUDDLE = BLOCK_REGISTER.register("puddle", () -> new PuddleBlock());

    public Puddles(){
    	IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::setup);
        bus.addListener(this::clientSetup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, PuddlesConfig.CONFIG);
        
        BLOCK_REGISTER.register(bus);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
    	
    }
    
    private void clientSetup(FMLClientSetupEvent event) {
    	event.enqueueWork(() -> {
    		ItemBlockRenderTypes.setRenderLayer(PUDDLE.get(), RenderType.translucent());
    		
    		BlockColors blockColors = Minecraft.getInstance().getBlockColors();
    		ItemColors itemColors = Minecraft.getInstance().getItemColors();
    		
    		//4159204 is the forest water color
    		blockColors.register((state, world, pos, tintIndex) -> (world != null && pos != null)
    				? BiomeColors.getAverageWaterColor(world, pos) : 4159204, PUDDLE.get());
    		
    		itemColors.register((itemstack, tintIndex) -> 4159204, PUDDLE.get());
    	});

    }

    @SubscribeEvent
    public void fillBottle(PlayerInteractEvent.RightClickBlock event) {
    	Player player = event.getPlayer();
    	InteractionHand hand = event.getHand();
    	
    	ItemStack itemstack = player.getItemInHand(hand);
    	if(itemstack.is(Items.GLASS_BOTTLE)) {
    		if(event.getFace() == Direction.UP) {
    			if(event.getWorld().getBlockState(event.getPos().above()).is(PUDDLE.get())) {
    				boolean flag = false;
    				if(itemstack.is(Items.GLASS_BOTTLE)) {
    					ItemStack waterBottle = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER);
    					itemstack.shrink(1);
    					event.getWorld().playSound(player, player.getX(), player.getY(), player.getZ(), SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
    					if(itemstack.isEmpty()) {
    						player.setItemInHand(hand, waterBottle);
    					} else if(!player.getInventory().add(waterBottle)) {
    						player.drop(waterBottle, false);
    					}
    					event.getWorld().removeBlock(event.getPos().above(), false);
    					event.getWorld().gameEvent(player, GameEvent.FLUID_PICKUP, event.getPos().above());
    					flag = true;
    					
    					if(flag && !event.getWorld().isClientSide()) {
    						player.awardStat(Stats.ITEM_USED.get(itemstack.getItem()));
    					}
    				}
    				
    				if(flag) {
    					player.swing(hand, true);
    					event.setCanceled(true);
    				}
    			}
    		}
    	}
    }
    
    @SubscribeEvent
	public void makeBigSplash(LivingFallEvent event) {
    	if(!PuddlesConfig.allowSplashing.get())
    		return;
    	
		Entity entity = event.getEntity();
		BlockPos pos = entity.blockPosition();
		Level level = entity.getLevel();

		if (!level.isClientSide()) {
			if (level.getBlockState(pos).getBlock() == Puddles.PUDDLE.get()) {
				float distance = event.getDistance();
				if(distance < 3.0F)
					((ServerLevel)level).sendParticles(ParticleTypes.SPLASH, entity.getX(), entity.getY(), entity.getZ(), 15, 0.0D, 0.0D, 0.0D, 0.13D);
				else{
		            float f = (float)Mth.ceil(distance - 3.0F);

	                double d0 = Math.min((double)(0.2F + f / 15.0F), 2.5D);
	                int i = (int)(200.0D * d0);
	                
					for (int a = 0; a < 20; a++) {
	                	double x = 0.8 * (level.getRandom().nextDouble() - level.getRandom().nextDouble());
	                	double z = 0.8 * (level.getRandom().nextDouble() - level.getRandom().nextDouble());
		                ((ServerLevel)level).sendParticles(ParticleTypes.SPLASH, entity.getX() + x, entity.getY(), entity.getZ() + z, i / 2, 0.0D, 0.0D, 0.0D, 0.25D);	
	                };
	                level.playSound(null, pos, SoundEvents.PLAYER_SPLASH, SoundSource.NEUTRAL, 1.0F, 1.0F);
	                
	                if(distance > 7.0F) {
	                	level.removeBlock(pos, false);
	                	Plane.HORIZONTAL.forEach(dir -> {
	                		BlockPos offset = pos.offset(dir.getNormal());
	                		if(level.isEmptyBlock(offset) && Block.isFaceFull(level.getBlockState(offset.below()).getCollisionShape(level, offset.below()), Direction.UP)) {
	                			level.setBlock(offset, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 7), 3);
	                		}
	                	});
	                }
				}
			}	
		}
	}
}
