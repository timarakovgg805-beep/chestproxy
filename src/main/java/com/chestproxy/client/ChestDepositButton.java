package com.chestproxy.client;

import com.chestproxy.ChestProxyMod;
import com.chestproxy.config.ChestProxyConfig;
import com.chestproxy.util.ChestHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
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
        if (event.getGui() instanceof GuiInventory) {
            GuiContainer gui = (GuiContainer) event.getGui();
            int x = gui.getGuiLeft() + gui.getXSize() - 18;
            int y = gui.getGuiTop() + gui.getYSize() + 2;
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

    public static void depositToChests() {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player == null) return;

        World world = player.world;
        EntityPlayerMP serverPlayer = null;
        if (world.isRemote) {
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            if (server != null) {
                world = server.getWorld(player.dimension);
                serverPlayer = server.getPlayerList().getPlayerByUUID(player.getUniqueID());
            }
        }

        List<IInventory> chests = ChestHelper.findNearbyInventories(world, player.getPosition());
        ChestProxyMod.info("Deposit button: found {} chests", chests.size());

        int minSlot = Math.max(0, ChestProxyConfig.depositSlotMin);
        int maxSlot = Math.min(35, ChestProxyConfig.depositSlotMax);
        for (int i = minSlot; i <= maxSlot; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            boolean typeExists = false;
            for (IInventory chest : chests) {
                for (int j = 0; j < chest.getSizeInventory(); j++) {
                    ItemStack chestStack = chest.getStackInSlot(j);
                    if (!chestStack.isEmpty() && ChestHelper.canMerge(chestStack, stack)) {
                        typeExists = true;
                        break;
                    }
                }
                if (typeExists) break;
            }
            if (!typeExists) continue;

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

            if (remaining < stack.getCount()) {
                ItemStack newStack;
                if (remaining <= 0) {
                    newStack = ItemStack.EMPTY;
                } else {
                    stack.setCount(remaining);
                    newStack = stack;
                }
                player.inventory.setInventorySlotContents(i, newStack);
                if (serverPlayer != null) {
                    serverPlayer.inventory.setInventorySlotContents(i, newStack.copy());
                }
            }
        }

        player.inventory.markDirty();
        if (serverPlayer != null) {
            serverPlayer.inventory.markDirty();
            if (serverPlayer.openContainer != null) {
                serverPlayer.openContainer.detectAndSendChanges();
            }
        }
        ChestProxyMod.info("Deposit complete");
    }
}
