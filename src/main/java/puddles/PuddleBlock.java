package puddles;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

public class PuddleBlock extends Block {

	public PuddleBlock() {
		super(Block.Properties.create(Material.WATER).doesNotBlockMovement().hardnessAndResistance(100F).noDrops()
				.tickRandomly());
	}

	@Override
	public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
		return VoxelShapes.empty();
	}

	@Override
	public boolean isNormalCube(BlockState state, IBlockReader world, BlockPos pos) {
		return false;
	}

	@Override
	public boolean isValidPosition(BlockState state, IWorldReader world, BlockPos pos) {
		return world.getBlockState(pos.down()).isSolid();
	}

	@Override
	public void neighborChanged(BlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
		if (!isValidPosition(state, world, pos)) {
			world.destroyBlock(pos, false);
		}
	}

	// on random tick
	@Override
	public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random rand) {
		if (!world.isRaining()) {
			if (rand.nextFloat() * 100 < PuddlesConfig.puddleEvaporationRate.get()) {
				world.removeBlock(pos, false);
			}
		} else {
			if (rand.nextInt(500) < PuddlesConfig.puddleEvaporationRate.get()) {
				world.removeBlock(pos, false);
			}
		}
	}

	@Override
	public void onEntityWalk(World world, BlockPos pos, Entity entity) {
		if(!world.isRemote)
			((ServerWorld) world).spawnParticle(ParticleTypes.SPLASH, entity.getPosX(), entity.getPosY(), entity.getPosZ(), 15, 0.0D, 0.0D, 0.0D, 0.13D);
	}
}
