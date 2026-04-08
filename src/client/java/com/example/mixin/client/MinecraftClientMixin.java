package com.example.mixin.client;

import com.example.config.HotbarPriorityConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(net.minecraft.client.Minecraft.class)
public class MinecraftClientMixin {

    // Track the winning key across the loop iteration
    private static int winningKeyIndex = -1;
    private static int highestPriority = -1;
    private static boolean checkedThisTick = false;

    @Redirect(
        method = "handleKeybinds",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z")
    )
    private boolean redirectWasPressed(KeyMapping keyMapping) {
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        
        if (client.player == null || client.screen != null) {
            return keyMapping.consumeClick();
        }
        
        HotbarPriorityConfig config = AutoConfig.getConfigHolder(HotbarPriorityConfig.class).getConfig();
        KeyMapping[] hotbarKeys = client.options.keyHotbarSlots;
        
        // Find which hotbar key this is
        int thisKeyIndex = -1;
        for (int i = 0; i < hotbarKeys.length; i++) {
            if (hotbarKeys[i] == keyMapping) {
                thisKeyIndex = i;
                break;
            }
        }
        
        // If not a hotbar key, return normal result
        if (thisKeyIndex == -1 || thisKeyIndex >= 9) {
            return keyMapping.consumeClick();
        }
        
        // First call of each tick - find the winner
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
        
        // Reset flag after we've processed all 9 keys
        if (thisKeyIndex == 8) {
            checkedThisTick = false;
        }
        
        // Only return true for the winning key
        return thisKeyIndex == winningKeyIndex && winningKeyIndex != -1;
    }

}
