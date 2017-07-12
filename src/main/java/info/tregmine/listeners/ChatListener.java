package info.tregmine.listeners;

import info.tregmine.Tregmine;
import info.tregmine.api.GenericPlayer;
import info.tregmine.api.Notification;
import info.tregmine.commands.VersionCommand;
import info.tregmine.database.*;
import info.tregmine.events.TregmineChatEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player.Spigot;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;
import java.util.regex.Pattern;

public class ChatListener implements Listener {
    private Tregmine plugin;

    public ChatListener(Tregmine instance) {
        this.plugin = instance;
    }

    private TextComponent createTC(String str) {
        return new TextComponent(str);
    }

    @EventHandler
    public void onPlayerCommandPreprocessEvent(PlayerCommandPreprocessEvent event) {
        if (!event.getMessage().equalsIgnoreCase("/version")) {
            return;
        }
        event.setCancelled(true);
        new VersionCommand(plugin).handlePlayer(this.plugin.getPlayer(event.getPlayer()), new String[0]);
        return;
    }

    @EventHandler
    public void onTregmineChat(TregmineChatEvent event) {
        GenericPlayer sender = event.getPlayer();
        if (sender.isMuted() && !sender.getMute().isExpired() && !sender.getMute().isCancelled()) {
            sender.sendMessage(ChatColor.YELLOW + "You have been muted; Your mute will expire in "
                    + sender.getMute().secondsLeft() + " seconds.");
            event.setCancelled(true);
            return;
        }
        String channel = sender.getChatChannel();
        if (event.getMessage().contains("%cancel%")) {
            event.setCancelled(true);
            return;
        }
        if (sender.isAfk()) {
            sender.setAfk(false);
        }

        try (IContext ctx = plugin.createContext()) {
            IPlayerDAO playerDAO = ctx.getPlayerDAO();
            String text = event.getMessage();
            for (GenericPlayer to : plugin.getOnlinePlayers()) {
                if (to.getChatState() == GenericPlayer.ChatState.SETUP) {
                    continue;
                }

                if (!sender.getRank().canNotBeIgnored()) {
                    if (playerDAO.doesIgnore(to, sender)) {
                        continue;
                    }
                }

                ChatColor txtColor = ChatColor.WHITE;

                for (GenericPlayer online : plugin.getOnlinePlayers()) {

                    if (text.contains(online.getRealName()) && !online.hasFlag(GenericPlayer.Flags.INVISIBLE)) {
                        if (text.toLowerCase().contains("@" + online.getRealName())) {

                            text = text.replaceAll("@" + online.getRealName(), ChatColor.ITALIC + ""
                                    + plugin.getRankColor(online.getRank()) + "@" + online.getChatNameNoHover());
                        } else {
                            text = text.replaceAll(online.getRealName(), online.getChatNameNoHover() + txtColor);
                        }
                        online.sendNotification(Notification.MESSAGE);
                    }

                }

                if (sender.getRank().canUseChatColors()) {
                    text = ChatColor.translateAlternateColorCodes('#', text);
                }

                List<String> player_keywords = playerDAO.getKeywords(to);

                if (player_keywords.size() > 0 && player_keywords != null) {
                    for (String keyword : player_keywords) {
                        if (text.toLowerCase().contains(keyword.toLowerCase())) {
                            text = text.replaceAll(Pattern.quote(keyword), ChatColor.AQUA + keyword + txtColor);
                        }
                    }
                }

                String frontBracket = "<";
                String endBracket = ChatColor.WHITE + "> ";
                String senderChan = sender.getChatChannel();
                String toChan = to.getChatChannel();
                Spigot toSpigot = to.getSpigot();

                TextComponent sendername;
                if (to.getIsStaff()) {
                    sendername = sender.getChatNameStaff();
                } else {
                    sendername = sender.getChatName();
                }
                if (senderChan.equalsIgnoreCase(toChan) || to.hasFlag(GenericPlayer.Flags.CHANNEL_VIEW)) {

                    if (event.isWebChat()) {
                        if ("GLOBAL".equalsIgnoreCase(senderChan)) {
                            TextComponent begin = createTC("(");
                            TextComponent end = createTC(ChatColor.WHITE + ") " + txtColor + text);
                            toSpigot.sendMessage(begin, sendername, end);
                        } else {
                            TextComponent begin = createTC(channel + "(");
                            TextComponent end = createTC(ChatColor.WHITE + ") " + txtColor + text);
                            toSpigot.sendMessage(begin, sendername, end);
                        }
                    } else {
                        if ("GLOBAL".equalsIgnoreCase(senderChan)) {
                            TextComponent begin = createTC(frontBracket);
                            TextComponent end = createTC(ChatColor.WHITE + endBracket + txtColor + text);
                            toSpigot.sendMessage(begin, sendername, end);
                        } else {
                            TextComponent begin = createTC(channel + frontBracket);
                            TextComponent end = createTC(ChatColor.WHITE + endBracket + txtColor + text);
                            toSpigot.sendMessage(begin, sendername, end);
                        }
                    }
                }

                if (text.contains(to.getRealName()) && "GLOBAL".equalsIgnoreCase(senderChan)
                        && !"GLOBAL".equalsIgnoreCase(toChan)) {

                    to.sendMessage(
                            new TextComponent(
                                    ChatColor.BLUE + "You were mentioned in GLOBAL by " + sender.getNameColor()),
                            sender.getChatName());
                }
            }
        } catch (DAOException e) {
            throw new RuntimeException(e);
        }

        if (event.isWebChat()) {
            Tregmine.LOGGER.info(channel + " (" + sender.getName() + ") " + event.getMessage());
        } else {
            Tregmine.LOGGER.info(channel + " <" + sender.getPlayer().getName() + "> " + event.getMessage());
        }
        try (IContext ctx = plugin.createContext()) {
            ILogDAO logDAO = ctx.getLogDAO();
            logDAO.insertChatMessage(sender, channel, event.getMessage());
        } catch (DAOException e) {
            throw new RuntimeException(e);
        }
    }
}