package senna.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import senna.Option;

public class Sentence extends SimpleMapping {

	protected List<Token> tokens = new ArrayList<Token>();
	protected Map<Option<? extends MultiToken>, List<? extends MultiToken>> multiTokens = new HashMap<Option<? extends MultiToken>, List<? extends MultiToken>>();

	protected Sentence(Integer documentStart, Integer documentEnd) {
		this(null, documentStart, documentEnd);
	}

	public Sentence(Object documentId, Integer documentStart, Integer documentEnd) {
		super(null);
		this.documentId = documentId;
		this.documentStart = documentStart;
		this.documentEnd = documentEnd;
	}

	public List<Token> getTokens() {
		return tokens;
	}

	@SuppressWarnings("unchecked")
	public <T extends MultiToken> List<T> geMultiTokens(Option<T> option) {
		return (List<T>) multiTokens.get(option);
	}

	protected void addToken(Token token) {
		tokens.add(token);
		token.sentence = this;
	}

}