package lc_analyze;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
import org.marc4j.MarcReader;
import org.marc4j.MarcXmlReader;
import org.marc4j.marc.Record;
import org.marc4j.marc.VariableField;

/**
 * This program is used to download the LC authority name/subject headings for a
 * set of URIs in NTRIPLES format and store them in the RDF_files folder. This
 * is useful for testing purposes, or if the full dataset (which for name
 * authorities is ~10GB) cannot be practically be loaded.
 **/
public class DlRDF {

	static ArrayList<String> authorityURIs = new ArrayList<String>();

	public static void main(String[] args) {
		File f = new File("./src/main/java/MARC_files/");
		File[] filesList = f.listFiles();
		for (File file : filesList) {
			if (file.isFile() && !file.getName().startsWith("._") && file.getName().endsWith(".xml")) {
				System.out.println(file.getName());
				try {
					extractAuthorityURIs(file);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		try {
			dlRDF(0);
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

	public static void dlRDF(int startIndex) throws IOException, URISyntaxException {
		for (int i = startIndex; i < authorityURIs.size(); i++) {
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

	public static String download(String uri) {
		
		String filepath = "./src/main/java/RDF_files/" + uri.substring(uri.lastIndexOf("/") + 1) + ".nt";
		String filename = uri.substring(uri.lastIndexOf("/") + 1) + ".nt";
		boolean check = new File("./src/main/java/RDF_files/", filename).exists();
		System.out.println(filename);
		// we use check to verify if the RDF file already exists (to avoid
		// re-downloading existing files)
		if (!check) {

			// dl file ...
			
			File rdfFile = new File(filepath);
			CloseableHttpClient client = HttpClientBuilder.create().build();
			HttpUriRequest request = RequestBuilder.get().setUri(uri).setHeader("accept", "text/plain").build();
			HttpResponse response = null;
			try {
				response = client.execute(request);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				try (FileOutputStream outstream = new FileOutputStream(rdfFile)) {
					entity.writeTo(outstream);
					System.out.println("COMPLETE");
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return filepath;
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

	public static int getBroader() {
		int startIndex = authorityURIs.size();

		return startIndex;
	}
}
