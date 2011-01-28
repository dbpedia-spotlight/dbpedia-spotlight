package org.dbpedia.spotlight.exceptions;

/**
 * Created by IntelliJ IDEA.
 * User: PabloMendes
 * Date: Jun 29, 2010
 * Time: 3:48:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class ItemNotFoundException extends Exception {

    public ItemNotFoundException(String msg, Exception e) {
        super(msg,e);
    }

    public ItemNotFoundException(String msg) {
        super(msg);
    }
}
