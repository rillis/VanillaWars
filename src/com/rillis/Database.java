package com.rillis;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class Database {
	String path = null;
	Connection c = null;
	
	public Database(String path) {
		this.path = path;
		String conUrl = "jdbc:sqlite:"+path;
		connect(conUrl);
	}
	
	private void connect(String url) {
		try {
			c = DriverManager.getConnection(url);
			
			System.out.println("Conexão criada com "+url);
		} catch (SQLException e) {
            System.out.println(e.getMessage());
        }
	}
	
	public ResultSet returnStatement(String stat) throws SQLException {
		Statement s = c.createStatement();
		ResultSet rs = null;
		do{
			rs = s.executeQuery(stat);
		}while(c.isReadOnly());
		//s.close();
         return rs; 
        
	}
	
	public boolean statement(String stat) {
		try {
			Statement statement = c.createStatement();
			do{
				statement.execute(stat);
			}while(c.isReadOnly());
	        
	        //statement.close();
	        return true;
		}catch(SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	void createFirstTable(String table, String args) {
		String statment = "CREATE TABLE IF NOT EXISTS "+table+"( "+args+")";
		statement(statment);
	}
	
	void truncate(String string) {
		statement("DELETE FROM \""+string+"\"");	
	}
	
	String[] getList(String table, String campo) {
		ArrayList<String> arr = new ArrayList<String>();
		try {
			ResultSet rs = returnStatement("SELECT * FROM "+table);
			while (rs.next()) {
				  arr.add(rs.getString(campo));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return arr.toArray(new String[0]);
	}

	public void close() {
		try {
			c.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
