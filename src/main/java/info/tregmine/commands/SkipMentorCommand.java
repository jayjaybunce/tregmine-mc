package info.tregmine.commands;

import info.tregmine.Tregmine;
import info.tregmine.api.GenericPlayer;
import info.tregmine.api.GenericPlayer.ChatState;
import info.tregmine.api.GenericPlayer.Flags;
import info.tregmine.api.Rank;
import info.tregmine.database.DAOException;
import info.tregmine.database.IContext;
import info.tregmine.database.IPlayerDAO;
import org.bukkit.ChatColor;

import java.util.List;

import static org.bukkit.ChatColor.RED;

public class SkipMentorCommand extends AbstractCommand {

    public SkipMentorCommand(Tregmine tregmine) {
        super(tregmine, "skipmentor");
    }

    @Override
    public boolean handlePlayer(GenericPlayer player, String[] args) {
        if (player.getRank() != Rank.SENIOR_ADMIN && player.getRank() != Rank.JUNIOR_ADMIN
                && player.getRank() != Rank.GUARDIAN) {
            player.sendMessage(RED
                    + "You are not authorized to perform that command. All online administrators have been notified.");
            return true;
        }
        if (args.length != 1) {
            // Player didn't enter two arguments, terminate.
            player.sendMessage(ChatColor.RED + "Invalid arguments! Use /skipmentor <player>");
            return true;
        }
        // The checks have finished, perform the command
        String possibleuser = args[0];
        List<GenericPlayer> candidate = tregmine.matchPlayer(possibleuser);
        if (candidate.size() != 1) {
            player.sendMessage(RED + "The player specified was not found. Please try again.");
            return true;
        }
        GenericPlayer user = candidate.get(0);
        if (user.hasFlag(Flags.HARDWARNED)) {
            // Players with a hardwarn cannot be promoted using this command.
            // They must be promoted manually.
            player.sendMessage(
                    RED + "The player specified has been hardwarned and is not eligible for promotion.");
            return true;
        }
        if (user.getRank() == Rank.UNVERIFIED || user.getRank() == Rank.TOURIST) {
            // Any other errors have now been checked and dealt with. Promote
            // the user.
            try (IContext ctx = tregmine.createContext()) {
                user.setRank(Rank.SETTLER);
                user.setMentor(null);
                user.setChatState(ChatState.CHAT);
                IPlayerDAO playerDAO = ctx.getPlayerDAO();
                playerDAO.updatePlayer(user);
                playerDAO.updatePlayerInfo(user);
            } catch (DAOException e) {
                throw new RuntimeException(e);
            }
            return true;
        } else {
            player.sendMessage(
                    RED + "This player cannot skip mentoring because their rank does not qualify them.");
            return true;
        }

    }

}