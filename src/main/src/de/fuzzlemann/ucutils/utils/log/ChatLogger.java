package de.fuzzlemann.ucutils.utils.log;

import de.fuzzlemann.ucutils.Main;
import de.fuzzlemann.ucutils.utils.config.ConfigUtil;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * @author Fuzzlemann
 */
@Mod.EventBusSubscriber
@SideOnly(Side.CLIENT)
public class ChatLogger {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    public static ChatLogger instance;
    private Logger logger;

    public ChatLogger() {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss").format(new Date());

        FileHandler fileHandler;
        try {
            File directory = new File(Main.MINECRAFT.mcDataDir, "chatlogs");
            File logFile = new File(directory, "chatlog-" + timeStamp + ".txt");
            if (directory.mkdirs())
                System.out.println("ChatLog Directory created");

            String path = logFile.getPath();
            if (logFile.createNewFile())
                System.out.println("ChatLog " + path + " created");
            fileHandler = new FileHandler(path);
        } catch (IOException e) {
            ConfigUtil.logChat = false;
            e.printStackTrace();
            return;
        }

        logger = Logger.getLogger("ChatLogger");
        logger.addHandler(fileHandler);
        fileHandler.setFormatter(new ChatLogFormatter());

        log("------------ Chat Log ------------");
    }

    @SubscribeEvent
    public static void onChat(ClientChatEvent e) {
        if (ConfigUtil.logChat)
            instance.log("[SELF] " + e.getOriginalMessage());
    }

    @SubscribeEvent
    public static void onReceiveChat(ClientChatReceivedEvent e) {
        if (ConfigUtil.logChat)
            instance.log("[CHAT] " + e.getMessage().getUnformattedText());
    }

    @SubscribeEvent
    public static void onClientConnect(FMLNetworkEvent.ClientConnectedToServerEvent e) {
        if (ConfigUtil.logChat)
            instance.log("---- Connected to " + e.getManager().getRemoteAddress() + " ----");
    }

    @SubscribeEvent
    public static void onClientConnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent e) {
        if (ConfigUtil.logChat)
            instance.log("---- Disconnected from " + e.getManager().getRemoteAddress() + " ----");
    }

    private void log(String message) {
        logger.info(message + "\n");
    }

    private class ChatLogFormatter extends Formatter {
        @Override
        public String format(LogRecord record) {
            return DATE_FORMAT.format(new Date()) + " | " + record.getMessage();
        }
    }
}
