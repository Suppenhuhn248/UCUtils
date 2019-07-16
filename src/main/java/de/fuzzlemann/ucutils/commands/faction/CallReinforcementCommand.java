package de.fuzzlemann.ucutils.commands.faction;

import de.fuzzlemann.ucutils.utils.abstraction.AbstractionHandler;
import de.fuzzlemann.ucutils.utils.abstraction.UPlayer;
import de.fuzzlemann.ucutils.utils.command.api.Command;
import de.fuzzlemann.ucutils.utils.command.api.CommandParam;
import de.fuzzlemann.ucutils.utils.command.api.ParameterParser;
import de.fuzzlemann.ucutils.utils.command.api.TabCompletion;
import de.fuzzlemann.ucutils.utils.location.navigation.NavigationUtil;
import de.fuzzlemann.ucutils.utils.text.Message;
import de.fuzzlemann.ucutils.utils.text.MessagePart;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Fuzzlemann
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber
public class CallReinforcementCommand implements TabCompletion {

    private static final Pattern REINFORCEMENT_PATTERN = Pattern.compile("^(.+ ((?:\\[UC])*[a-zA-Z0-9_]+)): Benötige Verstärkung! -> X: (-*\\d+) \\| Y: (-*\\d+) \\| Z: (-*\\d+)$");
    private static final Pattern ON_THE_WAY_PATTERN = Pattern.compile("^(.+ (?:\\[UC])*[a-zA-Z0-9_]+): ((?:\\[UC])*[a-zA-Z0-9_]+), ich bin zu deinem Verstärkungsruf unterwegs! \\((\\d+) Meter entfernt\\)$");

    private static ReinforcementType lastReinforcement;

    @SubscribeEvent
    public static void onChatReceived(ClientChatReceivedEvent e) {
        UPlayer p = AbstractionHandler.getInstance().getPlayer();
        ITextComponent messageComponent = e.getMessage();
        String msg = messageComponent.getUnformattedText();

        Matcher reinforcementMatcher = REINFORCEMENT_PATTERN.matcher(msg);
        if (reinforcementMatcher.find()) {
            String fullName = reinforcementMatcher.group(1);
            String name = reinforcementMatcher.group(2);

            int posX = Integer.parseInt(reinforcementMatcher.group(3));
            int posY = Integer.parseInt(reinforcementMatcher.group(4));
            int posZ = Integer.parseInt(reinforcementMatcher.group(5));

            int distance = (int) p.getPosition().getDistance(posX, posY, posZ);

            List<ITextComponent> siblings = messageComponent.getSiblings();
            if (siblings.size() != 3) return;

            boolean dChat = siblings.get(0).getStyle().getColor() == TextFormatting.RED && siblings.get(2).getStyle().getColor() == TextFormatting.RED;

            Message.Builder builder = Message.builder();

            if (lastReinforcement != null && name.equals(lastReinforcement.getIssuer()) && System.currentTimeMillis() - lastReinforcement.getTime() < 1000) {
                builder.of(lastReinforcement.getType().getMessage()).color(TextFormatting.RED).advance().space();
            }

            builder.of(fullName).color(TextFormatting.DARK_GREEN).advance()
                    .of(" benötigt Unterstützung bei X: " + posX + " | Y: " + posY + " | Z: " + posZ + "! (" + distance + " Meter entfernt)").color(TextFormatting.GREEN).advance()
                    .newLine()
                    .messageParts(NavigationUtil.getNavigationMessage(posX, posY, posZ).getMessageParts())
                    .of(" | ").color(TextFormatting.GRAY).advance()
                    .of("Unterwegs")
                    .hoverEvent(HoverEvent.Action.SHOW_TEXT, MessagePart.simple("Bescheid geben, dass man unterwegs ist", TextFormatting.RED))
                    .clickEvent(ClickEvent.Action.RUN_COMMAND, "/reinforcement ontheway " + name + " " + posX + " " + posY + " " + posZ + (dChat ? " -d" : ""))
                    .color(TextFormatting.RED).advance()
                    .send();

            e.setCanceled(true);
            return;
        }

        Matcher onTheWayMatcher = ON_THE_WAY_PATTERN.matcher(msg);
        if (onTheWayMatcher.find()) {
            String senderFullName = onTheWayMatcher.group(1);
            String reinforcementSenderName = onTheWayMatcher.group(2);
            String distance = onTheWayMatcher.group(3);

            Message.builder()
                    .of(senderFullName).color(TextFormatting.DARK_GREEN).advance()
                    .of(" kommt zum Verstärkungsruf von " + reinforcementSenderName + "! (" + distance + " Meter entfernt)").color(TextFormatting.GREEN).advance()
                    .send();

            e.setCanceled(true);
            return;
        }

        for (Type type : Type.values()) {
            Pattern pattern = type.getPattern();
            if (pattern == null) continue;

            Matcher matcher = pattern.matcher(msg);
            if (!matcher.find()) continue;

            String name = matcher.group(1);

            lastReinforcement = new ReinforcementType(name, type);
            e.setCanceled(true);
            return;
        }
    }

