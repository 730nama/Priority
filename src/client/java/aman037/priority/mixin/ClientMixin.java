package aman037.priority.mixin;

import aman037.priority.PriorityConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(net.minecraft.client.Minecraft.class)
public class ClientMixin {

    private static int winningKeyIndex = -1;
    private static int highestPriority = -1;
    private static boolean checkedThisTick = false;

    @Redirect(
        method = "handleKeybinds",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z")
    )
    private boolean redirectWasPressed(KeyMapping keyMapping) {
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();

        if (client.player == null || client.gui.screen() != null) {
            return keyMapping.consumeClick();
        }

        PriorityConfig config = AutoConfig.getConfigHolder(PriorityConfig.class).getConfig();
        KeyMapping[] hotbarKeys = client.options.keyHotbarSlots;

        int thisKeyIndex = -1;
        for (int i = 0; i < hotbarKeys.length; i++) {
            if (hotbarKeys[i] == keyMapping) {
                thisKeyIndex = i;
                break;
            }
        }

        if (thisKeyIndex == -1 || thisKeyIndex >= 9) {
            return keyMapping.consumeClick();
        }

        // figure out the winner on the first slot, then reset after slot 8
        if (!checkedThisTick) {
            winningKeyIndex = -1;
            highestPriority = -1;

            for (int i = 0; i < 9 && i < hotbarKeys.length; i++) {
                if (hotbarKeys[i].consumeClick()) {
                    int priority = config.getSlotPriority(i);
                    if (priority > highestPriority) {
                        highestPriority = priority;
                        winningKeyIndex = i;
                    }
                }
            }
            checkedThisTick = true;
        }

        if (thisKeyIndex == 8) {
            checkedThisTick = false;
        }

        return thisKeyIndex == winningKeyIndex && winningKeyIndex != -1;
    }
}
