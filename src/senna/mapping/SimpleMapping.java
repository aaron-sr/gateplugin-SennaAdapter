package senna.mapping;

public abstract class SimpleMapping implements Mapping {

	protected Object documentId;
	protected Integer documentStart;
	protected Integer documentEnd;

	protected Document sennaDocument;
	protected Integer sennaStart;
	protected Integer sennaEnd;

	protected SimpleMapping(Document document) {
		this.sennaDocument = document;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <C> C getDocumentId() {
		return (C) documentId;
	}

	@Override
	public Integer getDocumentStart() {
		return documentStart;
	}

	@Override
	public Integer getDocumentEnd() {
		return documentEnd;
	}

	@Override
	public String getDocumentText() {
		return sennaDocument.documentText.substring(documentStart, documentEnd);
	}

	@Override
	public Document getSennaDocument() {
		return sennaDocument;
	}

	@Override
	public Integer getSennaStart() {
		return sennaStart;
	}

	@Override
	public Integer getSennaEnd() {
		return sennaEnd;
	}

	@Override
	public String getSennaText() {
		return sennaDocument.sennaText.substring(sennaStart, sennaEnd);
	}

}
