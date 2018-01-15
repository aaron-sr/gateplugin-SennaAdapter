package senna.mapping;

import java.io.Serializable;

public interface Mapping extends Serializable {

	Integer getDocumentStart();

	Integer getDocumentEnd();

	String getDocumentText();

	Document getSennaDocument();

	Integer getSennaStart();

	Integer getSennaEnd();

	String getSennaText();

}
