package senna.mapping;

public abstract class SimpleMapping implements Mapping {

	protected Integer documentAnnotationId;
	protected Integer documentStart;
	protected Integer documentEnd;

	protected Document document;
	protected Integer sennaStart;
	protected Integer sennaEnd;

	protected SimpleMapping(Document document) {
		this.document = document;
	}

	@Override
	public Integer getDocumentAnnotationId() {
		return documentAnnotationId;
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
		return document.documentText.substring(documentStart, documentEnd);
	}

	@Override
	public Document getSennaDocument() {
		return document;
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
		return document.sennaText.substring(sennaStart, sennaEnd);
	}

}
