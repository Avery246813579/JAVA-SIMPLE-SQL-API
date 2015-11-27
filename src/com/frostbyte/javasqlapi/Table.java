package com.frostbyte.javasqlapi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public abstract class Table {
	public Map<String, String> variables = new LinkedHashMap<String, String>();
	private List<String> primaryKeys = new ArrayList<String>();
	private Map<String, String> foreignKeys = new HashMap<String, String>();
	private String database, table;
	
	public Table(String database, String table) {
		this.database = database;
		this.table = table;
	}

	public void addPrimaryKey(String key) {
		primaryKeys.add(key);
	}
	
	public void addForeignKey(String key, String referenceTable, String referenceKey){
		foreignKeys.put(key, referenceTable + "(" + referenceKey + ")");
	}

	/*
	 * Creates a row
	 */
	public boolean create(Map<String, Object> inputs) {
		Connection connection = null;
		try {
			connection = SqlHandler.getConnection(database);
		} catch (Exception exception) {
			SqlHandler.error("Could not connect to database! Is some info wrong?");
			return false;
		}

		String keys = "", values = "";
		List<Object> preparedValues = new ArrayList<Object>();
		Iterator<Entry<String, Object>> it = inputs.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Object> pair = (Map.Entry<String, Object>) it.next();
			keys = keys + ", " + pair.getKey();
			values = values + ", ?";
			preparedValues.add(pair.getValue());
		}
		String tableContent = "(" + keys.substring(2) + ") VALUES (" + values.substring(2) + ")";

		PreparedStatement preparedStatement = null;
		try {
			String statement = "INSERT INTO " + table + " " + tableContent + " ";
			
			preparedStatement = connection.prepareStatement(statement);
		} catch (Exception ex) {
			SqlHandler.error("Could not create table! Post a bug report with the following error: ");
			ex.printStackTrace();
			return false;
		}

		int i = 1;
		try {
			for (Object value : preparedValues) {
				preparedStatement.setObject(i, value);
				i++;
			}
		} catch (Exception ex) {
			SqlHandler.error("Can't input prepared statement values! Are your objects correct?");
		}

		boolean result;
		try {
			result = preparedStatement.execute();
		} catch (Exception exception) {
			SqlHandler.error("Could not execure query! Is your key and value correct?");
			return false;
		}

		return result;
	}

	/*
	 * Creates the table
	 */
	public boolean createTable() {
		Connection connection = null;
		try {
			connection = SqlHandler.getConnection(database);
		} catch (Exception exception) {
			SqlHandler.error("Could not connect to database! Is some info wrong?");
			return false;
		}

		String tableContent = "";
		Iterator<Entry<String, String>> it = variables.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, String> pair = (Map.Entry<String, String>) it.next();
			tableContent = tableContent + ", " + pair.getKey() + " " + pair.getValue();
		}

		String tableSuffix = "";
		if (!primaryKeys.isEmpty()) {
			for (String keys : primaryKeys) {
				tableSuffix = tableSuffix + ", PRIMARY KEY (" + keys + ")";
			}
		}

		PreparedStatement preparedStatement = null;
		try {
			String statement = "CREATE TABLE IF NOT EXISTS " + table + " (" + tableContent.substring(2) + tableSuffix;
		
			if(!foreignKeys.isEmpty()){
				for(String key : foreignKeys.keySet()){
					statement += ", FOREIGN KEY (" + key + ") REFERENCES " + foreignKeys.get(key);
				}
			}
			
			statement += ")";
			
			statement += ";";
			preparedStatement = connection.prepareStatement(statement);
		} catch (Exception ex) {
			SqlHandler.error("Could not create table! Is the com.mysql.jdbc.Driver driver a property?");
			ex.printStackTrace();
			return false;
		}

		boolean result;
		try {
			result = preparedStatement.execute();
		} catch (Exception exception) {
			SqlHandler.error("Could not execure query! Is your key and value correct?");
			return false;
		}

		if(result){
			Logger.log(Logger.SQL_STATUS, "Table " + table + " has been created!");
		}
		
		return result;
	}

	/*
	 * Gets all rows
	 */
	public List<Map<String, Object>> get() {
		List<Map<String, Object>> tables = new ArrayList<Map<String, Object>>();

		try {
			Connection connection = null;
			try {
				connection = SqlHandler.getConnection(database);
			} catch (Exception exception) {
				SqlHandler.error("Could not connect to database! Is some info wrong?");
				return null;
			}

			PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM " + table + ";");

			SqlHandler.log(preparedStatement.toString());

			ResultSet resultSet = null;
			try {
				resultSet = preparedStatement.executeQuery();
			} catch (Exception exception) {
				SqlHandler.error("Could not execure query! Is your key and value correct?");
				return null;
			}

			while (resultSet.next()) {
				Map<String, Object> table = new LinkedHashMap<String, Object>();

				Iterator<Entry<String, String>> it = variables.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<String, String> pair = (Map.Entry<String, String>) it.next();
					table.put(pair.getKey(), resultSet.getObject(pair.getKey()));

				}

				tables.add(table);
			}

			preparedStatement.close();
			connection.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return tables;

	}

	/*
	 * Get's certian rows
	 */
	public List<Map<String, Object>> get(String key, Object value) {
		List<Map<String, Object>> tables = new ArrayList<Map<String, Object>>();

		try {
			Connection connection = null;
			try {
				connection = SqlHandler.getConnection(database);
			} catch (Exception exception) {
				SqlHandler.error("Could not connect to database! Is some info wrong?");
				return null;
			}

			PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM " + table + " WHERE " + key + " = ?;");
			preparedStatement.setObject(1, value);
			SqlHandler.log(preparedStatement.toString());

			ResultSet resultSet = null;
			try {
				resultSet = preparedStatement.executeQuery();
			} catch (Exception exception) {
				SqlHandler.error("Could not execure query! Is your key and value correct?");
				return null;
			}

			while (resultSet.next()) {
				Map<String, Object> table = new LinkedHashMap<String, Object>();

				Iterator<Entry<String, String>> it = variables.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<String, String> pair = (Map.Entry<String, String>) it.next();
					table.put(pair.getKey(), resultSet.getObject(pair.getKey()));

				}

				tables.add(table);
			}

			preparedStatement.close();
			connection.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return tables;
	}
	
	/*
	 * Get multiple
	 */
	public List<Map<String, Object>> get(Map<String, Object> values) {
		List<Map<String, Object>> tables = new ArrayList<Map<String, Object>>();

		try {
			Connection connection = null;
			try {
				connection = SqlHandler.getConnection(database);
			} catch (Exception exception) {
				SqlHandler.error("Could not connect to database! Is some info wrong?");
				return null;
			}
			
			String tableContent = "";
			List<Object> preparedValues = new ArrayList<Object>();
			Iterator<Entry<String, Object>> ite = values.entrySet().iterator();
			while (ite.hasNext()) {
				Map.Entry<String, Object> pair = (Map.Entry<String, Object>) ite.next();
				tableContent = tableContent + "AND " + pair.getKey() + " = ? ";
				preparedValues.add(pair.getValue());
			}


			PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM " + table + " WHERE " + tableContent.substring(4) + ";");

			int i = 1;
			try {
				for (Object value : preparedValues) {
					preparedStatement.setObject(i, value);
					i++;
				}
			} catch (Exception ex) {
				SqlHandler.error("Can't input prepared statement values! Are your objects correct?");
			}
			
			SqlHandler.log(preparedStatement.toString());

			ResultSet resultSet = null;
			try {
				resultSet = preparedStatement.executeQuery();
			} catch (Exception exception) {
				SqlHandler.error("Could not execure query! Is your key and value correct?");
				return null;
			}

			while (resultSet.next()) {
				Map<String, Object> table = new LinkedHashMap<String, Object>();

				Iterator<Entry<String, String>> it = variables.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<String, String> pair = (Map.Entry<String, String>) it.next();
					table.put(pair.getKey(), resultSet.getObject(pair.getKey()));

				}

				tables.add(table);
			}

			preparedStatement.close();
			connection.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return tables;
	}

	public List<Map<String, Object>> get(Map<String, Object> values, int option) {
		List<Map<String, Object>> tables = new ArrayList<Map<String, Object>>();

		try {
			Connection connection = null;
			try {
				connection = SqlHandler.getConnection(database);
			} catch (Exception exception) {
				SqlHandler.error("Could not connect to database! Is some info wrong?");
				return null;
			}
			
			String tableContent = "";
			List<Object> preparedValues = new ArrayList<Object>();
			Iterator<Entry<String, Object>> ite = values.entrySet().iterator();
			while (ite.hasNext()) {
				Map.Entry<String, Object> pair = (Map.Entry<String, Object>) ite.next();
				
				if(option == SqlOptions.AND){
					tableContent = tableContent + "AND " + pair.getKey() + " = ? ";
				}else if(option == SqlOptions.OR){
					tableContent = tableContent + "OR " + pair.getKey() + " = ? ";
				}
				preparedValues.add(pair.getValue());
			}


			PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM " + table + " WHERE " + tableContent.substring(4) + ";");

			int i = 1;
			try {
				for (Object value : preparedValues) {
					preparedStatement.setObject(i, value);
					i++;
				}
			} catch (Exception ex) {
				SqlHandler.error("Can't input prepared statement values! Are your objects correct?");
			}
			
			SqlHandler.log(preparedStatement.toString());

			ResultSet resultSet = null;
			try {
				resultSet = preparedStatement.executeQuery();
			} catch (Exception exception) {
				SqlHandler.error("Could not execure query! Is your key and value correct?");
				return null;
			}

			while (resultSet.next()) {
				Map<String, Object> table = new LinkedHashMap<String, Object>();

				Iterator<Entry<String, String>> it = variables.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry<String, String> pair = (Map.Entry<String, String>) it.next();
					table.put(pair.getKey(), resultSet.getObject(pair.getKey()));

				}

				tables.add(table);
			}

			preparedStatement.close();
			connection.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return tables;
	}


	/*
	 * Contains
	 */
	public boolean contains(String key, String value) {
		List<Map<String, Object>> tables = new ArrayList<>();
		return !tables.isEmpty();
	}

	/*
	 * Creates a row
	 */
	public boolean update(Map<String, Object> inputs, Map<String, Object> where) {
		Connection connection = null;
		try {
			connection = SqlHandler.getConnection(database);
		} catch (Exception exception) {
			SqlHandler.error("Could not connect to database! Is some info wrong?");
			return false;
		}

		String tableContent = "";
		List<Object> preparedValues = new ArrayList<Object>();
		Iterator<Entry<String, Object>> it = inputs.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Object> pair = (Map.Entry<String, Object>) it.next();
			tableContent = tableContent + ", " + pair.getKey() + " = ?";
			preparedValues.add(pair.getValue());
		}
		
		String whereContent = "";
		Iterator<Entry<String, Object>> vit = where.entrySet().iterator();
		while (vit.hasNext()) {
			Map.Entry<String, Object> pair = (Map.Entry<String, Object>) vit.next();
			whereContent = whereContent + " AND " + pair.getKey() + " = ?";
			preparedValues.add(pair.getValue());
		}


		PreparedStatement preparedStatement = null;
		try {
			preparedStatement = connection.prepareStatement("UPDATE " + table + " SET " + tableContent.substring(2) + " WHERE " + whereContent.substring(4) + ";");
		} catch (Exception ex) {
			SqlHandler.error("Could not create table! Post a bug report with the following error: ");
			ex.printStackTrace();
			return false;
		}

		int i = 1;
		try {
			for (Object values : preparedValues) {
				preparedStatement.setObject(i, values);
				i++;
			}
		} catch (Exception ex) {
			SqlHandler.error("Can't input prepared statement values! Are your objects correct?");
		}

		SqlHandler.log(preparedStatement.toString());

		boolean result;
		try {
			result = preparedStatement.execute();
		} catch (Exception exception) {
			SqlHandler.error("Could not execure query! Is your key and value correct?");
			exception.printStackTrace();
			return false;
		}

		return result;
	}
	
	public boolean delete(Map<String, Object> where) {
		Connection connection = null;
		try {
			connection = SqlHandler.getConnection(database);
		} catch (Exception exception) {
			SqlHandler.error("Could not connect to database! Is some info wrong?");
			return false;
		}

		String tableContent = "";
		List<Object> preparedValues = new ArrayList<Object>();
		Iterator<Entry<String, Object>> it = where.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, Object> pair = (Map.Entry<String, Object>) it.next();
			tableContent = tableContent + " AND " + pair.getKey() + " = ?";
			preparedValues.add(pair.getValue());
		}

		PreparedStatement preparedStatement = null;
		try {
			preparedStatement = connection.prepareStatement("DELETE FROM " + table + " WHERE " + tableContent.substring(4));
		} catch (Exception ex) {
			SqlHandler.error("Could not create table! Post a bug report with the following error: ");
			ex.printStackTrace();
			return false;
		}

		int i = 1;
		try {
			for (Object values : preparedValues) {
				preparedStatement.setObject(i, values);
				i++;
			}
		} catch (Exception ex) {
			SqlHandler.error("Can't input prepared statement values! Are your objects correct?");
		}

		SqlHandler.log(preparedStatement.toString());

		boolean result;
		try {
			result = preparedStatement.execute();
		} catch (Exception exception) {
			SqlHandler.error("Could not execure query! Is your key and value correct?");
			return false;
		}

		return result;
	}
	
	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}
}
