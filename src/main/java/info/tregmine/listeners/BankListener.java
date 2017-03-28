package info.tregmine.listeners;

import com.google.common.collect.Maps;
import info.tregmine.Tregmine;
import info.tregmine.api.Account;
import info.tregmine.api.Bank;
import info.tregmine.api.TregminePlayer;
import info.tregmine.api.TregminePlayer.ChatState;
import info.tregmine.database.DAOException;
import info.tregmine.database.IBankDAO;
import info.tregmine.database.IContext;
import info.tregmine.database.IWalletDAO;
import info.tregmine.quadtree.Point;
import info.tregmine.zones.Lot;
import info.tregmine.zones.ZoneWorld;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Map;

public class BankListener implements Listener {
    private Tregmine plugin;
    private Map<TregminePlayer, Account> accounts = Maps.newHashMap();
    private Map<TregminePlayer, Bank> bankingPlayers = Maps.newHashMap();

    public BankListener(Tregmine plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {

        TregminePlayer player = plugin.getPlayer(event.getPlayer());
        if (player.getChatState() != ChatState.BANK) {
            return;
        }

        Bank bank = bankingPlayers.get(player);
        if (bank == null) {
            player.setChatState(ChatState.CHAT);
            return;
        }
        String[] args = event.getMessage().split(" ");
        String message = event.getMessage();
        event.setMessage("%cancel%");
        player.sendSpigotMessage(new TextComponent(ChatColor.AQUA + "[BANK] " + ChatColor.WHITE + "<"),
                player.getChatName(), new TextComponent(ChatColor.WHITE + "> " + ChatColor.AQUA + message));
        event.setCancelled(true);

        if ("help".equalsIgnoreCase(args[0])) {
            player.sendStringMessage(ChatColor.RED + "[BANK] " + "Type \"exit\" to go back to chat");
            player.sendStringMessage(
                    ChatColor.GREEN + "[BANK] " + "Type \"withdraw x\" to withdraw x from your account");
            player.sendStringMessage(
                    ChatColor.GREEN + "[BANK] " + "Type \"deposit x\" to deposit x into your account, \n"
                            + "if you do not have an account, a new one will be created.");
            player.sendStringMessage(ChatColor.GREEN + "[BANK] " + "Type \"balance\" to check your current balance");
            player.sendStringMessage(ChatColor.GREEN + "[BANK] " + "Type \"account x\" to switch account number, \n"
                    + "or type \"account\" to see your account number");
            player.sendStringMessage(
                    ChatColor.GREEN + "[BANK] " + "Type \"pin [change|x]\" to verify or change your pin, \n"
                            + "or just type \"pin\" to view your pin");
            return;
        } else if ("exit".equalsIgnoreCase(args[0])) {
            player.sendStringMessage("Thanks for stopping by, you're now in chat!");
            player.setChatState(ChatState.CHAT);
            accounts.remove(player);
            bankingPlayers.remove(player);
            return;
        }

        try (IContext ctx = plugin.createContext()) {
            IBankDAO dao = ctx.getBankDAO();
            IWalletDAO wDao = ctx.getWalletDAO();

            Account acct;
            if (accounts.containsKey(player)) {
                acct = accounts.get(player);
            } else {
                acct = dao.getAccountByPlayer(bank, player.getId());
                if (acct != null) {
                    acct.setVerified(true);
                    accounts.put(player, acct);
                }
            }

            if ("withdraw".equalsIgnoreCase(args[0])) {
                if (acct == null) {
                    player.sendStringMessage(ChatColor.RED + "[BANK] Open an account by making an deposit.");
                    return;
                }
                if (!acct.isVerified()) {
                    player.sendStringMessage(ChatColor.RED + "[BANK] Please verify pin before continuing.");
                    return;
                }

                if (args.length == 2) {
                    try {
                        long amount = Long.parseLong(args[1]);
                        if (dao.withdraw(bank, acct, player.getId(), amount)) {
                            wDao.add(player, amount);

                            // update local object
                            acct = dao.getAccount(bank, acct.getAccountNumber());
                            acct.setVerified(true);
                            accounts.put(player, acct);

                            player.sendStringMessage(ChatColor.AQUA + "[BANK] " + "You withdrew " + ChatColor.GOLD
                                    + amount + ChatColor.AQUA + ChatColor.AQUA + " tregs from your bank.");
                            player.sendStringMessage(ChatColor.AQUA + "[BANK] " + "You now have " + ChatColor.GOLD
                                    + acct.getBalance() + " tregs in your bank");
                        } else {
                            player.sendStringMessage(
                                    ChatColor.RED + "[BANK] " + "You do not have that much money in your bank");
                        }
                    } catch (NumberFormatException e) {
                        player.sendStringMessage(ChatColor.RED + "[BANK] " + args[1] + " is not a number");
                    }
                }

                return;
            } else if ("deposit".equalsIgnoreCase(args[0])) {
                if (args.length != 2) {
                    return;
                }

                if (acct != null && !acct.isVerified()) {
                    player.sendStringMessage(ChatColor.RED + "[BANK] Please verify pin before continuing.");
                    return;
                }

                long amount;
                try {
                    amount = Long.parseLong(args[1]);
                } catch (NumberFormatException e) {
                    player.sendStringMessage(ChatColor.RED + "[BANK] " + args[1] + " is not a number");
                    return;
                }

                if (acct == null) {
                    acct = new Account();
                    acct.setBank(bank);
                    acct.setPlayerId(player.getId());
                    acct.setVerified(true);
                    dao.createAccount(acct, player.getId());
                    accounts.put(player, acct);

                    player.sendStringMessage(ChatColor.AQUA + "[BANK] " + "You created a new account with a "
                            + "balance of " + ChatColor.GOLD + amount + ChatColor.AQUA + " tregs");
                    player.sendStringMessage(ChatColor.GREEN + "[BANK] " + "Your account number is " + ChatColor.GOLD
                            + acct.getAccountNumber());
                    player.sendStringMessage(ChatColor.GREEN + "[BANK] " + "Your pin number is " + ChatColor.GOLD
                            + acct.getPin() + ChatColor.GREEN + " WRITE IT DOWN!");
                }

                if (wDao.take(player, amount)) {
                    dao.deposit(bank, acct, player.getId(), amount);

                    // update local object
                    acct = dao.getAccount(bank, acct.getAccountNumber());
                    acct.setVerified(true);
                    accounts.put(player, acct);

                    player.sendStringMessage(ChatColor.AQUA + "[BANK] " + "You deposited " + ChatColor.GOLD + amount
                            + ChatColor.AQUA + " tregs " + "into your account");
                } else {
                    player.sendStringMessage(
                            ChatColor.RED + "[BANK] " + "You do not have enough money to deposit that much.");
                }

                return;
            } else if ("balance".equalsIgnoreCase(args[0])) {
                if (acct == null) {
                    player.sendStringMessage(ChatColor.RED + "[BANK] Open an account by making an deposit.");
                    return;
                }
                if (!acct.isVerified()) {
                    player.sendStringMessage(ChatColor.RED + "[BANK] " + "Please verify pin before continuing.");
                    return;
                }

                player.sendStringMessage(ChatColor.AQUA + "[BANK] " + "Your account balance is " + ChatColor.GOLD
                        + acct.getBalance() + ChatColor.AQUA + " tregs");

                return;
            } else if ("account".equalsIgnoreCase(args[0])) {
                if (args.length == 1) {
                    player.sendStringMessage(ChatColor.GREEN + "[BANK] + Your account " + "number is " + ChatColor.GOLD
                            + acct.getAccountNumber());
                    return;
                } else if (args.length == 2) {
                    int i = 0;
                    try {
                        i = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        i = acct.getAccountNumber();
                    }

                    Account newAccount = dao.getAccount(bank, i);
                    if (newAccount == null) {
                        player.sendStringMessage(ChatColor.GREEN + "[BANK] " + "No such account: " + i);
                        return;
                    }

                    accounts.put(player, newAccount);
                    player.sendStringMessage(ChatColor.GREEN + "[BANK] " + "Switched to account " + i);
                    return;
                }
            } else if ("pin".equalsIgnoreCase(args[0])) {
                if (acct == null) {
                    player.sendStringMessage(ChatColor.RED + "[BANK] Open an account by making an deposit.");
                    return;
                }
                if (args.length == 1) {
                    if (acct.getPlayerId() != player.getId()) {
                        player.sendStringMessage(
                                ChatColor.RED + "[BANK] To recieve the pin from another player's account, "
                                        + " you must get it directly from that player.");
                    }
                    player.sendStringMessage(
                            ChatColor.GREEN + "[BANK] + Your pin " + "number is " + ChatColor.GOLD + acct.getPin());
                    return;
                }
                if ("change".equalsIgnoreCase(args[1])) {
                    if (!acct.isVerified()) {
                        player.sendStringMessage(
                                ChatColor.RED + "[BANK] " + "Account must be verified before changing pin");
                        return;
                    }

                    String s = args[2];
                    if (s.length() > 4) {
                        player.sendStringMessage(ChatColor.RED + "[BANK] " + "Pin must be 4 digits long");
                        return;
                    } else {
                        dao.setPin(acct, s);
                        acct.setPin(s);
                        player.sendStringMessage(ChatColor.GREEN + "[BANK] " + "Changed pin to " + ChatColor.GOLD + s);
                        return;
                    }
                } else {
                    if (acct.isVerified()) {
                        player.sendStringMessage(ChatColor.GREEN + "[BANK] " + "No need, the account is verified");
                        return;
                    }

                    int i;
                    try {
                        i = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        player.sendStringMessage(ChatColor.RED + "[BANK] " + args[1] + " is not a number");
                        return;
                    }

                    if (i == Integer.parseInt(acct.getPin())) {
                        accounts.get(player).setVerified(true);
                        player.sendStringMessage(ChatColor.GREEN + "[BANK] Correct Pin");
                    } else {
                        player.sendStringMessage(ChatColor.RED + "[BANK] Incorrect pin");
                    }
                }
            } else {
                player.sendStringMessage(ChatColor.RED + "Unknown command: " + args[0]);
                return;
            }

        } catch (DAOException e) {
            throw new RuntimeException(e);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block.getType() != Material.IRON_BLOCK) {
            return;
        }

        TregminePlayer player = plugin.getPlayer(event.getPlayer());

        ZoneWorld world = plugin.getWorld(player.getWorld());
        Point p = new Point(block.getLocation().getBlockX(), block.getLocation().getBlockZ());
        Lot lot = world.findLot(p);
        if (lot == null) {
            return;
        }

        if (!lot.hasFlag(Lot.Flags.BANK)) {
            return;
        }

        try (IContext ctx = plugin.createContext()) {
            IBankDAO dao = ctx.getBankDAO();
            Bank bank = dao.getBank(lot.getId());
            if (bank == null) {
                Tregmine.LOGGER.info("Warning! No bank found for " + lot.getId() + "even though bank flag is set.");
                return;
            }

            Bank currentBank = bankingPlayers.get(player);
            if (player.getChatState() == ChatState.BANK && currentBank != null && currentBank.getId() == bank.getId()) {
                // Moved
                return;
            } else {
                String name = lot.getName().split("\\.")[0];
                player.setChatState(ChatState.BANK);
                player.sendStringMessage(
                        ChatColor.GREEN + "[BANK] " + "Welcome to " + name + "! Type \"help\" for help.");
                bankingPlayers.put(player, bank);
                accounts.remove(player);
            }
        } catch (DAOException e) {
            throw new RuntimeException(e);
        }
    }

    @EventHandler
    public void onPunchInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block.getType() != Material.IRON_BLOCK) {
            return;
        }
        TregminePlayer player = plugin.getPlayer(event.getPlayer());

        ZoneWorld world = plugin.getWorld(player.getWorld());
        Point p = new Point(block.getLocation().getBlockX(), block.getLocation().getBlockZ());
        Lot lot = world.findLot(p);
        if (lot == null) {
            return;
        }

        if (!lot.hasFlag(Lot.Flags.BANK)) {
            return;
        }
        try (IContext ctx = plugin.createContext()) {
            IBankDAO dao = ctx.getBankDAO();
            Bank bank = dao.getBank(lot.getId());
            Bank currentBank = bankingPlayers.get(player);
            if (player.getChatState() == ChatState.BANK && currentBank != null && currentBank.getId() == bank.getId()) {

                player.setChatState(ChatState.CHAT);
                player.sendStringMessage(ChatColor.GREEN + "Thanks for stopping by, you're now in chat!");
                bankingPlayers.remove(player);
                accounts.remove(player);
            } else {
                player.sendStringMessage(ChatColor.RED + "You cannot left click until you are in the bank.");
            }
        } catch (DAOException e) {
            throw new RuntimeException(e);
        }
    }

}
