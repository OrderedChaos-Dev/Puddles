package puddles;

import java.nio.file.Path;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;

import net.minecraftforge.common.ForgeConfigSpec;

public class PuddlesConfig {
	
	private static final ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();
	public static ForgeConfigSpec COMMON_CONFIG;
	
	
	public static ForgeConfigSpec.IntValue puddleRate;
	public static ForgeConfigSpec.IntValue puddleEvaporationRate;
	
	
	static {
		COMMON_BUILDER.comment("Puddles Settings").push("Puddles");
		COMMON_BUILDER.comment("Puddle Rate").comment("Default: 5");
		puddleRate = COMMON_BUILDER.comment("How fast puddles generate - (0-5 is reasonable)").defineInRange("puddleRate", 5, 0, 1200);
		COMMON_BUILDER.comment("Puddle Evaporation Rate").comment("Default: 30");
		puddleEvaporationRate = COMMON_BUILDER.comment("How fast puddles evaporate").defineInRange("puddleEvaporationRate", 30, 0, 100);
		COMMON_BUILDER.pop();
		
		COMMON_CONFIG = COMMON_BUILDER.build();
	}
	
	public static void loadConfig(ForgeConfigSpec spec, Path path) {
		final CommentedFileConfig configData = CommentedFileConfig.builder(path).sync().autosave().writingMode(WritingMode.REPLACE).build();
		
		configData.load();
		spec.setConfig(configData);
	}
}
