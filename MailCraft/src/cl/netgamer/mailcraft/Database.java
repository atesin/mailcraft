package cl.netgamer.mailcraft;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;

/**
 * class to connect and get/set data from/to mysql server
 * fix package name, set connection parmeters on instantiate and test connection immediately
 * 
 * when get or set data it creates an empty structure,
 * opens a connection, a statement, executes query, puts result in the data structure,
 * and securely closes all in reverse order and returns data structure filled
 * 
 * @author atesin#gmail,com
 */
class Database
{
	/**
	 * properties: connection parameters
	 */
	private String type;
	private String host;
	private int port;
	private String base;
	private String user;
	private String pass;
	private File dbFile;
	
	/**
	 * constructor just set connection parameters, do it and test connection immediately
	 */
	protected Database(ConfigurationSection conf, MC p)
	{
		type = conf.getString("type");
		host = conf.getString("host");
		port = conf.getInt("port");
		base = conf.getString("base");
		user = conf.getString("user");
		pass = conf.getString("pass");
		conf = null;
		dbFile = new File(p.getDataFolder(), "Database.sqlite");
	}
	
	/**
	 * test connection at very starting and exit if false
	 */
	protected boolean test()
	{
		try
		{
			Connection c = connect();
			c.close();
		}
		catch (ClassNotFoundException | SQLException | IOException e) 
		{
			return false;
		}
		return true;
	}
	
	/**
	 * internal method used by others
	 * must create object with parameters and test connection first
	 */
	private Connection connect() throws ClassNotFoundException, SQLException, IOException
	{
		switch (type)
		{
		case "mysql":
			Class.forName("com.mysql.jdbc.Driver");
			return DriverManager.getConnection("jdbc:mysql://"+host+":"+port+"/"+base, user, pass);
		case "sqlite-disabled":
			if (!dbFile.exists())
				dbFile.createNewFile();
			Class.forName("org.sqlite.JDBC");
			return DriverManager.getConnection("jdbc:sqlite:"+dbFile.getPath());
		}
		return null;
	}
	
	/**
	 * @param query the query in a string
	 * @return columns and rows contained in a list with objects
	 */
	protected List<HashMap<String, Object>> getTable(String query)
	{
		List<HashMap<String, Object>> set = new ArrayList<HashMap<String, Object>>();
		try
		{
			Connection con = connect();
			Statement sta = con.createStatement();
			ResultSet res = sta.executeQuery(query);
			ResultSetMetaData data = res.getMetaData();
			int cols = data.getColumnCount();

			while (res.next())
			{
		        HashMap<String, Object> row = new HashMap<String, Object>(cols);
		        for(int i=1; i<=cols; ++i)
		            row.put(data.getColumnName(i), res.getObject(i));
		        set.add(row);
			}
			res.close();
			sta.close();
			con.close();
		}
		catch (SQLException | ClassNotFoundException | IOException e) { return null; }
		return set;
	}
	
	/**
	 * @param query the query in a string
	 * @return the row contained in a hashmap of objects
	 */
	protected Map<String, Object> getRow(String query)
	{
		Map<String, Object> row = null;
		try
		{
			Connection con = connect();
			Statement sta = con.createStatement();
			ResultSet res = sta.executeQuery(query);
			ResultSetMetaData data = res.getMetaData();
			int cols = data.getColumnCount();

			if (res.next())
			{
		        row = new HashMap<String, Object>(cols);
		        for(int i=1; i<=cols; ++i)
		        	row.put(data.getColumnName(i), res.getObject(i));
			}
			res.close();
			sta.close();
			con.close();
		}
		catch (SQLException | ClassNotFoundException | IOException e) { return null; }
		return row;
	}
	
	/**
	 * @param query the query in a string
	 * @return the data contained in a a object
	 */
	protected Object getData(String query)
	{
		Object data = null;
		try
		{
			Connection con = connect();
			Statement sta = con.createStatement();
			ResultSet res = sta.executeQuery(query);

			if (res.next())
			{
	            data = res.getObject(1);
			}
			res.close();
			sta.close();
			con.close();
		}
		catch (SQLException | ClassNotFoundException | IOException e) { return null; }
		return data;
	}
	
	/**
	 * @param query the query in a string
	 * @return a sigle column the row contained in a list of objects
	 */
	protected List<Object> getCol(String query)
	{
		List<Object> col = new ArrayList<Object>();
		try
		{
			Connection con = connect();
			Statement sta = con.createStatement();
			ResultSet res = sta.executeQuery(query);

			while (res.next())
			{
		        col.add(res.getObject(1));
			}
			res.close();
			sta.close();
			con.close();
		}
		catch (SQLException | ClassNotFoundException | IOException e) { return null; }
		return col;
	}
	
	/**
	 * for insert, update, delete
	 * @param query the query in a string
	 * @return rows affected
	 */
	protected int set(String query)
	{
		int ret;
		try
		{
			Connection con = connect();
			Statement sta = con.createStatement();
			ret = sta.executeUpdate(query);
			sta.close();
			con.close();
		}
		catch (SQLException | ClassNotFoundException | IOException e) { return -1; }
		return ret;
	}
}
