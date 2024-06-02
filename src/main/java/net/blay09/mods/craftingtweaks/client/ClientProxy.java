package net.blay09.mods.craftingtweaks.client;

import static net.blay09.mods.craftingtweaks.EnumTweakAction.Balance;
import static net.blay09.mods.craftingtweaks.EnumTweakAction.Clear;
import static net.blay09.mods.craftingtweaks.EnumTweakAction.Compress;
import static net.blay09.mods.craftingtweaks.EnumTweakAction.Decompress;
import static net.blay09.mods.craftingtweaks.EnumTweakAction.Rotate;
import static net.blay09.mods.craftingtweaks.EnumTweakAction.Transfer;

import java.util.List;

import net.blay09.mods.craftingtweaks.CommonProxy;
import net.blay09.mods.craftingtweaks.CraftingTweaks;
import net.blay09.mods.craftingtweaks.EnumTweakAction;
import net.blay09.mods.craftingtweaks.addon.HotkeyCheck;
import net.blay09.mods.craftingtweaks.api.TweakProvider;
import net.blay09.mods.craftingtweaks.net.MessageBalance;
import net.blay09.mods.craftingtweaks.net.MessageClear;
import net.blay09.mods.craftingtweaks.net.MessageCompress;
import net.blay09.mods.craftingtweaks.net.MessageRotate;
import net.blay09.mods.craftingtweaks.net.MessageTransferStack;
import net.blay09.mods.craftingtweaks.net.NetworkHandler;
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
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

public class ClientProxy extends CommonProxy {

    private static final int HELLO_TIMEOUT = 20 * 10;
    private static final List<String> tooltipList = Lists.newArrayList();

    private int helloTimeout;
    private boolean isServerSide;
    private int previousKey = 0;

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

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        EntityPlayer entityPlayer = Minecraft.getMinecraft().thePlayer;
        if(entityPlayer == null) return;
        if (helloTimeout > 0) {
            helloTimeout--;
            if (helloTimeout <= 0) {
                entityPlayer.addChatMessage(
                    new ChatComponentText(
                        "This server does not have Crafting Tweaks installed. Functionality may be limited."));
                isServerSide = false;
            }
        }
        Container container = entityPlayer.openContainer;
        if (hotkeyCheck != null && !hotkeyCheck.allowHotkeys() || container == null) return;
        GuiScreen guiScreen = Minecraft.getMinecraft().currentScreen;
        TweakProvider provider = CraftingTweaks.instance.getProvider(container);
        if (provider == null) return;
        CraftingTweaks.ModSupportState config = CraftingTweaks.instance.getModSupportState(provider.getModId());
        if (config == CraftingTweaks.ModSupportState.ENABLED || config == CraftingTweaks.ModSupportState.HOTKEYS_ONLY) {
            if (handleKeyInput(keyRotate)) {
                doAction(Rotate, provider, container, 0);
            }

            if (handleKeyInput(keyClear)) {
                doAction(Clear, provider, container, 0);
            }

            if (handleKeyInput(keyBalance)) {
                doAction(Balance, provider, container, 0);
            }
        }

