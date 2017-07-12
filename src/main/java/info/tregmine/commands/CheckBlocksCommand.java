package info.tregmine.commands;

import info.tregmine.Tregmine;
import info.tregmine.api.GenericPlayer;
import info.tregmine.database.DAOException;
import info.tregmine.database.IContext;
import info.tregmine.database.IMiscDAO;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.ChatColor;

public class CheckBlocksCommand extends AbstractCommand {

    private CoreProtectAPI coapi = new CoreProtectAPI();

    public CheckBlocksCommand(Tregmine plugin) {
        super(plugin, "allclear");
    }

    @Override
    public boolean handlePlayer(GenericPlayer player, String[] args) {
        if (!player.getRank().canCheckBlocks()) {
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(ChatColor.RED + "Please select a radius");
            return true;
        }

        int radius = 0;
        try {
            radius = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Please select a valid radius");
        }

        try (IContext ctx = tregmine.createContext()) {
            IMiscDAO dao = ctx.getMiscDAO();
            boolean bool = dao.blocksWereChanged(player.getLocation(), radius);
            if (bool) {
                player.sendMessage(ChatColor.RED + "Blocks have been edited in a radius of " + ChatColor.GOLD
                        + radius + ChatColor.RED + " from your location");
            } else {
                player.sendMessage(ChatColor.AQUA + "No blocks have been edited within " + radius + " blocks");
            }
        } catch (DAOException e) {
            throw new RuntimeException(e);
        }

        return true;
    }

}