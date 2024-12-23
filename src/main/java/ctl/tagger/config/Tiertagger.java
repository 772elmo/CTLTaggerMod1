package ctl.tagger.config;

import net.fabricmc.api.ModInitializer;

public class Tiertagger implements ModInitializer {

	@Override
	public void onInitialize() {
		// Initialization logic for your mod (no rank fetching here)
		System.out.println("Tiertagger mod initialized.");

		// You could set the default gamemode here if needed
		// Example: You can set a default gamemode at initialization
		// Tiertagger.setSelectedGamemode("Netherite Potion");
	}
}
