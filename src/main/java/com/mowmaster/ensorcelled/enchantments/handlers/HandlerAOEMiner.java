package com.mowmaster.ensorcelled.enchantments.handlers;

import com.mowmaster.ensorcelled.enchantments.EnchantmentRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.Direction;
import net.minecraft.util.math.*;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class HandlerAOEMiner {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void AOEMining(BlockEvent.BreakEvent event)
    {

        World world = event.getPlayer().getEntityWorld();
        PlayerEntity player = event.getPlayer();
        BlockPos pos = event.getPos();
        Block block = event.getState().getBlock();
        BlockState state = world.getBlockState(pos);

        if(!world.isRemote)
        {
            if(player != null)
            {
                if(EnchantmentHelper.getEnchantmentLevel(EnchantmentRegistry.MINER,player.getHeldItem(player.getActiveHand()))!=0)
                {
                    ItemStack tool = player.getHeldItem(player.getActiveHand());
                    if (player.swingingHand == null) {return;}

                    // Used this to figure out a useable RayTraceResult: https://github.com/baileyholl/Ars-Nouveau/blob/0cdb8fbb483ca0f945de26c633955cfb1c05c925/src/main/java/com/hollingsworth/arsnouveau/common/event/EventHandler.java#L99
                    //RayTraceResult result = player.pick(player.getLookVec().length(),0,false); results in MISS type returns
                    RayTraceResult result = player.pick(5,0,false);
                    if(result != null)
                    {
                        //Setup a default because i like redundency?
                        Direction facing = Direction.UP;

                        if(result.getType() == RayTraceResult.Type.BLOCK)
                        {
                            ItemUseContext context = new ItemUseContext(player,player.getActiveHand(),((BlockRayTraceResult) result));
                            BlockRayTraceResult res = new BlockRayTraceResult(context.getHitVec(), context.getFace(), context.getPos(), false);
                            facing = res.getFace();
                        }

                        //String type = player.getHeldItem(player.getActiveHand()).getItem().getToolClasses(player.getHeldItem(player.getActiveHand())).toString();
                        //int damage = player.getHeldItem(player.getActiveHand()).getDamage();
                        int lvl = EnchantmentHelper.getEnchantmentLevel(EnchantmentRegistry.MINER,player.getHeldItem(player.getActiveHand()));
                        //float blockHardness = world.getBlockState(pos).getBlockHardness(world,pos);
                        int zmin=0;
                        int zmax=0;
                        int xmin=0;
                        int xmax=0;
                        int ymin=0;
                        int ymax=0;

                        if(player.isSwimming() || !player.isOnGround())
                        {
                            if(facing.equals(Direction.DOWN) || facing.equals(Direction.UP)) {zmin=-lvl;zmax=+lvl;xmin=-lvl;xmax=+lvl;ymin=0;ymax=0;}
                            else if(facing.equals(Direction.WEST) || facing.equals(Direction.EAST)) {zmin=-lvl;zmax=+lvl;xmin=0;xmax=0;ymin=-lvl;ymax=+lvl;}
                            else if(facing.equals(Direction.NORTH) || facing.equals(Direction.SOUTH)) {zmin=0;zmax=0;xmin=-lvl;xmax=+lvl;ymin=-lvl;ymax=+lvl;}
                        }
                        else
                        {
                            if(facing.equals(Direction.DOWN) || facing.equals(Direction.UP)) {zmin=-lvl;zmax=+lvl;xmin=-lvl;xmax=+lvl;ymin=0;ymax=0;}
                            else if(facing.equals(Direction.WEST) || facing.equals(Direction.EAST)) {zmin=-lvl;zmax=+lvl;xmin=0;xmax=0;ymin=-1;ymax=+((2*lvl)-1);}
                            else if(facing.equals(Direction.NORTH) || facing.equals(Direction.SOUTH)) {zmin=0;zmax=0;xmin=-lvl;xmax=+lvl;ymin=-1;ymax=+((2*lvl)-1);}
                        }

                        if(player.isSneaking()) {}
                        else
                        {
                            for(int c=zmin;c<=zmax;c++) {
                                for (int a = xmin; a <= xmax; a++)
                                    for (int b = ymin; b <= ymax; b++)
                                    {
                                        BlockPos posOfBlock = pos.add(a,b,c);
                                        BlockState blockToBreak = world.getBlockState(posOfBlock);
                                        if (ForgeEventFactory.doPlayerHarvestCheck(player,blockToBreak,true) && !blockToBreak.getBlock().isAir(blockToBreak, world, posOfBlock) && !(blockToBreak.getBlock() instanceof IFluidBlock || blockToBreak.getBlock() instanceof FlowingFluidBlock) && blockToBreak.getBlockHardness(world, posOfBlock) != -1.0F) {

                                            int maxdur = tool.getMaxDamage();
                                            int damdone = tool.getDamage();
                                            if ((Math.subtractExact(maxdur,damdone)>=0)) {
                                                player.getHeldItem(player.getActiveHand()).damageItem(1,player,playerEntity -> {});
                                                if(blockToBreak.canHarvestBlock(world,pos,player))
                                                {
                                                    blockToBreak.getBlock().harvestBlock(world, player, posOfBlock, blockToBreak, null, player.getHeldItemMainhand());
                                                    blockToBreak.getBlock().onBlockHarvested(world, posOfBlock, blockToBreak, player);
                                                    world.removeBlock(posOfBlock, false);
                                                }
                                            }
                                        }
                                    }
                            }
                        }
                    }
                }
            }
        }
    }
}
