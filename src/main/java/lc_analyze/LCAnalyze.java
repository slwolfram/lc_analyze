package lc_analyze;
//File:         LC_analyzer_main

//Created:      [creation date]
//Author:       <A HREF="mailto:[shanewolfram@hotmail.com]">[Shane Wolfram]</A>
//
//
//This program takes in a set of MARCXML catalog records and performs a specified operation (or series of 
//operations), utilizing Library of Congress name and subject heading authority linked data. This implementation 
//is designed to work specifically with Alma records (from the UW Madison library system) exported to Primo. 
//Other formats are likely to require (minor) modifications to work correctly, specifically w/r/t how URI's 
//are extracted from records (see genHeadings method). Note that records must already have LC URIs for subject 
//headings and/or names for this program to be of any use.
//
//List of operations:
//
//

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.marc4j.MarcReader;
import org.marc4j.MarcXmlReader;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;
import org.marc4j.marc.VariableField;

public class LCAnalyze {

	/**
	 * The main method for LC_analyzer. Specify action via command prompt: d -
	 * Downloads RDF subject/authority headings c - Verify correctness of MARCXML
	 * record subject/authority headings (needs RDF headings)
	 **/

	/**
	 * authorityURIS - a non-redundant list of all subject/name heading URIs. Used
	 * to download RDF headings.
	 */
	public static ArrayList<String> authorityURIs = new ArrayList<String>();
	/**
	 * rdfModel - This is the model rdf4j constructs from the lc rdf files
	 */
	public static Model rdfModel = new TreeModel();
	/**
	 * headingTrees - a list of all the LCHeading trees. Used to model the structure
	 * and distribution of LC name/subject headings for a given MARCXML dataset.
	 */
	public static ArrayList<LCHeading> headingTrees = new ArrayList<LCHeading>();

	public static void main(String[] args) throws URISyntaxException, IOException {

		if (args.length == 0) {
			System.out.println(
					"Please specify an action using command line arguments. For a list of actions, use the 'h' command.");
			System.exit(-1);
		}
		File f = new File("./src/main/java/MARC_files/");
		File[] filesList = f.listFiles();

		switch (args[0]) {

		// use to extract uri from MARCXML and download RDF headings
		case "d":
			for (File file : filesList) {
				if (file.isFile() && !file.getName().startsWith("._") && file.getName().endsWith(".xml")) {
					System.out.println(file.getName());
					extractAuthorityURIs(file);
				}
			}
			dlRDF(authorityURIs);
			System.exit(0);

			// analyzes the distribution of subject headings by constructing LCHeading trees
			// from RDF data
			// MUST already have dl'ed the RDF files for this to work!
		case "a":
			genRDFModel(new File("./src/main/java/RDF_files/"));
			for (File file : filesList) {
				if (file.isFile() && !file.getName().startsWith("._") && file.getName().endsWith(".xml")) {
					buildLCHeadings(file);
				}
			}
			for (int i = 0; i < headingTrees.size(); i++) {
				headingTrees.get(i).print();
			}
			System.exit(0);
		case "c":

			for (File file : filesList) {
				if (file.getName().endsWith(".xml") && !file.getName().startsWith("._")) {
					System.out.println(file.getName());
					extractAuthorityURIs(file);
				}
			}
			rdfModel = genRDFModel(authorityURIs);
			Model rdfHeadings = genRDFHeadings();
			verify(rdfHeadings);
			// ArrayList<String> LCSHStrings = getLCSHStrings(file.getName());
			// verify(authorityURIs, LCSHStrings);
			System.exit(0);

		case "h":
		default:
			System.out.println("The action you have specified - " + args[0]
					+ " - is not valid. Use the 'h' command or consult the README for a list of valid actions.");
		}

	}

