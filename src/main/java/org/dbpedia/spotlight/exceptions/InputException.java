package org.dbpedia.spotlight.exceptions;

/**
 * Thrown when the user provides not acceptable input (e.g. too short of a text).
 * 
 * @author maxjakob
 */
public class InputException extends AnnotationException {

    public InputException(String msg, Exception e) {
        super(msg,e);
    }

    public InputException(String msg) {
        super(msg);
    }

    public InputException(Exception e) {
        super(e);
    }

}