package senna.mapping;

import senna.Option;

public class SrlArgumentToken extends MultiToken {
	private static final long serialVersionUID = 1L;

	protected SrlVerbToken verbToken;

	protected SrlArgumentToken(Sentence sentence, String type, Token startToken, Token endToken) {
		super(sentence, Option.SRL, type, startToken, endToken);
	}

	@Override
	public String getType() {
		return type;
	}

	public SrlVerbToken getVerb() {
		return verbToken;
	}

}