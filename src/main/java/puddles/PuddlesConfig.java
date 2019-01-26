package puddles;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Comment;
import net.minecraftforge.common.config.Config.Name;
import net.minecraftforge.common.config.Config.Type;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = Puddles.MODID, name = Puddles.NAME, type = Type.INSTANCE)
public class PuddlesConfig
{
	@Name("Puddle Rate")
	@Comment({
		"The game will pick a random block every tick for every active chunk",
		"Then it will check if a puddle can be placed there",
		"Then it generates a random number between 0-99",
		"And if that number is less than this puddle rate number, it puts a puddle",
		"That means any value over 99 will flood your world with puddles"
	})
	public static int puddleRate = 15;
	
	@Name("Can Use Glass Bottle")
	@Comment({
		"Toggles filling glass bottles with puddle water"
	})
	public static boolean canUseGlassBottle = true;
	
	@Mod.EventBusSubscriber
	public static class ConfigEventHandler
	{
		@SubscribeEvent
		public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
		{
			if(event.getModID().equals(Puddles.MODID))
			{
				ConfigManager.sync(Puddles.MODID, Type.INSTANCE);
			}
		}
	}
}
