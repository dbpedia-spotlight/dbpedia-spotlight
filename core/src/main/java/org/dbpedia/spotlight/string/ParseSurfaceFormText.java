/**
 * Copyright 2011 Pablo Mendes, Max Jakob
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

package org.dbpedia.spotlight.string;

import org.dbpedia.spotlight.model.SurfaceForm;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.model.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Functions to deal with wiki markup.
 *
 * See also: MilneWittenSource.parse
 *
 * @author maxjakob
 */
public class ParseSurfaceFormText {

    public static List<SurfaceFormOccurrence> parse(String textWithMarkedSurfaceForms) {
        List<SurfaceFormOccurrence> sfOccs = new ArrayList<SurfaceFormOccurrence>();
        Text unMarkedUpText = new Text(textWithMarkedSurfaceForms.replaceAll("(\\[\\[|\\]\\])", ""));
        int i = 0;
        while (i < textWithMarkedSurfaceForms.length()) {
            int start = textWithMarkedSurfaceForms.indexOf("[[", i) + 2;
            if (start == 1)
                break;
            int end = textWithMarkedSurfaceForms.indexOf("]]", start);
            if (end == -1)
                break;
            SurfaceForm sf = new SurfaceForm(textWithMarkedSurfaceForms.substring(start, end));
            int offset = start - (sfOccs.size()*4) - 2;
            SurfaceFormOccurrence sfOcc = new SurfaceFormOccurrence(sf, unMarkedUpText, offset);
            sfOccs.add(sfOcc);
            i = end + 2;
        }
        return sfOccs;
    }

    public static String eraseMarkup(String textWithMarkedSurfaceForms) {
        return textWithMarkedSurfaceForms.replaceAll("(\\[\\[|\\]\\])", "");
    }
    
}
