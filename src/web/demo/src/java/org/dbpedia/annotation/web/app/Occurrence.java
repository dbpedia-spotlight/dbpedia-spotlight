/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.dbpedia.spotlight.web.app;

/**
 *
 * @author Andrés
 */
public class Occurrence {
    String uri;
    String surfaceForm;
    int offset;

    public Occurrence(int offset, String uri, String surfaceForm) {
        this.uri = uri;
        this.surfaceForm = surfaceForm;
        this.offset = offset;
    }
}
