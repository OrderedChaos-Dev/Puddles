package puddles;

import java.util.Iterator;
import java.util.Random;

import org.apache.logging.log4j.Logger;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod(modid = Puddles.MODID, name = Puddles.NAME, version = Puddles.VERSION)
public class Puddles
{
    public static final String MODID = "puddles";
    public static final String NAME = "Puddles";
    public static final String VERSION = "1.1";

    public static Logger logger;
    
    public static Block puddle;
    public static Item socks, wet_socks;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        logger.info("splish spash you have puddles installed");
        puddle = new BlockPuddle().setUnlocalizedName("puddle").setRegistryName(new ResourceLocation(MODID, "puddle"));
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new PuddlesConfig.ConfigEventHandler());
    }
    
	@SubscribeEvent
	public void registerBlocks(RegistryEvent.Register<Block> event)
	{
		event.getRegistry().register(puddle);
	}
	
	@SubscribeEvent
	public void registerItems(RegistryEvent.Register<Item> event)
	{
		ItemBlock puddle_item = new ItemBlock(puddle);
		puddle_item.setRegistryName(new ResourceLocation(MODID, "puddle"));
		event.getRegistry().register(puddle_item);
		
		socks = new ItemSocks(false);
		socks.setRegistryName(new ResourceLocation(MODID, "socks"));
		socks.setUnlocalizedName("socks");
		event.getRegistry().register(socks);
		
		wet_socks = new ItemSocks(true);
		wet_socks.setUnlocalizedName("wet_socks");
		wet_socks.setRegistryName(new ResourceLocation(MODID, "wet_socks"));
		event.getRegistry().register(wet_socks);
	}
	
	@SubscribeEvent
	public void registerItemModels(ModelRegistryEvent event)
	{
		Item puddle_item = Item.getItemFromBlock(puddle);
		ModelLoader.setCustomModelResourceLocation(puddle_item, 0, new ModelResourceLocation(puddle_item.getRegistryName(), "inventory"));
		
		ModelLoader.setCustomModelResourceLocation(socks, 0, new ModelResourceLocation(socks.getRegistryName(), "inventory"));
		ModelLoader.setCustomModelResourceLocation(wet_socks, 0, new ModelResourceLocation(wet_socks.getRegistryName(), "inventory"));
	}
	
	@SubscribeEvent
	public void placePuddles(TickEvent.ServerTickEvent event)
	{
		if(event.phase == TickEvent.Phase.END)
		{
			WorldServer world = DimensionManager.getWorld(0);
			try
			{
				if(world.getTotalWorldTime() % 10 == 0)
				{
					Iterator<Chunk> iterator = world.getPlayerChunkMap().getChunkIterator();
					
					while(iterator.hasNext())
					{
						Random random = world.rand;
						ChunkPos chunkPos = iterator.next().getPos();
						
						int x = random.nextInt(8) - random.nextInt(8);
						int z = random.nextInt(8) - random.nextInt(8);
						BlockPos pos = chunkPos.getBlock(8 + x, 0, 8 + z);
						
						int y = world.getHeight(pos).getY() + random.nextInt(4) - random.nextInt(4);
						BlockPos puddlePos = pos.add(0, y, 0);
						
						if(this.canSpawnPuddle(world, puddlePos))
						{
							if(random.nextInt(100) < PuddlesConfig.puddleRate)
							{
								world.setBlockState(puddlePos.up(), puddle.getDefaultState(), 2);
							}
						}
					}
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public boolean canSpawnPuddle(World world, BlockPos pos)
	{
		if(!world.isSideSolid(pos, EnumFacing.UP))
			return false;
		if(!world.isAirBlock(pos.up()))
			return false;
		if(!world.isRaining())
			return false;
		
		Biome biome = world.getBiomeForCoordsBody(pos);
		if(biome.canRain() && !biome.getEnableSnow())
		{
			for(int y = pos.getY() + 1; y < world.getHeight(); y++)
			{
				BlockPos up = new BlockPos(pos.getX(), y, pos.getZ());
				if(!world.isAirBlock(up))
					return false;
			}
			return true;
		}
		
		return false;
	}
	
	@SubscribeEvent
	public void puddleInteract(PlayerInteractEvent.RightClickBlock event)
	{
		ItemStack stack = event.getItemStack();
		World world = event.getWorld();
		BlockPos pos = event.getPos().up();
		EntityPlayer player = event.getEntityPlayer();
		if(world.getBlockState(pos).getBlock() == Puddles.puddle)
		{
			if(stack.getItem() == Items.GLASS_BOTTLE && PuddlesConfig.canUseGlassBottle)
			{
				if(event.getFace() == EnumFacing.UP)
				{
					if(!world.isRemote)
					{
						stack.shrink(1);
		                if (!player.inventory.addItemStackToInventory(PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.WATER)))
		                {
		                    player.dropItem(PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.WATER), false);
		                }
						world.setBlockToAir(pos);
					}
					else
					{
						world.playSound(player, player.posX, player.posY, player.posZ, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.NEUTRAL, 1.0F, 1.0F);
					}
				}
			}
			if(stack.getItem() instanceof ItemHoe)
			{
				world.setBlockToAir(pos);
				ItemHoe hoe = (ItemHoe)stack.getItem();
				hoe.onItemUse(player, world, pos.down(), event.getHand(), event.getFace(), 0, 0, 0);
			}
			if(stack.getItem() instanceof ItemSpade)
			{
				world.setBlockToAir(pos);
				ItemSpade shovel = (ItemSpade)stack.getItem();
				shovel.onItemUse(player, world, pos.down(), event.getHand(), event.getFace(), 0, 0, 0);
			}
		}
	}
	
	@SubscribeEvent
	public void makeBigSplash(LivingFallEvent event)
	{
		EntityLivingBase entity = event.getEntityLiving();
		BlockPos pos = entity.getPosition();
		World world = entity.getEntityWorld();

		if(!world.isRemote)
		{
			if(world.getBlockState(pos).getBlock() == Puddles.puddle)
			{
				float distance = event.getDistance();
				if(distance < 3.0F)
				{
	                ((WorldServer)world).spawnParticle(EnumParticleTypes.BLOCK_DUST, entity.posX, entity.posY, entity.posZ, 15, 0.0D, 0.0D, 0.0D, 0.13D, Block.getStateId(Puddles.puddle.getDefaultState()));
	                ((WorldServer)world).spawnParticle(EnumParticleTypes.WATER_SPLASH, entity.posX, entity.posY, entity.posZ, 15, 0.0D, 0.0D, 0.0D, 0.13D, Block.getStateId(Puddles.puddle.getDefaultState()));
				}
				else
				{
		            float f = (float)MathHelper.ceil(distance - 3.0F);

	                double d0 = Math.min((double)(0.2F + f / 15.0F), 2.5D);
	                int i = (int)(200.0D * d0);
	                
	                for(int a = 0; a < 20; a++)
	                {
	                	double x = 0.8 * (world.rand.nextDouble() - world.rand.nextDouble());
	                	double z = 0.8 * (world.rand.nextDouble() - world.rand.nextDouble());
		                ((WorldServer)world).spawnParticle(EnumParticleTypes.WATER_SPLASH, entity.posX + x, entity.posY, entity.posZ + z, i / 2, 0.0D, 0.0D, 0.0D, 0.25D);	
	                }
	                ((WorldServer)world).spawnParticle(EnumParticleTypes.BLOCK_DUST, entity.posX, entity.posY, entity.posZ, i, 0.0D, 0.0D, 0.0D, 0.4D, Block.getStateId(Puddles.puddle.getDefaultState()));
	                world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_SPLASH, SoundCategory.NEUTRAL, 1.0F, 1.0F);
				}	
			}	
		}
	}
	
	//WET SOCKS
	@SubscribeEvent
	public void makeSocksWet(LivingUpdateEvent event)
	{
		if(event.getEntityLiving() instanceof EntityPlayer)
		{
			EntityPlayer player = (EntityPlayer)event.getEntityLiving();
			BlockPos pos = player.getPosition();
			World world = player.getEntityWorld();
			if(world.getBlockState(pos).getBlock() == Puddles.puddle)
			{
				Iterator<ItemStack> armor = player.getArmorInventoryList().iterator();
				ItemStack socks = null;
				while(armor.hasNext())
				{
					ItemStack temp = armor.next();
					if(temp.getItem() == Puddles.socks)
					{
						socks = temp;
						break;
					}
				}

				if(socks != null)
				{
					player.setItemStackToSlot(EntityEquipmentSlot.FEET, new ItemStack(Puddles.wet_socks));
				}
			}
		}
	}
}
