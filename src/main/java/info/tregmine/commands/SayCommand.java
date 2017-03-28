package info.tregmine.commands;

import info.tregmine.Tregmine;
import info.tregmine.api.TregminePlayer;
import info.tregmine.discord.entities.EmbedAlertType;
import info.tregmine.discord.entities.TregmineEmbedBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.Collection;

import static org.bukkit.ChatColor.*;

public class SayCommand extends AbstractCommand {
    public SayCommand(Tregmine tregmine) {
        super(tregmine, "say");
    }

    private String argsToMessage(String[] args) {
        StringBuffer buf = new StringBuffer();
        buf.append(args[0]);
        for (int i = 1; i < args.length; ++i) {
            buf.append(" ");
            buf.append(args[i]);
        }

        return buf.toString();
    }

    @Override
    public boolean handleOther(Server server, String[] args) {
        String msg = argsToMessage(args);

        server.broadcastMessage("<" + BLUE + "GOD" + WHITE + "> " + LIGHT_PURPLE + msg);
        LOGGER.info("CONSOLE: <GOD> " + msg);
        if (this.tregmine.dsvEnabled()) {
            this.tregmine.getDiscordSRV().sendMessage(this.tregmine.getDiscordSRV().getChatChannel(),
                    TregmineEmbedBuilder.genericEmbed(EmbedAlertType.SAY.getDisplayName(), msg, Color.YELLOW));
        }
        return true;
    }

    @Override
    public boolean handlePlayer(TregminePlayer player, String[] args) {
        if (!player.getRank().canBeGod()) {
            return true;
        }
        if (args.length == 0) {
            player.sendStringMessage(RED + "WHERE ARE YOUR ARGUMENTS????!!!!");
            return true;
        }
        Server server = player.getServer();
        String msg = ChatColor.translateAlternateColorCodes('#', argsToMessage(args));

        server.broadcastMessage("<" + RED + "GOD" + WHITE + "> " + LIGHT_PURPLE + msg);

        LOGGER.info(player.getName() + ": <GOD> " + msg);

        if (this.tregmine.dsvEnabled()) {
            this.tregmine.getDiscordSRV().sendMessage(this.tregmine.getDiscordSRV().getChatChannel(),
                    TregmineEmbedBuilder.genericEmbed(EmbedAlertType.SAY.getDisplayName(), msg, Color.YELLOW));
        }

        Collection<? extends Player> players = server.getOnlinePlayers();
        for (Player p : players) {
            TregminePlayer current = tregmine.getPlayer(p);
            if (current.getRank().canBeGod()) {
                current.sendSpigotMessage(new TextComponent(DARK_AQUA + "/say used by: "), player.getChatName());
            }
        }

        return true;
    }
}
