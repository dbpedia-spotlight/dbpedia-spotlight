package org.dbpedia.spotlight.entityTopic;

public class Annotation {

    public final int begin;

    public final int end;

    public final String label;
    
    public final String value;

    public Annotation(int start, int end, String label, String value) {
        this.begin = start;
        this.end = end;
        this.label = label;
        this.value = value;
    }

}
