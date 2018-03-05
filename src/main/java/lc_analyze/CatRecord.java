package lc_analyze;

import java.io.Serializable;

public class CatRecord implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String title;
	private String author;
	private String placeOfPublication;
	private String publisher;
	private String publicationDate;
	private String oclc;
	private String description;
	private String contentType;
	private String mediaType;
	private String carrierType;

	public CatRecord(String title, String author, String placeOfPublication, String publisher, String publicationDate, String oclc,
			String description, String contentType, String mediaType, String carrierType) {
		this.setTitle(title);
		this.setAuthor(author);
		this.setPlaceOfPublication(placeOfPublication);
		this.setPublicationDate(publicationDate);
		this.setOclc(oclc);
		this.setDescription(description);
		this.setContentType(contentType);
		this.setMediaType(mediaType);
		this.setCarrierType(carrierType);
	}
	public CatRecord(String title, String author, String placeOfPublication, String publicationDate, String oclc,
			String description) {
		this.setTitle(title);
		this.setAuthor(author);
		this.setPlaceOfPublication(placeOfPublication);
		this.setPublicationDate(publicationDate);
		this.setOclc(oclc);
		this.setDescription(description);
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}
	public String getPlaceOfPublication() {
		return placeOfPublication;
	}
	public void setPlaceOfPublication(String placeOfPublication) {
		this.placeOfPublication = placeOfPublication;
	}
	public String getPublicationDate() {
		return publicationDate;
	}
	public void setPublicationDate(String publicationDate) {
		this.publicationDate = publicationDate;
	}
	public String getOclc() {
		return oclc;
	}
	public void setOclc(String oclc) {
		this.oclc = oclc;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getContentType() {
		return contentType;
	}
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	public String getMediaType() {
		return mediaType;
	}
	public void setMediaType(String mediaType) {
		this.mediaType = mediaType;
	}
	public String getPublisher() {
		return publisher;
	}
	public void setPublisher(String publisher) {
		this.publisher = publisher;
	}
	public String getCarrierType() {
		return carrierType;
	}
	public void setCarrierType(String carrierType) {
		this.carrierType = carrierType;
	}
	public void print() {
		System.out.println("Title: " + this.getTitle());
		System.out.println("Author: " + this.getAuthor());
		System.out.println("Place of Publication: " + this.getPlaceOfPublication());
		System.out.println("Publication date: " + this.getPublicationDate());
		System.out.println("OCLC number: " + this.getOclc());
		System.out.println("Description: " + this.getDescription());
		System.out.println("Content type: " + this.getContentType());
		System.out.println("Media type: " + this.getMediaType());
		System.out.println("Carrier type: " + this.getCarrierType());
	}
}
