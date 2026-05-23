package com.chestproxy.util;

import com.chestproxy.ChestProxyMod;
import com.chestproxy.config.ChestProxyConfig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class ChestHelper {

    public static List<IInventory> findNearbyInventories(World world, BlockPos pos) {
        List<IInventory> inventories = new ArrayList<>();
        int radius = ChestProxyConfig.getSearchRadius();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    BlockPos checkPos = pos.add(dx, dy, dz);
                    TileEntity tile = world.getTileEntity(checkPos);
                    if (tile instanceof IInventory && !tile.isInvalid()) {
                        inventories.add((IInventory) tile);
                    }
                }
            }
        }

        ChestProxyMod.info("Found {} inventories near {}", inventories.size(), pos);
        return inventories;
    }

    public static boolean canMerge(ItemStack a, ItemStack b) {
        if (a.isEmpty() || b.isEmpty()) return false;

        if (a.getItem() == b.getItem()) {
            if (a.getItemDamage() == b.getItemDamage() || a.getItemDamage() == OreDictionary.WILDCARD_VALUE || b.getItemDamage() == OreDictionary.WILDCARD_VALUE) {
                if (a.areCapsCompatible(b) && ItemStack.areItemStackTagsEqual(a, b)) {
                    return true;
                }
            }
            // For damageable items (tools, armour), treat different damage as same type
            if (a.isItemStackDamageable()) {
                if (a.areCapsCompatible(b) && ItemStack.areItemStackTagsEqual(a, b)) {
                    return true;
                }
            }
        }

        int[] idsA = OreDictionary.getOreIDs(a);
        int[] idsB = OreDictionary.getOreIDs(b);
        if (idsA.length > 0 && idsB.length > 0) {
            for (int idA : idsA) {
                for (int idB : idsB) {
                    if (idA == idB) return true;
                }
            }
        }

        return false;
    }

    public static int countInInventory(EntityPlayer player, ItemStack stack) {
        int count = 0;
        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack invStack = player.inventory.getStackInSlot(i);
            if (!invStack.isEmpty() && canMerge(invStack, stack)) {
                count += invStack.getCount();
            }
        }
        return count;
    }

    public static int countInChests(List<IInventory> chests, ItemStack stack) {
        int count = 0;
        for (IInventory inv : chests) {
            for (int i = 0; i < inv.getSizeInventory(); i++) {
                ItemStack s = inv.getStackInSlot(i);
                if (!s.isEmpty() && canMerge(s, stack)) {
                    count += s.getCount();
                }
            }
        }
        return count;
    }

    @Nullable
    public static ItemStack extractFromChests(List<IInventory> chests, ItemStack template, int needed) {
        int remaining = needed;
        ItemStack result = ItemStack.EMPTY;

        for (IInventory chest : chests) {
            if (remaining <= 0) break;
            for (int i = 0; i < chest.getSizeInventory(); i++) {
                if (remaining <= 0) break;
                ItemStack chestStack = chest.getStackInSlot(i);
                if (chestStack.isEmpty()) continue;
                if (!canMerge(chestStack, template)) continue;

                int toTake = Math.min(chestStack.getCount(), remaining);
                ItemStack taken = chestStack.splitStack(toTake);
                if (result.isEmpty()) {
                    result = taken;
                } else {
                    result.grow(toTake);
                }
                if (chestStack.isEmpty()) {
                    chest.setInventorySlotContents(i, ItemStack.EMPTY);
                }
                chest.markDirty();
                remaining -= toTake;
            }
        }

        return result.isEmpty() ? null : result;
    }
}
