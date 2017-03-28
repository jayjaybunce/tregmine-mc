package info.tregmine.commands;

import info.tregmine.Tregmine;
import info.tregmine.api.TregminePlayer;
import info.tregmine.database.DAOException;
import info.tregmine.database.IContext;
import info.tregmine.database.IPlayerDAO;

import static org.bukkit.ChatColor.AQUA;
import static org.bukkit.ChatColor.RED;

public class TeleportShieldCommand extends AbstractCommand {
    public TeleportShieldCommand(Tregmine tregmine) {
        super(tregmine, "tpshield");
    }

    @Override
    public boolean handlePlayer(TregminePlayer player, String[] args) {
        if (!player.getRank().canShieldTeleports()) {
            return true;
        }

        if (args.length < 1) {
            player.sendStringMessage(
                    "Your tpblock is set to " + (player.hasFlag(TregminePlayer.Flags.TPSHIELD) ? "on" : "off") + ".");
            return true;
        }

        String state = args[0];

        if ("on".equalsIgnoreCase(state)) {
            player.setFlag(TregminePlayer.Flags.TPSHIELD);
            player.sendStringMessage(AQUA + "Teleportation is now blocked to you.");
        } else if ("off".equalsIgnoreCase(state)) {
            player.removeFlag(TregminePlayer.Flags.TPSHIELD);
            player.sendStringMessage(AQUA + "Teleportation is now allowed to you.");
        } else if ("status".equalsIgnoreCase(state)) {
            player.sendStringMessage(
                    "Your tpblock is set to " + (player.hasFlag(TregminePlayer.Flags.TPSHIELD) ? "on" : "off") + ".");
        } else {
            player.sendStringMessage(RED + "The commands are /tpblock on, /tpblock off and /tpblock status.");
        }

        try (IContext ctx = tregmine.createContext()) {
            IPlayerDAO playerDAO = ctx.getPlayerDAO();
            playerDAO.updatePlayer(player);
        } catch (DAOException e) {
            throw new RuntimeException(e);
        }

        return true;
    }
}
