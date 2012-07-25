package org.dbpedia.spotlight.web.rest.wadl;

import com.sun.jersey.server.wadl.WadlGenerator;
import com.sun.jersey.server.wadl.WadlGeneratorImpl;
import com.sun.research.ws.wadl.Resources;
import org.dbpedia.spotlight.web.rest.wadl.ExternalUriWadlGeneratorConfig;

/**
 * This class overrides the baseURI generation so that we can expose the correct endpoint to external users
 *
 * "The problem here is that base param in <resources /> elem is generated from injected UriInfo, BUT you can "reset" it by subclassing WadlGeneratorImpl and setting this value by yourself."
 *  http://java.net/projects/jersey/lists/users/archive/2011-03/message/214
 *
 *  @author pablomendes (adapted from Pavel Bucek)
 */
public class ExternalUriWadlGenerator extends WadlGeneratorImpl {

    @Override
    public Resources createResources() {
        Resources resources = super.createResources();
        resources.setBase(ExternalUriWadlGeneratorConfig.externalEndpointUri);
        return resources;
    }

    @Override
    public void setWadlGeneratorDelegate(WadlGenerator delegate) {
        // nothing
    }

}