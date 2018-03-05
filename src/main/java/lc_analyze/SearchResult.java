package lc_analyze;

import java.io.Serializable;

public class SearchResult implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int rank;
	private LCSubjectHeading subjectHeading;
	private String note;

	public SearchResult(LCSubjectHeading subjectHeading, int rank, String note) {
		this.setRank(rank);
		this.setSubjectHeading(subjectHeading);
		this.setNote(note);
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public LCSubjectHeading getSubjectHeading() {
		return subjectHeading;
	}

	public void setSubjectHeading(LCSubjectHeading subjectHeading) {
		this.subjectHeading = subjectHeading;
	}

	public int getRank() {
		return rank;
	}

	public void setRank(int rank) {
		this.rank = rank;
	}
}
