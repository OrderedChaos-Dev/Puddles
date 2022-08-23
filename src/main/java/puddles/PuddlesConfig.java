package puddles;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.ConfigValue;

public class PuddlesConfig {
	
	private static final ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();
	public static ForgeConfigSpec CONFIG;
	
	public static ForgeConfigSpec.IntValue puddleRate;
	public static ForgeConfigSpec.IntValue puddleEvaporationRate;
	public static ForgeConfigSpec.BooleanValue allowSplashing;
	public static ConfigValue<List<? extends String>> biomeBlacklist;
	
	static {
		COMMON_BUILDER.comment("Puddles Settings").push("Puddles");
		puddleRate = COMMON_BUILDER.comment("How fast puddles generate - (0-5 is reasonable) Default: 5 - this is about 40 times as slow as snowfall").defineInRange("puddleRate", 5, 0, 100);
		puddleEvaporationRate = COMMON_BUILDER.comment("How fast puddles evaporate. Default: 50 - about twice as slow as snow/ice melting").defineInRange("puddleEvaporationRate", 50, 0, 100);
		allowSplashing = COMMON_BUILDER.comment("Allow splashing when jumping on puddles").define("allowSplashing", true);
		biomeBlacklist = COMMON_BUILDER.comment("Biome blacklist (e.g [\"minecraft:forest\",\"minecraft:plains\"])").defineList("biomeBlacklist", Collections.emptyList(), (o) -> true);
		COMMON_BUILDER.pop();
		
		CONFIG = COMMON_BUILDER.build();
	}
	
	public static void loadConfig(ForgeConfigSpec spec, Path path) {
		final CommentedFileConfig configData = CommentedFileConfig.builder(path).sync().autosave().writingMode(WritingMode.REPLACE).build();
		
		configData.load();
		spec.setConfig(configData);
	}
}