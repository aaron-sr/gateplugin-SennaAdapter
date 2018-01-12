package senna.mapping;

import java.util.List;
import java.util.Map;

public class SubDocument extends Document {

	protected Document original;
	protected Map<Sentence, Sentence> sentenceMapping;
	protected Map<Token, Token> tokenMapping;

	protected SubDocument(Integer documentOffet, String documentText, List<Sentence> sentences, Document original,
			Map<Sentence, Sentence> sentenceMapping, Map<Token, Token> tokenMapping) {
		super(documentOffet.longValue(), documentText, sentences, original.userTokens);
		this.original = original;
		this.sentenceMapping = sentenceMapping;
		this.tokenMapping = tokenMapping;
	}

	public void mergeToOriginal() {
		for (Sentence sentence : sentences) {
			Sentence originalSentence = sentenceMapping.get(sentence);
			for (Token token : sentence.tokens) {
				if (userTokens) {
					Token originalToken = tokenMapping.get(token);
					originalToken.features = token.features;
					originalToken.srlValues = token.srlValues;
				} else {
					Token originalToken = new Token(token.documentId, (int) (documentOffet + token.documentStart),
							(int) (documentOffet + token.documentEnd));
					originalSentence.addToken(originalToken);
					originalToken.features = token.features;
					originalToken.srlValues = token.srlValues;
				}
			}
		}
	}

}
