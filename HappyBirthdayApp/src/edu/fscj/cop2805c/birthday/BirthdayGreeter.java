// BirthdayGreeter.java
// D. Singletary
// 2/15/23
// An interface for building and sending birthday cards.

// D. Singletary
// 3/5/23
// moved builder to separate interface

package edu.fscj.cop2805c.birthday;

public interface BirthdayGreeter {
    // send a birthday card
    public void sendCard(BirthdayCard bc);
}
