package edu.fscj.cop2805c.birthday;

import com.microsoft.sqlserver.jdbc.SQLServerException;

import java.sql.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Locale;

public final class UserDB {

    private UserDB() {}

    private static Connection con = null;
    private static Statement stmt = null;
    private static PreparedStatement pStatement = null;
    private static ResultSet rSet = null;
    private static boolean connected = false;
    private static boolean dbCreated = false;

    private static final String DB_NAME = "BirthdayGreetings";
    private static final String USER_TABLE_NAME = "Users";

    // connection URLs: one for no specified DB, other for DB name
    private static final String CONN_URL = "jdbc:sqlserver://localhost:1433;" +
            "integratedSecurity=true;" +
            "dataBaseName=" + DB_NAME + ";" +
            "loginTimeout=2;" +
            "trustServerCertificate=true";
    private static final String CONN_NODB_URL = "jdbc:sqlserver://localhost:1433;" +
            "integratedSecurity=true;" +
            "loginTimeout=2;" +
            "trustServerCertificate=true";
    private static final String DB_CREATE = "CREATE DATABASE " + DB_NAME + ";";
    private static final String TABLE_CREATE = "USE " + DB_NAME + ";" +
            "CREATE TABLE " + USER_TABLE_NAME +
            " (ID smallint PRIMARY KEY NOT NULL," +
            "FNAME varchar(80) NOT NULL," +
            "LNAME varchar(80) NOT NULL," +
            "EMAIL varchar(80) NOT NULL," +
            "LOCALE varchar(80) NOT NULL," +
            "BIRTHDAY varchar(80) NOT NULL);";
    private static final String TABLE_INSERT = "USE " + DB_NAME + ";" +
            "INSERT INTO " + USER_TABLE_NAME +
            "(ID, FNAME, LNAME, EMAIL, LOCALE, BIRTHDAY)" +
            "VALUES(?, ?, ?, ?, ?, ?)";
    private static final String TABLE_SELECT = "SELECT * FROM " + USER_TABLE_NAME + ";";
    private static final String TABLE_DROP = "DROP TABLE " + USER_TABLE_NAME + ";";
    private static final String DB_DROP = "DROP DATABASE " + DB_NAME + ";";

    public static void createDB() {

        // try to connect using the DB name first, if this fails
        // fall back to the no-DB URL and create the DB
        String url = CONN_URL;
        int tries = 0;
        while (connected == false) {
            tries++;
            if (tries > 2) {  // no infinite loops allowed
                System.out.println("could not get connection, exiting");
                System.exit(0);
            }
            try {
                con = DriverManager.getConnection(url);
                System.out.println("got connection");
                connected = true;
                ;
            } catch (SQLServerException e) {
                if (tries == 1) { // failed with the db name, fall back to no-name
                    System.out.println("could not connect to DB, trying alternate URL");
                    url = CONN_NODB_URL;
                }
            } catch (SQLException e) { // Handle any errors that may have occurred.
                e.printStackTrace();
            }
        }

        if (connected == false) // no DB connection, give up
            System.exit(0);

        try {
            stmt = con.createStatement(); // this can be reused
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("could not create statement");
        }

        dbCreated = true; // assume success, may change

        // if we fell back to the no-DB URL, assume we need to create the DB
        if (url == CONN_NODB_URL) {
            try {
                stmt.executeUpdate(DB_CREATE);
                System.out.println("DB created");
            } catch (SQLException e) { // this is a problem
                dbCreated = false;
                e.printStackTrace();
                System.out.println("could not create DB");
            }
        }

        if (dbCreated == false) // no DB, give up
            System.exit(0);

        try {
            stmt.executeUpdate(TABLE_CREATE);
            System.out.println("Table created");
        } catch (SQLServerException e) {
            System.out.println("could not create table - already exists?");
            url = CONN_NODB_URL;
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("could not create table");
        }

        // we're good to continue, create our data
        ZonedDateTime currentDate = ZonedDateTime.now();
        try {

            pStatement = con.prepareStatement(TABLE_INSERT);

            // temp list for DB insertions
            ArrayList<User> userList = new ArrayList<>();

            // negative test
            userList.add(new User(0, "Dianne", "Romero", "Dianne.Romero@email.test",
                    new Locale("en"), currentDate.minusDays(1)));

            // positive tests
            // test with odd length full name and english locale
            userList.add(new User(0, "Sally", "Ride", "Sally.Ride@email.test",
                    new Locale("en"), currentDate));

            // test french locale
            userList.add(new User(0, "René", "Descartes", "René.Descartes@email.test",
                    new Locale("fr"), currentDate));

            // test with even length full name and german locale
            userList.add(new User(0, "Johannes", "Brahms", "Johannes.Brahms@email.test",
                    new Locale("de"), currentDate));

            // test chinese locale
            userList.add(new User(0, "Charles", "Kao", "Charles.Kao@email.test",
                    new Locale("zh"), currentDate));

            for (User u : userList) {
                System.out.println("key=" + u.getId());
                pStatement.setInt(1, u.getId());
                pStatement.setString(2, u.getFName());
                pStatement.setString(3, u.getLName());
                pStatement.setString(4, u.getEmail());
                pStatement.setString(5, u.getLocale().toString());
                pStatement.setString(6, u.getBirthday().toString());

                pStatement.addBatch();
            }
            pStatement.executeBatch();
            System.out.println("Records inserted");
        } catch (BatchUpdateException e) { // records exist, warn and carry on
            System.out.println("could not insert record, primary key violation?");
            e.printStackTrace();
        } catch (SQLException e) { // some other problem TBD
            e.printStackTrace();
            System.out.println("could not insert record");
        }
    }

    public static ArrayList<User> readUserDB() {

        ArrayList<User> userList = new ArrayList<>();

        // select the data from the table, save to ResultSet
        System.out.println("Reading from DB");
        try {
            rSet = stmt.executeQuery(TABLE_SELECT);

            // show the data using the next() iterator
            while (rSet.next()) {
                int id = rSet.getInt("ID");
                String lName = rSet.getString("LNAME");
                String fName = rSet.getString("FNAME");
                String email = rSet.getString("EMAIL");
                String locale = rSet.getString("LOCALE");
                String birthday = rSet.getString("BIRTHDAY");
//                // debug
//                System.out.println(id + "," +
//                        lName + "," +
//                        fName + "," +
//                        email + "," +
//                        locale + "," +
//                        birthday);
                userList.add(new User(id, fName, lName, email,
                        new Locale(locale), ZonedDateTime.parse(birthday)));
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }

        return userList;
    }

    public static void deleteDB() {

        // try to drop the table and DB
        // DB drop can fail due to "in use", deal with this when we connect
        try {
            stmt.executeUpdate(TABLE_DROP);
            stmt.executeUpdate(DB_DROP);
            System.out.println("DB dropped");
        } catch (SQLException e) {
            System.out.println("could not drop DB, in use");
        }

        // clean up
        // close can also throw an exception, we want to continue
        // to close other objects if it does so we do a
        // try/catch for each close operation
        if (rSet != null) try { rSet.close(); } catch(Exception e) {}
        if (stmt != null) try { stmt.close(); } catch(Exception e) {}
        if (pStatement != null) try { pStatement.close(); } catch(Exception e) {}
        if (con != null) try { con.close(); } catch(Exception e) {}
    }
}
