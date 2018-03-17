package de.fuzzlemann.ucutils.commands.time;

import de.fuzzlemann.ucutils.utils.command.Command;
import de.fuzzlemann.ucutils.utils.command.CommandExecutor;
import de.fuzzlemann.ucutils.utils.text.Message;
import de.fuzzlemann.ucutils.utils.text.TextUtils;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * @author Fuzzlemann
 */
@SideOnly(Side.CLIENT)
public class TimerCommand implements CommandExecutor {
    private final Timer timer = new Timer();
    private final Map<Integer, Long> timers = new HashMap<>();
    private int timerID = 0;

    @Override
    @Command(labels = "timer", usage = "/%label% [list/start] (Zeit in Sekunden)")
    public boolean onCommand(EntityPlayerSP p, String[] args) {
        if (args.length == 0) return false;

        switch (args[0].toLowerCase()) {
            case "start":
                if (args.length < 2) return false;

                int seconds;
                try {
                    seconds = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    return false;
                }

                int currentTimerID = ++timerID;

                Message.MessageBuilder builder = Message.builder();

                builder.of("Der Timer ").color(TextFormatting.AQUA).advance()
                        .of(String.valueOf(currentTimerID)).color(TextFormatting.RED).advance()
                        .of(" wurde gestartet.").color(TextFormatting.AQUA).advance();

                p.sendMessage(builder.build().toTextComponent());

                long millis = TimeUnit.SECONDS.toMillis(seconds);

                timers.put(currentTimerID, System.currentTimeMillis() + millis);
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Message.MessageBuilder builder2 = Message.builder();

                        builder2.of("Der Timer ").color(TextFormatting.AQUA).advance()
                                .of(String.valueOf(currentTimerID)).color(TextFormatting.RED).advance()
                                .of(" ist abgelaufen. ").color(TextFormatting.AQUA).advance()
                                .of(seconds + (seconds == 1 ? " Sekunde" : " Sekunden")).color(TextFormatting.RED).advance()
                                .of(" sind vergangen.").color(TextFormatting.AQUA).advance();

                        p.sendMessage(builder2.build().toTextComponent());
                        timers.remove(currentTimerID);
                    }
                }, millis);
                break;
            case "list":
                sendTimerList(p);
                break;
            default:
                return false;
        }

        return true;
    }

    private void sendTimerList(EntityPlayerSP p) {
        if (timers.isEmpty()) {
            TextUtils.error("Es ist derzeit kein Timer aktiv.");
            return;
        }

        Message.MessageBuilder builder = Message.builder();

        builder.of("\u00bb ").color(TextFormatting.GOLD).advance().of("Timer\n").color(TextFormatting.DARK_PURPLE).advance();

        for (Map.Entry<Integer, Long> entry : timers.entrySet()) {
            int id = entry.getKey();
            long time = entry.getValue();

            long timeLeft = TimeUnit.MILLISECONDS.toSeconds(time - System.currentTimeMillis());

            builder.of("  * Timer " + id).color(TextFormatting.GRAY).advance()
                    .of(": ").color(TextFormatting.DARK_GRAY).advance()
                    .of(timeLeft + " Sekunden verbleibend\n").color(TextFormatting.RED).advance();
        }

        p.sendMessage(builder.build().toTextComponent());
    }
}