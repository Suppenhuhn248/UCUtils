package de.fuzzlemann.ucutils.commands.faction.badfaction;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import de.fuzzlemann.ucutils.utils.command.api.Command;
import de.fuzzlemann.ucutils.utils.location.navigation.NavigationUtil;
import de.fuzzlemann.ucutils.utils.text.Message;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.Map;

/**
 * @author Fuzzlemann
 */
@SideOnly(Side.CLIENT)
public class SchwarzmarktLocationsCommand {

    private static final List<Map.Entry<String, BlockPos>> BLACK_MARKET_LIST = Lists.newArrayList(
            Maps.immutableEntry("Containerhalle", new BlockPos(-100, 69, -447)),
            Maps.immutableEntry("Lagerhalle", new BlockPos(-70, 69, 529)),
            Maps.immutableEntry("Alte Scheune", new BlockPos(552, 66, 566)),
            Maps.immutableEntry("Frachtschiff", new BlockPos(-436, 74, -12)),
            Maps.immutableEntry("Flughafen", new BlockPos(-78, 63, 637))
    );

    @Command({"schwarzmarktlocations", "schwarzmarktlocs"})
    public boolean onCommand() {
        Message.Builder builder = Message.builder();

        builder.of("» ").color(TextFormatting.DARK_GRAY).advance()
                .of("Positionen aller möglichen Schwarzmärkte\n").color(TextFormatting.DARK_AQUA).advance();

        for (Map.Entry<String, BlockPos> entry : BLACK_MARKET_LIST) {
            String blackMarketName = entry.getKey();
            BlockPos blockPos = entry.getValue();

            builder.of("  * ").color(TextFormatting.DARK_GRAY).advance()
                    .of(blackMarketName + ": ").color(TextFormatting.GRAY).advance()
                    .messageParts(NavigationUtil.getRawNavigationMessage(blockPos).getMessageParts())
                    .newLine();
        }

        builder.send();
        return true;
    }
}
