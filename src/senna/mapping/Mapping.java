package senna.mapping;

public interface Mapping {

	Integer getDocumentAnnotationId();

	Integer getDocumentStart();

	Integer getDocumentEnd();

	String getDocumentText();

	Document getSennaDocument();

	Integer getSennaStart();

	Integer getSennaEnd();

	String getSennaText();

}
