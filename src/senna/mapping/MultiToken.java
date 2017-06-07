package senna.mapping;

import senna.Option;

public class MultiToken implements Mapping {

	protected Sentence sentence;
	protected Option<? extends MultiToken> option;
	protected String type;
	protected Token startToken;
	protected Token endToken;

	protected MultiToken(Sentence sentence, Option<? extends MultiToken> option, String type, Token startToken) {
		this(sentence, option, type, startToken, null);
	}

	protected MultiToken(Sentence sentence, Option<? extends MultiToken> option, String type, Token startToken,
			Token endToken) {
		this.sentence = sentence;
		this.option = option;
		this.type = type;
		this.startToken = startToken;
		this.endToken = endToken;
	}

	public Option<? extends MultiToken> getOption() {
		return option;
	}

	public String getType() {
		return type;
	}

	@Override
	public Integer getDocumentAnnotationId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Integer getDocumentStart() {
		return startToken.getDocumentStart();
	}

	@Override
	public Integer getDocumentEnd() {
		return endToken.getDocumentEnd();
	}

	@Override
	public String getDocumentText() {
		return sentence.document.documentText.substring(startToken.documentStart, endToken.documentEnd);
	}

	@Override
	public Document getSennaDocument() {
		return sentence.document;
	}

	@Override
	public Integer getSennaStart() {
		return startToken.getSennaStart();
	}

	@Override
	public Integer getSennaEnd() {
		return endToken.getSennaEnd();
	}

	@Override
	public String getSennaText() {
		return sentence.document.sennaText.substring(startToken.sennaStart, endToken.sennaEnd);
	}

}
