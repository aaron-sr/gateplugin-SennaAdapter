package senna.mapping;

import java.util.Collections;
import java.util.List;

public class Document extends SimpleMapping {
	private static final long serialVersionUID = 1L;

	protected String documentText;
	protected List<Sentence> sentences;

	protected boolean userTokens;
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
		this.sentences = Collections.unmodifiableList(DocumentBuilder.sort(sentences));
		this.userTokens = true;

		for (Sentence sentence : this.sentences) {
			sentence.sennaDocument = this;
			if (sentence.tokens.isEmpty()) {
				userTokens = false;
			} else {
				sentence.tokens = Collections.unmodifiableList(DocumentBuilder.sort(sentence.tokens));
				for (Token token : sentence.tokens) {
					token.sennaDocument = this;
				}
			}
		}

		this.sennaText = DocumentBuilder.calculateSennaOffsets(this.sentences, this.userTokens);
	}

	public boolean isUserTokens() {
		return userTokens;
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
