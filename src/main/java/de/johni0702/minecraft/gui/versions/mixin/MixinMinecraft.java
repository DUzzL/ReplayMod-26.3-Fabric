// Note: this will also work in 1.13 but we don't really support 1.13 and MixinGradle is yet to be updated
//#if FABRIC>=1
package de.johni0702.minecraft.gui.versions.mixin;

import de.johni0702.minecraft.gui.versions.callbacks.OpenGuiScreenCallback;
import de.johni0702.minecraft.gui.versions.callbacks.PreTickCallback;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

//#if MC >= 26.2
//$$ import net.minecraft.client.gui.Gui;
//#else
import net.minecraft.client.MinecraftClient;
//#endif

//#if MC >= 26.2
//$$ @Mixin(Gui.class)
//#else
@Mixin(MinecraftClient.class)
//#endif
public abstract class MixinMinecraft {
    @Inject(method = "tick", at = @At("HEAD"))
    private void preTick(CallbackInfo ci) {
        PreTickCallback.EVENT.invoker().preTick();
    }

    //#if MC >= 26.2
    //$$ @Inject(method = "setScreen", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Gui;screen:Lnet/minecraft/client/gui/screens/Screen;"))
    //#else
    @Inject(method = "openScreen", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;currentScreen:Lnet/minecraft/client/gui/screen/Screen;"))
    //#endif
    private void openGuiScreen(Screen newGuiScreen, CallbackInfo ci) {
        OpenGuiScreenCallback.EVENT.invoker().openGuiScreen(newGuiScreen);
    }
}
//#endif
