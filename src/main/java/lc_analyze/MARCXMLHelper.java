package lc_analyze;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.lang.StringUtils;
import org.marc4j.marc.DataField;
import org.marc4j.marc.Record;

public class MARCXMLHelper {
	/**
	 * Method name: extractURIs
	 * 
	 * Description: This method takes a marc4j record and returns an ArrayList
	 * containing all of the uris for name/subject headings, excluding duplicates.
	 */
	public static ArrayList<URI> extractURIs(Record record) {
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
	public static String extractOCLC(Record record) {
		String oclc = "";
		oclc = record.getVariableField("035").toString();
		// remove whitespaces (just in case)
		oclc.replaceAll("\\s+", "");
		oclc = StringUtils.substringAfter(oclc, "(OCoLC)");
		// System.out.println(oclc);
		return oclc;
	}

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

	public static String extractTitle(Record record) {
		String title = "";
		if (record.getVariableField("245") != null) {
			title = record.getVariableField("245").toString();
			// remove subheading tags
			title = StringUtils.substringAfter(title, "$a");
			if (title.contains("$c")) {
				title = StringUtils.substringBefore(title, "$c");
			}
			title = title.replaceAll("\\$.", "");
			System.out.print(title);
		} else {
			title = "[Not given]";
		}
		return title;
	}

	public static String extractAuthor(Record record) {
		String author = "";
		System.out.println(record.toString());
		if (record.getVariableField("100") != null) {
			author = record.getVariableField("100").toString();
			// remove subheading tags
			author = StringUtils.substringAfter(author, "$a");
			author = StringUtils.substringBefore(author, "$2");
			author = author.replaceAll("\\$.", "");
			System.out.print(author);
		} else {
			author = "[Not given]";
		}
		return author;
	}

	public static String extractPlaceOfPublication(Record record) {

		String placeOfPublication = "";
		System.out.println(record.toString());
		if (record.getVariableField("264") != null) {
			placeOfPublication = record.getVariableField("264").toString();
			// remove subheading tags
			placeOfPublication = StringUtils.substringBetween(placeOfPublication, "$a", "$b");
			System.out.print(placeOfPublication);
		} else if (record.getVariableField("260") != null) {
			placeOfPublication = record.getVariableField("260").toString();
			// remove subheading tags
			placeOfPublication = StringUtils.substringBetween(placeOfPublication, "$a", "$b");
			System.out.print(placeOfPublication);
		} else {
			placeOfPublication = "[Not given]";
		}
		return placeOfPublication;
	}

	public static String extractDescription(Record record) {
		String description = "";
		System.out.println(record.toString());
		if (record.getVariableField("520") != null) {
			description = record.getVariableField("520").toString();
			// remove subheading tags
			description = StringUtils.substringAfter(description, "$a");
			System.out.println(description);
		} else if (record.getVariableField("500") != null) {
			description = record.getVariableField("500").toString();
			// remove subheading tags
			description = StringUtils.substringAfter(description, "$a");
			System.out.println(description);
		} else {
			description = "[Not given]";
		}
		return description;
	}

	public static String extractContentType(Record record) {
		String contentType = "";
		if (record.getVariableField("336") != null) {
			contentType = record.getVariableField("336").toString();
			contentType = StringUtils.substringBetween(contentType, "$a", "$b");
		}
		return contentType;
	}

	public static String extractMediaType(Record record) {
		String mediaType = "";
		if (record.getVariableField("337") != null) {
			mediaType = record.getVariableField("337").toString();
			mediaType = StringUtils.substringBetween(mediaType, "$a", "$b");
		}
		return mediaType;
	}

	public static String extractDateOfPublication(Record record) {
		String dateOfPublication = "";
		System.out.println(record.toString());
		if (record.getVariableField("264") != null) {
			dateOfPublication = record.getVariableField("264").toString();
			// remove subheading tags
			dateOfPublication = StringUtils.substringAfter(dateOfPublication, "$c");
			System.out.println(dateOfPublication);
		} else if (record.getVariableField("260") != null) {
			dateOfPublication = record.getVariableField("260").toString();
			// remove subheading tags
			dateOfPublication = StringUtils.substringAfter(dateOfPublication, "$c");
			System.out.println(dateOfPublication);
		} else {
			dateOfPublication = "[Not given]";
		}
		return dateOfPublication;
	}

	public static String extractPublisher(Record record) {
		String publisher = "";
		System.out.println(record.toString());
		if (record.getVariableField("264") != null) {
			publisher = record.getVariableField("264").toString();
			// remove subheading tags
			publisher = StringUtils.substringBetween(publisher, "$b", "$c");
			System.out.println(publisher);
		} else if (record.getVariableField("260") != null) {
			publisher = record.getVariableField("260").toString();
			// remove subheading tags
			publisher = StringUtils.substringBetween(publisher, "$b", "$c");
			System.out.println(publisher);
		} else {
			publisher = "[Not given]";
		}
		return publisher;
	}

	public static String extractCarrierType(Record record) {
		String carrierType = "";
		if (record.getVariableField("338") != null) {
			carrierType = record.getVariableField("338").toString();
			carrierType = StringUtils.substringBetween(carrierType, "$a", "$b");
		}
		return carrierType;
	}

}
