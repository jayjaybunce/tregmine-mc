package info.tregmine.database.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.dbcp.BasicDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import com.mysql.jdbc.exceptions.jdbc4.CommunicationsException;

import info.tregmine.Tregmine;
import info.tregmine.database.DAOException;
import info.tregmine.database.IContext;
import info.tregmine.database.IContextFactory;

public class DBContextFactory implements IContextFactory {
	private BasicDataSource ds;
	private Map<String, LoggingConnection.LogEntry> queryLog;
	private Tregmine plugin;
	
	private String driver;
	private String url;
	private String user;
	private String password;

	public DBContextFactory(FileConfiguration config, Tregmine instance) {
		queryLog = new HashMap<>();

		String driver = config.getString("db.driver");
		if (driver == null) {
			driver = "com.mysql.jdbc.Driver";
		}

		try {
			Class.forName(driver).newInstance();
		} catch (ClassNotFoundException ex) {
			throw new RuntimeException(ex);
		} catch (IllegalAccessException ex) {
			throw new RuntimeException(ex);
		} catch (InstantiationException ex) {
			throw new RuntimeException(ex);
		}

		String user = config.getString("db.user");
		String password = config.getString("db.password");
		String url = config.getString("db.url");

		ds = new BasicDataSource();
		ds.setDriverClassName(driver);
		ds.setUrl(url);
		ds.setUsername(user);
		ds.setPassword(password);
		ds.setMaxActive(5);
		ds.setMaxIdle(5);
		ds.setDefaultAutoCommit(true);
		
		this.driver = driver;
		this.url = url;
		this.user = user;
		this.password = password;

		this.plugin = instance;
	}
	
	public BasicDataSource getDataSource(){
		return this.ds;
	}

	@Override
	public IContext createContext() throws DAOException {
		try {
			// It's the responsibility of the context to make sure that the
			// connection is correctly closed
			Connection conn = ds.getConnection();
			try (Statement stmt = conn.createStatement()) {
				stmt.execute("SET NAMES latin1");
			}

			return new DBContext(new LoggingConnection(conn, queryLog), plugin);
		} catch (SQLException e) {
			throw new DAOException(e);
		}
	}

	public Map<String, LoggingConnection.LogEntry> getLog() {
		return queryLog;
	}
}
