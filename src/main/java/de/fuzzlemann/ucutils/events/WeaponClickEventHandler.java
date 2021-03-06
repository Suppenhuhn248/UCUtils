package de.fuzzlemann.ucutils.events;

import com.google.common.collect.ImmutableSet;
import de.fuzzlemann.ucutils.Main;
import de.fuzzlemann.ucutils.config.UCUtilsConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Set;

/**
 * @author Fuzzlemann
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber
public class WeaponClickEventHandler {

    private static final Set<String> WEAPONS = ImmutableSet.of("§8M4", "§8MP5", "§8Pistole", "§8Jagdflinte");

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteract(PlayerInteractEvent e) {
        if (!UCUtilsConfig.munitionDisplay) return;
        if (!(e instanceof PlayerInteractEvent.RightClickItem || e instanceof PlayerInteractEvent.RightClickBlock || e instanceof PlayerInteractEvent.EntityInteractSpecific))
            return;

        ItemStack is = e.getItemStack();
        if (!isWeapon(is)) return;

        handleMunitionDisplay(is);
    }

    private static void handleMunitionDisplay(ItemStack is) {
        String text = getText(is);
        if (text == null) return;

        Main.MINECRAFT.ingameGUI.setOverlayMessage(text, true);
    }

    private static String getText(ItemStack is) {
        NBTTagCompound nbt = is.getTagCompound();
        if (nbt == null) return null;

        NBTTagCompound display = nbt.getCompoundTag("display");

        String lore = display.getTagList("Lore", Constants.NBT.TAG_STRING).getStringTagAt(0);
        String[] splittedLore = lore.split("/");
        if (splittedLore.length != 2) return null;

        String munitionString = splittedLore[0];
        if (munitionString.length() < 2) return null;

        int munition = Integer.parseInt(munitionString.substring(2));

        return (--munition < 1 ? "§c0" : "§6" + munition) + "§b/§6" + splittedLore[1];
    }

    private static boolean isWeapon(ItemStack is) {
        if (is == null) return false;

        return WEAPONS.contains(is.getDisplayName());
    }
}
