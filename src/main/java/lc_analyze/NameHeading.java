package lc_analyze;

import java.util.ArrayList;

public class NameHeading {
		public String recordName;
		public int OCLC_number;
		public String mainHeading;
		public ArrayList<String> subheadings = new ArrayList<String>();
		public String uri;

		public NameHeading(String recordName, String mainHeading, String uri) {
			this.recordName = recordName;
			this.mainHeading = mainHeading;
			this.uri = uri;
		}
		public NameHeading(String recordName, String mainHeading, String uri, int OCLC_number) {
			this.recordName = recordName;
			this.mainHeading = mainHeading;
			this.uri = uri;
			this.OCLC_number = OCLC_number;
		}
		public NameHeading(String recordName, String mainHeading, ArrayList<String> subheadings, String uri) {
			this.recordName = recordName;
			this.mainHeading = mainHeading;
			this.uri = uri;
			this.subheadings = subheadings;
		}
		public String getRecordName() {
			return this.recordName;
		}
		public String getMainHeading() {
			return this.mainHeading;
		}
		public String getString() {
			return this.uri;
		}
		public int getOCLC() {
			return this.OCLC_number;
		}
	}

