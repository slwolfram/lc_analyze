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
import org.marc4j.marc.Record;
import org.marc4j.marc.VariableField;

public class LCAnalyze {

	/**
	 * The main method for LC_analyzer. Specify action via command prompt: d -
	 * Downloads RDF subject/authority headings c - Verify correctness of MARCXML
	 * record subject/authority headings (needs RDF headings)
	 **/

	public static ArrayList<String> authorityURIs = new ArrayList<String>();
	public static Model rdfModel;
	public static ArrayList<SubjectHeading> subjectHeadings = new ArrayList<SubjectHeading>();
	public static ArrayList<NameHeading> nameHeadings = new ArrayList<NameHeading>();

	public static void main(String[] args) throws URISyntaxException, IOException {

		if (args.length == 0) {
			System.out.println(
					"Please specify an action using command line arguments. For a list of actions, use the 'h' command.");
			System.exit(-1);
		}
		File f = new File("./src/main/java/MARC_files/");
		File[] filesList = f.listFiles();

		switch (args[0]) {

		case "d":
			for (File file : filesList) {
				if (file.isFile() && !file.getName().startsWith("._") && file.getName().endsWith(".xml")) {
					System.out.println(file.getName());
					genHeadings(file);
				}
			}
			dlRDF(authorityURIs);
			System.exit(0);

		case "c":

			for (File file : filesList) {
				if (file.getName().endsWith(".xml") && !file.getName().startsWith("._")) {
					System.out.println(file.getName());
					genHeadings(file);
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

	public static void genHeadings(File f) throws IOException {

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

					boolean isName = true;
					String uri;
					String recordname = f.getName();
					String mainHeading;
					ArrayList<String> subheadings = new ArrayList<String>();
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
						isName = false;

					}
					// lc names control numbers start with 'n'
					else if (uri.startsWith("n")) {
						uri = "http://id.loc.gov/authorities/names/" + uri;
						System.out.println(uri);
						isName = true;

					} else {
						System.out.println("unrecognized control number format. Skipping.");
						continue;
					}
					boolean hasURI = false;
					for (int j = 0; j < authorityURIs.size(); j++) {
						if (authorityURIs.get(j).contentEquals(uri))
							hasURI = true;
					}
					System.out.println(hasURI);
					if (!hasURI) {
						authorityURIs.add(uri);
						mainHeading = StringUtils.substringBetween(field, "$a", "$");
						System.out.println(StringUtils.substringBetween(field, "$a", "$"));
						for (int j = 0; j < field.length(); j++) {
							System.out.println(field.charAt(j));
							if (field.charAt(j) == '$' && field.charAt(j + 1) != 'a'
									&& Character.isLetter(field.charAt(j + 1))) {
								System.out.println(field.substring(j, j + 2));
								System.out.println(StringUtils.substringBetween(field, field.substring(j, j + 2), "$"));
								subheadings.add(StringUtils.substringBetween(field, field.substring(j, j + 2), "$"));
							}
						}
						if (isName) {
							nameHeadings.add(new NameHeading(recordname, mainHeading, subheadings, uri));
						} else {
							subjectHeadings.add(new SubjectHeading(recordname, mainHeading, subheadings, uri));
						}
						System.out.println(uri);
					}
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
		String authorityLabel;
		for (int i = 0; i < subjectHeadings.size(); i++) {
			ValueFactory vf = SimpleValueFactory.getInstance();
			IRI iri = vf.createIRI(subjectHeadings.get(i).getURI());
			IRI auth = vf.createIRI("http://www.loc.gov/mads/rdf/v1#authoritativeLabel");
			System.out.println(rdfModel.filter(iri, auth, null).toString());
			authorityLabel = StringUtils.substringBetween(rdfModel.filter(iri, auth, null).toString(), "Label, \"",
					"\"@");
			System.out.println(authorityLabel);
			System.out.println(subjectHeadings.get(i).getMainHeading());
			for (int j = 0; j < subjectHeadings.get(i).getSubdivision().size(); j++) {
				System.out.println(subjectHeadings.get(i).getSubdivision().get(j));
			}
		}
		for (int i = 0; i < nameHeadings.size(); i++) {

		}
	}
}