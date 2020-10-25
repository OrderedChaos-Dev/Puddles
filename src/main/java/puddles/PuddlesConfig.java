package puddles;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;

import net.minecraftforge.common.ForgeConfigSpec;

public class PuddlesConfig {
	
	private static final ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();
	public static ForgeConfigSpec COMMON_CONFIG;
	
	
	public static ForgeConfigSpec.IntValue puddleRate;
	public static ForgeConfigSpec.IntValue puddleEvaporationRate;
	public static ForgeConfigSpec.ConfigValue<List<String>> biomeBlacklist;
	
	static {
		COMMON_BUILDER.comment("Puddles Settings").push("Puddles");
		puddleRate = COMMON_BUILDER.comment("How fast puddles generate - (0-5 is reasonable) Default: 5").defineInRange("puddleRate", 5, 0, 1200);
		puddleEvaporationRate = COMMON_BUILDER.comment("How fast puddles evaporate. Default: 40").defineInRange("puddleEvaporationRate", 50, 0, 100);
		biomeBlacklist = COMMON_BUILDER.comment("Biome blacklist").define("biomeBlacklist", new ArrayList<String>());
		COMMON_BUILDER.pop();
		
		COMMON_CONFIG = COMMON_BUILDER.build();
	}
	
	public static void loadConfig(ForgeConfigSpec spec, Path path) {
		final CommentedFileConfig configData = CommentedFileConfig.builder(path).sync().autosave().writingMode(WritingMode.REPLACE).build();
		
		configData.load();
		spec.setConfig(configData);
	}
}
