package org.dbpedia.spotlight.exceptions;

/**
 * Thrown when the user tries to call some unauthorized action.
 * 
 * @author pablomendes
 */
public class AuthenticationException extends AnnotationException {

    public AuthenticationException(String msg, Exception e) {
        super(msg,e);
    }

    public AuthenticationException(String msg) {
        super(msg);
    }

    public AuthenticationException(Exception e) {
        super(e);
    }

}