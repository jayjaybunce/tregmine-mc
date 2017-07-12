package info.tregmine.commands;

import info.tregmine.Tregmine;
import info.tregmine.api.GenericPlayer;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;

import static org.bukkit.ChatColor.YELLOW;

public class ChannelCommand extends AbstractCommand {
    public ChannelCommand(Tregmine tregmine) {
        super(tregmine, "channel");
    }

    @Override
    public boolean handlePlayer(GenericPlayer player, String[] args) {
        if (args.length != 1) {
            return false;
        }

        String channel = args[0];
        String oldchannel = player.getChatChannel();

        player.sendMessage(YELLOW + "You are now talking in channel " + channel + ".");
        player.sendMessage(YELLOW + "Write /channel global to switch to " + "the global chat.");
        player.setChatChannel(channel);

        if (player.hasFlag(GenericPlayer.Flags.INVISIBLE))
            return true; // Doesn't announce channel change if invisible.

        if (oldchannel.equalsIgnoreCase(channel)) {
            return true;
        }

        for (GenericPlayer players : tregmine.getOnlinePlayers()) {
            if (oldchannel.equalsIgnoreCase(players.getChatChannel())) {
                players.sendMessage(player.decideVS(player),
                        new TextComponent(ChatColor.YELLOW + " has left channel " + oldchannel));
            } else if (channel.equalsIgnoreCase(players.getChatChannel())) {
                players.sendMessage(player.decideVS(players),
                        new TextComponent(ChatColor.YELLOW + " has joined channel " + channel));
            }
        }

        return true;
    }
}