        if (guiScreen instanceof GuiContainer guiContainer) {
            if (handleKeyInput(keyToggleButtons)) {
                CraftingTweaks.hideButtons = !CraftingTweaks.hideButtons;
                if (CraftingTweaks.hideButtons) {
                    guiContainer.buttonList.removeIf(guiButton -> guiButton instanceof GuiTweakButton);
                } else {
                    initGui(guiContainer);
                }
                CraftingTweaks.saveConfig();
            }

            if (mouseSlot == null) return;
            if (handleKeyInput(keyCompress)) {
                doAction(Compress, provider, container, 0);
            }

            if (handleKeyInput(keyDecompress)) {
                doAction(Decompress, provider, container, 0);
            }

            if (Keyboard.isKeyDown(keyTransferStack.getKeyCode())) {
                doAction(Transfer, provider, container, 0);
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
            if (event.gui instanceof GuiContainer container) {
                initGui(container);
            }
        }
    }

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (event.gui == null) {
            // WAILA somehow breaks DrawScreenEvent when exiting its menu
            return;
        }
        if (event.gui instanceof GuiContainer container) {
            mouseSlot = container.getSlotAtPosition(event.mouseX, event.mouseY);
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
            switch (btn.getTweakOption()) {
                case Rotate:
                    doAction(Rotate, provider, container, btn.getTweakId());
                    event.setCanceled(true);
                    break;
                case Balance:
                    doAction(Balance, provider, container, btn.getTweakId());
                    event.setCanceled(true);
                    break;
                case Clear:
                    doAction(Clear, provider, container, btn.getTweakId());
                    event.setCanceled(true);
                    break;
            }
        }
    }

    public void doAction(EnumTweakAction action, TweakProvider provider, Container container, int id) {
        if (container == null || provider == null) return;
        boolean isShiftDown = GuiScreen.isShiftKeyDown();
        boolean isCtrlDown = GuiScreen.isCtrlKeyDown();
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (isServerSide) {
            switch (action) {
                case Rotate -> NetworkHandler.instance.sendToServer(new MessageRotate(id, isShiftDown));
                case Balance -> NetworkHandler.instance.sendToServer(new MessageBalance(id, isShiftDown));
                case Clear -> NetworkHandler.instance.sendToServer(new MessageClear(id, isShiftDown));
                case Compress -> NetworkHandler.instance
                    .sendToServer(new MessageCompress(mouseSlot.slotNumber, isCtrlDown, !isShiftDown));
                case Decompress -> NetworkHandler.instance
                    .sendToServer(new MessageCompress(mouseSlot.slotNumber, true, isShiftDown));
                case Transfer -> doTransfer(provider, container);
            }
        } else {
            switch (action) {
                case Rotate -> clientProvider.rotateGrid(provider, player, container, id, isShiftDown);
                case Balance -> {
                    if (isShiftDown) {
                        clientProvider.spreadGrid(provider, player, container, id);
                    } else {
                        clientProvider.balanceGrid(provider, player, container, id);
                    }
                }
                case Clear -> clientProvider.clearGrid(provider, player, container, id, isShiftDown);
                case Compress -> clientProvider
                    .compress(provider, player, container, mouseSlot, !isShiftDown, isCtrlDown);
                case Decompress -> clientProvider.decompress(provider, player, container, mouseSlot, isShiftDown);
                case Transfer -> doTransfer(provider, container);
            }
        }
    }

    public void doTransfer(TweakProvider provider, Container container) {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (mouseSlot.getHasStack()) {
            List<Slot> transferSlots = Lists.newArrayList();
            transferSlots.add(mouseSlot);
            if (GuiScreen.isShiftKeyDown()) {
                ItemStack mouseSlotStack = mouseSlot.getStack();
                for (Slot slot : container.inventorySlots) {
                    if (!slot.getHasStack() || mouseSlot == slot) {
                        continue;
                    }
                    ItemStack slotStack = slot.getStack();
                    if (slotStack.isItemEqual(mouseSlotStack)
                        && ItemStack.areItemStackTagsEqual(slotStack, mouseSlotStack)) {
                        transferSlots.add(slot);
                    }
                }
            }
            for (Slot slot : transferSlots) {
                if (isServerSide) {
                    NetworkHandler.instance.sendToServer(new MessageTransferStack(0, slot.slotNumber));
                } else {
                    clientProvider.transferIntoGrid(provider, player, container, 0, slot);
                }
            }
        }
    }

    public boolean handleKeyInput(KeyBinding key) {
        int keyCode = key.getKeyCode();
        if (!Keyboard.isKeyDown(keyCode)) {
            if (previousKey == keyCode) {
                previousKey = 0;
            }
            return false;
        }

        if (previousKey == keyCode) {
            return false;
        }

        previousKey = keyCode;
        return true;
    }

    @Override
    public void receivedHello(EntityPlayer entityPlayer) {
        helloTimeout = 0;
        isServerSide = true;
    }
}
