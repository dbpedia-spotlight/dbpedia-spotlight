package org.dbpedia.spotlight.exceptions;

/**
 * Created by IntelliJ IDEA.
 * User: PabloMendes
 * Date: Jun 29, 2010
 * Time: 3:48:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class SearchException extends Exception {

    public SearchException(String msg, Exception e) {
        super(msg,e);
    }

    public SearchException(String msg) {
        super(msg);
    }
}
