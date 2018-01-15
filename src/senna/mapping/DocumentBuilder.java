package senna.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class DocumentBuilder {

	public static final Integer SENNA_MAX_SENTENCE_SIZE = 1024;
	public static final String SENNA_SENTENCESPLIT = "\n";
	public static final String SENNA_TOKENSPLIT = " ";

	private static Comparator<SimpleMapping> MAPPING_COMPARATOR = new Comparator<SimpleMapping>() {
		@Override
		public int compare(SimpleMapping o1, SimpleMapping o2) {
			if (o1.getDocumentEnd() < o2.getDocumentStart()) {
				return -1;
			} else if (o1.getDocumentStart() > o2.getDocumentEnd()) {
				return 1;
			}
			return 0;
		}
	};

	protected static String calculateSennaOffsets(List<Sentence> sortedSentences, boolean userTokens) {
		StringBuilder sennaText = new StringBuilder();
		Integer sentenceSennaOffset = 0;
		Sentence previousSentence = null;
		Iterator<Sentence> sentenceIterator = sortedSentences.iterator();
		while (sentenceIterator.hasNext()) {
			Sentence sentence = sentenceIterator.next();
			StringBuilder sentenceSennaText = new StringBuilder();
			if (userTokens) {
				Integer tokenSennaOffset = sentenceSennaOffset;
				Token previousToken = null;
				Iterator<Token> tokenIterator = sentence.tokens.iterator();
				while (tokenIterator.hasNext()) {
					Token token = tokenIterator.next();
					String tokenSennaText = token.getDocumentText().replaceAll(SENNA_TOKENSPLIT, "")
							.replaceAll(SENNA_SENTENCESPLIT, "");
					if (previousToken != null) {
						sentenceSennaText.append(SENNA_TOKENSPLIT);
					}
					sentenceSennaText.append(tokenSennaText);

					token.sennaStart = tokenSennaOffset;
					token.sennaEnd = token.sennaStart + tokenSennaText.length();
					tokenSennaOffset = token.sennaEnd + SENNA_TOKENSPLIT.length();

					previousToken = token;
				}
			} else {
				sentenceSennaText.append(sentence.getDocumentText().replaceAll(SENNA_SENTENCESPLIT, ""));
			}
			if (previousSentence != null) {
				sennaText.append(SENNA_SENTENCESPLIT);
			}
			if (sentenceSennaText.length() > SENNA_MAX_SENTENCE_SIZE) {
				throw new IllegalStateException(
						"sentence to long, max size is " + SENNA_MAX_SENTENCE_SIZE + "\n" + sentenceSennaText);
			}
			sennaText.append(sentenceSennaText.toString());

			sentence.sennaStart = sentenceSennaOffset;
			sentence.sennaEnd = sentence.sennaStart + sentenceSennaText.length();
			sentenceSennaOffset = sentence.sennaEnd + SENNA_SENTENCESPLIT.length();
			previousSentence = sentence;
		}
		return sennaText.toString();
	}

	protected static <M extends SimpleMapping> List<M> sort(Collection<M> mappings) {
		List<M> sortedMappings = new ArrayList<>(mappings);
		Collections.sort(sortedMappings, MAPPING_COMPARATOR);
		return sortedMappings;
	}

}
