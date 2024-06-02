package net.blay09.mods.craftingtweaks.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.EnumChatFormatting;

public class GuiTweakButton extends GuiImageButton implements ITooltipProvider {

    public enum TweakOption {
        Rotate,
        Balance,
        Clear
    }

    private final TweakOption tweakOption;
    private final int tweakId;
    private final GuiContainer parentGui;
    private int lastGuiLeft;
    private int lastGuiTop;

    public GuiTweakButton(GuiContainer parentGui, int xPosition, int yPosition, int texCoordX, int texCoordY,
        TweakOption tweakOption, int tweakId) {
        super(-1, xPosition, yPosition, texCoordX, texCoordY);
        this.parentGui = parentGui;
        this.tweakOption = tweakOption;
        this.tweakId = tweakId;
    }

    public TweakOption getTweakOption() {
        return tweakOption;
    }

    public int getTweakId() {
        return tweakId;
    }

    @Override
    public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
        int oldX = xPosition;
        int oldY = yPosition;
        // If parentGui is set, we only store the relative position in the button for mods that do hacky things where
        // guiLeft/guiTop constantly changes
        if (parentGui != null) {
            xPosition += lastGuiLeft;
            yPosition += lastGuiTop;
        }
        boolean result = super.mousePressed(mc, mouseX, mouseY);
        xPosition = oldX;
        yPosition = oldY;
        return result;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        int oldX = xPosition;
        int oldY = yPosition;
        // If parentGui is set, we only store the relative position in the button for mods that do hacky things where
        // guiLeft/guiTop constantly changes
        if (parentGui != null) {
            lastGuiLeft = parentGui.guiLeft;
            lastGuiTop = parentGui.guiTop;
            xPosition += lastGuiLeft;
            yPosition += lastGuiTop;
        }
        int oldTexCoordX = texCoordX;
        if (GuiScreen.isShiftKeyDown()) {
            texCoordX += 48;
        }
        super.drawButton(mc, mouseX, mouseY);
        texCoordX = oldTexCoordX;
        xPosition = oldX;
        yPosition = oldY;
    }

    @Override
    public void addInformation(List<String> tooltip) {
        boolean isShiftDown = GuiScreen.isShiftKeyDown();
        switch (tweakOption) {
            case Rotate -> tooltip.add(I18n.format("tooltip.craftingtweaks.rotate"));
            case Clear -> {
                if (isShiftDown) {
                    tooltip.add(I18n.format("tooltip.craftingtweaks.forceClear"));
                    tooltip.add(EnumChatFormatting.GRAY + I18n.format("tooltip.craftingtweaks.forceClearInfo"));
                } else {
                    tooltip.add(I18n.format("tooltip.craftingtweaks.clear"));
                }
            }
            case Balance -> {
                if (isShiftDown) {
                    tooltip.add(I18n.format("tooltip.craftingtweaks.spread"));
                } else {
                    tooltip.add(I18n.format("tooltip.craftingtweaks.balance"));
                }
            }
        }
    }
}
