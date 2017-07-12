package info.tregmine.web;

import info.tregmine.Tregmine;
import info.tregmine.WebHandler;
import info.tregmine.api.GenericPlayer;
import info.tregmine.api.PlayerReport;
import info.tregmine.database.DAOException;
import info.tregmine.database.IContext;
import info.tregmine.database.IPlayerReportDAO;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.eclipse.jetty.server.Request;
import org.json.JSONException;
import org.json.JSONWriter;

import java.io.PrintWriter;

import static org.bukkit.ChatColor.AQUA;

public class PlayerKickAction implements WebHandler.Action {
    private int subjectId;
    private int issuerId;
    private String message;
    private boolean status;
    private String error;

    public PlayerKickAction(int subjectId, int issuerId, String message) {
        this.subjectId = subjectId;
        this.issuerId = issuerId;
        this.message = message;

        this.status = true;
        this.error = null;
    }

    @Override
    public void generateResponse(PrintWriter writer) throws WebHandler.WebException {
        try {
            JSONWriter json = new JSONWriter(writer);
            json.object().key("status").value(status ? "ok" : "error").key("error").value(error).endObject();

            writer.close();
        } catch (JSONException e) {
            throw new WebHandler.WebException(e);
        }
    }

    @Override
    public void queryGameState(Tregmine tregmine) {
        GenericPlayer subject = tregmine.getPlayer(subjectId);
        if (subject == null) {
            status = false;
            error = "Subject not found.";
            Tregmine.LOGGER.info("Web: PlayerKickAction - Subject not found: " + subjectId);
            return;
        }

        GenericPlayer issuer = tregmine.getPlayer(issuerId);
        if (issuer == null) {
            issuer = tregmine.getPlayerOffline(issuerId);
        }

        subject.kickPlayer(tregmine, "Kicked by " + issuer.getChatNameNoHover() + ChatColor.WHITE + ": " + message);

        Tregmine.LOGGER.info(subject.getChatName() + " was kicked by " + issuer.getName() + " (from web)");
        tregmine.broadcast(new TextComponent(issuer.getChatNameNoHover() + "" + AQUA + " kicked "),
                subject.getChatName(), new TextComponent(AQUA + ": " + message));

        if (status) {
            try (IContext ctx = tregmine.createContext()) {
                PlayerReport report = new PlayerReport();
                report.setSubjectId(subjectId);
                report.setIssuerId(issuerId);
                report.setAction(PlayerReport.Action.KICK);
                report.setMessage(message);

                IPlayerReportDAO reportDAO = ctx.getPlayerReportDAO();
                reportDAO.insertReport(report);
            } catch (DAOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class Factory implements WebHandler.ActionFactory {
        public Factory() {
        }

        @Override
        public WebHandler.Action createAction(Request request) throws WebHandler.WebException {
            try {
                int subjectId = Integer.parseInt(request.getParameter("subjectId"));
                int issuerId = Integer.parseInt(request.getParameter("issuerId"));
                String message = request.getParameter("message");
                return new PlayerKickAction(subjectId, issuerId, message);
            } catch (NullPointerException e) {
                throw new WebHandler.WebException(e);
            } catch (NumberFormatException e) {
                throw new WebHandler.WebException(e);
            }
        }

        @Override
        public String getName() {
            return "/playerkick";
        }
    }
}