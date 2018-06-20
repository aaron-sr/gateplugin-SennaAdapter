package senna.mapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SubDocument extends Document {
	private static final long serialVersionUID = 1L;

	protected Document original;
	protected Integer documentOffet;
	protected Integer sennaOffet;
	protected Map<Sentence, Sentence> sentenceMapping;
	protected Map<Token, Token> tokenMapping;

	public SubDocument(Document document, Sentence fromSentence, Sentence toSentence) {
		super(document.documentText.substring(fromSentence.documentStart, toSentence.documentEnd));
		this.original = document;
		this.documentOffet = fromSentence.documentStart;
		this.sennaOffet = fromSentence.sennaStart;

		List<Sentence> subSentences = new ArrayList<>();
		this.sentenceMapping = new LinkedHashMap<>();
		this.tokenMapping = new LinkedHashMap<>();
		for (Sentence sentence : document.sentences) {
			if (sentence.documentStart >= fromSentence.documentStart
					&& sentence.documentEnd <= toSentence.documentEnd) {
				Sentence subSentence = new Sentence(sentence.documentId,
						sentence.documentStart - fromSentence.documentStart,
						sentence.documentEnd - fromSentence.documentStart);
				subSentence.userTokens = sentence.userTokens;
				subSentences.add(subSentence);
				sentenceMapping.put(subSentence, sentence);
				if (sentence.userTokens) {
					for (Token token : sentence.tokens) {
						Token subToken = new Token(token.documentId, token.documentStart - fromSentence.documentStart,
								token.documentEnd - fromSentence.documentStart);
						subSentence.addToken(subToken);
						tokenMapping.put(subToken, token);
					}
				}
			}
		}

		setSentences(subSentences);
	}

	public void mergeToOriginal() {
		for (Sentence sentence : sentences) {
			Sentence originalSentence = sentenceMapping.get(sentence);
			for (Token token : sentence.tokens) {
				if (sentence.userTokens) {
					Token originalToken = tokenMapping.get(token);
					originalToken.features = token.features;
					originalToken.srlValues = token.srlValues;
				} else {
					Token originalToken = new Token(originalSentence, sennaOffet + token.sennaStart,
							sennaOffet + token.sennaEnd, documentOffet + token.sennaStart,
							documentOffet + token.sennaEnd);
					originalSentence.addToken(originalToken);
					originalToken.features = token.features;
					originalToken.srlValues = token.srlValues;
				}
			}
		}
	}

}
