package org.dbpedia.spotlight.exceptions;

/**
 * @author Joachim Daiber
 */
public class SurfaceFormNotFoundException extends ItemNotFoundException {

    public SurfaceFormNotFoundException(String msg, Exception e) {
        super(msg, e);
    }

    public SurfaceFormNotFoundException(String msg) {
        super(msg);
    }

}
