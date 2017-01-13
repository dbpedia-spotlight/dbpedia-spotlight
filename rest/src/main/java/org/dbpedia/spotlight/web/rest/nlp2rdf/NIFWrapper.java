package org.dbpedia.spotlight.web.rest.nlp2rdf;

import org.dbpedia.spotlight.model.DBpediaResourceOccurrence;
import org.dbpedia.spotlight.model.OntologyType;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.nlp2rdf.NIF;
import org.nlp2rdf.bean.NIFBean;
import org.nlp2rdf.bean.NIFType;
import org.nlp2rdf.nif21.impl.NIF21;

import java.util.ArrayList;
import java.util.List;

public class NIFWrapper {

    private final String SPOTLIGHT_URL = "http://spotlight.dbpedia.org";

    private List<NIFBean> entities = new ArrayList<NIFBean>();

    private NIFBean beanContext;

    private String baseURI;

    public NIFWrapper(String baseURI) {

        this.baseURI = baseURI;
        formatBaseURI();
    }

    public static String fromResourceOccs(String text, List<DBpediaResourceOccurrence> occList, String format, String prefix) {

        NIFWrapper wrapper = new NIFWrapper(prefix);

        wrapper.context(text);

        for(DBpediaResourceOccurrence occs: occList) {
            wrapper.entity(occs);
        }

        return wrapper.getNIF();

    }

    public static String fromSurfaceFormOccs(String text, List<SurfaceFormOccurrence> occList, String format, String prefix) {

        NIFWrapper wrapper = new NIFWrapper(prefix);

        wrapper.context(text);

        for(SurfaceFormOccurrence sfo: occList) {
            wrapper.entity(sfo);
        }

        return wrapper.getNIF();

    }

    public void context(String mention) {

        int beginIndex = 0;
        int endIndex = mention.length();

        NIFBean.NIFBeanBuilder contextBuilder = new NIFBean.NIFBeanBuilder();

        contextBuilder.context(baseURI, beginIndex, endIndex).mention(mention).nifType(NIFType.CONTEXT);

        beanContext = new NIFBean(contextBuilder);

    }

    private void formatBaseURI() {
        if (baseURI != null && !baseURI.isEmpty() &&
                !"/".equals(baseURI.substring(baseURI.length() - 1))) {
            baseURI = baseURI.concat("/");
        }
    }

    public void entity(DBpediaResourceOccurrence occs) {

        NIFBean.NIFBeanBuilder entity = new NIFBean.NIFBeanBuilder();

        entity.context(baseURI, occs.textOffset(), occs.textOffset() + occs.surfaceForm().name().length()).mention(occs.surfaceForm().name());
        entity.score(occs.contextualScore());
        entity.beginIndex(occs.textOffset());
        entity.endIndex(occs.textOffset() + occs.surfaceForm().name().length());
        entity.nifType(NIFType.ENTITY);
        entity.annotator(SPOTLIGHT_URL);
        entity.taIdentRef(occs.resource().getFullUri());
        entity.referenceContext(beanContext.getContext().getNIF21());


        if (!occs.resource().getTypes().isEmpty()) {

            List<String> types = new ArrayList<String>();


            for (OntologyType ontologyType : occs.resource().getTypes()) {
                types.add(ontologyType.getFullUri());
            }
            entity.taClassRef(types);
        }

        entities.add(new NIFBean(entity));

    }


    public void entity(SurfaceFormOccurrence sfo) {

        NIFBean.NIFBeanBuilder entity = new NIFBean.NIFBeanBuilder();

        entity.context(baseURI, sfo.textOffset(), sfo.textOffset() + sfo.surfaceForm().name().length()).mention(sfo.surfaceForm().name());
        entity.score(sfo.spotProb());
        entity.beginIndex(sfo.textOffset());
        entity.endIndex(sfo.textOffset() + sfo.surfaceForm().name().length());
        entity.nifType(NIFType.ENTITY);
        entity.annotator(SPOTLIGHT_URL);
        entity.taIdentRef(sfo.surfaceForm().name());
        entity.referenceContext(beanContext.getContext().getNIF21());

        entities.add(new NIFBean(entity));

    }

    public String getNIF() {
        List<NIFBean> entitiesToProcess = new ArrayList<NIFBean>(entities.size() + 1);

        entitiesToProcess.add(beanContext);
        entitiesToProcess.addAll(entities);

        NIF nif =  new NIF21(entitiesToProcess);

        return nif.getTurtle();
    }

}
