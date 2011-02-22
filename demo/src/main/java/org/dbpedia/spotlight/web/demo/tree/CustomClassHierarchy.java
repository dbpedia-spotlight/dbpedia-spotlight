///**
// * Copyright 2011 Andrés García-Silva
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//
//package dbpediaontology2xml;
//
///**
// *
// * @author hagarcia
// */
//import com.hp.hpl.jena.ontology.*;
//import com.hp.hpl.jena.rdf.model.*;
//import com.hp.hpl.jena.shared.PrefixMapping;
//import com.hp.hpl.jena.util.iterator.Filter;
//
//import java.io.PrintStream;
//import java.util.*;
//import org.w3c.dom.*;
//
//public class CustomClassHierarchy {
//    protected OntModel m_model;
//    private Map<AnonId,String> m_anonIDs = new HashMap<AnonId, String>();
//    private int m_anonCount = 0;
//    Document document;
//    OntologyXMLFile xmlFileMgr;
//
//  public void showHierarchy( OntModel m ) throws Exception {
//        xmlFileMgr = new OntologyXMLFile();
//        document = xmlFileMgr.getDocument();
//        Element owlThing = xmlFileMgr.addItem(document, document.getDocumentElement(),
//                                              "thing", "1", "1");
//
//        // create an iterator over the root classes that are not anonymous class expressions
//        Iterator<OntClass> i = m.listHierarchyRootClasses()
//                      .filterDrop( new Filter<OntClass>() {
//                                    @Override
//                                    public boolean accept( OntClass r ) {
//                                        return r.isAnon();
//                                    }} );
//
//        while (i.hasNext()) {
//            showClass( i.next(), new ArrayList<OntClass>(), 0,owlThing);
//        }
//        xmlFileMgr.printXML();
//    }
//
//      protected void showClass( OntClass cls, List<OntClass> occurs, int depth, Element parentElement ) {
//
//        Element newItem = renderClassDescription( cls, depth, parentElement );
//        System.out.println();
//
//        // recurse to the next level down
//        if (cls.canAs( OntClass.class )  &&  !occurs.contains( cls )) {
//            for (Iterator<OntClass> i = cls.listSubClasses( true );  i.hasNext(); ) {
//                OntClass sub = i.next();
//
//                // we push this expression on the occurs list before we recurse
//                occurs.add( cls );
//                showClass( sub, occurs, depth + 1,newItem);
//                occurs.remove( cls );
//            }
//        }
//    }
//
//public Element renderClassDescription( OntClass c, int depth, Element parentElement ) {
//        Element newItem = null;
//        indent( depth );
//        if (!c.isAnon()) {
//                System.out.print( "Class " );
//                renderURI( c.getModel(), c.getURI() );
//                System.out.print( ' ' );
//                String className = c.getModel().shortForm( c.getURI() );
//                className=className.replace(":", "");
//                   newItem=  xmlFileMgr.addItem(document, parentElement,
//                                        className);
//        }
//        return newItem;
//}
//
//    protected void renderURI( PrefixMapping prefixes, String uri ) {
//        System.out.print( prefixes.shortForm( uri ) );
//    }
//
//    protected void indent( int depth ) {
//        for (int i = 0;  i < depth; i++) {
//            System.out.print( "  " );
//        }
//    }
//
// }