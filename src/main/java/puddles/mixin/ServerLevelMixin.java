package puddles.mixin;

import java.util.function.Supplier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biome.Precipitation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.WritableLevelData;
import puddles.Puddles;
import puddles.PuddlesConfig;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends Level {

	protected ServerLevelMixin(WritableLevelData writableLevelData, ResourceKey<Level> level,
			Holder<DimensionType> dimension, Supplier<ProfilerFiller> profilerFiller, boolean isClientSide, boolean isDebug,
			long seed) {
		super(writableLevelData, level, dimension, profilerFiller, isClientSide, isDebug, seed);
	}

	@Inject(at = @At("TAIL"), method="tickChunk")
	public void tickChunk(LevelChunk levelChunk, int randomTickSpeed, CallbackInfo info) {
		ProfilerFiller profilerfiller = this.getProfiler();
		
		profilerfiller.push("puddles");
		if(this.random.nextInt(32) == 0 && this.random.nextFloat() * 100 < PuddlesConfig.puddleRate.get()) {
			ChunkPos chunkpos = levelChunk.getPos();
			int i = chunkpos.getMinBlockX();
			int j = chunkpos.getMinBlockZ();
			BlockPos blockpos2 = this.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, this.getBlockRandomPos(i, 0, j, 15));
			BlockPos blockpos3 = blockpos2.below();
			boolean flag = this.isRainingAt(blockpos2);
			Biome biome = this.getBiome(blockpos2).value();
			if(!PuddlesConfig.biomeBlacklist.get().contains(biome.getRegistryName().toString())) {
				if (this.isAreaLoaded(blockpos2, 1)) {
					if (flag) {
						if (!biome.shouldSnow(this, blockpos2) && biome.getPrecipitation() == Precipitation.RAIN) {
							if(this.getBlockState(blockpos2).is(Blocks.AIR) && Block.isFaceFull(this.getBlockState(blockpos3).getCollisionShape(levelChunk, blockpos3), Direction.UP))
								this.setBlockAndUpdate(blockpos2, Puddles.PUDDLE.get().defaultBlockState());
						}
					}
				}
			}
		}
		profilerfiller.pop();
	}
}
