//File:         LCHeading.java
//PartOf:		LCAnalyze

//Created:      [creation date]
//Author:       <A HREF="mailto:[shanewolfram@hotmail.com]">[Shane Wolfram]</A>

package lc_analyze;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;

/**
 * This class is used to represent LC Subject headings.
 * 
 * In the Library of Congress RDF authority headings, subject headings are
 * represented as "strings" (e.g. American
 * literature--Bibliography--Periodicals). RDF name authorities are structured
 * essentially the same, with narrower subdivisions separated by periods rather
 * than dashes.
 * 
 * This class is designed to capture the structure of a collection of such
 * strings, where broader concepts may overlap and narrow concepts further down
 * the string may diverge. It also records the number of concept occurrences.
 * For example, the above string would increment 3 counters, once for American
 * literature, once for American literature subdivided by Bibliography, and once
 * for American literature subdivided by Bibliography subdivided by Periodicals.
 * 
 ***/

public class LCSubjectHeading implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public enum type {
		NAME, SUBJECT;
	}

	// The heading or concept represented by this LCHeading instance
	public String headingName = null;

	/**
	 * The subheadings ArrayList *does not* simply contain the LCSH/name string.
	 * This gets constructed recursively down the subheading array. Rather, adjacent
	 * elements of the array are parallel lcsh strings which emanate from the same
	 * root concept.
	 **/
	public ArrayList<LCSubjectHeading> subheadings = new ArrayList<LCSubjectHeading>();

	/**
	 * Count simply records the number of instances of a concept within an LCHeading
	 * tree. Note that the LCSH strings "American literature--bibliography" and
	 * "Bibliography" are will not be counted together as instances of the concept
	 * "bibliography", since they do not share a root concept. (As opposed to
	 * "American literature" and "American literature--bibliography", which do share
	 * the root concept "American History." So to count all instances of a concept,
	 * you will need to search for it across all LCHeading trees and add the counts
	 * together.
	 **/
	public int count;

	/**
	 * The URI for a LC name or subject heading. For a concept string, the URI will
	 * be associated with the last concept in the string. (In other words, it will
	 * be recorded in the uri field for the lowest LCHeading object in the
	 * hierarchy.) Other concepts will have their URI field set to null, unless
	 * already exist and have had the field set.
	 **/
	public URI uri = null;
	/**
	 * A list of all the oclc numbers for records which contain the given heading
	 * concept or string.
	 **/
	public ArrayList<CatRecord> catRecords = new ArrayList<CatRecord>();

	/**
	 * A simple constructor for the LCHeading class, used for headings which are not
	 * subdivided.
	 **/

	public ArrayList<LCSubjectHeading> broaderHeadings = new ArrayList<LCSubjectHeading>();
	public ArrayList<LCSubjectHeading> narrowerHeadings = new ArrayList<LCSubjectHeading>();
	public ArrayList<LCSubjectHeading> relatedHeadings = new ArrayList<LCSubjectHeading>();
	public ArrayList<String> variantTerms = new ArrayList<String>();

	public LCSubjectHeading(String headingName, URI uri) {
		this.headingName = headingName;
		this.uri = uri;
		this.count = 1;
	}

	/**
	 * A constructor for the LCHeading class, taking a concept string in an
	 * ArrayList where element 0 is the root concept and subsequent elements are
	 * less general. The method recursively constructs the heading string and
	 * assigns the uri to the last element.
	 * 
	 * @param oclc
	 * @param count
	 **/
	public LCSubjectHeading(ArrayList<String> conceptString, URI uri, CatRecord catRecord, int count) {
		System.out.println("Starting LCHeading constructor");
		if (conceptString.isEmpty()) {
			System.exit(-1);
		}
		this.headingName = conceptString.get(0);
		conceptString.remove(0);
		this.count = count;
		if (!conceptString.isEmpty()) {
			this.subheadings.add(new LCSubjectHeading(conceptString, uri, catRecord, count));
		} else {

			this.uri = uri;
			if (!this.catRecords.contains(catRecord)) {
				this.catRecords.add(catRecord);
			}

		}
	}

	/**
	 * /** A constructor for the LCHeading class, taking a concept string (or an RDF
	 * "complexTopic") in an ArrayList where element 0 is the root concept and
	 * subsequent concepts are less general. The method recursively constructs the
	 * heading string and assigns the uri to the last element.
	 * 
	 * @param conceptString
	 *            - ArrayList where element 0 is the root concept and subsequent
	 *            concepts are less general.
	 * @param rootURI
	 *            - the URI for the root concept
	 * @param complexURI
	 *            - the URI for the complexTopic
	 * @param oclc
	 *            - the OCLC accession number for the item which contains the topic
	 * @param stringLength
	 *            - the size of the initial arraylist. (A bit sloppy, but need a way
	 *            of knowing which concept is the root in order to add the rootURI)
	 * @param count
	 */
	public LCSubjectHeading(ArrayList<String> conceptString, URI rootURI, URI complexURI,
			CatRecord catRecord, int stringLength, int count) {
		System.out.println("Starting LCHeading constructor");
		if (conceptString.isEmpty()) {
			System.exit(-1);
		}

		this.headingName = conceptString.get(0);
		if (conceptString.size() == stringLength) {
			this.uri = rootURI;
		}
		conceptString.remove(0);
		this.count = count;
		if (!conceptString.isEmpty()) {
			if (!this.catRecords.contains(catRecord)) {
				this.catRecords.add(catRecord);
			}
			this.subheadings
					.add(new LCSubjectHeading(conceptString, rootURI, complexURI, catRecord, stringLength, count));
		} else {

			this.uri = complexURI;
			if (!this.catRecords.contains(catRecord)) {
				this.catRecords.add(catRecord);
			}

		}
	}

	/**
	 * Returns the subheadings for an LCHeading object.
	 **/
	public ArrayList<LCSubjectHeading> getSubheadings() {
		return this.subheadings;
	}

	/**
	 * This method takes a concept string array where element 0 is the root concept,
	 * etc, and attempts to add new concepts to the LCHeading hierarchy at the
	 * appropriate level. If the LCHeading object on which this method is called
	 * does not have a root concept matching the first element in the conceptString,
	 * this method returns -1. For instantiating concept hierarchies from a new
	 * root, use the constructor instead.
	 **/

	public boolean addSubdivisions(ArrayList<String> conceptString, URI uri, CatRecord catRecord, int count) {
		System.out.println("Starting addSubdivisions w/ " + conceptString.get(0));
		// because the if statement below checks for equality, this can only be false if
		// the first element of conceptString doesn't match the root concept in the
		// hierarchy.

		if (!this.headingName.contentEquals(conceptString.get(0))) {
			System.out.println("returning");
			System.exit(0);
			return false;
		}
		// the current node in the LCHeading hierarchy currently matches
		// the first element of the conceptString, so increment the counter for the
		// LCHeading and remove the concept from the string.
		this.count += count;
		if (!this.catRecords.contains(catRecord)) {
			this.catRecords.add(catRecord);
		}
		conceptString.remove(0);
		if (conceptString.isEmpty()) {
			if (this.uri == null) {
				this.uri = uri;
			}

			return true;
		}
		for (int i = 0; i < subheadings.size(); i++) {
			// if a subheading matches the current root string element, we move one level
			// down
			// the hierarchy.
			if (conceptString.get(0).contentEquals(subheadings.get(i).getHeading())) {
				if (!this.catRecords.contains(catRecord)) {
					this.catRecords.add(catRecord);
				}
				return this.subheadings.get(i).addSubdivisions(conceptString, uri, catRecord, count);
			}
		}
		// since no subheadings are a match, we have reached the part of the concept
		// string that is new to the hierarchy (and the level of the hierarchy at which
		// we can add the remaining part of the string.)
		this.subheadings.add(new LCSubjectHeading(conceptString, uri, catRecord, count));
		return true;
	}

	/**
	 * Returns the LCHeading heading name.
	 **/
	public String getHeading() {
		return this.headingName;
	}

	public void setBroaderHeadings(ArrayList<LCSubjectHeading> broaderTerms) {
		this.broaderHeadings = broaderTerms;
	}

	public void setNarrowerHeadings(ArrayList<LCSubjectHeading> narrowerTerms) {
		this.narrowerHeadings = narrowerTerms;
	}
	public void setRelatedHeadings(ArrayList<LCSubjectHeading> relatedTerms) {
		this.relatedHeadings = relatedTerms;
	}

	/**
	 * Returns the number of instances of an LCHeading concept, at the given level
	 * of the LCHeading hierarchy.
	 **/
	public int getCount() {
		return this.count;
	}

	/**
	 * Returns the URI for an LCHeading concept or concept string.
	 **/
	public URI getURI() {
		return this.uri;
	}

	/**
	 * Returns a string representation of an LCHeading object.
	 * 
	 * @param level
	 *            - set to 1 for default
	 * @return
	 */
	public String toString(int level) {
		String headingTree = "";
		String uri = "";
		for (int i = 1; i < level; i++) {
			headingTree += "\t";
		}
		if (this.uri != null)
			uri = this.uri.toString();

		headingTree += this.headingName + " (count:" + this.count + ") (URI:" + uri + ") (catRecords: " + this.catRecords
				+ ")\n";
		for (int j = 0; j < this.subheadings.size(); j++) {
			headingTree += this.subheadings.get(j).toString(level + 1);
		}
		return headingTree;
	}

	/**
	 * Prints the LCHeading tree
	 */
	public void print() {
		System.out.println(this.toString(1));
	}

	public void setVariantTerms(ArrayList<String> variantTerms) {
		this.variantTerms = variantTerms;
		
	}
}
