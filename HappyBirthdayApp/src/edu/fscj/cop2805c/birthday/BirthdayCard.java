// BirthdayCard.java
// D. Singletary
// 2/15/23
// Class which represents a birthday card

// D. Singletary
// 2/26/23
// Added toString method

// D. Singletary
// 3/5/23
// moved buildCard to here, implement new BirthdayCardBuilder interface

package edu.fscj.cop2805c.birthday;

import java.time.LocalDate;
import java.util.Arrays;

public class BirthdayCard implements BirthdayCardBuilder {

    // test with odd length (comment to test with even length, below)
    public static final String WISHES = "Hope all of your birthday wishes come true!";
    // uncomment to test with even length
    //final String WISHES = "Hope all of your birthday wishes come true!x";

    User user;
    private String message;

    public BirthdayCard(User user, String message) {
        this.user = user;
        this.message = message;
        // user and message are localized in buildCard()
        buildCard(user, message);
    }

    public User getUser() {
        return user;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        String s = "Birthday card for " + this.getUser().getName() + "\n";
        s += getMessage();
        return s;
    }

    public void buildCard(User u, String msg) {

        final String NEWLINE = "\n";

        // get the widest line and number of lines in the message
        int longest = getLongest(msg);

        // need to center lines
        // dashes on top (header) and bottom (footer)
        // vertical bars on the sides
        // |-----------------------|
        // | longest line in group |
        // |      other lines      |
        // |-----------------------|
        //
        // pad with an extra space if the length is odd

        int numDashes = (longest + 2) + (longest % 2);  // % 2 to pad if odd length
        char[] dashes = new char[numDashes];  // header and footer
        char[] spaces = new char[numDashes];  // body lines
        Arrays.fill(dashes, '-');
        Arrays.fill(spaces, ' ');
        String headerFooter = "|" + new String(dashes) + "|\n";
        String spacesStr = "|" + new String(spaces) + "|\n";

        // start the message with the header
        String msgbuild = headerFooter;

        // split the message into separate strings
       // String[] splitStr = msgbuild.split(NEWLINE);
        String[] splitStr = msg.split(NEWLINE);
        for (String s : splitStr) {
            String line = spacesStr;  // start with all spaces

            // create a StringBuilder with all spaces,
            // then replace some spaces with the centered string
            StringBuilder buildLine = new StringBuilder(spacesStr);

            // start at the middle minus half the length of the string (0-based)
            int start = (spacesStr.length() / 2) - (s.length() / 2);
            // end at the starting index plus the length of the string
            int end = start + s.length();
            /// replace the spaces and create a String, then append
            buildLine.replace(start, end, s);
            line = new String(buildLine);
            msgbuild += line;
        }
        // append the footer
        msgbuild += headerFooter;
        // set the instance variables to the localized values
        this.message = msgbuild;
        this.user = u;
    }

    // compare current month and day to user's data
    // to see if it is their birthday
    public static boolean isBirthday(User u) {
        boolean result = false;

        LocalDate today = LocalDate.now();
        if (today.getMonth() == u.getBirthday().getMonth() &&
                today.getDayOfMonth() == u.getBirthday().getDayOfMonth())
            result = true;

        return result;
    }

    // given a String containing a (possibly) multi-line message,
    // split the lines, find the longest line, and return its length
    public static int getLongest(String s) {
        final String NEWLINE = "\n";
        int maxLength = 0;
        String[] splitStr = s.split(NEWLINE);
        for (String line : splitStr)
            if (line.length() > maxLength)
                maxLength = line.length();
        return maxLength;
    }
}
