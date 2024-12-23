package ctl.tagger.mixin;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import ctl.tagger.config.PlayerRank;
import ctl.tagger.screen.ConfigManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Unique;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

@Mixin(PlayerEntity.class)
public class MixinPlayerEntity {

    @Unique
    private static final Map<String, String> SHEET_URLS = Map.of(
            "Netherite Potion", "https://script.google.com/macros/s/AKfycbxnpruKlEmunN478oiCrrEIE2e9pggP_qFwxaB1j1Qf8_QClZYvcEXFh1EuqL3BsPeS/exec",
            "Diamond Potion", "https://script.google.com/macros/s/AKfycbxR1u6zyjV9o_6oHJgwUMC0i9iXYtn6pNjtwwFbd_LtNVzJ3Z0J75Y_aBr_x5e3nycX/exec",
            "Sword", "https://script.google.com/macros/s/AKfycbyLVdgV8bpj5A0myS-ezMtWH_IFxJcYfwGX2DlEo0y-ZyStziBsNR4s7ml_9uqMnyB7/exec",
            "Crystal", "https://script.google.com/macros/s/AKfycbydPMaGODUbO-CUJphZq3o72bD8waxReGBpVVazdFWYoo5u0dhlreRogVwnf9b-vzevZA/exec",
            "UHC", "https://script.google.com/macros/s/AKfycbxSv7ZZh-o7Q5g0YneYNJkkAIJzfwXrRL_CMH9DdODMpJeTAD3cJhOlX84WxRocnDsE/exec",
            "SMP", "https://script.google.com/macros/s/AKfycbxJVHUqHQsQ9HXDq_Y_6jBihpP_WQdMfPYHY3M7dGtMNQjx2PRSSgP-rknbDAXiCSpSpQ/exec",
            "Axe", "https://script.google.com/macros/s/AKfycbzj4oPhyEovm9uzUVkJTQ2R2qgLSmmtFUA9V7aafWCh2ErVDrQJ8kydbTzI_JuAZ4WphQ/exec"
    );

    @Unique
    private static String lastGamemode = null;

    @Unique
    private static List<PlayerRank> cachedPlayerRanks = null;

