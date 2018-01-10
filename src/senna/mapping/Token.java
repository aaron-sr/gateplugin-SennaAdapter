package senna.mapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import senna.Option;

public class Token extends SimpleMapping {

	protected Sentence sentence;

	protected Map<Option<?>, String> features = new LinkedHashMap<Option<?>, String>();
	protected List<String> srlValues;

	public Token(Object documentId, Integer documentStart, Integer documentEnd) {
		super(null);
		this.documentId = documentId;
		this.documentStart = documentStart;
		this.documentEnd = documentEnd;
	}

	protected Token(Sentence sentence, Integer sennaStart, Integer sennaEnd, Integer documentStart,
			Integer documentEnd) {
		super(sentence.sennaDocument);
		this.sentence = sentence;
		this.sentence.tokens.add(this);
		this.sennaStart = sennaStart;
		this.sennaEnd = sennaEnd;
		this.documentStart = documentStart;
		this.documentEnd = documentEnd;
	}

	public Sentence getSentence() {
		return sentence;
	}

	public Integer getDocumentSentenceStart() {
		return documentStart - sentence.documentStart;
	}

	public Integer getDocumentSentenceEnd() {
		return documentEnd - sentence.documentEnd;
	}

	public Integer getSennaSentenceStart() {
		return sennaStart - sentence.sennaStart;
	}

	public Integer getSennaSentenceEnd() {
		return sennaEnd - sentence.sennaEnd;
	}

	public Map<Option<?>, String> getFeatures() {
		return features;
	}

	public List<String> getSrlValues() {
		return srlValues;
	}

	public String getFeature(Option<? extends MultiToken> option) {
		return features.get(option);
	}

}