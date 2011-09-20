package org.dbpedia.spotlight.spot.cooccurrence.weka;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dbpedia.spotlight.spot.cooccurrence.features.data.OccurrenceDataProvider;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import weka.core.Attribute;
import weka.core.Instance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builder for WEKA Instances. An InstanceBuilder builds a WEKA instance for unigrams and n-gram
 * surface form occurrences.
 *
 * @author Joachim Daiber
 */
public abstract class InstanceBuilder {

	/**
	 * WEKA class attribute.
	 */
	public static Attribute candidate_class = new Attribute("class", Arrays.asList("valid", "common"));

	public static final String FUNCTION_WORD_PATTERN = "(at|,|\\.|dt|to|wp.|c.*)";

	protected Log LOG = LogFactory.getLog(this.getClass());
	protected OccurrenceDataProvider dataProvider;


	protected boolean verboseMode = false;


	/**
	 * Create an instance builder with an instance of a {@link OccurrenceDataProvider}.
	 * @param dataProvider provider for occurrence data.
	 */
	protected InstanceBuilder(OccurrenceDataProvider dataProvider) {
		this.dataProvider = dataProvider;
	}

	
	/**
	 * Set verbose mode (extends Logging)
	 * @param verboseMode true if logging should be extended
	 */
	public void setVerboseMode(boolean verboseMode) {
		this.verboseMode = verboseMode;
	}


	/**
	 * Get the index of an attribute.
	 * 
	 * @param attribute the attribute
	 * @param attributeList list of attributes
	 * @return index of attribute
	 */
	public int i(Attribute attribute, List<Attribute> attributeList) {
		return attributeList.indexOf(attribute);
	}


	/**
	 * Build the List of Attributes
	 *
	 * @return List of Attributes
	 */
	public abstract ArrayList<Attribute> buildAttributeList();

	

	/**
	 * Build/fill a WEKA Instance for a surface form occurrence.
	 *
	 * @param surfaceFormOccurrence the surface form occurrence for which the Instance is built
	 * @param instance the empty WEKA Instance (may be {@link weka.core.SparseInstance} or
	 * 			{@link weka.core.DenseInstance} depending on the classifier.
	 * @return the filled WEKA instance
	 */
	public abstract Instance buildInstance(SurfaceFormOccurrence surfaceFormOccurrence, Instance instance);


	/**
	 * Write details about the built instance to the Logger.
	 *
	 * @param surfaceFormOccurrence the surface form occurrence for which the Instance is built
	 * @param instance the WEKA Instance (may be {@link weka.core.SparseInstance} or
	 * {@link weka.core.DenseInstance} depending on the classifier.
	 */
	public void explain(SurfaceFormOccurrence surfaceFormOccurrence, Instance instance) {

		LOG.info("Building instance for: " + surfaceFormOccurrence);
			for(int i = 0; i < instance.numAttributes(); i++) {
				LOG.info(instance.attribute(i) + ": " + instance.toString(i));
		}
		
	}

}
