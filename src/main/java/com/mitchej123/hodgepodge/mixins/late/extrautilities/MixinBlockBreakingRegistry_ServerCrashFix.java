package com.mitchej123.hodgepodge.mixins.late.extrautilities;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockLiquid;
import net.minecraft.init.Blocks;
import net.minecraftforge.fluids.IFluidBlock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import com.rwtema.extrautils.LogHelper;
import com.rwtema.extrautils.tileentity.enderquarry.BlockBreakingRegistry;
import com.rwtema.extrautils.tileentity.enderquarry.BlockDummy;

@Mixin(value = BlockBreakingRegistry.class, remap = false)
public abstract class MixinBlockBreakingRegistry_ServerCrashFix {

    @Shadow
    public static Set<String> methodNames = null;

    @Shadow
    public static HashMap<Block, BlockBreakingRegistry.entry> entries = new HashMap();

    @Shadow
    public abstract boolean hasSpecialBreaking(Class clazz);

    /**
     * @author lynxx131
     * @reason Original method calls client side code on the server leading to a crash on startup
     */
    @Overwrite
    public void setupBreaking() {
        if (methodNames == null) {
            methodNames = new HashSet();

            for (Method m : BlockDummy.class.getDeclaredMethods()) {
                methodNames.add(m.getName());
            }

            for (Object aBlockRegistry : Block.blockRegistry) {
                entries.put((Block) aBlockRegistry, new BlockBreakingRegistry.entry());
            }

            ((BlockBreakingRegistry.entry) entries.get(Blocks.torch)).blackList = true;

            for (Object aBlockRegistry : Block.blockRegistry) {
                Block block = (Block) aBlockRegistry;
                BlockBreakingRegistry.entry e = (BlockBreakingRegistry.entry) entries.get(block);
                String name = block.getClass().getName();
                if (block.getUnlocalizedName() != null) {
                    name = block.getUnlocalizedName();
                }

                try {
                    name = Block.blockRegistry.getNameForObject(block);
                } catch (Exception err) {
                    LogHelper.error("Error getting name for block " + name, new Object[0]);
                    err.printStackTrace();
                }

                // Removed a try/catch here block that was calling block.getRenderType() due to it causing server
                // crashes
                // instead we just check that the block is an instance of BlockFence
                e.isFence = block instanceof BlockFence;

                if (block instanceof BlockLiquid || block instanceof IFluidBlock) {
                    e.blackList = true;
                    e.isFluid = true;
                }

                e.isSpecial = this.hasSpecialBreaking(block.getClass());
            }

        }
    }

}
