package ctl.tagger.screen;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import ctl.tagger.screen.ConfigManager;  // New class to manage config

public class TTConfigScreen {

    public static Screen getConfigScreen(Screen parent) {
        // Create a configuration builder
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("config.tiertagger.title"));

        // Create a category for general settings
        ConfigCategory general = builder.getOrCreateCategory(Text.translatable("category.tiertagger.general"));

        // Options for the dropdown menu
        String[] gamemodeOptions = new String[]{
                "Netherite Potion", "Diamond Potion", "Sword", "Crystal", "UHC", "SMP", "Axe"
        };

        // Add the dropdown menu for gamemode selection
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();
        general.addEntry(entryBuilder.startSelector(
                        Text.translatable("option.tiertagger.gamemode"),   // Label
                        gamemodeOptions,                                   // Array of options
                        ConfigManager.getSelectedGamemode())               // Default value
                .setSaveConsumer(ConfigManager::setSelectedGamemode)  // Save the selected option
                .setTooltip(Text.translatable("tooltip.tiertagger.gamemode"))
                .build());

        // Build and return the screen
        return builder.build();
    }
}
