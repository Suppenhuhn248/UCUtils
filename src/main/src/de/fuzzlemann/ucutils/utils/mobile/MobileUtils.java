package de.fuzzlemann.ucutils.utils.mobile;

import de.fuzzlemann.ucutils.utils.io.JsonManager;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * @author Fuzzlemann
 */
@Mod.EventBusSubscriber
@SideOnly(Side.CLIENT)
public class MobileUtils {

    private static File blockedFile = new File(JsonManager.directory, "blocked.storage");
    private static List<String> blockedPlayers = JsonManager.loadObjects(blockedFile, String.class)
            .stream()
            .map(object -> (String) object)
            .collect(Collectors.toList());
    private static boolean blockNextMessage = false;

    private static CompletableFuture<Integer> future;

    public static void block(String playerName) {
        blockedPlayers.add(playerName);
        save();
    }

    public static void unblock(String playerName) {
        blockedPlayers.remove(playerName);
        save();
    }

    public static boolean isBlocked(String playerName) {
        return blockedPlayers.contains(playerName);
    }

    public static List<String> getBlockedPlayers() {
        return blockedPlayers;
    }

    private static void save() {
        new Thread(() -> JsonManager.writeList(blockedFile, blockedPlayers)).start();
    }

    public static Future<Integer> getNumber(EntityPlayerSP p, String numberPlayer) {
        p.sendChatMessage("/nummer " + numberPlayer);

        future = new CompletableFuture<>();
        return future;
    }

    @SubscribeEvent
    public static void onChat(ClientChatReceivedEvent e) {
        if (blockNextMessage) {
            e.setCanceled(true);
            return;
        }

        String message = e.getMessage().getUnformattedText();

        if (message.startsWith("Dein Handy klingelt! Eine Nachricht von ")) {
            String playerName = message.split(" ")[6];
            if (MobileUtils.isBlocked(playerName)) {
                e.setCanceled(true);
                blockNextMessage = true;
                return;
            }
        }

        if (future != null) {
            if (message.equals("Spieler nicht gefunden.")) {
                future.complete(-1);
                future = null;
                return;
            }

            if (!message.startsWith("Nummer von")) return;
            e.setCanceled(true);

            String numberString = message.split(":")[1];
            int number = Integer.parseInt(numberString.substring(1, numberString.length()));

            future.complete(number);
            future = null;
        }
    }
}
