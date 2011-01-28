package org.dbpedia.spotlight.exceptions;

/**
 * Thrown when the user tries to call some unauthorized action.
 *
 * @author pablomendes
 */
public class AnnotationException extends Exception {

    public AnnotationException(String msg, Exception e) {
        super(msg,e);
    }

    public AnnotationException(String msg) {
        super(msg);
    }

    public AnnotationException(Exception e) {
        super(e);
    }

}