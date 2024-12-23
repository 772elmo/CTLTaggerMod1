package ctl.tagger.screen;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.gui.screen.Screen;

public class TiertaggerModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<Screen> getModConfigScreenFactory() {
        // Return the ConfigScreen factory for ModMenu using a method reference
        return TTConfigScreen::getConfigScreen;
    }
}
