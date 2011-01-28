package org.dbpedia.spotlight.string;

import org.dbpedia.spotlight.model.SurfaceForm;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.model.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Max
 * Date: 20.09.2010
 * Time: 15:52:05
 * To change this template use File | Settings | File Templates.
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
