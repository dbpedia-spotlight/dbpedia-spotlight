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
//
//import java.io.StringWriter;
//import javax.xml.parsers.*;
//import javax.xml.transform.*;
//import javax.xml.transform.dom.*;
//import javax.xml.transform.stream.*;
//import org.w3c.dom.*;
//
///**
// *
// * @author hagarcia
// */
//public class OntologyXMLFile {
//    Document document;
//
//    public OntologyXMLFile() throws ParserConfigurationException {
//        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
//        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
//        document = documentBuilder.newDocument();
//        Element rootElement = document.createElement("tree");
//        rootElement.setAttribute("id","0");
//        document.appendChild(rootElement);
//    }
//
//    public Document getDocument() {
//        return document;
//    }
//
//    public void setDocument(Document document) {
//        this.document = document;
//    }
//
//    public Element addItem(Document document, Element parent, String toAdd){
//       return addItem(document, parent, toAdd, null, null);
//    }
//    public Element addItem(Document document, Element parent, String toAdd, String callValue, String selectValue){
//        Element newItemEle = document.createElement("item");
//        newItemEle.setAttribute("text",toAdd);
//        newItemEle.setAttribute("id",toAdd);
//        newItemEle.setAttribute("open","1");
//        newItemEle.setAttribute("im0","arrow_next.gif");
//        newItemEle.setAttribute("im1","arrow_next.gif");
//        newItemEle.setAttribute("im2","arrow_next.gif");
//        if (callValue!=null)
//            newItemEle.setAttribute("call",callValue);
//        if (selectValue!=null)
//            newItemEle.setAttribute("select",selectValue);
//        parent.appendChild(newItemEle);
//        return newItemEle;
//    }
//
//    public void printXML() throws TransformerConfigurationException, TransformerException{
//        TransformerFactory transformerFactory = TransformerFactory.newInstance();
//        Transformer transformer = transformerFactory.newTransformer();
//        DOMSource source = new DOMSource(document);
//       // StreamResult result =  new StreamResult(System.out);
//        StringWriter writer = new StringWriter();
//        StreamResult result = new StreamResult(writer);
//        transformer.transform(source, result);
//        System.out.println(writer.toString());
//        //return writer.toString();
//    }
//}
