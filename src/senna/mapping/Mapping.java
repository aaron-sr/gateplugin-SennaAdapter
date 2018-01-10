package senna.mapping;

public interface Mapping {

	<C> C getDocumentId();

	Integer getDocumentStart();

	Integer getDocumentEnd();

	String getDocumentText();

	Document getSennaDocument();

	Integer getSennaStart();

	Integer getSennaEnd();

	String getSennaText();

}
