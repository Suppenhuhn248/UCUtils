package de.fuzzlemann.ucutils.commands.faction;

import de.fuzzlemann.ucutils.utils.ForgeUtils;
import de.fuzzlemann.ucutils.base.abstraction.AbstractionLayer;
import de.fuzzlemann.ucutils.base.abstraction.UPlayer;
import de.fuzzlemann.ucutils.base.command.Command;
import de.fuzzlemann.ucutils.base.command.CommandParam;
import de.fuzzlemann.ucutils.utils.location.navigation.NavigationUtil;
import de.fuzzlemann.ucutils.base.text.Message;
import de.fuzzlemann.ucutils.base.text.TextUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Fuzzlemann
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber
public class ShareLocationCommand {

    private static final Pattern SHARE_LOCATION_PATTERN = Pattern.compile("^(.+ (?:\\[UC])*[a-zA-Z0-9_]+): Positionsteilung für ([a-zA-Z0-9_, ]+)! -> X: (-*\\d+) \\| Y: (-*\\d+) \\| Z: (-*\\d+)$");

    @SubscribeEvent
    public static void onChatReceived(ClientChatReceivedEvent e) {
        String message = e.getMessage().getUnformattedText();

        Matcher shareLocationMatcher = SHARE_LOCATION_PATTERN.matcher(message);
        if (!shareLocationMatcher.find()) return;

        UPlayer p = AbstractionLayer.getPlayer();
        String playerName = p.getName();

        e.setCanceled(true);

        String names = shareLocationMatcher.group(2);

        List<String> nameList = Arrays.asList(names.split(", "));
        if (!nameList.contains(playerName)) return;

        String fullName = shareLocationMatcher.group(1);

        int posX = Integer.parseInt(shareLocationMatcher.group(3));
        int posY = Integer.parseInt(shareLocationMatcher.group(4));
        int posZ = Integer.parseInt(shareLocationMatcher.group(5));

        int distance = (int) p.getPosition().getDistance(posX, posY, posZ);

        Message.builder()
                .of(fullName).color(TextFormatting.DARK_GREEN).advance()
                .of(" hat seine Position mit dir geteilt! -> X: " + posX + " | Y: " + posY + " | Z: " + posZ + " (" + distance + " Meter entfernt)").color(TextFormatting.GREEN).advance()
                .newLine()
                .messageParts(NavigationUtil.getNavigationMessage(posX, posY, posZ).getMessageParts())
                .send();

        e.setCanceled(true);
    }

    @Command(value = {"sharelocation", "shareloc", "sloc"}, usage = "/sharelocation [Spieler...] (-d)")
    public boolean onCommand(UPlayer p, @CommandParam(arrayStart = true) String[] players, @CommandParam(required = false, requiredValue = "-d") boolean allianceChat) {
        List<String> onlinePlayers = ForgeUtils.getOnlinePlayers();
        Set<String> playerNames = new LinkedHashSet<>();

        for (String player : players) {
            String foundPlayer = ForgeUtils.getMostMatching(onlinePlayers, player);
            if (foundPlayer == null) continue;

            playerNames.add(foundPlayer);
        }

        if (playerNames.isEmpty()) {
            TextUtils.error("Der Spieler wurde nicht gefunden.");
            return true;
        }

        String playerString = String.join(", ", playerNames);
        String command = allianceChat ? "/d" : "/f";

        BlockPos position = p.getPosition();
        int posX = position.getX();
        int posY = position.getY();
        int posZ = position.getZ();

        String fullCommand = command + " Positionsteilung für " + playerString + "! -> X: " + posX + " | Y: " + posY + " | Z: " + posZ;
        p.sendChatMessage(fullCommand);

        Message.builder()
                .prefix()
                .of("Du hast deine Position mit ").color(TextFormatting.GRAY).advance()
                .joiner(players)
                .consumer((b, s) -> b.of(s).color(TextFormatting.BLUE).advance())
                .commaJoiner()
                .andNiceJoiner()
                .advance()
                .of(" geteilt.").color(TextFormatting.GRAY).advance()
                .send();
        return true;
    }
}
