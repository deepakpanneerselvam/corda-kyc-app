package com.biksen.kyc.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

public class DatabaseUtil {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		
		Connection conn = DriverManager.getConnection("jdbc:h2:tcp://localhost:62482/node", "sa", "");
		
		System.out.println("Connection successfully created...."+conn);
		
		DatabaseMetaData dbmd = conn.getMetaData();
        String[] types = {"TABLE"};
        ResultSet rs = dbmd.getTables(null, null, "%", types);
        while (rs.next()) {
            System.out.println(rs.getString("TABLE_NAME"));
        }
		
		//Create a Statement class to execute the SQL statement
	    Statement stmt = conn.createStatement();
	    
	  //Execute the SQL statement and get the results in a Resultset
	    ResultSet rs1 = stmt.executeQuery("SELECT * FROM NODE_TRANSACTIONS");
	 
	    ResultSetMetaData rsmd = rs1.getMetaData();
	    int columnCount = rsmd.getColumnCount();

	    // The column count starts from 1
	    for (int i = 1; i <= columnCount; i++ ) {
	      String name = rsmd.getColumnName(i);
	      System.out.println("Column["+i+"]"+"="+name);
	    }
	    
	    while (rs1.next())
	        System.out.println("KEY_HASH= " + rs1.getString(1) + ", SEQ_NO= " + rs1.getString(2) + ", TX_ID= " + rs1.getString(3) + ", TRANSACTION= " + rs1.getString(4));

	}

}
