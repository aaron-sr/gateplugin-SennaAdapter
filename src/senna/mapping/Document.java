package senna.mapping;

import java.util.List;

public class Document extends SimpleMapping {
	private static final long serialVersionUID = 1L;

	protected String documentText;
	protected List<Sentence> sentences;

	protected String sennaText;

	public Document(String documentText, List<Sentence> sentences) {
		this(documentText);
		setSentences(sentences);
	}

	protected Document(String documentText) {
		super(null);
		this.sennaDocument = this;

		this.documentText = documentText;
		this.documentStart = 0;
		this.documentEnd = documentText.length();
	}

	protected void setSentences(List<Sentence> sentences) {
		this.sentences = DocumentBuilder.sort(sentences);

		for (Sentence sentence : this.sentences) {
			sentence.sennaDocument = this;
			sentence.tokens = DocumentBuilder.sort(sentence.tokens);
			for (Token token : sentence.tokens) {
				token.sennaDocument = this;
			}
		}

		DocumentBuilder.calculateSennaTextAndOffsets(this);
	}

	public List<Sentence> getSentences() {
		return sentences;
	}

	@Override
	public String getDocumentText() {
		return documentText;
	}

	@Override
	public String getSennaText() {
		return sennaText;
	}

	protected void setSennaText(String sennaText) {
		this.sennaText = sennaText;
		this.sennaStart = 0;
		this.sennaEnd = sennaText.length();
	}

}
