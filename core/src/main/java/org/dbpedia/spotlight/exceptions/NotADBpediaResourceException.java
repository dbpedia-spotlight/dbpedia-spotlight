package org.dbpedia.spotlight.exceptions;

/**
 * @author Joachim Daiber
 */
public class NotADBpediaResourceException extends ItemNotFoundException {

    private static final long serialVersionUID = 5557180885572250317L;

    public NotADBpediaResourceException(String msg) {
        super(msg);
    }

    public NotADBpediaResourceException(String msg, Exception e) {
        super(msg, e);
    }

}
