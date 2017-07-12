//This timer is a butchered version of the Essentials plug-in timer, partial credit to Essentials and its creators
package info.tregmine.api;

import info.tregmine.Tregmine;
import info.tregmine.api.GenericPlayer.Flags;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;

import java.util.logging.Level;

public class Timer implements Runnable {
    private final long maxTime = 10 * 1000000;
    Tregmine t;
    private transient long lastPoll = System.nanoTime();
    private int skip1 = 0;

    public Timer(Tregmine plugin) {
        t = plugin;
    }

    @Override
    public void run() {
        final long startTime = System.nanoTime();

        long timeSpent = (startTime - lastPoll) / 1000;
        if (timeSpent == 0) {
            timeSpent = 1;
        }
        lastPoll = startTime;
        int count = 0;
        for (GenericPlayer player : t.getOnlinePlayers()) {
            count++;
            if (skip1 > 0) {
                skip1--;
                continue;
            }
            if (count % 10 == 0) {
                if (System.nanoTime() - startTime > maxTime / 2) {
                    skip1 = count - 1;
                }
            }
            try {
                if (player.getWorld().getName().equalsIgnoreCase("vanilla")) {
                    if (player.hasFlag(Flags.FLY_ENABLED) || player.isFlying()) {
                        player.removeFlag(Flags.FLY_ENABLED);
                        player.setFlying(false);
                        TextComponent message = new TextComponent(ChatColor.RED
                                + "You're not allowed to fly in this world, your flying has been disabled.");
                        message.setHoverEvent(
                                t.buildHover(ChatColor.RED + "The vanilla world has opted to remove flying."));
                        player.sendMessage(message);
                    }
                    if (player.getGameMode() != GameMode.SURVIVAL) {
                        player.setGameMode(GameMode.SURVIVAL);
                        TextComponent message = new TextComponent(
                                ChatColor.RED + "You must be in survival mode in this world.");
                        message.setHoverEvent(
                                t.buildHover(ChatColor.RED + "The vanilla world has opted to remove survival mode."));
                        player.sendMessage(message);

                    }
                }
                player.checkActivity();

            } catch (Exception e) {
                t.getLogger().log(Level.WARNING, "Timer Exception", e);
            }
        }
        // try {
        // Thread.sleep(30 * 1000);
        // } catch (InterruptedException e) {
        // e.printStackTrace();
        // }
    }
}