    // Timer to refresh ranks periodically
    static {
        Timer timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (lastGamemode != null) {
                    System.out.println("Refreshing rank data for gamemode: " + lastGamemode);
                    cachedPlayerRanks = fetchPlayerRanksForGamemodeStatic(lastGamemode);
                }
            }
        }, 60000, 60000); // Refresh every minute
    }

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    public void modifyNameTag(CallbackInfoReturnable<Text> cir) {
        String selectedGamemode = ConfigManager.getSelectedGamemode();
        if (!selectedGamemode.equals(lastGamemode)) {
            // Gamemode has changed; fetch new ranks
            lastGamemode = selectedGamemode;
            cachedPlayerRanks = fetchPlayerRanksForGamemodeStatic(selectedGamemode);
        }

        String gamemodeSymbol = getGamemodeSymbol(selectedGamemode);
        Style gamemodeStyle = getGamemodeSymbolStyle(selectedGamemode);

        // Use cached ranks for the current gamemode
        String currentPlayerName = ((PlayerEntity) (Object) this).getName().getString();
        boolean rankFound = false;

        // Get the original display name, which includes the server-side prefix
        Text originalDisplayName = cir.getReturnValue();

        // Build gamemode symbol
        Text gamemodeText = Text.literal(gamemodeSymbol + " ").setStyle(gamemodeStyle);

        if (cachedPlayerRanks != null) {
            for (PlayerRank rank : cachedPlayerRanks) {
                if (rank.player().equalsIgnoreCase(currentPlayerName)) {
                    String rankPrefix = rank.retired().equalsIgnoreCase("TRUE") ? "R" : "";
                    Style rankStyle = getRankColorStyle(rank.rank(), rank.retired());

                    // Build rank text
                    Text rankText = Text.literal(rankPrefix + rank.rank()).setStyle(rankStyle);

                    // Divider text
                    Text dividerText = Text.literal(" | ").setStyle(Style.EMPTY.withColor(0xA0A0A0));

                    // Combine everything
                    Text modifiedDisplayName = Text.empty()
                            .append(gamemodeText) // Gamemode symbol
                            .append(rankText) // Rank prefix
                            .append(dividerText) // Divider
                            .append(originalDisplayName.copy()); // Original player name (unchanged style)

                    cir.setReturnValue(modifiedDisplayName);
                    rankFound = true;
                    break;
                }
            }
        }

        if (!rankFound) {
            // Player is unranked, append "Unranked" instead of a rank
            Text unrankedText = Text.literal("Unranked").setStyle(Style.EMPTY.withColor(0xA0A0A0));
            Text dividerText = Text.literal(" | ").setStyle(Style.EMPTY.withColor(0xA0A0A0));

            Text modifiedDisplayName = Text.empty()
                    .append(gamemodeText) // Gamemode symbol
                    .append(unrankedText) // Unranked text
                    .append(dividerText) // Divider
                    .append(originalDisplayName.copy()); // Original player name (unchanged style)

            cir.setReturnValue(modifiedDisplayName);
        }
    }

    @Unique
    private static List<PlayerRank> fetchPlayerRanksForGamemodeStatic(String gamemode) {
        List<PlayerRank> playerRanks = new ArrayList<>();
        String apiUrl = SHEET_URLS.get(gamemode);

        if (apiUrl == null) {
            System.err.println("No sheet URL found for gamemode: " + gamemode);
            return null;
        }

        try {
            // Replace spaces with %20 in the gamemode name
            String encodedGamemode = gamemode.replace(" ", "%20");

            URL url = new URL(apiUrl + "?id=" + encodedGamemode);  // Use encoded gamemode in the URL
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("User-Agent", "Minecraft Mod");

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    Gson gson = new Gson();
                    PlayerRank[] ranks = gson.fromJson(response.toString(), PlayerRank[].class);
                    for (PlayerRank rank : ranks) {
                        playerRanks.add(rank);
                    }
                    System.out.println("Fetched " + ranks.length + " ranks for gamemode: " + gamemode);
                }
            } else {
                System.err.println("Error fetching data. HTTP Code: " + connection.getResponseCode());
            }
        } catch (IOException | JsonSyntaxException e) {
            System.err.println("Error fetching ranks for gamemode " + gamemode + ": " + e.getMessage());
        }

        return playerRanks;
    }

    @Unique
    private String getGamemodeSymbol(String gamemode) {
        return switch (gamemode) {
            case "Sword" -> "🗡";
            case "SMP" -> "🛡";
            case "Axe" -> "🪓";
            case "UHC" -> "🪣";
            case "Crystal" -> "◆";
            case "Netherite Potion" -> "🗡";
            case "Diamond Potion" -> "🧪";
            default -> "❓";
        };
    }

    @Unique
    private Style getGamemodeSymbolStyle(String gamemode) {
        return switch (gamemode) {
            case "Sword" -> Style.EMPTY.withColor(0x00FFFF);
            case "SMP" -> Style.EMPTY.withColor(0x006400);
            case "Axe" -> Style.EMPTY.withColor(0xFFFF00);
            case "UHC" -> Style.EMPTY.withColor(0xFFA500);
            case "Crystal" -> Style.EMPTY.withColor(0xFF69B4);
            case "Netherite Potion" -> Style.EMPTY.withColor(0x800080);
            case "Diamond Potion" -> Style.EMPTY.withColor(0xFF0000);
            default -> Style.EMPTY.withColor(0xA0A0A0);
        };
    }

    @Unique
    private Style getRankColorStyle(String rank, String retired) {
        if (retired.equalsIgnoreCase("TRUE")) {
            return Style.EMPTY.withColor(0x800080); // Purple for retired ranks
        }
        return switch (rank) {
            case "HT1" -> Style.EMPTY.withColor(0xFF0000);
            case "LT1" -> Style.EMPTY.withColor(0xFFFFC5);
            case "HT2" -> Style.EMPTY.withColor(0xFF6347);
            case "LT2" -> Style.EMPTY.withColor(0x00FF00);
            case "HT3" -> Style.EMPTY.withColor(0x00FFFF);
            case "LT3" -> Style.EMPTY.withColor(0x0697ff);
            case "HT4" -> Style.EMPTY.withColor(0x006400);
            case "LT4" -> Style.EMPTY.withColor(0x90EE90);
            case "HT5" -> Style.EMPTY.withColor(0xA9A9A9);
            case "LT5" -> Style.EMPTY.withColor(0x808080);
            default -> Style.EMPTY.withColor(0xA0A0A0);
        };
    }
}
