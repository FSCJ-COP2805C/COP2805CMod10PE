// BirthdayCardBuilder.java
// D. Singletary
// 3/17/23
// An interface for building birthday cards.

package edu.fscj.cop2805c.birthday;

public interface BirthdayCardBuilder {
    // build a birthday card for a with a greeting
    public void buildCard(User u, String msg);
}
