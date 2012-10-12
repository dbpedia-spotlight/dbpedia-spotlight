package org.dbpedia.spotlight.exceptions;

/**
 * @author Joachim Daiber
 */
public class DBpediaResourceNotFoundException extends ItemNotFoundException {

    public DBpediaResourceNotFoundException(String msg) {
        super(msg);
    }

    public DBpediaResourceNotFoundException(String msg, Exception e) {
        super(msg, e);
    }

}
