package senna.mapping;

import java.util.List;

public class Document extends SimpleMapping {

	protected Long documentOffet;
	protected boolean userTokens;
	protected List<Sentence> sentences;
	protected String documentText;
	protected String sennaText;

	protected Document(Long documentOffet, String documentText, List<Sentence> sentences, boolean userTokens) {
		super(null);
		this.document = this;
		this.documentOffet = documentOffet;

		this.documentText = documentText;
		this.documentStart = 0;
		this.documentEnd = documentText.length();

		this.sentences = sentences;

		for (Sentence sentence : sentences) {
			sentence.document = this;
			for (Token token : sentence.tokens) {
				token.document = this;
			}
		}

		this.userTokens = userTokens;
	}

	public Long getDocumentOffet() {
		return documentOffet;
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