	/**
	 * Method name: genRDFModel
	 * 
	 * Description: This method takes a pointer to the folder where the RDF ntriples
	 * files are kept, and generates an rdf4j model from the .nt files.
	 * 
	 * @throws FileNotFoundException
	 */
	private static void genRDFModel(File file) throws FileNotFoundException {
		File[] filesList = file.listFiles();
		for (File f : filesList) {
			if (f.isFile() && f.getName().endsWith(".nt")) {
				System.out.println(f.getPath());
				InputStream input = new FileInputStream(f.getPath());
				try {
					rdfModel.addAll(Rio.parse(input, "", RDFFormat.NTRIPLES));
				} catch (RDFParseException | UnsupportedRDFormatException | IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Method name: buildLCHeadings
	 * 
	 * Description: This method takes a MARCXML file, extracts the uris for its
	 * name/subject headings, builds LCHeading strings for the headings from the RDF
	 * authority data, and adds the strings to the headingTrees database.
	 */
	private static void buildLCHeadings(File file) throws FileNotFoundException, UnsupportedEncodingException {
		MarcReader marcReader = new MarcXmlReader(getRecordElement(file));
		while (marcReader.hasNext()) {
			Record record = marcReader.next();
			String oclc = extractOCLC(record);
			// extract URI & save to array
			ArrayList<URI> uris = extractURIs(record);
			// build subject heading string arrays from RDF, and add to headingTrees
			for (int i = 0; i < uris.size(); i++) {
				ArrayList<String> conceptString = buildConceptString(uris.get(i));
				addConceptString(conceptString, uris.get(i), oclc);
			}
		}
	}

	private static void addConceptString(ArrayList<String> conceptString, URI uri, String oclc) {
		System.out.println("starting addConceptString...");
		for (int i = 0; i < headingTrees.size(); i++) {
			if (headingTrees.get(i).getHeading().contentEquals(conceptString.get(0))) {
				headingTrees.get(i).addHeadingElements(conceptString, uri, oclc);
				return;
			}
		}
		headingTrees.add(new LCHeading(conceptString, uri, oclc));
	}

	/**
	 * Method name: buildConceptString
	 * 
	 * Description: This method queries an RDF file for the authoritativeLabel
	 * corresponding to a heading uri converts the concept string into an ArrayList,
	 * with the root concept at index 0 and subsequent concepts following
	 * respectively.
	 */
	private static ArrayList<String> buildConceptString(URI uri) {
		ArrayList<String> conceptString = new ArrayList<String>();
		String authorityLabel;
		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI iri = vf.createIRI(uri.toString());
		IRI auth = vf.createIRI("http://www.loc.gov/mads/rdf/v1#authoritativeLabel");
		System.out.println(rdfModel.filter(iri, auth, null).toString());
		authorityLabel = StringUtils.substringBetween(rdfModel.filter(iri, auth, null).toString(), "Label, \"", "\"@");
		
		if (uri.toString().contains("subjects")) {
			String[] s = authorityLabel.split("--");
			for (int i = 0; i < s.length; i++) {
				conceptString.add(s[i]);
				System.out.println(conceptString.get(i));
			}
		} else {
			String[] s = authorityLabel.split("--");
			for (int i = 0; i < s.length; i++) {
				conceptString.add(s[i]);
				System.out.println(conceptString.get(i));
			}
		}
		
		
		return conceptString;
	}

	/**
	 * Method name: extractURIs
	 * 
	 * Description: This method takes a marc4j record and returns an ArrayList
	 * containing all of the uris for name/subject headings, excluding duplicates.
	 */
	private static ArrayList<URI> extractURIs(Record record) {
		ArrayList<URI> uris = new ArrayList<URI>();
		List<DataField> fields = record.getDataFields();
		// extract URI & save to array

		for (int i = 0; i < fields.size(); i++) {
			// the subfield for uris is $041, but since marc4j only recognizes character
			// subheading tags,
			// we need to search for the following prefix:
			if (fields.get(i).toString().contains("$041-LIBRARY_OF_CONGRESS")) {
				String uri;
				String field = fields.get(i).toString();
				uri = StringUtils.substringBetween(field, "CONGRESS-", "$9");
				uri = uri.replaceAll("\\s", "");

				// lc subject heading control numbers start with 'sh'
				if (uri.startsWith("sh")) {
					uri = "http://id.loc.gov/authorities/subjects/" + uri;
					System.out.println(uri);
				}
				// lc names control numbers start with 'n'
				else if (uri.startsWith("n")) {
					uri = "http://id.loc.gov/authorities/names/" + uri;
					System.out.println(uri);
				} else {
					System.out.println("unrecognized control number format. Skipping.");
					continue;
				}
				// try to add uri to arraylist
				boolean hasURI = false;
				for (int j = 0; j < uris.size(); j++) {
					if (uris.get(j).toString().contentEquals(uri)) {
						hasURI = true;
						break;
					}
				}
				try {

					if (!hasURI)
						uris.add(new URI(uri));
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
			}
		}

		return uris;
	}

	/**
	 * Method name: extractOCLC This method takes a list of record fields and
	 * returns the OCLC number as a string. However, it only looks in the "035"
	 * field and expects the format (OCoLC)#######, as this matches the format of
	 * records exported from Primo (and saves the time of having to look through all
	 * record fields.)
	 */
	private static String extractOCLC(Record record) {
		String oclc = "";
		oclc = record.getVariableField("035").toString();
		// remove whitespaces (just in case)
		oclc.replaceAll("\\s+", "");
		oclc = StringUtils.substringAfter(oclc, "(OCoLC)");
		// System.out.println(oclc);
		return oclc;
	}

	/**
	 * This method receives an array of URIs representing lc names/subject headings,
	 * and an RDF Model containing the headings/concepts to which the URIs refer. It
	 * returns an RDF model containing just the authoritative label corresponding to
	 * the URI.
	 * 
	 * @return An RDF model containing just the authoritativeLabels corresponding to
	 *         an array of URIs.
	 **/

	private static Model genRDFHeadings() {

		Model headingTriples = new TreeModel();
		for (int i = 0; i < authorityURIs.size(); i++) {
			ValueFactory vf = SimpleValueFactory.getInstance();
			IRI iri = vf.createIRI(authorityURIs.get(i));
			IRI auth = vf.createIRI("http://www.loc.gov/mads/rdf/v1#authoritativeLabel");
			headingTriples.addAll(rdfModel.filter(iri, auth, null));
		}
		System.out.println("printing heading triples...");
		for (Statement statement : headingTriples) {
			System.out.println(statement);
		}
		return headingTriples;
	}

	/**
	 * Method name: extractAuthorityURIs
	 *
	 * This method extracts the LC name/subject uris and adds them to the public
	 * authorityURIs ArrayList, also checking to avoid redundant URIs.
	 */
	public static void extractAuthorityURIs(File f) throws IOException {

		MarcReader marcReader = new MarcXmlReader(getRecordElement(f));
		while (marcReader.hasNext()) {
			Record record = marcReader.next();
			/* extract subject fields */
			List<VariableField> fields = new ArrayList<>();
			fields = record.getVariableFields();
			System.out.println(fields.toString());
			// extract URI & save to array
			for (int i = 0; i < fields.size(); i++) {
				// the subfield for uris is $041, but since marc4j only recognizes character
				// subheading tags,
				// we need to search for the following prefix:
				if (fields.get(i).toString().contains("$041-LIBRARY_OF_CONGRESS")) {

					String uri;
					String field = fields.get(i).toString();
					System.out.println(field.toString());

					uri = StringUtils.substringBetween(field, "CONGRESS-", "$9");
					uri = uri.replaceAll("\\s", "");
					if (uri.contentEquals("sh85064852")) {
						System.out.println("FOUND IT");
					}
					// lc subject heading control numbers start with 'sh'
					if (uri.startsWith("sh")) {
						uri = "http://id.loc.gov/authorities/subjects/" + uri;
						System.out.println(uri);

					}
					// lc names control numbers start with 'n'
					else if (uri.startsWith("n")) {
						uri = "http://id.loc.gov/authorities/names/" + uri;
						System.out.println(uri);

					} else {
						System.out.println("unrecognized control number format. Skipping.");
						continue;
					}
					boolean hasURI = false;
					for (int j = 0; j < authorityURIs.size(); j++) {
						if (authorityURIs.get(j).contentEquals(uri))
							hasURI = true;
					}

					if (!hasURI)
						authorityURIs.add(uri);
				}
			}
		}
	}

	/**
	 * This method receives an array of URIs, and generates an RDF model containing
	 * the headings represented by the URIs from NTRIPLES files stored in the
	 * RDF_files folder. These will have to be downloaded beforehand, either by
	 * running the dlRDF method or downloading the complete vocabularies, which can
	 * be found at https://id.loc.gov/download/
	 * 
	 * @return An RDF model containing the authoritative headings for a given object
	 **/

	private static Model genRDFModel(ArrayList<String> authorityURIs)
			throws RDFParseException, UnsupportedRDFormatException, IOException {
		Model model = new TreeModel();
		boolean usedb = false; // if true, ALL authority and subject heading triples will be added to the
								// model.
		File f = new File("./src/main/java/RDF_files/");
		File[] filesList = f.listFiles();

		if (usedb) {
			InputStream input = LCAnalyze.class.getResourceAsStream("./RDF_files/authoritiessubjects.madsrdf.nt");
			model.addAll(Rio.parse(input, "", RDFFormat.NTRIPLES));

		} else {
			for (File file : filesList) {
				if (file.isFile() && file.getName().endsWith(".nt")) {
					InputStream input = LCAnalyze.class.getResourceAsStream("./../RDF_files/" + file.getName());
					System.out.println(file.getPath());
					if (input != null) {
						model.addAll(Rio.parse(input, "", RDFFormat.NTRIPLES));
					} else {
						System.out.println("NOT FOUND");
						System.exit(0);
					}
				}
			}
			for (Statement statement : model) {
				System.out.println(statement);
			}
		}
		return model;
	}

	/**
	 * This method takes in a MARCXML file and returns an ArrayList of LC subject
	 * heading/name authority URIs from in the file.
	 * 
	 * @return An ArrayList of LCSH/name authority URIs.
	 **/

	/**
	 * This method takes a MARCXML file wrapped in OAI-PMH and extracts the record
	 * element (which can then be loaded by MarcXMLReader.)
	 * 
	 * @return A FileInputStream for a MARCXML file.
	 **/

	public static FileInputStream getRecordElement(File f) throws FileNotFoundException, UnsupportedEncodingException {
		Scanner in = new Scanner(f, "utf-8");
		File tmp = new File("tmp.xml");
		PrintWriter out = new PrintWriter(tmp, "utf-8");
		boolean foundRecord = false;

		while (in.hasNextLine()) {
			String ln = in.nextLine();
			if (ln.startsWith("<record xmlns=") && ln.endsWith("</record>")) {
				out.write(ln);
				foundRecord = true;
				break;
			}
		}
		if (!foundRecord) {
			System.out.println(f.getPath() + " is not in the expected format.");
			System.exit(-1);
		}
		in.close();
		out.close();
		return new FileInputStream(tmp);
	}

	/**
	 * This method is used to download the LC authority name/subject headings for a
	 * set of URIs in NTRIPLES format and store them in the RDF_files folder. This
	 * is useful for testing purposes, or if the full dataset (which for name
	 * authorities is ~10GB) cannot be practically be loaded.
	 **/
	public static void dlRDF(ArrayList<String> authorityURIs) throws IOException, URISyntaxException {
		for (int i = 0; i < authorityURIs.size(); i++) {
			String filename = authorityURIs.get(i).substring(authorityURIs.get(i).lastIndexOf("/") + 1) + ".nt";
			boolean check = new File("./src/main/java/RDF_files/", filename).exists();
			System.out.println(filename);
			// we use check to verify if the RDF file already exists (to avoid
			// re-downloading existing files)
			if (!check) {
				URI uri = new URI(authorityURIs.get(i));

				// dl file ...

				File rdf_file = new File("./src/main/java/RDF_files/"
						+ uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1) + ".nt");
				CloseableHttpClient client = HttpClientBuilder.create().build();
				HttpUriRequest request = RequestBuilder.get().setUri(uri).setHeader("accept", "text/plain").build();
				HttpResponse response = client.execute(request);
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					try (FileOutputStream outstream = new FileOutputStream(rdf_file)) {
						entity.writeTo(outstream);
						System.out.println("COMPLETE");
					}
				}
			}
		}
	}

	public static void verify(Model model) {
		/*
		 * String authorityLabel; for (int i = 0; i < subjectHeadings.size(); i++) {
		 * ValueFactory vf = SimpleValueFactory.getInstance(); IRI iri =
		 * vf.createIRI(subjectHeadings.get(i).getURI()); IRI auth =
		 * vf.createIRI("http://www.loc.gov/mads/rdf/v1#authoritativeLabel");
		 * System.out.println(rdfModel.filter(iri, auth, null).toString());
		 * authorityLabel = StringUtils.substringBetween(rdfModel.filter(iri, auth,
		 * null).toString(), "Label, \"", "\"@"); System.out.println(authorityLabel);
		 * System.out.println(subjectHeadings.get(i).getMainHeading()); for (int j = 0;
		 * j < subjectHeadings.get(i).getSubdivision().size(); j++) {
		 * System.out.println(subjectHeadings.get(i).getSubdivision().get(j)); } } for
		 * (int i = 0; i < nameHeadings.size(); i++) {
		 * 
		 * }
		 */
	}
}