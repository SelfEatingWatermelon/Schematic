package com.dyn.schematics.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import javax.annotation.Nullable;

import com.dyn.schematics.SchematicMod;
import com.dyn.schematics.gui.GuiClaimBlock;
import com.dyn.schematics.registry.SchematicRenderingRegistry;
import com.dyn.schematics.utils.SimpleItemStack;

import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockSchematicClaim extends BlockHorizontal implements ITileEntityProvider {

	protected BlockSchematicClaim() {
		super(Material.WOOD);
	}

	/**
	 * Return true if an entity can be spawned inside the block (used to get the
	 * player's bed spawn location)
	 */
	@Override
	public boolean canSpawnInBlock() {
		return true;
	}

	@Override
	public TileEntity createNewTileEntity(World worldIn, int meta) {
		return new ClaimBlockTileEntity();
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		EnumFacing enumfacing = state.getValue(BlockHorizontal.FACING);

		switch (enumfacing) {
		case EAST:
			return new AxisAlignedBB(7F / 16F, 0.0F, 0.05F, 10.1F / 16F, 1, 0.95F);
		case WEST:
			return new AxisAlignedBB(1 - (10.1F / 16F), 0.0F, 0.05F, 1 - (7F / 16F), 1, 0.95F);
		case SOUTH:
			return new AxisAlignedBB(0.05F, 0.0F, 7F / 16F, 0.95F, 1, 10.1F / 16F);
		case NORTH:
			return new AxisAlignedBB(0.05F, 0.0F, 1 - (10.1F / 16F), 0.95F, 1, 1 - (7F / 16F));
		default:
			return new AxisAlignedBB(7F / 16F, 0.0F, 0.05F, 10.1F / 16F, 1, 0.95F);
		}
	}

	@Override
	@Nullable
	public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState, IBlockAccess worldIn, BlockPos pos) {
		return Block.NULL_AABB;
	}

	@Override
	public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
		// we need this so that multiple items dont drop
		return new ArrayList<>();
	}

	/**
	 * Get the Item that this Block should drop when harvested.
	 */
	@Override
	public Item getItemDropped(IBlockState state, Random rand, int fortune) {
		return SchematicMod.schematic;
	}

	/**
	 * Called when a user uses the creative pick block button on this block
	 *
	 * @param target
	 *            The full target the player is looking at
	 * @return A ItemStack to add to the player's inventory, Null if nothing should
	 *         be added.
	 */
	@Override
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos,
			EntityPlayer player) {
		TileEntity tileentity = world.getTileEntity(pos);
		if ((tileentity instanceof ClaimBlockTileEntity)
				&& (((ClaimBlockTileEntity) tileentity).getSchematic() != null)) {
			ItemStack is = new ItemStack(SchematicMod.schematic);
			NBTTagCompound compound = new NBTTagCompound();
			((ClaimBlockTileEntity) tileentity).getSchematic().writeToNBT(compound);
			compound.setString("title", ((ClaimBlockTileEntity) tileentity).getSchematic().getName()
					.replace("" + ((ClaimBlockTileEntity) tileentity).getSchematicPos().toLong(), ""));
			compound.setInteger("cost", ((ClaimBlockTileEntity) tileentity).getSchematic().getTotalMaterialCost());
			NBTTagList materials = new NBTTagList();
			int counter = 0;
			for (Entry<SimpleItemStack, Integer> material : ((ClaimBlockTileEntity) tileentity).getSchematic()
					.getRequiredMaterials().entrySet()) {
				if (counter > 5) {
					break;
				}
				NBTTagCompound mat_tag = new NBTTagCompound();
				mat_tag.setString("name", material.getKey().getVanillStack().getDisplayName());
				mat_tag.setInteger("total", material.getValue());
				materials.appendTag(mat_tag);
				counter++;
			}
			compound.setTag("com_mat", materials);
			is.setTagCompound(compound);
			return is;
		} else {
			return new ItemStack(SchematicMod.schematic);
		}
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	/**
	 * Used to determine ambient occlusion and culling when rebuilding chunks for
	 * render
	 */
	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isPassable(IBlockAccess worldIn, BlockPos pos) {
		return true;
	}

	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
			EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		TileEntity tileentity = worldIn.getTileEntity(pos);
		if ((tileentity instanceof ClaimBlockTileEntity)
				&& (((ClaimBlockTileEntity) tileentity).getSchematic() != null)) {
			if (playerIn.isSneaking()) {
				if (!worldIn.isRemote) {
					((ClaimBlockTileEntity) tileentity)
							.setRotation((((ClaimBlockTileEntity) tileentity).getRotation() + 1) % 4);
					((ClaimBlockTileEntity) tileentity).markForUpdate();
				}
			} else {
				if (((ClaimBlockTileEntity) tileentity).isActive()) {
					playerIn.openGui(SchematicMod.instance, GuiClaimBlock.ID, worldIn, pos.getX(), pos.getY(),
							pos.getZ());
				} else {
					((ClaimBlockTileEntity) tileentity).setActive(true);
				}
			}
		}
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void randomDisplayTick(IBlockState state, World worldIn, BlockPos pos, Random rand) {
		TileEntity tileentity = worldIn.getTileEntity(pos);
		if ((tileentity instanceof ClaimBlockTileEntity) && (((ClaimBlockTileEntity) tileentity).getSchematic() != null)
				&& ((ClaimBlockTileEntity) tileentity).isActive()) {
			Minecraft.getMinecraft().addScheduledTask(() -> {
				SchematicRenderingRegistry.addSchematic(((ClaimBlockTileEntity) tileentity).getSchematic(),
						((ClaimBlockTileEntity) tileentity).getSchematicPos(), state.getValue(BlockHorizontal.FACING),
						((ClaimBlockTileEntity) tileentity).getRotation());
			});
		}
	}

	@Override
	public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player,
			boolean willHarvest) {
		TileEntity tileentity = world.getTileEntity(pos);
		boolean result = true;
		if ((tileentity instanceof ClaimBlockTileEntity)) {
			if (!world.isRemote) {
				if (FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList()
						.canSendCommands(player.getGameProfile())
						&& (world.getPlayerEntityByUUID(((ClaimBlockTileEntity) tileentity).getPlacer()) != player)
						&& (player.getUniqueID() != ((ClaimBlockTileEntity) tileentity).getPlacer())) {
					result = false;
					player.sendMessage(new TextComponentString("Cannot break other players claimed plots"));
				} else {
					result = super.removedByPlayer(state, world, pos, player, willHarvest);
				}
			}

			if (result) {
				if (!world.isRemote) {
					if (willHarvest) {
						if (((ClaimBlockTileEntity) tileentity).getSchematic() != null) {
							ItemStack is = new ItemStack(SchematicMod.schematic);
							NBTTagCompound compound = new NBTTagCompound();
							((ClaimBlockTileEntity) tileentity).getSchematic().writeToNBT(compound);
							compound.setString("title", ((ClaimBlockTileEntity) tileentity).getSchematic().getName()
									.replace("" + ((ClaimBlockTileEntity) tileentity).getSchematicPos().toLong(), ""));
							compound.setInteger("cost",
									((ClaimBlockTileEntity) tileentity).getSchematic().getTotalMaterialCost());
							NBTTagList materials = new NBTTagList();
							int counter = 0;
							for (Entry<SimpleItemStack, Integer> material : ((ClaimBlockTileEntity) tileentity)
									.getSchematic().getRequiredMaterials().entrySet()) {
								if (counter > 5) {
									break;
								}
								NBTTagCompound mat_tag = new NBTTagCompound();
								mat_tag.setString("name", material.getKey().getVanillStack().getDisplayName());
								mat_tag.setInteger("total", material.getValue());
								materials.appendTag(mat_tag);
								counter++;
							}
							compound.setTag("com_mat", materials);
							is.setTagCompound(compound);
							// we have to do this here otherwise it spawns in an
							// empty schematic
							Block.spawnAsEntity(world, pos, is);

							for (Entry<SimpleItemStack, Integer> entry : ((ClaimBlockTileEntity) tileentity)
									.getSchematic().getRequiredMaterials().entrySet()) {
								int diff = entry.getValue() - ((ClaimBlockTileEntity) tileentity).getInventory()
										.getTotalMaterials().get(entry.getKey());
								ItemStack spawn_is = entry.getKey().getVanillStack();
								spawn_is.setCount(diff);
								InventoryHelper.spawnItemStack(world, pos.getX(), pos.getY(), pos.getZ(), spawn_is);
							}
						} else {
							Block.spawnAsEntity(world, pos, new ItemStack(SchematicMod.schematic));
						}
					}
				} else {
					if ((tileentity instanceof ClaimBlockTileEntity)
							&& (((ClaimBlockTileEntity) tileentity).getSchematic() != null)) {
						SchematicRenderingRegistry.removeSchematic(((ClaimBlockTileEntity) tileentity).getSchematic());
					}
				}
			}
		}
		return result;
	}
}