package ctl.tagger.screen;

public class ConfigManager {

    private static String selectedGamemode = "Netherite Potion"; // Default gamemode

    // Get the selected gamemode
    public static String getSelectedGamemode() {
        return selectedGamemode;
    }

    // Set the selected gamemode
    public static void setSelectedGamemode(String selectedGamemode) {
        ConfigManager.selectedGamemode = selectedGamemode;
        // Save the selected gamemode to a config file or local storage (optional)
        saveConfig();
    }

    // (Optional) Save configuration to a file or a settings storage (example)
    private static void saveConfig() {
        // Logic to save the selected gamemode in a persistent config file (like in a .json or .xml file).
        // This could be handled by a mod configuration system or file system API.
    }
}
