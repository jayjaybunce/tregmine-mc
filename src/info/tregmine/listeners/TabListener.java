package info.tregmine.listeners;

import info.tregmine.Tregmine;
import info.tregmine.api.TregminePlayer;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatTabCompleteEvent;

public class TabListener implements Listener//, TabCompleter
{
    private Tregmine plugin;

    public TabListener(Tregmine instance)
    {
        this.plugin = instance;
    }


    @EventHandler
    public void tabcomplete(PlayerChatTabCompleteEvent e) {

        TregminePlayer p = plugin.getPlayer(e.getPlayer());
        if (p.getRank().canGetTrueTab()) {
            return;
        }

        e.getTabCompletions().clear()
        List<String> nonOps = new ArrayList<String>();

        for (Player player : Bukkit.getOnlinePlayers ()) {
            if (!player.isOp())
                nonOps.add(player.getName());
        }

        e.getTabCompletions().addAll(nonOps);
    }
}


