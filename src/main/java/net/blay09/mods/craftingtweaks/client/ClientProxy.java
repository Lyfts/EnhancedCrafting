package net.blay09.mods.craftingtweaks.client;

import java.util.List;

import net.blay09.mods.craftingtweaks.CommonProxy;
import net.blay09.mods.craftingtweaks.CraftingTweaks;
import net.blay09.mods.craftingtweaks.GuiClickEvent;
import net.blay09.mods.craftingtweaks.addon.HotkeyCheck;
import net.blay09.mods.craftingtweaks.api.TweakProvider;
import net.blay09.mods.craftingtweaks.net.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import org.lwjgl.input.Keyboard;

import com.google.common.collect.Lists;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

public class ClientProxy extends CommonProxy {

    private static final int HELLO_TIMEOUT = 20 * 10;
    private static final List<String> tooltipList = Lists.newArrayList();

    private int helloTimeout;
    private boolean isServerSide;

    private boolean wasRotated;
    private boolean wasCleared;
    private boolean wasBalanced;
    private boolean wasToggleButtons;
    private boolean wasCompressed;
    private boolean wasDecompressed;

    private final ClientProvider clientProvider = new ClientProvider();
    private final KeyBinding keyRotate = new KeyBinding(
        "key.craftingtweaks.rotate",
        Keyboard.KEY_R,
        "key.categories.craftingtweaks");
    private final KeyBinding keyBalance = new KeyBinding(
        "key.craftingtweaks.balance",
        Keyboard.KEY_B,
        "key.categories.craftingtweaks");
    private final KeyBinding keyClear = new KeyBinding(
        "key.craftingtweaks.clear",
        Keyboard.KEY_C,
        "key.categories.craftingtweaks");
    private final KeyBinding keyToggleButtons = new KeyBinding(
        "key.craftingtweaks.toggleButtons",
        0,
        "key.categories.craftingtweaks");
    private final KeyBinding keyCompress = new KeyBinding(
        "key.craftingtweaks.compress",
        Keyboard.KEY_K,
        "key.categories.craftingtweaks");
    private final KeyBinding keyDecompress = new KeyBinding(
        "key.craftingtweaks.decompress",
        0,
        "key.categories.craftingtweaks");
    private KeyBinding keyTransferStack;

    private Slot mouseSlot;
    private HotkeyCheck hotkeyCheck;

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance()
            .bus()
            .register(this);

        ClientRegistry.registerKeyBinding(keyRotate);
        ClientRegistry.registerKeyBinding(keyBalance);
        ClientRegistry.registerKeyBinding(keyClear);
        ClientRegistry.registerKeyBinding(keyToggleButtons);
        ClientRegistry.registerKeyBinding(keyCompress);
        ClientRegistry.registerKeyBinding(keyDecompress);
        keyTransferStack = Minecraft.getMinecraft().gameSettings.keyBindForward;
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {
        super.postInit(event);

        hotkeyCheck = (HotkeyCheck) event
            .buildSoftDependProxy("NotEnoughItems", "net.blay09.mods.craftingtweaks.addon.NEIHotkeyCheck");
    }

