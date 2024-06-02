package net.blay09.mods.craftingtweaks;

import java.util.List;

import net.blay09.mods.craftingtweaks.api.CraftingTweaksAPI;
import net.blay09.mods.craftingtweaks.api.DefaultProviderV2;
import net.blay09.mods.craftingtweaks.api.RotationHandler;
import net.blay09.mods.craftingtweaks.api.SimpleTweakProvider;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class SimpleTweakProviderImpl implements SimpleTweakProvider {

    private final RotationHandler smallRotationHandler = new RotationHandler() {

        @Override
        public boolean ignoreSlotId(int slotId) {
            return false;
        }

        @Override
        public int rotateSlotId(int slotId, boolean counterClockwise) {
            if (!counterClockwise) {
                switch (slotId) {
                    case 0:
                        return 1;
                    case 1:
                        return 3;
                    case 2:
                        return 0;
                    case 3:
                        return 2;
                }
            } else {
                switch (slotId) {
                    case 1:
                        return 0;
                    case 3:
                        return 1;
                    case 0:
                        return 2;
                    case 2:
                        return 3;
                }
            }
            return 0;
        }
    };

    public static class TweakSettings {

        public final boolean enabled;
        public final boolean showButton;
        public final int buttonX;
        public final int buttonY;

        public TweakSettings(boolean enabled, boolean showButton, int buttonX, int buttonY) {
            this.enabled = enabled;
            this.showButton = showButton;
            this.buttonX = buttonX;
            this.buttonY = buttonY;
        }
    }

    private final String modid;
    private final DefaultProviderV2 defaultProvider = CraftingTweaksAPI.createDefaultProviderV2();
    private int gridSlotNumber = 1;
    private int gridSize = 9;
    private boolean hideButtons;
    private boolean phantomItems;
    private TweakSettings tweakRotate = new TweakSettings(true, true, -16, 16);
    private TweakSettings tweakBalance = new TweakSettings(true, true, -16, 16 + 18);
    private TweakSettings tweakClear = new TweakSettings(true, true, -16, 16 + 18 + 18);
    private EnumFacing alignToGrid;

    public SimpleTweakProviderImpl(String modid) {
        this.modid = modid;
    }

    public void setAlignToGrid(EnumFacing direction) {
        this.alignToGrid = direction;
    }

    @Override
    public void setTweakRotate(boolean enabled, boolean showButton, int x, int y) {
        tweakRotate = new TweakSettings(enabled, showButton, x, y);
    }

    @Override
    public void setTweakBalance(boolean enabled, boolean showButton, int x, int y) {
        tweakBalance = new TweakSettings(enabled, showButton, x, y);
    }

    @Override
    public void setTweakClear(boolean enabled, boolean showButton, int x, int y) {
        tweakClear = new TweakSettings(enabled, showButton, x, y);
    }

    @Override
    public void setPhantomItems(boolean phantomItems) {
        this.phantomItems = phantomItems;
    }

    @Override
    public void setGrid(int slotNumber, int size) {
        gridSlotNumber = slotNumber;
        gridSize = size;
    }

    @Override
    public void setHideButtons(boolean hideButtons) {
        this.hideButtons = hideButtons;
    }

    @Override
    public String getModId() {
        return modid;
    }

    @Override
    public boolean load() {
        return true;
    }

    @Override
    public void clearGrid(EntityPlayer entityPlayer, Container container, int id, boolean forced) {
        if (tweakClear.enabled) {
            defaultProvider.clearGrid(this, id, entityPlayer, container, phantomItems, forced);
        }
    }

    @Override
    public void rotateGrid(EntityPlayer entityPlayer, Container container, int id, boolean counterClockwise) {
        if (tweakRotate.enabled) {
            defaultProvider.rotateGrid(
                this,
                id,
                entityPlayer,
                container,
                getCraftingGridSize(entityPlayer, container, id) == 4 ? smallRotationHandler
                    : defaultProvider.getRotationHandler(),
                counterClockwise);
        }
    }

    @Override
    public void balanceGrid(EntityPlayer entityPlayer, Container container, int id) {
        if (tweakBalance.enabled) {
            defaultProvider.balanceGrid(this, id, entityPlayer, container);
        }
    }

    @Override
    public void spreadGrid(EntityPlayer entityPlayer, Container container, int id) {
        if (tweakBalance.enabled) {
            defaultProvider.spreadGrid(this, id, entityPlayer, container);
        }
    }

    @Override
    public boolean canTransferFrom(EntityPlayer entityPlayer, Container container, int id, Slot sourceSlot) {
        return defaultProvider.canTransferFrom(entityPlayer, container, sourceSlot);
    }

    @Override
    public boolean transferIntoGrid(EntityPlayer entityPlayer, Container container, int id, Slot sourceSlot) {
        return defaultProvider.transferIntoGrid(this, id, entityPlayer, container, sourceSlot);
    }

    @Override
    public ItemStack putIntoGrid(EntityPlayer entityPlayer, Container container, int id, ItemStack itemStack,
        int index) {
        return defaultProvider.putIntoGrid(this, id, entityPlayer, container, itemStack, index);
    }

    @Override
    public IInventory getCraftMatrix(EntityPlayer entityPlayer, Container container, int id) {
        return container.inventorySlots.get(getCraftingGridStart(entityPlayer, container, id)).inventory;
    }

    @Override
    public boolean requiresServerSide() {
        return phantomItems;
    }

    @Override
    public int getCraftingGridStart(EntityPlayer entityPlayer, Container container, int id) {
        return gridSlotNumber;
    }

    @Override
    public int getCraftingGridSize(EntityPlayer entityPlayer, Container container, int id) {
        return gridSize;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void initGui(GuiContainer guiContainer, List<GuiButton> buttonList) {
        if (!hideButtons) {
            int index = 0;
            if (tweakRotate.enabled && tweakRotate.showButton) {
                int buttonX = tweakRotate.buttonX;
                int buttonY = tweakRotate.buttonY;
                if (alignToGrid != null) {
                    buttonX = getButtonX(guiContainer, index);
                    buttonY = getButtonY(guiContainer, index);
                }
                buttonList.add(CraftingTweaksAPI.createRotateButtonRelative(0, guiContainer, buttonX, buttonY));
                index++;
            }
            if (tweakBalance.enabled && tweakBalance.showButton) {
                int buttonX = tweakBalance.buttonX;
                int buttonY = tweakBalance.buttonY;
                if (alignToGrid != null) {
                    buttonX = getButtonX(guiContainer, index);
                    buttonY = getButtonY(guiContainer, index);
                }
                buttonList.add(CraftingTweaksAPI.createBalanceButtonRelative(0, guiContainer, buttonX, buttonY));
                index++;
            }
            if (tweakClear.enabled && tweakClear.showButton) {
                int buttonX = tweakClear.buttonX;
                int buttonY = tweakClear.buttonY;
                if (alignToGrid != null) {
                    buttonX = getButtonX(guiContainer, index);
                    buttonY = getButtonY(guiContainer, index);
                }
                buttonList.add(CraftingTweaksAPI.createClearButtonRelative(0, guiContainer, buttonX, buttonY));
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private int getButtonX(GuiContainer guiContainer, int index) {
        Slot firstSlot = guiContainer.inventorySlots.inventorySlots.get(
            getCraftingGridStart(
                FMLClientHandler.instance()
                    .getClientPlayerEntity(),
                guiContainer.inventorySlots,
                0));
        return switch (alignToGrid) {
            case NORTH, UP, SOUTH, DOWN -> firstSlot.xDisplayPosition + 18 * index;
            case EAST -> firstSlot.xDisplayPosition + 18 * 3 + 1;
            case WEST -> firstSlot.xDisplayPosition - 19;
        };
    }

    @SideOnly(Side.CLIENT)
    private int getButtonY(GuiContainer guiContainer, int index) {
        Slot firstSlot = guiContainer.inventorySlots.inventorySlots.get(
            getCraftingGridStart(
                FMLClientHandler.instance()
                    .getClientPlayerEntity(),
                guiContainer.inventorySlots,
                0));
        return switch (alignToGrid) {
            case NORTH, UP -> firstSlot.yDisplayPosition - 18 - 1;
            case SOUTH, DOWN -> firstSlot.yDisplayPosition + 18 * 3 + 1;
            case EAST, WEST -> firstSlot.yDisplayPosition + 18 * index;
        };
    }

}
