package com.mowmaster.ensorcelled.enchantments.handlers;

import com.mowmaster.ensorcelled.enchantments.EnchantmentRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.item.PickaxeItem;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber
public class HandlerAOEMiner {

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void AOEMining(BlockEvent.BreakEvent event)
    {
        PlayerEntity player = event.getPlayer();
        if (!(player instanceof ServerPlayerEntity)) {
            return;
        }

        World world = event.getPlayer().getEntityWorld();
        if(!world.isRemote)
        {
            if(player != null)
            {
                ItemStack getTool = player.getHeldItem(player.getActiveHand());
                if(getTool.getItem() instanceof PickaxeItem || getTool.getToolTypes().contains(ToolType.PICKAXE))
                {
                    if(EnchantmentHelper.getEnchantmentLevel(EnchantmentRegistry.MINER,player.getHeldItem(player.getActiveHand()))!=0)
                    {
                        BlockPos pos = event.getPos();
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

                            //Build Work Queue
                            List<BlockPos> workQueue = new ArrayList<>();
                            for(int c=zmin;c<=zmax;c++) {
                                for (int a = xmin; a <= xmax; a++)
                                {
                                    for (int b = ymin; b <= ymax; b++)
                                    {
                                        workQueue.add(pos.add(a,b,c));
                                    }
                                }
                            }

                            if(player.isSneaking()) {}
                            else
                            {
                                for(int i=0;i<workQueue.size();i++)
                                {
                                    //Should avoid breaking any TE's
                                    if(world.getTileEntity(workQueue.get(i)) instanceof TileEntity)continue;

                                    BlockState blockToBreak = world.getBlockState(workQueue.get(i));
                                    if (ForgeEventFactory.doPlayerHarvestCheck(player,blockToBreak,true) && !blockToBreak.getBlock().isAir(blockToBreak, world, workQueue.get(i)) && !(blockToBreak.getBlock() instanceof IFluidBlock || blockToBreak.getBlock() instanceof FlowingFluidBlock) && blockToBreak.getBlockHardness(world, workQueue.get(i)) != -1.0F) {
System.out.print("WORKING");
                                        int maxdur = tool.getMaxDamage();
                                        int damdone = tool.getDamage();
                                        if ((Math.subtractExact(maxdur,damdone)>=0)) {
                                            player.getHeldItem(player.getActiveHand()).damageItem(1,player,playerEntity -> {});
                                            if(blockToBreak.canHarvestBlock(world,pos,player))
                                            {
                                                blockToBreak.getBlock().harvestBlock(world, player, workQueue.get(i), blockToBreak, null, player.getHeldItemMainhand());
                                                blockToBreak.getBlock().onBlockHarvested(world, workQueue.get(i), blockToBreak, player);
                                                int expdrop = blockToBreak.getBlock().getExpDrop(blockToBreak,world,workQueue.get(i),
                                                        (EnchantmentHelper.getEnchantments(player.getHeldItemMainhand()).containsKey(Enchantments.FORTUNE))?(EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE,player.getHeldItemMainhand())):(0),
                                                        (EnchantmentHelper.getEnchantments(player.getHeldItemMainhand()).containsKey(Enchantments.SILK_TOUCH))?(EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH,player.getHeldItemMainhand())):(0));
                                                //drop xp above player
                                                if(expdrop>0)blockToBreak.getBlock().dropXpOnBlockBreak((ServerWorld)world,player.getPosition().add(0,1,0),expdrop);
                                                world.removeBlock(workQueue.get(i), false);
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
