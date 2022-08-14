package puddles.block;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import puddles.PuddlesConfig;

public class PuddleBlock extends Block {
	
	private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 0.0125D, 16.0D);

	public PuddleBlock() {
		super(Block.Properties.of(Material.WATER).sound(SoundType.WOOL).noCollission().strength(100F).noDrops().randomTicks());
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		BlockState blockstate = level.getBlockState(pos.below());
		if (!blockstate.is(Blocks.ICE) && !blockstate.is(Blocks.PACKED_ICE) && !blockstate.is(Blocks.BARRIER)) {
			if (!blockstate.is(Blocks.HONEY_BLOCK) && !blockstate.is(Blocks.SOUL_SAND)) {
				return Block.isFaceFull(blockstate.getCollisionShape(level, pos.below()), Direction.UP);
			} else {
				return true;
			}
		} else {
			return false;
		}
	}
	
	@Override
	public BlockState updateShape(BlockState state, Direction direction, BlockState state2, LevelAccessor level, BlockPos pos1, BlockPos pos2) {
		return !state.canSurvive(level, pos1) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, state2, level, pos1, pos2);
	}
	
	@Override
	public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
		return true;
	}

	@Override
	public void randomTick(BlockState state, ServerLevel world, BlockPos pos, Random rand) {
		if(!world.isRainingAt(pos)) {
			if(rand.nextFloat() * 100 < PuddlesConfig.puddleEvaporationRate.get()) {
				world.removeBlock(pos, false);
			}

		} else {
			//evaporation 10 times slower when raining
			if(rand.nextFloat() * 10 < PuddlesConfig.puddleEvaporationRate.get()) {
				world.removeBlock(pos, false);
			}
		}
	}
	
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, Random rand) { 
		if(level.canSeeSky(pos) && level.isRaining()) {
			double x = rand.nextDouble();
			double z = rand.nextDouble();
			for(int i = 0; i < 5; i++)
				level.addParticle(ParticleTypes.SPLASH, pos.getX() + x, pos.getY(), pos.getZ() + z, 0.0D, 0.0D, 0.0D);
		}
	}
}