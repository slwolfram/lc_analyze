package lc_analyze;

import java.util.ArrayList;

public class SubjectHeading {
	public String recordName;
	public int OCLC_number;
	public String mainHeading;
	public ArrayList<String> subheadings = new ArrayList<String>();
	public String uri;

	public SubjectHeading(String recordName, String mainHeading, ArrayList<String> subheadings, String uri) {
		this.recordName = recordName;
		this.mainHeading = mainHeading;
		this.subheadings = subheadings;
		this.uri = uri;
	}
	public SubjectHeading(String recordName, String mainHeading, ArrayList<String> subheadings, String uri, int OCLC_number) {
		this.recordName = recordName;
		this.mainHeading = mainHeading;
		this.subheadings = subheadings;
		this.uri = uri;
		this.OCLC_number = OCLC_number;
	}
	public String getRecordName() {
		return this.recordName;
	}
	public String getMainHeading() {
		return this.mainHeading;
	}
	public ArrayList<String> getSubdivision() {
		return this.subheadings;
	}
	public String getString() {
		return this.uri;
	}
	public int getOCLC() {
		return this.OCLC_number;
	}
	public String getURI() {
		return this.uri;
	}
}
