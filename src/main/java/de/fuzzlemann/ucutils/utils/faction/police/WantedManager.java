package de.fuzzlemann.ucutils.utils.faction.police;

import com.google.common.util.concurrent.Uninterruptibles;
import de.fuzzlemann.ucutils.base.abstraction.AbstractionLayer;
import de.fuzzlemann.ucutils.base.udf.UDFLoader;
import de.fuzzlemann.ucutils.base.udf.UDFModule;
import de.fuzzlemann.ucutils.common.udf.DataRegistry;
import de.fuzzlemann.ucutils.events.NameFormatEventHandler;
import de.fuzzlemann.ucutils.utils.ForgeUtils;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Fuzzlemann
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber
@UDFModule(value = DataRegistry.WANTED_CATALOG, version = 1)
public class WantedManager implements UDFLoader<List<WantedReason>> {

    private static final List<WantedReason> WANTED_LIST = new ArrayList<>();
    private static final Pattern WANTED_INFO_PATTERN = Pattern.compile("^HQ: (?:\\[UC])*([a-zA-Z0-9_]+) wird aus folgendem Grund gesucht: (.+), Over.$");

    private static boolean blockNextMessage;
    private static CompletableFuture<Wanted> future;

    public static List<String> getWantedReasons() {
        return WANTED_LIST.stream()
                .map(WantedReason::getReason)
                .collect(Collectors.toList());
    }

    public static WantedReason getWantedReason(String reason) {
        return ForgeUtils.getMostMatching(WANTED_LIST, reason, WantedReason::getReason);
    }

    public static Wanted getWanteds(String player) {
        future = new CompletableFuture<>();
        AbstractionLayer.getPlayer().sendChatMessage("/wantedinfo " + player);

        try {
            return Uninterruptibles.getUninterruptibly(future);
        } catch (ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    @SubscribeEvent
    public static void onChatReceived(ClientChatReceivedEvent e) {
        if (blockNextMessage) {
            e.setCanceled(true);
            blockNextMessage = false;
            return;
        }

        if (future == null) return;
        String message = e.getMessage().getUnformattedText();

        if (message.equals("HQ: Der Spieler wird nicht gesucht, Over.")) {
            future.complete(null);
            future = null;
            return;
        }

        Matcher matcher = WANTED_INFO_PATTERN.matcher(message);
        if (!matcher.find()) return;

        String player = matcher.group(1);
        Integer wanteds = NameFormatEventHandler.WANTED_MAP.get(player);

        if (wanteds == null) {
            blockNextMessage = true;
            future.complete(null);
            future = null;
            return;
        }

        String reason = matcher.group(2);

        future.complete(new Wanted(reason, wanteds));
        future = null;

        blockNextMessage = true;
        e.setCanceled(true);
    }

    @Override
    public void supply(List<WantedReason> wantedReasons) {
        WANTED_LIST.addAll(wantedReasons);
    }

    @Override
    public void cleanUp() {
        WANTED_LIST.clear();
    }
}
