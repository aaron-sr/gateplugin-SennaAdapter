package senna.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DocumentBuilder {

	protected static final String SENNA_SENTENCESPLIT = "\n";
	protected static final String SENNA_TOKENSPLIT = " ";

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

	public static Document buildFrom(Long documentOffset, String documentText, Collection<Sentence> sentences,
			boolean userTokens, Collection<Token> tokens) {

		checkOffsets(documentText.length(), sentences);
		List<Sentence> sortedSentences = buildSortedSentences(documentText, sentences);

		if (userTokens) {
			checkOffsets(documentText.length(), tokens);
			List<Token> sortedTokens = buildSortedTokens(tokens);
			addTokenToSentences(sortedSentences, sortedTokens);
		}

		Document document = new Document(documentOffset, documentText, sortedSentences, userTokens);
		String sennaText = calculateSennaOffsets(sortedSentences, userTokens);
		document.setSennaText(sennaText);

		return document;
	}

	public static SubDocument buildSubDocument(Document document, Sentence fromSentence, Sentence toSentence) {
		List<Sentence> subSentences = new ArrayList<>();
		Map<Sentence, Sentence> sentenceMapping = new LinkedHashMap<>();
		Map<Token, Token> tokenMapping = new LinkedHashMap<>();
		for (Sentence sentence : document.sentences) {
			if (sentence.documentStart >= fromSentence.documentStart
					&& sentence.documentEnd <= toSentence.documentEnd) {
				Sentence subSentence = new Sentence(sentence.documentId,
						sentence.documentStart - fromSentence.documentStart,
						sentence.documentEnd - fromSentence.documentStart);
				subSentences.add(subSentence);
				sentenceMapping.put(subSentence, sentence);
				if (document.userTokens) {
					for (Token token : sentence.tokens) {
						Token subToken = new Token(token.documentId, token.documentStart - fromSentence.documentStart,
								token.documentEnd - fromSentence.documentStart);
						subSentence.tokens.add(subToken);
						tokenMapping.put(subToken, token);
					}
				}
			}
		}
		SubDocument subDocument = new SubDocument(fromSentence.documentStart,
				document.documentText.substring(fromSentence.documentStart, toSentence.documentEnd), subSentences,
				document, sentenceMapping, tokenMapping);
		String sennaText = calculateSennaOffsets(subSentences, document.userTokens);
		subDocument.setSennaText(sennaText);

		return subDocument;
	}

	private static void checkOffsets(Integer documentLength, Collection<? extends Mapping> mappings) {
		for (Mapping mapping : mappings) {
			if (mapping.getDocumentStart() < 0 || mapping.getDocumentStart() > documentLength
					|| mapping.getDocumentEnd() < 0 || mapping.getDocumentEnd() > documentLength) {
				throw new IllegalStateException(String.format(
						"%s mapping incorrect start: %d end: %d documentLength: %d", mapping.getClass().getSimpleName(),
						mapping.getDocumentStart(), mapping.getDocumentEnd(), documentLength));
			}
		}
	}

	private static String calculateSennaOffsets(List<Sentence> sortedSentences, boolean userTokens) {
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
			sennaText.append(sentenceSennaText.toString());

			sentence.sennaStart = sentenceSennaOffset;
			sentence.sennaEnd = sentence.sennaStart + sentenceSennaText.length();
			sentenceSennaOffset = sentence.sennaEnd + SENNA_SENTENCESPLIT.length();
			previousSentence = sentence;
		}
		return sennaText.toString();
	}

	private static List<Token> buildSortedTokens(Collection<Token> tokens) {
		List<Token> sortedTokens = new ArrayList<>(tokens);
		Collections.sort(sortedTokens, MAPPING_COMPARATOR);
		return sortedTokens;
	}

	private static void addTokenToSentences(List<Sentence> sortedSentences, List<Token> sortedTokens) {
		for (Token token : sortedTokens) {
			Sentence sentence = findSentence(token, sortedSentences);
			sentence.addToken(token);
		}
	}

	private static List<Sentence> buildSortedSentences(String documentText, Collection<Sentence> sentences) {
		List<Sentence> sortedSentences = new ArrayList<>();
		if (sentences != null && !sentences.isEmpty()) {
			sortedSentences.addAll(sentences);
			Collections.sort(sortedSentences, MAPPING_COMPARATOR);
		} else {
			sortedSentences.add(new Sentence(0, documentText.length()));
		}
		return sortedSentences;
	}

	private static Sentence findSentence(Token token, List<Sentence> sortedSentences) {
		for (Sentence sentence : sortedSentences) {
			if (sentence.documentStart <= token.documentStart && sentence.documentEnd >= token.documentEnd) {
				return sentence;
			}
		}
		throw new IllegalStateException(
				String.format("token %d %d is not inside a sentence.", token.documentStart, token.documentEnd));
	}

}