    @SubscribeEvent
    public void connectedToServer(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        helloTimeout = HELLO_TIMEOUT;
        isServerSide = false;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onGuiClick(GuiClickEvent event) {
        EntityPlayer entityPlayer = FMLClientHandler.instance()
            .getClientPlayerEntity();
        if (entityPlayer != null) {
            Container container = entityPlayer.openContainer;
            if (container != null) {
                Slot mouseSlot = event.gui instanceof GuiContainer
                    ? ((GuiContainer) event.gui).getSlotAtPosition(event.mouseX, event.mouseY)
                    : null;
                TweakProvider provider = CraftingTweaks.instance.getProvider(container);
                if (provider != null) {
                    if (keyTransferStack.getKeyCode() > 0 && Keyboard.isKeyDown(keyTransferStack.getKeyCode())) {
                        if (mouseSlot != null && mouseSlot.getHasStack()) {
                            List<Slot> transferSlots = Lists.newArrayList();
                            transferSlots.add(mouseSlot);
                            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT)) {
                                ItemStack mouseSlotStack = mouseSlot.getStack();
                                for (Slot obj : container.inventorySlots) {
                                    if (!obj.getHasStack() || mouseSlot == obj) {
                                        continue;
                                    }
                                    ItemStack slotStack = obj.getStack();
                                    if (slotStack.isItemEqual(mouseSlotStack)
                                        && ItemStack.areItemStackTagsEqual(slotStack, mouseSlotStack)) {
                                        transferSlots.add(obj);
                                    }
                                }
                            }
                            if (isServerSide) {
                                for (Slot slot : transferSlots) {
                                    NetworkHandler.instance.sendToServer(new MessageTransferStack(0, slot.slotNumber));
                                }
                            } else {
                                for (Slot slot : transferSlots) {
                                    clientProvider.transferIntoGrid(provider, entityPlayer, container, 0, slot);
                                }
                            }
                            event.setCanceled(true);
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        EntityPlayer entityPlayer = FMLClientHandler.instance()
            .getClientPlayerEntity();
        if (entityPlayer != null) {
            if (helloTimeout > 0) {
                helloTimeout--;
                if (helloTimeout <= 0) {
                    entityPlayer.addChatMessage(
                        new ChatComponentText(
                            "This server does not have Crafting Tweaks installed. Functionality may be limited."));
                    isServerSide = false;
                }
            }
            if ((hotkeyCheck != null && !hotkeyCheck.allowHotkeys())) {
                return;
            }
            boolean isShiftDown = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
            Container container = entityPlayer.openContainer;
            if (container != null) {
                GuiScreen guiScreen = Minecraft.getMinecraft().currentScreen;
                TweakProvider provider = CraftingTweaks.instance.getProvider(container);
                if (provider != null) {
                    CraftingTweaks.ModSupportState config = CraftingTweaks.instance
                        .getModSupportState(provider.getModId());
                    if (config == CraftingTweaks.ModSupportState.ENABLED
                        || config == CraftingTweaks.ModSupportState.HOTKEYS_ONLY) {
                        if (keyRotate.getKeyCode() > 0 && Keyboard.isKeyDown(keyRotate.getKeyCode())) {
                            if (!wasRotated) {
                                if (isServerSide) {
                                    NetworkHandler.instance.sendToServer(new MessageRotate(0, isShiftDown));
                                } else {
                                    clientProvider.rotateGrid(provider, entityPlayer, container, 0, isShiftDown);
                                }
                                wasRotated = true;
                            }
                        } else {
                            wasRotated = false;
                        }
                        if (keyClear.getKeyCode() > 0 && Keyboard.isKeyDown(keyClear.getKeyCode())) {
                            if (!wasCleared) {
                                if (isServerSide) {
                                    NetworkHandler.instance.sendToServer(new MessageClear(0, isShiftDown));
                                } else {
                                    clientProvider.clearGrid(provider, entityPlayer, container, 0, isShiftDown);
                                }
                                wasCleared = true;
                            }
                        } else {
                            wasCleared = false;
                        }
                        if (keyBalance.getKeyCode() > 0 && Keyboard.isKeyDown(keyBalance.getKeyCode())) {
                            if (!wasBalanced) {
                                if (isServerSide) {
                                    NetworkHandler.instance.sendToServer(new MessageBalance(0, isShiftDown));
                                } else {
                                    if (isShiftDown) {
                                        clientProvider.spreadGrid(provider, entityPlayer, container, 0);
                                    } else {
                                        clientProvider.balanceGrid(provider, entityPlayer, container, 0);
                                    }
                                }
                                wasBalanced = true;
                            } else {
                                wasBalanced = false;
                            }
                        }
                    }
                    if (guiScreen instanceof GuiContainer) {
                        if (keyToggleButtons.getKeyCode() > 0 && Keyboard.isKeyDown(keyToggleButtons.getKeyCode())) {
                            if (!wasToggleButtons) {
                                CraftingTweaks.hideButtons = !CraftingTweaks.hideButtons;
                                if (CraftingTweaks.hideButtons) {
                                    guiScreen.buttonList.removeIf(o -> o instanceof GuiTweakButton);
                                } else {
                                    initGui((GuiContainer) guiScreen);
                                }
                                CraftingTweaks.saveConfig();
                                wasToggleButtons = true;
                            } else {
                                wasToggleButtons = false;
                            }
                        }
                    }
                }
                if (guiScreen instanceof GuiContainer) {
                    if (keyCompress.getKeyCode() > 0 && Keyboard.isKeyDown(keyCompress.getKeyCode())) {
                        if (!wasCompressed) {
                            if (mouseSlot != null) {
                                boolean isDecompress = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)
                                    || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL);
                                if (isServerSide) {
                                    NetworkHandler.instance.sendToServer(
                                        new MessageCompress(mouseSlot.slotNumber, isDecompress, !isShiftDown));
                                } else if (isDecompress && provider != null) {
                                    clientProvider
                                        .decompress(provider, entityPlayer, container, mouseSlot, isShiftDown);
                                } else if (provider != null) {
                                    clientProvider.compress(provider, entityPlayer, container, mouseSlot, !isShiftDown);
                                }
                            }
                            wasCompressed = true;
                        }
                    } else {
                        wasCompressed = false;
                    }
                    if (keyDecompress.getKeyCode() > 0 && Keyboard.isKeyDown(keyDecompress.getKeyCode())) {
                        if (!wasDecompressed) {
                            if (mouseSlot != null) {
                                if (isServerSide) {
                                    NetworkHandler.instance
                                        .sendToServer(new MessageCompress(mouseSlot.slotNumber, true, isShiftDown));
                                } else if (provider != null) {
                                    clientProvider
                                        .decompress(provider, entityPlayer, container, mouseSlot, isShiftDown);
                                }
                            }
                            wasDecompressed = true;
                        }
                    } else {
                        wasDecompressed = false;
                    }
                }
            }
        }
    }

    private void initGui(GuiContainer guiContainer) {
        TweakProvider provider = CraftingTweaks.instance.getProvider(guiContainer.inventorySlots);
        if (provider != null) {
            CraftingTweaks.ModSupportState config = CraftingTweaks.instance.getModSupportState(provider.getModId());
            if (config == CraftingTweaks.ModSupportState.ENABLED
                || config == CraftingTweaks.ModSupportState.BUTTONS_ONLY) {
                provider.initGui(guiContainer, guiContainer.buttonList);
            }
        }
    }

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!CraftingTweaks.hideButtons) {
            if (event.gui instanceof GuiContainer) {
                initGui((GuiContainer) event.gui);
            }
        }
    }

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (event.gui == null) {
            // WAILA somehow breaks DrawScreenEvent when exiting its menu
            return;
        }
        if (event.gui instanceof GuiContainer guiContainer) {
            mouseSlot = guiContainer.getSlotAtPosition(event.mouseX, event.mouseY);
        } else {
            mouseSlot = null;
        }
        if (!CraftingTweaks.hideButtonTooltips) {
            tooltipList.clear();
            for (GuiButton btn : event.gui.buttonList) {
                if (btn instanceof ITooltipProvider tooltipProvider && btn.func_146115_a()) {
                    tooltipProvider.addInformation(tooltipList);
                    break;
                }
            }
            if (!tooltipList.isEmpty()) {
                event.gui.func_146283_a(tooltipList, event.mouseX, event.mouseY);
            }
        }
    }

    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (event.button instanceof GuiTweakButton btn) {
            event.button.func_146113_a(
                Minecraft.getMinecraft()
                    .getSoundHandler());
            EntityPlayer entityPlayer = FMLClientHandler.instance()
                .getClientPlayerEntity();
            Container container = entityPlayer.openContainer;
            TweakProvider provider = CraftingTweaks.instance.getProvider(container);
            boolean isShiftDown = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
            switch (btn.getTweakOption()) {
                case Rotate:
                    if (isServerSide) {
                        NetworkHandler.instance.sendToServer(new MessageRotate(btn.getTweakId(), isShiftDown));
                    } else {
                        clientProvider.rotateGrid(provider, entityPlayer, container, btn.getTweakId(), isShiftDown);
                    }
                    event.setCanceled(true);
                    break;
                case Balance:
                    if (isServerSide) {
                        NetworkHandler.instance.sendToServer(new MessageBalance(btn.getTweakId(), isShiftDown));
                    } else {
                        if (isShiftDown) {
                            clientProvider.spreadGrid(provider, entityPlayer, container, btn.getTweakId());
                        } else {
                            clientProvider.balanceGrid(provider, entityPlayer, container, btn.getTweakId());
                        }
                    }
                    event.setCanceled(true);
                    break;
                case Clear:
                    if (isServerSide) {
                        NetworkHandler.instance.sendToServer(new MessageClear(btn.getTweakId(), isShiftDown));
                    } else {
                        clientProvider.clearGrid(provider, entityPlayer, container, btn.getTweakId(), isShiftDown);
                    }
                    event.setCanceled(true);
                    break;
            }
        }
    }

    @Override
    public void receivedHello(EntityPlayer entityPlayer) {
        helloTimeout = 0;
        isServerSide = true;
    }
}
