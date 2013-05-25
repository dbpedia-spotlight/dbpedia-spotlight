

/* First created by JCasGen Mon Feb 25 23:16:48 EST 2013 */
package org.dbpedia.spotlight.uima.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** 
 * Updated by JCasGen Wed May 01 20:41:38 EDT 2013
 * XML source: /home/nural/workspace/Spotlight-Uima/desc/SpotlightAnnotator.xml
 * @generated */
public class JCasResource extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(JCasResource.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated  */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected JCasResource() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated */
  public JCasResource(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated */
  public JCasResource(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated */  
  public JCasResource(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** <!-- begin-user-doc -->
    * Write your own initialization here
    * <!-- end-user-doc -->
  @generated modifiable */
  private void readObject() {/*default - does nothing empty block */}
     
 
    
  //*--------------*
  //* Feature: similarityScore

  /** getter for similarityScore - gets 
   * @generated */
  public double getSimilarityScore() {
    if (JCasResource_Type.featOkTst && ((JCasResource_Type)jcasType).casFeat_similarityScore == null)
      jcasType.jcas.throwFeatMissing("similarityScore", "org.dbpedia.spotlight.uima.types.JCasResource");
    return jcasType.ll_cas.ll_getDoubleValue(addr, ((JCasResource_Type)jcasType).casFeatCode_similarityScore);}
    
  /** setter for similarityScore - sets  
   * @generated */
  public void setSimilarityScore(double v) {
    if (JCasResource_Type.featOkTst && ((JCasResource_Type)jcasType).casFeat_similarityScore == null)
      jcasType.jcas.throwFeatMissing("similarityScore", "org.dbpedia.spotlight.uima.types.JCasResource");
    jcasType.ll_cas.ll_setDoubleValue(addr, ((JCasResource_Type)jcasType).casFeatCode_similarityScore, v);}    
   
    
  //*--------------*
  //* Feature: support

  /** getter for support - gets 
   * @generated */
  public int getSupport() {
    if (JCasResource_Type.featOkTst && ((JCasResource_Type)jcasType).casFeat_support == null)
      jcasType.jcas.throwFeatMissing("support", "org.dbpedia.spotlight.uima.types.JCasResource");
    return jcasType.ll_cas.ll_getIntValue(addr, ((JCasResource_Type)jcasType).casFeatCode_support);}
    
  /** setter for support - sets  
   * @generated */
  public void setSupport(int v) {
    if (JCasResource_Type.featOkTst && ((JCasResource_Type)jcasType).casFeat_support == null)
      jcasType.jcas.throwFeatMissing("support", "org.dbpedia.spotlight.uima.types.JCasResource");
    jcasType.ll_cas.ll_setIntValue(addr, ((JCasResource_Type)jcasType).casFeatCode_support, v);}    
   
    
  //*--------------*
  //* Feature: types

  /** getter for types - gets 
   * @generated */
  public String getTypes() {
    if (JCasResource_Type.featOkTst && ((JCasResource_Type)jcasType).casFeat_types == null)
      jcasType.jcas.throwFeatMissing("types", "org.dbpedia.spotlight.uima.types.JCasResource");
    return jcasType.ll_cas.ll_getStringValue(addr, ((JCasResource_Type)jcasType).casFeatCode_types);}
    
  /** setter for types - sets  
   * @generated */
  public void setTypes(String v) {
    if (JCasResource_Type.featOkTst && ((JCasResource_Type)jcasType).casFeat_types == null)
      jcasType.jcas.throwFeatMissing("types", "org.dbpedia.spotlight.uima.types.JCasResource");
    jcasType.ll_cas.ll_setStringValue(addr, ((JCasResource_Type)jcasType).casFeatCode_types, v);}    
   
    
  //*--------------*
  //* Feature: URI

  /** getter for URI - gets 
   * @generated */
  public String getURI() {
    if (JCasResource_Type.featOkTst && ((JCasResource_Type)jcasType).casFeat_URI == null)
      jcasType.jcas.throwFeatMissing("URI", "org.dbpedia.spotlight.uima.types.JCasResource");
    return jcasType.ll_cas.ll_getStringValue(addr, ((JCasResource_Type)jcasType).casFeatCode_URI);}
    
  /** setter for URI - sets  
   * @generated */
  public void setURI(String v) {
    if (JCasResource_Type.featOkTst && ((JCasResource_Type)jcasType).casFeat_URI == null)
      jcasType.jcas.throwFeatMissing("URI", "org.dbpedia.spotlight.uima.types.JCasResource");
    jcasType.ll_cas.ll_setStringValue(addr, ((JCasResource_Type)jcasType).casFeatCode_URI, v);}    
  }

    