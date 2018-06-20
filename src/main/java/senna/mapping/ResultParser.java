package senna.mapping;

import static senna.Option.CHK;
import static senna.Option.NER;
import static senna.Option.POS;
import static senna.Option.PSG;
import static senna.Option.SRL;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import senna.Option;
import senna.Util;

public class ResultParser {

	private static final String SENTENCE_SPLIT_REGEX = "\r?\n\r?\n";
	private static final String LINE_SPLIT_REGEX = "\r?\n";
	private static final Pattern COLUMN_SPLIT_PATTERN = Pattern.compile("\\s*(\\S+)(\\s+.*|)");

	@SuppressWarnings("unused")
	private static final int TOKEN_COLUMN = 0;
	private static final int START_COLUMN = 1;
	private static final int END_COLUMN = 2;

	private static final String IOB_INSIDE_PREFIX = "I-";
	private static final String IOB_OUTSIDE = "O";
	private static final String IOB_BEGIN_PREFIX = "B-";
	private static final String IOB_END_PREFIX = "E-";
	private static final String IOB_SINGLE_PREFIX = "S-";

	private static final String SRL_NONVERB = "-";
	private static final String SRL_VERB_TYPE = "V";
	private static final Pattern BRACKETTAGS_SPLIT_PATTERN = Pattern.compile("\\*?([\\(\\)][^\\(\\)\\*]*)\\*?(.*)");

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void parse(Document document, String output, Collection<Option<? extends MultiToken>> options) {
		List sortedOptions = Util.sort((Collection) options);
		String[] split = output.split(SENTENCE_SPLIT_REGEX);
		for (int sentenceNumber = 0; sentenceNumber < split.length
				&& sentenceNumber < document.getSentences().size(); sentenceNumber++) {
			Sentence sentence = document.getSentences().get(sentenceNumber);
			String sentenceOutput = split[sentenceNumber];
			parseSentence(sentence, Arrays.asList(sentenceOutput.split(LINE_SPLIT_REGEX)), sortedOptions);
		}
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void parse(Document document, InputStream inputStream,
			Collection<Option<? extends MultiToken>> options) throws IOException {
		List sortedOptions = Util.sort((Collection) options);
		int sentenceNumber = 0;
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		String line;
		List<String> lines = new ArrayList<>();
		while ((line = reader.readLine()) != null) {
			if (line.length() > 0) {
				lines.add(line);
			} else {
				if (sentenceNumber < document.getSentences().size()) {
					Sentence sentence = document.getSentences().get(sentenceNumber++);
					parseSentence(sentence, lines, sortedOptions);
				}
				lines.clear();
			}
		}
	}

	protected static void parseSentence(Sentence sentence, List<String> lines,
			List<Option<? extends MultiToken>> options) {
		List<List<String>> sentenceData = new ArrayList<>();
		for (String line : lines) {
			sentenceData.add(Arrays.asList(line.trim().split("\\s+")));
		}
		boolean hasPsg = options.contains(Option.PSG);
		for (int tokenNumber = 0; tokenNumber < sentenceData.size(); tokenNumber++) {
			List<String> tokenData = sentenceData.get(tokenNumber);
			Map<Option<? extends MultiToken>, String> features = new LinkedHashMap<>();
			List<String> srlValues = new ArrayList<>();
			for (int columnNumber = END_COLUMN + 1, optionNumber = 0; columnNumber < tokenData.size()
					&& optionNumber < options.size(); columnNumber++, optionNumber++) {
				Option<? extends MultiToken> option = options.get(optionNumber);
				if (option.equals(Option.PSG)) {
					continue;
				}
				String columnValue = tokenData.get(columnNumber);
				features.put(option, columnValue);
				if (option.equals(Option.SRL)) {
					for (int srlNumber = columnNumber + 1; hasPsg ? srlNumber < tokenData.size() - 1
							: srlNumber < tokenData.size(); srlNumber++) {
						String srlValue = tokenData.get(srlNumber);
						srlValues.add(srlValue);
					}
				}
			}
			if (hasPsg) {
				String psg = tokenData.get(tokenData.size() - 1);
				features.put(Option.PSG, psg);
			}
			if (sentence.userTokens) {
				Token token = sentence.tokens.get(tokenNumber);
				token.features.putAll(features);
				token.srlValues = srlValues;
			} else {
				// String tokenText = tokenData.get(TOKEN_COLUMN);
				Integer start = Integer.parseInt(tokenData.get(START_COLUMN));
				Integer end = Integer.parseInt(tokenData.get(END_COLUMN));
				Token token = new Token(sentence, sentence.sennaStart + start, sentence.sennaStart + end,
						sentence.documentStart + start, sentence.documentStart + end);
				sentence.addToken(token);
				token.features.putAll(features);
				token.srlValues = srlValues;
			}

		}
	}

	public static void parseAnnotations(Document document, final Option<? extends MultiToken> option,
			boolean bracketTags) {
		if (option.equals(POS)) {
			throw new UnsupportedOperationException();
		} else if (option.equals(SRL)) {
			parseSrl(document, bracketTags);
		} else if (option.equals(CHK) || option.equals(NER)) {
			for (Sentence sentence : document.sentences) {
				List<MultiToken> multiTokens = extractMultiToken(sentence, option, bracketTags,
						new ParserHelper<MultiToken>() {
							@Override
							public String getValue(Token token) {
								return token.getFeature(option);
							}

							@Override
							public MultiToken createToken(Sentence sentence, String type, Token startToken,
									Token endToken, MultiToken parent) {
								return new MultiToken(sentence, option, type, startToken, endToken);
							}
						});
				sentence.multiTokens.put(option, multiTokens);
			}
		} else if (option.equals(PSG)) {
			for (Sentence sentence : document.sentences) {
				List<PsgToken> multiTokens = extractBracketTokens(sentence, new ParserHelper<PsgToken>() {
					@Override
					public String getValue(Token token) {
						return token.getFeature(option);
					}

					@Override
					public PsgToken createToken(Sentence sentence, String type, Token startToken, Token endToken,
							PsgToken parent) {
						return new PsgToken(sentence, type, startToken, parent);
					}
				});
				sentence.multiTokens.put(option, multiTokens);
			}
		}
	}

	private static interface ParserHelper<T extends MultiToken> {
		String getValue(Token token);

		T createToken(Sentence sentence, String type, Token startToken, Token endToken, T parent);
	}

	private static <T extends MultiToken> List<T> extractMultiToken(Sentence sentence,
			Option<? extends MultiToken> option, boolean bracketTags, ParserHelper<T> helper) {
		if (bracketTags) {
			return extractBracketTokens(sentence, helper);
		} else {
			return extractIobesToken(sentence, helper);
		}
	}

	private static <T extends MultiToken> List<T> extractIobesToken(Sentence sentence, ParserHelper<T> helper) {
		Token firstToken = null;
		Token previousToken = null;
		String previousTokenIobValue = null;
		List<T> tokens = new ArrayList<>();
		for (Token token : sentence.tokens) {
			String iobValue = helper.getValue(token);
			if (iobValue.contentEquals(IOB_OUTSIDE)) {
				if (firstToken != null && previousToken != null) {
					addIobToken(sentence, helper, firstToken, previousToken, previousTokenIobValue, tokens);
				}
				firstToken = null;
				continue;
			} else if (iobValue.startsWith(IOB_SINGLE_PREFIX)) {
				if (firstToken != null && previousToken != null) {
					addIobToken(sentence, helper, firstToken, previousToken, previousTokenIobValue, tokens);
				}
				addIobToken(sentence, helper, token, token, iobValue, tokens);
				firstToken = null;
			} else if (iobValue.startsWith(IOB_BEGIN_PREFIX)) {
				if (firstToken != null && previousToken != null) {
					addIobToken(sentence, helper, firstToken, previousToken, previousTokenIobValue, tokens);
				}
				firstToken = token;
			}
			previousToken = token;
			previousTokenIobValue = iobValue;
		}
		if (firstToken != null && previousToken != null) {
			String iobValue = previousTokenIobValue;
			if (iobValue.startsWith(IOB_BEGIN_PREFIX) || iobValue.startsWith(IOB_INSIDE_PREFIX)
					|| iobValue.startsWith(IOB_END_PREFIX)) {
				addIobToken(sentence, helper, firstToken, previousToken, iobValue, tokens);
			}
		}
		return tokens;
	}

	private static <T extends MultiToken> void addIobToken(Sentence sentence, ParserHelper<T> helper, Token startToken,
			Token endToken, String iobValue, List<T> tokens) {
		String type = iobValue.substring(2);
		T token = helper.createToken(sentence, type, startToken, endToken, null);
		tokens.add(token);
	}

	private static <T extends MultiToken> List<T> extractBracketTokens(Sentence sentence, ParserHelper<T> helper) {
		Deque<T> stack = new ArrayDeque<>();
		List<T> tokens = new ArrayList<>();
		for (Token token : sentence.tokens) {
			String value = helper.getValue(token);
			List<String> bracketTags = Util.readColumnLine(value, BRACKETTAGS_SPLIT_PATTERN);
			for (String tag : bracketTags) {
				if (tag.startsWith("(")) {
					T parent = !stack.isEmpty() ? stack.getFirst() : null;
					T psgToken = helper.createToken(sentence, tag.substring(1), token, null, parent);
					stack.addFirst(psgToken);
					if (parent == null) {
						tokens.add(psgToken);
					}
				} else {
					MultiToken multiToken = stack.removeFirst();
					multiToken.endToken = token;
				}
			}
		}
		return tokens;
	}

	private static void parseSrl(Document document, boolean bracketTags) {
		for (Sentence sentence : document.sentences) {
			List<SrlVerbToken> verbs = new ArrayList<>();
			Integer verbNumber = 0;
			for (int tokenNumber = 0; tokenNumber < sentence.tokens.size(); tokenNumber++) {
				Token token = sentence.tokens.get(tokenNumber);
				if (token.getFeature(Option.SRL).compareTo(SRL_NONVERB) != 0) {
					SrlVerbToken verb = new SrlVerbToken(sentence, SRL_VERB_TYPE, token, token);
					List<SrlArgumentToken> arguments = extractSrlVerbArguments(sentence, verbNumber, bracketTags);
					verb.addArguments(arguments);
					verbs.add(verb);
					verbNumber++;
				}
			}
			sentence.multiTokens.put(Option.SRL, verbs);
		}
	}

	private static List<SrlArgumentToken> extractSrlVerbArguments(final Sentence sentence, final Integer verbNumber,
			boolean bracketTags) {
		List<SrlArgumentToken> arguments;
		ParserHelper<SrlArgumentToken> helper = new ParserHelper<SrlArgumentToken>() {

			@Override
			public String getValue(Token token) {
				return token.getSrlValue(verbNumber);
			}

			@Override
			public SrlArgumentToken createToken(Sentence sentence, String type, Token startToken, Token endToken,
					SrlArgumentToken parent) {
				return new SrlArgumentToken(sentence, type, startToken, endToken);
			}
		};
		if (bracketTags) {
			arguments = extractBracketTokens(sentence, helper);
		} else {
			arguments = extractIobesToken(sentence, helper);
		}
		Iterator<SrlArgumentToken> iterator = arguments.iterator();
		while (iterator.hasNext()) {
			SrlArgumentToken argument = iterator.next();
			if (argument.type.contentEquals(SRL_VERB_TYPE)) {
				iterator.remove();
			}
		}
		return arguments;
	}

}
