package com.jelly.farmhelperv2.command;

import cc.polyfrost.oneconfig.utils.commands.annotations.Command;
import cc.polyfrost.oneconfig.utils.commands.annotations.Main;
import com.jelly.farmhelperv2.config.FarmHelperConfig;
import com.jelly.farmhelperv2.handler.MacroHandler;
import com.jelly.farmhelperv2.macro.impl.GlaciteWalkerMacro;
import com.jelly.farmhelperv2.util.LogUtils;

@Command(value = "glacite", aliases = {"gw", "glacitewalker"}, description = "Glacite Walker combat macro commands")
public class GlaciteWalkerCommand {

    @Main
    public void mainCommand() {
        if (!FarmHelperConfig.enableGlaciteWalkerCombat) {
            LogUtils.sendError("[Glacite Walker] Combat macro is disabled in config!");
            return;
        }

        if (GlaciteWalkerMacro.getInstance().isEnabled()) {
            GlaciteWalkerMacro.getInstance().onDisable();
            LogUtils.sendSuccess("[Glacite Walker] Combat macro disabled!");
        } else {
            // Stop any running farming macro first
            if (MacroHandler.getInstance().isMacroToggled()) {
                MacroHandler.getInstance().disableMacro();
            }
            
            GlaciteWalkerMacro.getInstance().onEnable();
            LogUtils.sendSuccess("[Glacite Walker] Combat macro enabled!");
        }
    }
}