    @Command({"reinforcement", "callreinforcement", "verstärkung"})
    public boolean onCommand(UPlayer p,
                             @CommandParam(required = false, defaultValue = CommandParam.NULL) Type firstType,
                             @CommandParam(required = false, requiredValue = "ontheway") boolean onTheWay,
                             @CommandParam(required = false, defaultValue = CommandParam.NULL) String name,
                             @CommandParam(required = false, defaultValue = CommandParam.NULL) Integer x,
                             @CommandParam(required = false, defaultValue = CommandParam.NULL) Integer y,
                             @CommandParam(required = false, defaultValue = CommandParam.NULL) Integer z,
                             @CommandParam(required = false, defaultValue = CommandParam.NULL) Type secondType) {
        Type type = ObjectUtils.firstNonNull(firstType, secondType, Type.DEFAULT);
        String chatType = type.getChatType();

        if (onTheWay) {
            if (name == null || x == null || y == null || z == null) return true;

            String message = "/" + chatType + " " + name + ", ich bin zu deinem Verstärkungsruf unterwegs! (" + (int) p.getPosition().getDistance(x, y, z) + " Meter entfernt)";
            p.sendChatMessage(message);
            return true;
        }

        BlockPos position = p.getPosition();
        int posX = position.getX();
        int posY = position.getY();
        int posZ = position.getZ();

        if (type.getMessage() != null)
            p.sendChatMessage("/" + chatType + " " + type.getMessage());

        p.sendChatMessage("/" + chatType + " Benötige Verstärkung! -> X: " + posX + " | Y: " + posY + " | Z: " + posZ);
        return true;
    }

    @Override
    public List<String> getTabCompletions(UPlayer p, String[] args) {
        if (args.length != 1) return null;

        return Arrays.stream(Type.values())
                .map(Type::getArgument)
                .collect(Collectors.toList());
    }

    @ParameterParser.At(TypeParser.class)
    private enum Type {
        DEFAULT("-f", "f", null),
        D_CHAT("-d", "d", null),
        RAM("-r", "f", "Rammen!"),
        EMERGENCY("-e", "f", "Dringend!"),
        EMERGENCY_D("-ed", "d", "Dringend!"),
        MEDIC("-m", "d", "Medic benötigt!"),
        CORPSE_GUARDING("-lb", "d", "Leichenbewachung!"),
        DRUG_REMOVAL("-da", "d", "Drogenabnahme!"),
        PLANT("-p", "d", "Plant!");

        private final String argument;
        private final String chatType;
        private final String message;
        private final Pattern pattern;

        Type(String argument, String chatType, String message) {
            this.argument = argument;
            this.chatType = chatType;
            this.message = message;

            this.pattern = message != null ? Pattern.compile("^.+ ((?:\\[UC])*[a-zA-Z0-9_]+): " + message + "$") : null;
        }

        public String getArgument() {
            return argument;
        }

        public String getChatType() {
            return chatType;
        }

        public String getMessage() {
            return message;
        }

        public Pattern getPattern() {
            return pattern;
        }
    }

    private static class ReinforcementType {
        private final String issuer;
        private final Type type;
        private final long time;

        public ReinforcementType(String issuer, Type type) {
            this.issuer = issuer;
            this.type = type;
            this.time = System.currentTimeMillis();
        }

        public String getIssuer() {
            return issuer;
        }

        public Type getType() {
            return type;
        }

        public long getTime() {
            return time;
        }
    }

    public static class TypeParser implements ParameterParser<String, Type> {
        @Override
        public Type parse(String input) {
            for (Type type : Type.values()) {
                if (input.equalsIgnoreCase(type.getArgument())) return type;
            }

            return null;
        }
    }
}