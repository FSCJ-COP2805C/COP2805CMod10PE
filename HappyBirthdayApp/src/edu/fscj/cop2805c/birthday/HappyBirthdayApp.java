// HappyBirthdayApp.java
// D. Singletary
// 1/29/23
// wish multiple users a happy birthday

// D. Singletary
// 2/26/23
// Added Stream and localization code

// D. Singletary
// 3/7/23
// Changed to thread-safe queue
// Moved buildCard to BirthdayCard class
// Instantiate the BirthdayCardProcessor object
// added test data for multi-threading tests

package edu.fscj.cop2805c.birthday;

import com.microsoft.sqlserver.jdbc.SQLServerException;
import edu.fscj.cop2805c.dispatch.Dispatcher;

import java.io.*;
import java.sql.*;
import java.time.format.DateTimeFormatter;
import java.time.ZonedDateTime;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

// main mpplication class
public class HappyBirthdayApp implements BirthdayGreeter, Dispatcher<BirthdayCard>  {

    private static final String USER_FILE = "user.dat";

    private ArrayList<User> birthdays = new ArrayList<>();
    // Use a thread-safe Queue<LinkedList> to act as message queue for the dispatcher
    ConcurrentLinkedQueue safeQueue = new ConcurrentLinkedQueue(
           new LinkedList<BirthdayCard>()
    );

    private Stream<BirthdayCard> stream = safeQueue.stream();
    //ObjectOutputStream userData = null;

    public HappyBirthdayApp() { }

    // dispatch the card using the dispatcher
    public void dispatch(BirthdayCard bc) {
        this.safeQueue.add(bc);
    }

    // send the card
    public void sendCard(BirthdayCard bc) {
        // dispatch the card
        Dispatcher<BirthdayCard> d = (c)-> {
            this.safeQueue.add(c);
        };
        d.dispatch(bc);
    }

    // show prompt msg with no newline
    public static void prompt(String msg) {
        System.out.print(msg + ": ");
    }

    public void generateCards() {

        for (User u : birthdays) {
            System.out.println(u.getName());
            // see if today is their birthday
            // if not, show sorry message
            if (!BirthdayCard.isBirthday(u))
                System.out.println("Sorry, today is not their birthday.");
                // otherwise build the card
            else {
                String msg = "";
                try {
                    // load the property and create the localized greeting
                    ResourceBundle res = ResourceBundle.getBundle(
                            "edu.fscj.cop2805c.birthday.Birthday", u.getLocale());
                    String happyBirthday = res.getString("HappyBirthday");

                    // format and display the date
                    DateTimeFormatter formatter =
                            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
                    formatter =
                            formatter.localizedBy(u.getLocale());
                    msg = u.getBirthday().format(formatter) + "\n";

                    // add the localized greeting
                    msg += happyBirthday + " " + u.getName() + "\n" +
                            BirthdayCard.WISHES;
                } catch (java.util.MissingResourceException e) {
                    System.err.println(e);
                    msg = "Happy Birthday, " + u.getName() + "\n" +
                            BirthdayCard.WISHES;
                }
                BirthdayCard bc = new BirthdayCard(u, msg);
                sendCard(bc);
            }
        }
        birthdays.clear(); // clear the list
    }

    // add multiple birthdays
    public void addBirthdays(User... users) {
        for (User u : users) {
            birthdays.add(u);
        }
    }

