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
//////
//////

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
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
	 * lcshList - a list of all the LCHeading objects. Used to look up individual
	 * headings and traverse adjacent nodes. MARCXML dataset.
	 */
	public static ArrayList<LCSubjectHeading> lcshList = new ArrayList<LCSubjectHeading>();
	/**
	 * Assign value in buildLCHeadings method. This controls the depth of
	 * broader/narrower headings generated for each LCSubjectHeading within the
	 * record set.
	 */
	public static final int maxDepth = 4;

	/**
	 * nameHeadingTrees - a list of all the LCHeading name trees. Used to model the
	 * structure and distribution of LC name headings for a given MARCXML dataset.
	 */
	public static ArrayList<LCSubjectHeading> nameHeadingTrees = new ArrayList<LCSubjectHeading>();

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws URISyntaxException, IOException {

		File f = new File("./src/main/java/MARC_files/");
		File[] filesList = f.listFiles();
		// load lcshList
		File f2 = new File("./");
		File[] filesList2 = f2.listFiles();
		boolean save = false;
		for (int i = 0; i < f2.listFiles().length; i++) {
			if (filesList2[i].getName().equals("save")) {
				try {
					FileInputStream fis = new FileInputStream("save");
					ObjectInputStream ois = new ObjectInputStream(fis);
					lcshList = (ArrayList<LCSubjectHeading>) ois.readObject();
					ois.close();
					fis.close();
				} catch (IOException ioe) {
					ioe.printStackTrace();
					return;
				} catch (ClassNotFoundException c) {
					System.out.println("Class not found");
					c.printStackTrace();
					return;
				}
				save = true;
			}
		}
		if (!save) {
			genRDFModel(new File("./src/main/java/RDF_files/"));
			for (File file : filesList) {
				if (file.isFile() && !file.getName().startsWith("._") && file.getName().endsWith(".xml")) {
					buildLCHeadings(file);
				}
			}
		}
		// save lcshList
		try {
			FileOutputStream fos = new FileOutputStream("save");
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(lcshList);
			oos.close();
			fos.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		for (int i = 0; i < lcshList.size(); i++) {
			if (lcshList.get(i).getCount() > 0) {
				lcshList.get(i).print();
			}
		}
		console();
		/*
		 * for (int i = 0; i < nameHeadingTrees.size(); i++) {
		 * nameHeadingTrees.get(i).print(); }
		 */
		System.exit(0);

	}

	private static void console() {
		Scanner reader = new Scanner(System.in); // Reading from System.in
		boolean exit = false;
		while (!exit) {
			System.out.println("Console started. For a list of commands, type 'h'");
			System.out.print("> ");
			String input = reader.nextLine();
			String[] result = input.split(" ");
			switch (result[0]) {
			case "ls":
				if (result.length == 1) {
					for (int i = 0; i < lcshList.size(); i++) {
						if (lcshList.get(i).getCount() > 0) {
							lcshList.get(i).print();
						}
					}
				} else if (result[1].equals("*")) {
					for (int i = 0; i < lcshList.size(); i++) {
						lcshList.get(i).print();
					}
				} else if (StringUtils.isAlpha(result[1])) {

					System.out.println(result[1]);
					System.out.println(result[1]);

					for (int i = 0; i < lcshList.size(); i++) {
						if (lcshList.get(i).getHeading().startsWith(result[1])) {
							lcshList.get(i).print();
						}
					}
				}
				break;
			case "search":
				ArrayList<String> query = new ArrayList<String>();
				if (result.length == 1) {
					System.out.print("Enter search keyword: ");
					String[] q = reader.nextLine().split("\\s+");
					for (int i = 0; i < q.length; i++) {
						query.add(q[i]);
					}
				} else {
					for (int i = 1; i < result.length; i++) {
						query.add(result[i]);
					}
				}

				ArrayList<SearchResult> resultsList = new ArrayList<SearchResult>();
				boolean match = true;
				for (int i = 0; i < lcshList.size(); i++) {
					for (int j = 0; j < query.size(); j++) {
						if (lcshList.get(i).getHeading().toLowerCase().contains(query.get(j).toLowerCase()) || lcshList
								.get(i).getURI().toString().toLowerCase().contains(query.get(j).toLowerCase())) {
							match = true;
						} else {
							match = false;
							break;
						}
					}
					String note = "";
					if (match == false) {
						for (int j = 0; j < lcshList.get(i).getVariantTerms().size(); j++) {
							for (int k = 0; k < query.size(); k++) {
								if (lcshList.get(i).getVariantTerms().get(j).toLowerCase()
										.contains(query.get(k).toLowerCase())) {
									match = true;
								} else {
									match = false;
								}
							}
							if (match == true) {
								note = lcshList.get(i).getVariantTerms().get(j);
							}
						}
					}
					if (match) {
						String queryString = query.get(0);
						for (int j = 1; j < query.size(); j++) {
							queryString += " " + query.get(j);
						}
						if (queryString.equals(lcshList.get(i).getHeading()) || queryString.equals(note)) {
							resultsList.add(0, new SearchResult(lcshList.get(i), 1, note));
						} else {
							resultsList.add(new SearchResult(lcshList.get(i), 2, note));
						}
					}
				}
				for (int i = 0; i < resultsList.size(); i++) {
					System.out.println("(" + (i + 1) + ") " + resultsList.get(i).getSubjectHeading().getHeading() + " ("
							+ resultsList.get(i).getSubjectHeading().getCount() + " records)");
					if (resultsList.get(i).getNote().length() > 1) {
						System.out.println("----Variant Term: " + resultsList.get(i).getNote());
					}
				}
				System.out.println("\n (Enter heading number or 'exit' to return)");
				boolean e = false;
				while (!e) {
					System.out.print("> ");
					String input2 = reader.nextLine();
					if (input2.equalsIgnoreCase("exit")) {
						e = true;
					} else if (StringUtils.isNumeric(input2) && Integer.parseInt(input2) > 0
							&& Integer.parseInt(input2) <= resultsList.size()) {
						headingNavigator(resultsList.get(Integer.parseInt(input2) - 1).getSubjectHeading(), reader);
						e = true;
					}
				}
				break;
			case "exit":
				exit = true;
				break;
			case "h":
				System.out.println("Command list:\nls - list all headings w/ with >= 1 record");
				System.out.println("exit - exit console");

				// once finished

			}
		}
		System.out.println("Exiting console");
		reader.close();
	}

	private static void headingNavigator(LCSubjectHeading lcsh, Scanner reader) {
		boolean exit = false;
		while (!exit) {
			System.out.println(lcsh.getHeading() + " (" + lcsh.getCount() + " records)");
			System.out.println("URI: " + lcsh.getURI());

			if (!lcsh.getBroaderTerms().isEmpty()) {
				System.out.println("Broader terms:");
				for (int i = 0; i < lcsh.getBroaderTerms().size(); i++) {
					System.out.println("(" + (i + 1) + ") " + lcsh.getBroaderTerms().get(i).getHeading() + " ("
							+ lcsh.getBroaderTerms().get(i).getCount() + " records)");
				}
			}
			if (!lcsh.getNarrowerTerms().isEmpty()) {
				System.out.println("Narrower terms:");
				for (int i = 0; i < lcsh.getNarrowerTerms().size(); i++) {
					System.out.println("(" + (i + lcsh.getBroaderTerms().size() + 1) + ") "
							+ lcsh.getNarrowerTerms().get(i).getHeading() + " ("
							+ lcsh.getNarrowerTerms().get(i).getCount() + " records)");
				}
			}
			if (!lcsh.getRelatedTerms().isEmpty()) {
				System.out.println("Related terms:");
				for (int i = 0; i < lcsh.getRelatedTerms().size(); i++) {
					System.out.println("(" + (i + lcsh.getBroaderTerms().size() + lcsh.getNarrowerTerms().size() + 1)
							+ ") " + lcsh.getRelatedTerms().get(i).getHeading() + " ("
							+ lcsh.getRelatedTerms().get(i).getCount() + " records)");
				}
			}
			System.out.println("\nEnter heading number / 'r' - view item records / 'exit' - return");
			System.out.print("> ");
			String input = reader.nextLine();
			int maxInt = lcsh.getBroaderTerms().size() + lcsh.getNarrowerTerms().size() + lcsh.getRelatedTerms().size();

			if (input.equalsIgnoreCase("r")) {
				System.out.println("Record titles:");
				for (int i = 0; i < lcsh.getRecords().size(); i++) {
					System.out.println("(" + (i + 1) + ") " + lcsh.getRecords().get(i).getTitle());
				}
				System.out.println("\nEnter record number / 'exit' - return");
				System.out.print("> ");
				input = reader.nextLine();
				if (StringUtils.isNumeric(input) && Integer.parseInt(input) > 0
						&& Integer.parseInt(input) <= lcsh.getRecords().size()) {
					lcsh.getRecords().get(Integer.parseInt(input)-1).print();
					System.out.println("\nPress any key to continue:");
					System.out.print("> ");
					input = reader.nextLine();
				}
			} else if (input.equalsIgnoreCase("exit")) {
				exit = true;
			} else if (StringUtils.isNumeric(input) && Integer.parseInt(input) > 0
					&& Integer.parseInt(input) <= maxInt) {
				if (Integer.parseInt(input) <= lcsh.getBroaderTerms().size()) {
					lcsh = lcsh.getBroaderTerms().get(Integer.parseInt(input) - 1);
				} else if (Integer
						.parseInt(input) <= (lcsh.getBroaderTerms().size() + lcsh.getNarrowerTerms().size())) {
					lcsh = lcsh.getNarrowerTerms().get(Integer.parseInt(input) - lcsh.getBroaderTerms().size() - 1);
				} else {
					lcsh = lcsh.getRelatedTerms().get(Integer.parseInt(input) - lcsh.getBroaderTerms().size()
							- lcsh.getNarrowerTerms().size() - 1);
				}
			}
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
		MarcReader marcReader = new MarcXmlReader(MARCXMLHelper.getRecordElement(file));
		while (marcReader.hasNext()) {
			Record record = marcReader.next();
			CatRecord catRecord = createCatRecord(record);
			// extract URI & save to array
			ArrayList<URI> uris = MARCXMLHelper.extractURIs(record);
			// build subject heading string arrays from RDF, and add to headingTrees
			for (int i = 0; i < uris.size(); i++) {
				ArrayList<String> conceptString = buildConceptString(uris.get(i));
				addConceptString(conceptString, uris.get(i), catRecord, 1, 0);
			}
		}
	}

	private static CatRecord createCatRecord(Record record) {
		String title = MARCXMLHelper.extractTitle(record);
		String author = MARCXMLHelper.extractAuthor(record);
		String placeOfPublication = MARCXMLHelper.extractPlaceOfPublication(record);
		String publisher = MARCXMLHelper.extractPublisher(record);
		String dateOfPublication = MARCXMLHelper.extractDateOfPublication(record);
		String oclc = MARCXMLHelper.extractOCLC(record);
		String description = MARCXMLHelper.extractDescription(record);
		String contentType = MARCXMLHelper.extractContentType(record);
		String mediaType = MARCXMLHelper.extractMediaType(record);
		String carrierType = MARCXMLHelper.extractCarrierType(record);
		return new CatRecord(title, author, placeOfPublication, publisher, dateOfPublication, oclc, description,
				contentType, mediaType, carrierType);
	}

	/**
	 * Method name: addConceptString
	 * 
	 * Description: This method adds a new LCSH heading or heading string to the
	 * lcsh list. If the heading already exists, it can be incremented by passing
	 * the value 1 in the count parameter. (Headings which are not counted are,
	 * e.g., those that are added as broader/narrow terms for an existing heading)
	 */
	private static LCSubjectHeading addConceptString(ArrayList<String> conceptString, URI uri, CatRecord catRecord,
			int count, int depth) throws FileNotFoundException {

		LCSubjectHeading lcsh = null;
		System.out.println("DEPTH: " + depth);

		if (uri.toString().contains("subject")) {
			for (int i = 0; i < lcshList.size(); i++) {
				if (lcshList.get(i).getHeading().equals(conceptString.get(0))) {
					lcshList.get(i).addSubdivisions(conceptString, uri, catRecord, count);
					return lcshList.get(i);
				}
			}
			// need to make sure new root concepts have an associated uri. This happens when
			// a string has > 1 element, since the uri applies to the complex object.
			if (conceptString.size() > 1) {
				URI rootURI = getRootURI(conceptString.get(0));
				lcsh = new LCSubjectHeading(conceptString, rootURI, uri, catRecord, conceptString.size(), count);
			} else {
				lcsh = new LCSubjectHeading(conceptString, uri, catRecord, count);
			}
			lcshList.add(lcsh);
			int narrowerDepth = depth + 1;
			int broaderDepth = depth + 1;
			int relatedDepth = depth + 1;
			lcsh.setVariantTerms(getVariantTerms(lcsh));
			if (narrowerDepth <= maxDepth)
				lcsh.setNarrowerHeadings(getNarrowerHeadings(lcsh, narrowerDepth));
			else
				lcsh.setNarrowerHeadings(new ArrayList<LCSubjectHeading>());
			if (broaderDepth <= maxDepth)
				lcsh.setBroaderHeadings(getBroaderHeadings(lcsh, broaderDepth));
			else
				lcsh.setBroaderHeadings(new ArrayList<LCSubjectHeading>());
			if (relatedDepth <= maxDepth)
				lcsh.setRelatedHeadings(getRelatedHeadings(lcsh, relatedDepth));
			else
				lcsh.setRelatedHeadings(new ArrayList<LCSubjectHeading>());
		}
		return lcsh;
	}

	private static ArrayList<String> getVariantTerms(LCSubjectHeading heading) {
		ArrayList<String> variantTerms = new ArrayList<String>();
		if (heading.getURI() != null) {
			String v = "";
			ValueFactory vf = SimpleValueFactory.getInstance();
			IRI iri = vf.createIRI(heading.getURI().toString());
			IRI auth = vf.createIRI("http://www.loc.gov/mads/rdf/v1#hasVariant");
			v = rdfModel.filter(iri, auth, null).toString();
			if (!v.contentEquals("[]")) {
				ArrayList<String> bnodeList = new ArrayList<String>();

				// extract all the heading uris from the string ..
				Pattern p = Pattern.compile(Pattern.quote("hasVariant, ") + "(.*?)" + Pattern.quote(")"));
				Matcher m = p.matcher(v);
				while (m.find()) {
					if (!m.group(1).equals("[]")) {
						bnodeList.add(m.group(1));
					}
				}
				for (int i = 0; i < bnodeList.size(); i++) {
					IRI auth2 = vf.createIRI("http://www.loc.gov/mads/rdf/v1#variantLabel");
					Resource bnode = vf.createBNode(StringUtils.substringAfter(bnodeList.get(i), "_:"));
					String v2 = rdfModel.filter(bnode, auth2, null).toString();

					variantTerms.add(StringUtils.substringBetween(v2, "variantLabel, \"", "\"@en"));

				}
			}
		}
		return variantTerms;
	}

	private static ArrayList<LCSubjectHeading> getRelatedHeadings(LCSubjectHeading heading, int depth)
			throws FileNotFoundException {
		ArrayList<LCSubjectHeading> relatedHeadings = new ArrayList<LCSubjectHeading>();
		if (heading.getURI() != null) {
			String b = "";
			ValueFactory vf = SimpleValueFactory.getInstance();
			IRI iri = vf.createIRI(heading.getURI().toString());
			IRI auth = vf.createIRI("http://www.loc.gov/mads/rdf/v1#hasReciprocalAuthority");
			b = rdfModel.filter(iri, auth, null).toString();
			if (!b.contentEquals("[]")) {
				ArrayList<String> uriList = new ArrayList<String>();

				// extract all the heading uris from the string ..
				Pattern p = Pattern.compile(Pattern.quote("hasReciprocalAuthority, ") + "(.*?)" + Pattern.quote(")"));
				Matcher m = p.matcher(b);
				while (m.find()) {
					if (!m.group(1).equals("[]")) {
						uriList.add(m.group(1));
					}
				}

				// first see if the related headings already exist in lcshList!
				for (int i = 0; i < lcshList.size(); i++) {
					for (int j = 0; j < uriList.size(); j++) {
						if (lcshList.get(i).getURI().toString().equals(uriList.get(j))) {
							uriList.remove(j);
							relatedHeadings.add(lcshList.get(i));
						}
					}
					if (uriList.isEmpty())
						return relatedHeadings;
				}

				if (depth <= maxDepth) {
					System.out.println("DEPTH: " + depth);
					for (int i = 0; i < uriList.size(); i++) {
						String filepath = DlRDF.download(uriList.get(i));
						InputStream input = new FileInputStream(filepath);
						try {
							rdfModel.addAll(Rio.parse(input, "", RDFFormat.NTRIPLES));
						} catch (RDFParseException | UnsupportedRDFormatException | IOException e) {
							e.printStackTrace();
						}
						// construct concepts from uri
						ArrayList<String> relatedHeadingString = new ArrayList<String>();
						try {
							relatedHeadingString = buildConceptString(new URI(uriList.get(i)));
							LCSubjectHeading relatedHeading = addConceptString(relatedHeadingString,
									new URI(uriList.get(i)), null, 0, depth);
							if (relatedHeading != null)
								relatedHeadings.add(relatedHeading);

						} catch (URISyntaxException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}

		return relatedHeadings;
	}

	private static ArrayList<LCSubjectHeading> getBroaderHeadings(LCSubjectHeading heading, int depth)
			throws FileNotFoundException {
		ArrayList<LCSubjectHeading> broaderHeadings = new ArrayList<LCSubjectHeading>();
		if (heading.getURI() != null) {
			String b = null;
			ValueFactory vf = SimpleValueFactory.getInstance();
			IRI iri = vf.createIRI(heading.getURI().toString());
			IRI auth = vf.createIRI("http://www.loc.gov/mads/rdf/v1#hasBroaderAuthority");
			b = rdfModel.filter(iri, auth, null).toString();
			if (!b.contentEquals("[]")) {
				ArrayList<String> uriList = new ArrayList<String>();

				// extract all the heading uris from the string ..
				Pattern p = Pattern.compile(Pattern.quote("hasBroaderAuthority, ") + "(.*?)" + Pattern.quote(")"));
				Matcher m = p.matcher(b);
				while (m.find()) {
					if (!m.group(1).equals("[]")) {
						uriList.add(m.group(1));
					}
				}

				// first see if the narrower headings already exist in lcshList!
				for (int i = 0; i < lcshList.size(); i++) {
					for (int j = 0; j < uriList.size(); j++) {
						if (lcshList.get(i).getURI().toString().equals(uriList.get(j))) {
							uriList.remove(j);
							broaderHeadings.add(lcshList.get(i));
						}
					}
					if (uriList.isEmpty())
						return broaderHeadings;
				}

				if (depth <= maxDepth) {
					System.out.println("DEPTH: " + depth);
					for (int i = 0; i < uriList.size(); i++) {
						String filepath = DlRDF.download(uriList.get(i));
						InputStream input = new FileInputStream(filepath);
						try {
							rdfModel.addAll(Rio.parse(input, "", RDFFormat.NTRIPLES));
						} catch (RDFParseException | UnsupportedRDFormatException | IOException e) {
							e.printStackTrace();
						}
						// construct concepts from uri
						ArrayList<String> broaderHeadingString = new ArrayList<String>();
						try {
							broaderHeadingString = buildConceptString(new URI(uriList.get(i)));
							LCSubjectHeading broaderHeading = addConceptString(broaderHeadingString,
									new URI(uriList.get(i)), null, 0, depth);
							if (broaderHeading != null)
								broaderHeadings.add(broaderHeading);

						} catch (URISyntaxException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}

		return broaderHeadings;
	}

	private static ArrayList<LCSubjectHeading> getNarrowerHeadings(LCSubjectHeading heading, int depth)
			throws FileNotFoundException {
		ArrayList<LCSubjectHeading> narrowerHeadings = new ArrayList<LCSubjectHeading>();
		if (heading.getURI() != null) {
			String b = null;
			ValueFactory vf = SimpleValueFactory.getInstance();
			IRI iri = vf.createIRI(heading.getURI().toString());
			IRI auth = vf.createIRI("http://www.loc.gov/mads/rdf/v1#hasNarrowerAuthority");
			b = rdfModel.filter(iri, auth, null).toString();
			if (!b.contentEquals("[]")) {
				ArrayList<String> uriList = new ArrayList<String>();

				// extract all the heading uris from the string ..
				Pattern p = Pattern.compile(Pattern.quote("hasNarrowerAuthority, ") + "(.*?)" + Pattern.quote(")"));
				Matcher m = p.matcher(b);
				while (m.find()) {
					if (!m.group(1).equals("[]")) {
						uriList.add(m.group(1));
					}
				}

				// first see if the narrower headings already exist in lcshList!
				for (int i = 0; i < lcshList.size(); i++) {
					for (int j = 0; j < uriList.size(); j++) {
						if (lcshList.get(i).getURI().toString().equals(uriList.get(j))) {
							uriList.remove(j);
							narrowerHeadings.add(lcshList.get(i));
						}
					}
					if (uriList.isEmpty())
						return narrowerHeadings;
				}

				if (depth <= maxDepth) {
					for (int i = 0; i < uriList.size(); i++) {
						String filepath = DlRDF.download(uriList.get(i));
						InputStream input = new FileInputStream(filepath);
						try {
							rdfModel.addAll(Rio.parse(input, "", RDFFormat.NTRIPLES));
						} catch (RDFParseException | UnsupportedRDFormatException | IOException e) {
							e.printStackTrace();
						}
						// construct concepts from uri
						ArrayList<String> narrowerHeadingString = new ArrayList<String>();
						try {
							narrowerHeadingString = buildConceptString(new URI(uriList.get(i)));
							LCSubjectHeading narrowerHeading = addConceptString(narrowerHeadingString,
									new URI(uriList.get(i)), null, 0, depth);
							if (narrowerHeading != null)
								narrowerHeadings.add(narrowerHeading);

						} catch (URISyntaxException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}
		return narrowerHeadings;
	}

	/**
	 * Method name: getRootURI
	 * 
	 * Description: This method takes a name of a concept that is the root of an
	 * lcsh string (i.e. an RDF "complex topic") and returns the URI for the root
	 * concept in the string. If the url provided is not for a complex topic, the
	 * method returns null.
	 */
	private static URI getRootURI(String rootConcept) {
		URI uri = null;
		ValueFactory vf = SimpleValueFactory.getInstance();
		IRI auth = vf.createIRI("http://www.loc.gov/mads/rdf/v1#authoritativeLabel");
		Value topic = vf.createLiteral(rootConcept, "en");

		try {
			// I am embarrassed by how messy this is, but it works ...
			uri = new URI("http://id.loc.gov/authorities/subjects/" + StringUtils.substringBetween(
					rdfModel.filter(null, auth, topic).toString(), "http://id.loc.gov/authorities/subjects/", ","));
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		DlRDF.download(uri.toString());
		return uri;
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

		// gets the subject heading part of the RDF statement
		authorityLabel = StringUtils.substringBetween(rdfModel.filter(iri, auth, null).toString(), "Label, \"", "\"@");

		if (uri.toString().contains("subjects")) {
			while (authorityLabel.contains("--")) {
				conceptString.add(authorityLabel);
				authorityLabel = StringUtils.substringBeforeLast(authorityLabel, "--");
			}
			conceptString.add(authorityLabel);
			Collections.reverse(conceptString);

		} else {
			conceptString.add("authorityLabel");
		}
		return conceptString;
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
		/*
		 * System.out.println("printing heading triples..."); for (Statement statement :
		 * headingTriples) { System.out.println(statement); }
		 */
		return headingTriples;
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