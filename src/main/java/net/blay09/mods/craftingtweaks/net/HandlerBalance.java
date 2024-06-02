package net.blay09.mods.craftingtweaks.net;

import net.blay09.mods.craftingtweaks.CraftingTweaks;
import net.blay09.mods.craftingtweaks.api.TweakProvider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class HandlerBalance implements IMessageHandler<MessageBalance, IMessage> {

    @Override
    public IMessage onMessage(final MessageBalance message, final MessageContext ctx) {
        EntityPlayer entityPlayer = ctx.getServerHandler().playerEntity;
        Container container = entityPlayer.openContainer;
        if (container != null) {
            TweakProvider tweakProvider = CraftingTweaks.instance.getProvider(container);
            if (tweakProvider != null) {
                if (message.isSpread()) {
                    tweakProvider.spreadGrid(entityPlayer, container, message.getId());
                } else {
                    tweakProvider.balanceGrid(entityPlayer, container, message.getId());
                }
            }
        }
        return null;
    }

}
