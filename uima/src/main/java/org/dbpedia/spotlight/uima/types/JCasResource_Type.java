
/* First created by JCasGen Mon Feb 25 23:16:48 EST 2013 */
package org.dbpedia.spotlight.uima.types;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.tcas.Annotation_Type;

/** 
 * Updated by JCasGen Wed May 01 20:41:39 EDT 2013
 * @generated */
public class JCasResource_Type extends Annotation_Type {
  /** @generated */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (JCasResource_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = JCasResource_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new JCasResource(addr, JCasResource_Type.this);
  			   JCasResource_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new JCasResource(addr, JCasResource_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasResource.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("org.dbpedia.spotlight.uima.types.JCasResource");
 
  /** @generated */
  final Feature casFeat_similarityScore;
  /** @generated */
  final int     casFeatCode_similarityScore;
  /** @generated */ 
  public double getSimilarityScore(int addr) {
        if (featOkTst && casFeat_similarityScore == null)
      jcas.throwFeatMissing("similarityScore", "org.dbpedia.spotlight.uima.types.JCasResource");
    return ll_cas.ll_getDoubleValue(addr, casFeatCode_similarityScore);
  }
  /** @generated */    
  public void setSimilarityScore(int addr, double v) {
        if (featOkTst && casFeat_similarityScore == null)
      jcas.throwFeatMissing("similarityScore", "org.dbpedia.spotlight.uima.types.JCasResource");
    ll_cas.ll_setDoubleValue(addr, casFeatCode_similarityScore, v);}
    
  
 
  /** @generated */
  final Feature casFeat_support;
  /** @generated */
  final int     casFeatCode_support;
  /** @generated */ 
  public int getSupport(int addr) {
        if (featOkTst && casFeat_support == null)
      jcas.throwFeatMissing("support", "org.dbpedia.spotlight.uima.types.JCasResource");
    return ll_cas.ll_getIntValue(addr, casFeatCode_support);
  }
  /** @generated */    
  public void setSupport(int addr, int v) {
        if (featOkTst && casFeat_support == null)
      jcas.throwFeatMissing("support", "org.dbpedia.spotlight.uima.types.JCasResource");
    ll_cas.ll_setIntValue(addr, casFeatCode_support, v);}
    
  
 
  /** @generated */
  final Feature casFeat_types;
  /** @generated */
  final int     casFeatCode_types;
  /** @generated */ 
  public String getTypes(int addr) {
        if (featOkTst && casFeat_types == null)
      jcas.throwFeatMissing("types", "org.dbpedia.spotlight.uima.types.JCasResource");
    return ll_cas.ll_getStringValue(addr, casFeatCode_types);
  }
  /** @generated */    
  public void setTypes(int addr, String v) {
        if (featOkTst && casFeat_types == null)
      jcas.throwFeatMissing("types", "org.dbpedia.spotlight.uima.types.JCasResource");
    ll_cas.ll_setStringValue(addr, casFeatCode_types, v);}
    
  
 
  /** @generated */
  final Feature casFeat_URI;
  /** @generated */
  final int     casFeatCode_URI;
  /** @generated */ 
  public String getURI(int addr) {
        if (featOkTst && casFeat_URI == null)
      jcas.throwFeatMissing("URI", "org.dbpedia.spotlight.uima.types.JCasResource");
    return ll_cas.ll_getStringValue(addr, casFeatCode_URI);
  }
  /** @generated */    
  public void setURI(int addr, String v) {
        if (featOkTst && casFeat_URI == null)
      jcas.throwFeatMissing("URI", "org.dbpedia.spotlight.uima.types.JCasResource");
    ll_cas.ll_setStringValue(addr, casFeatCode_URI, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	* @generated */
  public JCasResource_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_similarityScore = jcas.getRequiredFeatureDE(casType, "similarityScore", "uima.cas.Double", featOkTst);
    casFeatCode_similarityScore  = (null == casFeat_similarityScore) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_similarityScore).getCode();

 
    casFeat_support = jcas.getRequiredFeatureDE(casType, "support", "uima.cas.Integer", featOkTst);
    casFeatCode_support  = (null == casFeat_support) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_support).getCode();

 
    casFeat_types = jcas.getRequiredFeatureDE(casType, "types", "uima.cas.String", featOkTst);
    casFeatCode_types  = (null == casFeat_types) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_types).getCode();

 
    casFeat_URI = jcas.getRequiredFeatureDE(casType, "URI", "uima.cas.String", featOkTst);
    casFeatCode_URI  = (null == casFeat_URI) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_URI).getCode();

  }
}



    