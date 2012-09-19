/**
 * Copyright 2011 DBpedia Spotlight Team 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dbpedia.spotlight.web.rest.output;


import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import org.dbpedia.spotlight.model.DBpediaResourceOccurrence;
import org.dbpedia.spotlight.model.Feature;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;

import java.util.LinkedList;
import java.util.List;

@XStreamAlias("surfaceForm")
public class Spot {

    @XStreamAsAttribute
    private String name;

    @XStreamAsAttribute
    private int offset;

    @XStreamAsAttribute
    private String nerType;

    @XStreamImplicit
    private List<Resource> resources;

    public void setName(String name) {
        this.name = name;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    public void setResource(Resource resource) {
        List<Resource> r = new LinkedList<Resource>();
        r.add(resource);
        this.resources = r;
    }

    public static Spot getInstance(SurfaceFormOccurrence sfOcc) {
        Spot spot = new Spot();
        spot.setName(sfOcc.surfaceForm().name());
        spot.setOffset(sfOcc.textOffset());

        String typeFeature = null;
        if(!sfOcc.features().isEmpty())
            typeFeature = (String) sfOcc.featureValue("type").get();

        if (typeFeature != null)
            spot.setNerType(typeFeature);

        return spot;
    }

    public static Spot getInstance(DBpediaResourceOccurrence occ) {
        Spot spot = new Spot();
        spot.setName(occ.surfaceForm().name());
        spot.setOffset(occ.textOffset());
        spot.setResource(Resource.getInstance(occ));
//        Feature typeFeature = sfOcc.features().get("type");
//        if (typeFeature != null)
//            spot.setNerType(typeFeature.value().toString());
        return spot;
    }

    public void setNerType(String nerType) {
        this.nerType = nerType;
    }
}
