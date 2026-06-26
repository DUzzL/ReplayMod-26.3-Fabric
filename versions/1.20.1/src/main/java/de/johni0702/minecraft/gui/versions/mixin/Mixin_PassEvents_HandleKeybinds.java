package de.johni0702.minecraft.gui.versions.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import de.johni0702.minecraft.gui.versions.ScreenExt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

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
public class Mixin_PassEvents_HandleKeybinds {
    @ModifyExpressionValue(
            method = "tick",
            //#if MC >= 26.2
            //$$ at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Gui;screen:Lnet/minecraft/client/gui/screens/Screen;", ordinal = 0),
            //#else
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;currentScreen:Lnet/minecraft/client/gui/screen/Screen;", ordinal = 0),
            //#endif
            //#if MC>=12109
            //$$ slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=Ticking screen"))
            //#else
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;wrapScreenError(Ljava/lang/Runnable;Ljava/lang/String;Ljava/lang/String;)V"))
            //#endif
    )
    private Screen doesScreenPassEvents(Screen screen) {
        if (screen instanceof ScreenExt ext && ext.doesPassEvents()) {
            screen = null;
        }
        return screen;
    }
}
