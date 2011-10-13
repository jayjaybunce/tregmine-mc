package info.tregmine.database;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
//import java.sql.*;

public class Mysql {

	public Connection connect = null;
	public Statement statement = null;
	//	private PreparedStatement preparedStatement = null;
	public ResultSet resultSet = null;

	private String url = null; //"jdbc:mysql://127.0.0.1/minecraft";
	private String user = null; // "minecraft";
	private String pw = null; //"cUqeWemUGeYaquxUpHaye8rUcrAteWre";
	//private String url = "jdbc:mysql://mc.tregmine.info:3306/minecraft"; 
	//private String user = "minecraft"; 
	//private String pw = "cUqeWemUGeYaquxUpHaye8rUcrAteWre";

	// cUqeWemUGeYaquxUpHaye8rUcrAteWre

	public Mysql() {
		try {
			Class.forName("com.mysql.jdbc.Driver");


		} catch (ClassNotFoundException ex) {
			ex.printStackTrace();
		}
		//       		this.connect();

		Properties settings = new Properties();
		try {
			settings.load(new FileInputStream("mysql.cfg"));
			this.url = settings.getProperty("url");
			this.user = settings.getProperty("user");
			this.pw = settings.getProperty("password");
		}
		catch(IOException e) {
			e.printStackTrace();
		}

	}

	public void connect(){
		try {
			this.connect = DriverManager.getConnection( url, user, pw );
			this.statement = this.connect.createStatement();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void close(){
		try {
			this.connect.close();
			this.statement.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