    // write user ArrayList to save file
    public void writeUsers(ArrayList<User> ul) {
        try (ObjectOutputStream userData =  new ObjectOutputStream(
                new FileOutputStream(USER_FILE));) {
                userData.writeObject(ul);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // read saved user ArrayList
    public ArrayList<User> readUsers() {
        ArrayList<User> list = new ArrayList();

        try (ObjectInputStream userData =
                     new ObjectInputStream(
                             new FileInputStream(USER_FILE));) {
            list = (ArrayList<User>) (userData.readObject());
            for (User u : list)
                System.out.println("readUsers: read " + u);
        } catch (FileNotFoundException e) {
            // not  a problem if nothing was saved
            System.err.println("readUsers: no input file");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
           e.printStackTrace();
        }
        return list;    
    }

    // main program
    public static void main(String[] args) {

        HappyBirthdayApp hba = new HappyBirthdayApp();

        // restore saved data
        ArrayList<User> userList = hba.readUsers();

        System.out.println("Current JVM version - " + System.getProperty("java.version"));
        String classpath = System.getProperty("java.class.path");
        String[] classPathValues = classpath.split(File.pathSeparator);
        System.out.println(Arrays.toString(classPathValues));
        // Declare the JDBC objects.
        Connection con = null;
        // Many things can go wrong here that in the real world would
        // need to be dealt with gracefully. To keep it simple, however,
        // we are just using a monolithic try/catch
        boolean connected = false;

        final String CONN_URL = "jdbc:sqlserver://localhost:1433;" +
                "integratedSecurity=true;" +
                "dataBaseName=BirthdayGreetings;" +
                "loginTimeout=2;" +
                "trustServerCertificate=true";
        final String CONN_NODB_URL = "jdbc:sqlserver://localhost:1433;" +
                "integratedSecurity=true;" +
                "loginTimeout=2;" +
                "trustServerCertificate=true";

        String url = CONN_URL;

        while (connected == false) {
            try {
                con = DriverManager.getConnection(url);
                System.out.println("got connection");
                connected = true;;
            } catch (SQLServerException e) {
                System.out.println("could not connect to DB, will try alternate URL");
                url = CONN_NODB_URL;
            } catch (SQLException e) { // Handle any errors that may have occurred.
                e.printStackTrace();
            }
        }

        if (connected == false) // no DB connection, give up
            System.exit(0);

        Statement stmt = null;

        try {
            stmt = con.createStatement(); // this can be reused
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("could not create statement");
        }

        boolean dbCreated = true;

        if (url == CONN_NODB_URL) {
            try {
                stmt.executeUpdate("CREATE DATABASE " + "BirthdayGreetings" + ";");
                System.out.println("DB created");
            } catch (SQLException e) {
                dbCreated = false;
                e.printStackTrace();
                System.out.println("could not create DB");
            }
        }

        if (dbCreated == false) // no DB, give up
            System.exit(0);

        try {
            final String TABLECREATE = "USE " + "BirthdayGreetings" + ";" +
                    "CREATE TABLE Users " +
                    "(ID smallint PRIMARY KEY NOT NULL," +
                    "FNAME varchar(80) NOT NULL," +
                    "LNAME varchar(80) NOT NULL," +
                    "EMAIL varchar(80) NOT NULL," +
                    "LOCALE varchar(80) NOT NULL);";

            stmt.executeUpdate(TABLECREATE);
            System.out.println("Table created");
        } catch (SQLServerException e) {
            System.out.println("could not create table - already exists?");
            url = CONN_NODB_URL;
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("could not create table");
        }

        ZonedDateTime currentDate = ZonedDateTime.now();

        User u = null;

        try {
            u = new User("Dianne", "Romero", "Dianne.Romero@email.test",
                    new Locale("en"), currentDate.minusDays(1));
            final String TABLEINSERT = "USE " + "BirthdayGreetings" + ";" +
                    "INSERT INTO Users(" +
                    "ID, FNAME, LNAME, EMAIL, LOCALE)" +
                    "VALUES(?, ?, ?, ?, ?)";

            PreparedStatement pstmt = con.prepareStatement(TABLEINSERT);
            pstmt.setInt(1, u.getId());
            pstmt.setString(2, u.getFName());
            pstmt.setString(3, u.getLName());
            pstmt.setString(4, u.getEmail());
            pstmt.setString(5, u.getLocale().toString());
            pstmt.addBatch();
            /*for (Penguin penguin : penguins) {
                pstmt.setInt(1, penguin.getSampleNum());
                pstmt.setDouble(2, penguin.getCulmenLen());
                pstmt.setDouble(3, penguin.getCulmenDepth());
                pstmt.setInt(4, penguin.getBodyMass());
                pstmt.setString(5, penguin.getSex().toString());
                pstmt.setString(6, penguin.getSpecies().toString());
                pstmt.setDouble(7, penguin.getFlipperLen());
                pstmt.addBatch();
            }*/
            pstmt.executeBatch();
            //stmt.executeUpdate(TABLEINSERT);
            System.out.println("Record inserted");
        } catch (BatchUpdateException e) {
            System.out.println("could not insert record, primary key violation?");
            System.out.println("(key value is " + u.getId());
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("could not insert record");
        }

        // start the processor thread
        //BirthdayCardProcessor processor = new BirthdayCardProcessor(hba.safeQueue);

        // use current date for testing, adjust where necessary
;
/*
        // if no users, generate some for testing
        if (userList.isEmpty()) {
            // negative test
            userList.add(new User("Dianne", "Romero", "Dianne.Romero@email.test",
                    new Locale("en"), currentDate.minusDays(1)));

            // positive tests
            // test with odd length full name and english locale
            userList.add(new User("Sally", "Ride", "Sally.Ride@email.test",
                    new Locale("en"), currentDate));

            // test french locale
            userList.add(new User("René", "Descartes", "René.Descartes@email.test",
                    new Locale("fr"), currentDate));

            // test with even length full name and german locale
            userList.add(new User("Johannes", "Brahms", "Johannes.Brahms@email.test",
                    new Locale("de"), currentDate));

            // test chinese locale
            userList.add(new User("Charles", "Kao", "Charles.Kao@email.test",
                    new Locale("zh"), currentDate));
        }
        else
            System.out.println("Users were read from data file");
*/
        // convert ArrayList of users to array for addBirthdays method
        // User[] userArray = new User[userList.size()];
        // userArray = userList.toArray(userArray);
        //hba.addBirthdays(userArray);
        //hba.generateCards();

        // wait for a bit
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ie) {
            System.out.println("sleep interrupted! " + ie);
        }

        //processor.endProcessing();
/*
        // generate (or regenerate) the user data file
        hba.writeUsers(userList);
 */
    }
}