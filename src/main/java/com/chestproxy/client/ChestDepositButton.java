package com.chestproxy.client;

import com.chestproxy.ChestProxyMod;
import com.chestproxy.config.ChestProxyConfig;
import com.chestproxy.util.ChestHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerWorkbench;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

@SideOnly(Side.CLIENT)
public class ChestDepositButton {

    private static final int BUTTON_ID = 54321;
    private static final ItemStack CHEST_ICON = new ItemStack(Blocks.CHEST);

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.getGui() instanceof GuiContainer && ((GuiContainer) event.getGui()).inventorySlots instanceof ContainerWorkbench) {
            GuiContainer gui = (GuiContainer) event.getGui();
            int x = gui.getGuiLeft() + 152;
            int y = gui.getGuiTop() + 16;
            event.getButtonList().add(new GuiButton(BUTTON_ID, x, y, 18, 18, "") {
                @Override
                public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
                    if (!visible) return;
                    hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
                    GlStateManager.enableDepth();
                    RenderHelper.enableGUIStandardItemLighting();
                    mc.getRenderItem().renderItemAndEffectIntoGUI(CHEST_ICON, x + 1, y + 1);
                    RenderHelper.disableStandardItemLighting();
                    GlStateManager.disableDepth();
                    if (hovered) {
                        drawGradientRect(x, y, x + width, y + height, 0x80FFFFFF, 0x80FFFFFF);
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (event.getButton().id == BUTTON_ID) {
            event.setCanceled(true);
            depositToChests();
        }
    }

    private void depositToChests() {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return;

        World world = player.world;
        if (world.isRemote) {
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            if (server != null) {
                world = server.getWorld(player.dimension);
            }
        }

        List<IInventory> chests = ChestHelper.findNearbyInventories(world, player.getPosition());
        ChestProxyMod.LOG.info("Deposit button: found {} chests", chests.size());

        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            int remaining = stack.getCount();

            for (IInventory chest : chests) {
                if (remaining <= 0) break;
                for (int j = 0; j < chest.getSizeInventory(); j++) {
                    if (remaining <= 0) break;
                    ItemStack chestStack = chest.getStackInSlot(j);
                    if (chestStack.isEmpty()) continue;
                    if (ChestHelper.canMerge(chestStack, stack)) {
                        int space = Math.min(chestStack.getMaxStackSize() - chestStack.getCount(), remaining);
                        if (space > 0) {
                            chestStack.grow(space);
                            remaining -= space;
                            chest.markDirty();
                        }
                    }
                }
            }

            for (IInventory chest : chests) {
                if (remaining <= 0) break;
                for (int j = 0; j < chest.getSizeInventory(); j++) {
                    if (remaining <= 0) break;
                    ItemStack chestStack = chest.getStackInSlot(j);
                    if (chestStack.isEmpty()) {
                        int toPut = Math.min(remaining, stack.getMaxStackSize());
                        ItemStack putStack = stack.copy();
                        putStack.setCount(toPut);
                        chest.setInventorySlotContents(j, putStack);
                        remaining -= toPut;
                        chest.markDirty();
                    }
                }
            }

            if (remaining <= 0) {
                player.inventory.setInventorySlotContents(i, ItemStack.EMPTY);
            } else {
                stack.setCount(remaining);
                player.inventory.setInventorySlotContents(i, stack);
            }
        }

        player.inventory.markDirty();
        ChestProxyMod.LOG.info("Deposit complete");
    }
}
