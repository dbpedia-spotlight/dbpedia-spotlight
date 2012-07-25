package org.dbpedia.spotlight.web.rest.output;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Created with IntelliJ IDEA.
 * User: dirk
 * Date: 5/30/12
 * Time: 4:35 PM
 * To change this template use File | Settings | File Templates.
 */
@XStreamAlias("topic")
public class Topic {

    @XStreamAsAttribute
    private String topic;

    @XStreamAsAttribute
    private Double score;

    public Topic(String topic, Double score) {
       this.score = score;
        this.topic = topic;
    }

}
