// Logger.java
// D. Singletary
// 3/18/23
// A generic interface for logging data.

package edu.fscj.cop2805c.log;

public interface Logger<T> {
    public void log(T logObj);
}
