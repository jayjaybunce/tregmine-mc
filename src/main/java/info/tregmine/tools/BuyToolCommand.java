package info.tregmine.tools;

import info.tregmine.Tregmine;
import info.tregmine.api.TregminePlayer;
import info.tregmine.commands.AbstractCommand;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.List;

public class BuyToolCommand extends AbstractCommand {
    Tregmine plugin;

    public BuyToolCommand(Tregmine tregmine) {
        super(tregmine, "buytool");
        plugin = tregmine;
    }

    @Override
    public boolean handlePlayer(TregminePlayer player, String[] args) {
        PlayerInventory inv = player.getInventory();
        Material price = Material.DIAMOND_BLOCK;
        int total = 16;
        int amount = 0;
        ItemStack inhand = inv.getItemInMainHand();
        if (inhand.hasItemMeta()) {
            ItemMeta itemmeta = inhand.getItemMeta();
            List<String> lore = itemmeta.getLore();
            if (lore.get(0).contains("CREATIVE") || lore.get(0).contains("SPAWNED")) {
                player.sendStringMessage(ChatColor.RED + "" + ChatColor.BOLD + "You CANNOT sell illegal items.");
                return true;
            }
        }
        amount = inhand.getAmount();
        if (amount >= total) {
            ItemStack tool = null;
            if (args.length == 0) {
                player.sendStringMessage(ChatColor.RED + "Usage: /buytool <lumber/vein>");
                return true;
            }
            switch (args[0]) {
                case "lumber":
                    tool = ToolsRegistry.LumberAxe();
                    break;
                case "vein":
                    tool = ToolsRegistry.VeinMiner();
                    break;
            }
            if (tool == null) {
                player.sendStringMessage(ChatColor.RED + "Usage: /buytool <lumber/vein>");
                return true;
            }
            HashMap<Integer, ItemStack> failedItems = player.getInventory().addItem(tool);

            if (failedItems.size() > 0) {
                player.sendStringMessage(ChatColor.RED + "You have a full inventory, Can't add tool!");
                return true;
            }
            inhand.setAmount(amount - total);
            player.setItemInHand(inhand);
            player.sendStringMessage(total + " " + price.name() + " have been taken from your inventory.");
            player.sendStringMessage(ChatColor.GREEN + "Spawned in tool token successfully!");
            return true;

        } else {
            player.sendStringMessage(
                    ChatColor.RED + "You need " + total + " " + price.name() + " in your hand to use this command.");
        }
        return true;
    }
}
