package aman037.priority.mixin;

import aman037.priority.PriorityConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MinecraftClient.class)
public class ClientMixin {

    private static int winningKeyIndex = -1;
    private static int highestPriority = -1;
    private static boolean checkedThisTick = false;

    @Redirect(
        method = "handleInputEvents",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/KeyBinding;wasPressed()Z")
    )
    private boolean redirectWasPressed(KeyBinding keyBinding) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (client.player == null || client.currentScreen != null) {
            return keyBinding.wasPressed();
        }

        PriorityConfig config = AutoConfig.getConfigHolder(PriorityConfig.class).getConfig();
        KeyBinding[] hotbarKeys = client.options.hotbarKeys;

        int thisKeyIndex = -1;
        for (int i = 0; i < hotbarKeys.length; i++) {
            if (hotbarKeys[i] == keyBinding) {
                thisKeyIndex = i;
                break;
            }
        }

        if (thisKeyIndex == -1 || thisKeyIndex >= 9) {
            return keyBinding.wasPressed();
        }

        // figure out the winner on the first slot, then reset after slot 8
        if (!checkedThisTick) {
            winningKeyIndex = -1;
            highestPriority = -1;

            for (int i = 0; i < 9 && i < hotbarKeys.length; i++) {
                if (hotbarKeys[i].wasPressed()) {
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
