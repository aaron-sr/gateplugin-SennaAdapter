package gate.senna;

import static gate.util.Files.fileFromURL;
import static senna.Option.CHK;
import static senna.Option.NER;
import static senna.Option.POS;
import static senna.Option.PSG;
import static senna.Option.SRL;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import gate.Annotation;
import gate.AnnotationSet;
import gate.DocumentContent;
import gate.Factory;
import gate.FeatureMap;
import gate.Resource;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.Optional;
import gate.creole.metadata.RunTime;
import gate.util.InvalidOffsetException;
import senna.Option;
import senna.Senna;
import senna.SennaBuilder;
import senna.mapping.Document;
import senna.mapping.MultiToken;
import senna.mapping.PsgToken;
import senna.mapping.Sentence;
import senna.mapping.SrlArgumentToken;
import senna.mapping.SrlVerbToken;
import senna.mapping.Token;

/**
 * This class is the implementation of the resource SENNAADAPTER.
 */
@CreoleResource(name = "SennaAdapter", comment = "Integrate SENNA v3.0 (https://ronan.collobert.com/senna/) as a Processing Resource")
public class SennaAdapter extends AbstractLanguageAnalyser {

	private static final long serialVersionUID = 781830027240661071L;

	private static final Integer MAX_INPUT_LENGTH = Integer.MAX_VALUE / 2;

	private static final String ANNOTATION_TOKEN_NAME = "Token";
	private static final String ANNOTATION_TOKEN_FEATURE_POS_NAME = "POS";
	private static final String ANNOTATION_TOKEN_FEATURE_CHK_NAME = "CHK";
	private static final String ANNOTATION_TOKEN_FEATURE_NER_NAME = "NER";
	private static final String ANNOTATION_TOKEN_FEATURE_SRL_NAME = "SRL";
	private static final String ANNOTATION_TOKEN_FEATURE_PSG_NAME = "PSG";

	private static final String ANNOTATION_CHK_NAME = "CHK";
	private static final String ANNOTATION_NER_NAME = "NER";
	private static final String ANNOTATION_SRL_NAME = "SRL";
	private static final String ANNOTATION_PSG_NAME = "PSG";
	private static final String ANNOTATION_CHK_FEATURE_NAME = "value";
	private static final String ANNOTATION_NER_FEATURE_NAME = "value";
	private static final String ANNOTATION_SRL_FEATURE_VERB_NAME = "verb";
	private static final String ANNOTATION_SRL_FEATURE_TYPE_NAME = "type";
	private static final String ANNOTATION_SRL_FEATURE_ARGUMENT_JOIN = " [...] ";
	private static final String ANNOTATION_PSG_FEATURE_NAME = "value";
	private static final String ANNOTATION_PSG_FEATURE_PARENTID_NAME = "parent";
	private static final String ANNOTATION_PSG_FEATURE_CHILDRENIDS_NAME = "children";
	private static final String RELATION_SRL_NAME = "SRL";

	private URL executableFile;
	private Integer parallelProcesses;

	private Boolean iobTags;
	private Boolean bracketTags;
	private Boolean posVerbs;
	private URL verbsFile;

	private Boolean executePOS;
	private Boolean executeCHK;
	private Boolean executeNER;
	private Boolean executeSRL;
	private Boolean executePSG;

	private String inputASName;
	private String inputSentenceType;
	private String inputTokenType;

	private String outputASName;
	private Boolean outputCHKAnnotations;
	private Boolean outputNERAnnotations;
	private Boolean outputSRLAnnotations;
	private Boolean outputPSGAnnotations;

	@Override
	public Resource init() throws ResourceInstantiationException {
		return this;
	}

	@Override
	public void execute() throws ExecutionException {
		AnnotationSet inputAnnotationSet = document.getAnnotations(inputASName);
		AnnotationSet outputAnnotationSet = document.getAnnotations(outputASName);

		try {
			executeContent(document.getContent(), inputAnnotationSet, outputAnnotationSet);
		} catch (Exception e) {
			throw new ExecutionException(e);
		}
	}

	private void executeContent(DocumentContent documentContent, AnnotationSet inputAnnotationSet,
			AnnotationSet outputAnnotationSet) throws Exception {
		List<Sentence> sentences = new ArrayList<>();
		boolean userTokens = hasValue(inputTokenType);
		boolean reuseAnnotations = equals(inputASName, outputASName);
		if (hasValue(inputSentenceType)) {

			Long documentOffset = 0l;
			Long lastSentenceEnd = 0l;

			AnnotationSet annotationSet = inputAnnotationSet.get(inputSentenceType);
			for (Annotation sentenceAnnotation : annotationSet.inDocumentOrder()) {
				Long sentenceStart = sentenceAnnotation.getStartNode().getOffset();
				Long sentenceEnd = sentenceAnnotation.getEndNode().getOffset();

				if ((sentenceEnd - documentOffset) > MAX_INPUT_LENGTH.longValue()) {
					String documentText = documentContent.getContent(documentOffset, lastSentenceEnd).toString();
					Document document = new Document(documentText, sentences);
					executeSenna(documentOffset, document, outputAnnotationSet);

					documentOffset = lastSentenceEnd;

					sentences = new ArrayList<>();
				}

				Integer id = reuseAnnotations ? sentenceAnnotation.getId() : null;
				int documentStart = (int) (sentenceStart - documentOffset);
				int documentEnd = (int) (sentenceEnd - documentOffset);
				Sentence sentence;
				if (userTokens) {
					List<Token> tokens = buildTokens(documentOffset, inputAnnotationSet, sentenceStart, sentenceEnd,
							reuseAnnotations);
					sentence = new Sentence(id, documentStart, documentEnd, tokens);
				} else {
					sentence = new Sentence(id, documentStart, documentEnd);
				}
				sentences.add(sentence);
				lastSentenceEnd = sentenceEnd;
			}
			if (!sentences.isEmpty()) {
				String documentText = documentContent.getContent(documentOffset, lastSentenceEnd).toString();
				Document sennaDocument = new Document(documentText, sentences);
				executeSenna(documentOffset, sennaDocument, outputAnnotationSet);
			}
		} else if (documentContent.size() < MAX_INPUT_LENGTH.longValue()) {
			Sentence sentence;
			if (userTokens) {
				List<Token> tokens = buildTokens(0l, inputAnnotationSet, 0l, documentContent.size(), reuseAnnotations);
				sentence = new Sentence(null, 0, documentContent.size().intValue(), tokens);
			} else {
				sentence = new Sentence(null, 0, documentContent.size().intValue());
			}
			sentences.add(sentence);
			Document sennaDocument = new Document(documentContent.getContent(0l, documentContent.size()).toString(),
					sentences);
			executeSenna(0l, sennaDocument, outputAnnotationSet);
		} else {
			throw new IllegalStateException();
		}
	}

	private List<Token> buildTokens(Long documentOffset, AnnotationSet inputAnnotationSet, Long sentenceStart,
			Long sentenceEnd, boolean reuseAnnotations) {
		List<Token> tokens = new ArrayList<>();
		AnnotationSet inputTokenSet = inputAnnotationSet.get(inputTokenType, sentenceStart, sentenceEnd);
		Iterator<Annotation> tokenAnnotationIterator = inputTokenSet.iterator();
		while (tokenAnnotationIterator.hasNext()) {
			Annotation tokenAnnotation = tokenAnnotationIterator.next();
			Long tokenStart = tokenAnnotation.getStartNode().getOffset();
			Long tokenEnd = tokenAnnotation.getEndNode().getOffset();
			Integer id = reuseAnnotations ? tokenAnnotation.getId() : null;
			tokens.add(new Token(id, (int) (tokenStart - documentOffset), (int) (tokenEnd - documentOffset)));
		}
		return tokens;
	}

	protected void executeSenna(Long documentOffset, final Document document, AnnotationSet outputAnnotationSet)
			throws Exception {

		SennaBuilder builder = new SennaBuilder(fileFromURL(executableFile), parallelProcesses);
		builder.withIobTags(iobTags);
		builder.withBracketTags(bracketTags);
		builder.withUserTokens(hasValue(inputTokenType));
		builder.withPosVerbs(posVerbs);
		if (verbsFile != null) {
			builder.withUserVerbs(fileFromURL(verbsFile));
		}

		builder.outputPos(executePOS);
		builder.outputChk(executeCHK);
		builder.outputNer(executeNER);
		builder.outputSrl(executeSRL);
		builder.outputPsg(executePSG);

		builder.parseChk(outputCHKAnnotations);
		builder.parseNer(outputNERAnnotations);
		builder.parseSrl(outputSRLAnnotations);
		builder.parsePsg(outputPSGAnnotations);

		Senna process = builder.build();
		process.execute(document);

		addAnnotations(documentOffset, document, outputAnnotationSet);
		addAnnotations(documentOffset, document, outputAnnotationSet, CHK, ANNOTATION_CHK_NAME,
				ANNOTATION_CHK_FEATURE_NAME);
		addAnnotations(documentOffset, document, outputAnnotationSet, NER, ANNOTATION_NER_NAME,
				ANNOTATION_NER_FEATURE_NAME);
		addSrlAnnotations(documentOffset, document, outputAnnotationSet);
		addPsgAnnotations(documentOffset, document, outputAnnotationSet);

	}

	protected void addAnnotations(Long documentOffset, Document document, AnnotationSet outputAnnotationSet)
			throws InvalidOffsetException {
		for (Sentence sentence : document.getSentences()) {
			for (Token token : sentence.getTokens()) {
				FeatureMap map = extractFeaturesToMap(token);
				Annotation annotation = null;
				if (token.getDocumentId() != null) {
					annotation = outputAnnotationSet.get((Integer) token.getDocumentId());
					annotation.getFeatures().putAll(map);
				} else {
					Long start = documentOffset
							+ (token.getDocumentStart() != null ? token.getDocumentStart() : token.getSennaStart());
					Long end = documentOffset
							+ (token.getDocumentEnd() != null ? token.getDocumentEnd() : token.getSennaEnd());
					outputAnnotationSet.add(start, end, ANNOTATION_TOKEN_NAME, map);
				}
			}
		}
	}

	private FeatureMap extractFeaturesToMap(Token token) {
		FeatureMap map = Factory.newFeatureMap();
		for (Entry<Option<?>, String> featureEntry : token.getFeatures().entrySet()) {
			Option<?> option = featureEntry.getKey();
			String value = featureEntry.getValue();
			String key = null;
			if (option.equals(POS)) {
				key = ANNOTATION_TOKEN_FEATURE_POS_NAME;
			} else if (option.equals(CHK)) {
				key = ANNOTATION_TOKEN_FEATURE_CHK_NAME;
			} else if (option.equals(NER)) {
				key = ANNOTATION_TOKEN_FEATURE_NER_NAME;
			} else if (option.equals(SRL)) {
				key = ANNOTATION_TOKEN_FEATURE_SRL_NAME;
			} else if (option.equals(PSG)) {
				key = ANNOTATION_TOKEN_FEATURE_PSG_NAME;
			}
			if (key != null) {
				map.put(key, value);
			}
		}
		return map;
	}

	protected void addAnnotations(Long documentOffset, Document document, AnnotationSet outputAnnotationSet,
			Option<? extends MultiToken> option, String annotationName, String featureName)
			throws InvalidOffsetException {
		for (Sentence sentence : document.getSentences()) {
			for (MultiToken token : sentence.getMultiTokens(option)) {
				FeatureMap features = Factory.newFeatureMap();
				features.put(featureName, token.getType());
				outputAnnotationSet.add(documentOffset + token.getDocumentStart(),
						documentOffset + token.getDocumentEnd(), annotationName, features);
			}
		}
	}

	protected void addSrlAnnotations(Long documentOffset, Document document, AnnotationSet outputAnnotationSet)
			throws InvalidOffsetException {
		for (Sentence sentence : document.getSentences()) {
			for (SrlVerbToken verb : sentence.getMultiTokens(SRL)) {
				List<Integer> relationIds = new ArrayList<>();

				DocumentContent verbText = this.document.getContent()
						.getContent(documentOffset + verb.getDocumentStart(), documentOffset + verb.getDocumentEnd());

				FeatureMap verbFeatures = Factory.newFeatureMap();
				verbFeatures.put(ANNOTATION_SRL_FEATURE_TYPE_NAME, verb.getType());
				verbFeatures.put(ANNOTATION_SRL_FEATURE_VERB_NAME, verbText);

				for (SrlArgumentToken argument : verb.getArguments()) {
					DocumentContent argumentText = this.document.getContent().getContent(
							documentOffset + argument.getDocumentStart(), documentOffset + argument.getDocumentEnd());
					if (verbFeatures.get(argument.getType()) != null) {
						verbFeatures.put(argument.getType(), verbFeatures.get(argument.getType())
								+ ANNOTATION_SRL_FEATURE_ARGUMENT_JOIN + argumentText);
					} else {
						verbFeatures.put(argument.getType(), argumentText);
					}
					FeatureMap features = Factory.newFeatureMap();
					features.put(ANNOTATION_SRL_FEATURE_TYPE_NAME, argument.getType());
					features.put(ANNOTATION_SRL_FEATURE_VERB_NAME, verbText);
					Integer argumentId = outputAnnotationSet.add(documentOffset + argument.getDocumentStart(),
							documentOffset + argument.getDocumentEnd(), ANNOTATION_SRL_NAME, features);
					relationIds.add(argumentId);
				}

				Integer verbId = outputAnnotationSet.add(documentOffset + verb.getDocumentStart(),
						documentOffset + verb.getDocumentEnd(), ANNOTATION_SRL_NAME, verbFeatures);
				if (!relationIds.isEmpty()) {
					relationIds.add(0, verbId);
					outputAnnotationSet.getRelations().addRelation(RELATION_SRL_NAME, toIntArray(relationIds));
				}
			}
		}
	}

	protected void addPsgAnnotations(Long documentOffset, Document document, AnnotationSet outputAnnotationSet)
			throws InvalidOffsetException {
		for (Sentence sentence : document.getSentences()) {
			addPsgAnnotations(null, sentence.getMultiTokens(PSG), outputAnnotationSet, documentOffset);
		}
	}

	protected List<Integer> addPsgAnnotations(Integer parentId, List<PsgToken> tokens,
			AnnotationSet outputAnnotationSet, Long offset) throws InvalidOffsetException {
		List<Integer> ids = new ArrayList<>();
		for (PsgToken token : tokens) {
			FeatureMap features = Factory.newFeatureMap();
			features.put(ANNOTATION_PSG_FEATURE_NAME, token.getType());
			features.put(ANNOTATION_PSG_FEATURE_PARENTID_NAME, parentId);
			Integer id = outputAnnotationSet.add(offset + token.getDocumentStart(), offset + token.getDocumentEnd(),
					ANNOTATION_PSG_NAME, features);
			ids.add(id);
			List<Integer> childrenIds = addPsgAnnotations(id, token.getChildren(), outputAnnotationSet, offset);
			features.put(ANNOTATION_PSG_FEATURE_CHILDRENIDS_NAME, childrenIds);
		}
		return ids;
	}

	private static boolean hasValue(String string) {
		return string != null && string.length() > 0;
	}

	private static <E> boolean equals(E e1, E e2) {
		if (e1 == null && e2 == null)
			return true;
		if (e1 == null || e2 == null)
			return false;
		return e1.equals(e2);
	}

	private static int[] toIntArray(List<Integer> list) {
		int[] ret = new int[list.size()];
		int i = 0;
		for (Integer e : list)
			ret[i++] = e.intValue();
		return ret;
	}

	@Optional(false)
	@CreoleParameter(comment = "SENNA executable")
	public void setExecutableFile(URL executableFile) {
		this.executableFile = executableFile;
	}

	public URL getExecutableFile() {
		return executableFile;
	}

	@Optional
	@RunTime
	@CreoleParameter(comment = "Run # senna processes in parallel", defaultValue = "1")
	public void setParallelProcesses(Integer parallelProcesses) {
		this.parallelProcesses = parallelProcesses;
	}

	public Integer getParallelProcesses() {
		return parallelProcesses;
	}

	@Optional
	@RunTime
	@CreoleParameter(comment = "Output IOB tags instead of IOBES.", defaultValue = "false")
	public void setIobTags(Boolean iobTags) {
		this.iobTags = iobTags;
	}

	public Boolean getIobTags() {
		return iobTags;
	}

	@Optional
	@RunTime
	@CreoleParameter(comment = "Output 'bracket' tags instead of IOBES.", defaultValue = "false")
	public void setBracketTags(Boolean bracketTags) {
		this.bracketTags = bracketTags;
	}

	public Boolean getBracketTags() {
		return bracketTags;
	}

	@Optional
	@RunTime
	@CreoleParameter(comment = "Use verbs outputed by the POS tagger instead of SRL style verbs for SRL task", defaultValue = "true")
	public void setPosVerbs(Boolean posVerbs) {
		this.posVerbs = posVerbs;
	}

	public Boolean getPosVerbs() {
		return posVerbs;
	}

	@Optional
	@RunTime
	@CreoleParameter(comment = "Use user's verbs instead of SENNA verbs for SRL task. The file must contain one line per token, with an empty line between each sentence. A line which is not a \"-\" corresponds to a verb.")
	public void setVerbsFile(URL verbsFile) {
		this.verbsFile = verbsFile;
	}

	public URL getVerbsFile() {
		return verbsFile;
	}

	@Optional
	@RunTime
	@CreoleParameter(comment = "output POS tags", defaultValue = "true")
	public void setExecutePOS(Boolean executePOS) {
		this.executePOS = executePOS;
	}

	public Boolean getExecutePOS() {
		return executePOS;
	}

	@Optional
	@RunTime
	@CreoleParameter(comment = "output CHK tags", defaultValue = "true")
	public void setExecuteCHK(Boolean executeCHK) {
		this.executeCHK = executeCHK;
	}

	public Boolean getExecuteCHK() {
		return executeCHK;
	}

	@Optional
	@RunTime
	@CreoleParameter(comment = "output NER tags", defaultValue = "true")
	public void setExecuteNER(Boolean executeNER) {
		this.executeNER = executeNER;
	}

	public Boolean getExecuteNER() {
		return executeNER;
	}

	@Optional
	@RunTime
	@CreoleParameter(comment = "output SRL tags", defaultValue = "true")
	public void setExecuteSRL(Boolean executeSRL) {
		this.executeSRL = executeSRL;
	}

	public Boolean getExecuteSRL() {
		return executeSRL;
	}

	@Optional
	@RunTime
	@CreoleParameter(comment = "output PSG tags", defaultValue = "true")
	public void setExecutePSG(Boolean executePSG) {
		this.executePSG = executePSG;
	}

	public Boolean getExecutePSG() {
		return executePSG;
	}

	@Optional
	@RunTime
	@CreoleParameter(comment = "Input annotation set name", defaultValue = "")
	public void setInputASName(String inputASName) {
		this.inputASName = inputASName;
	}

	public String getInputASName() {
		return inputASName;
	}

	@Optional
	@RunTime
	@CreoleParameter(comment = "Input sentence annotation name", defaultValue = "Sentence")
	public void setInputSentenceType(String inputSentenceType) {
		this.inputSentenceType = inputSentenceType;
	}

	public String getInputSentenceType() {
		return inputSentenceType;
	}

	@Optional
	@RunTime
	@CreoleParameter(comment = "Use tokens by GATE Annotation instead of SENNA tokenizer.")
	public void setInputTokenType(String inputTokenType) {
		this.inputTokenType = inputTokenType;
	}

	public String getInputTokenType() {
		return inputTokenType;
	}

	@Optional
	@RunTime
	@CreoleParameter(comment = "Output annotation set name", defaultValue = "")
	public void setOutputASName(String outputASName) {
		this.outputASName = outputASName;
	}

	public String getOutputASName() {
		return this.outputASName;
	}

	@Optional
	@RunTime
	@CreoleParameter(comment = "Output extra annotations for CHK", defaultValue = "true")
	public void setOutputCHKAnnotations(Boolean outputCHKAnnotations) {
		this.outputCHKAnnotations = outputCHKAnnotations;
	}

	public Boolean getOutputCHKAnnotations() {
		return outputCHKAnnotations;
	}

	@Optional
	@RunTime
	@CreoleParameter(comment = "Output extra annotations for NER", defaultValue = "true")
	public void setOutputNERAnnotations(Boolean outputNERAnnotations) {
		this.outputNERAnnotations = outputNERAnnotations;
	}

	public Boolean getOutputNERAnnotations() {
		return outputNERAnnotations;
	}

	@Optional
	@RunTime
	@CreoleParameter(comment = "Output extra annotations for SRL", defaultValue = "true")
	public void setOutputSRLAnnotations(Boolean outputSRLAnnotations) {
		this.outputSRLAnnotations = outputSRLAnnotations;
	}

	public Boolean getOutputSRLAnnotations() {
		return outputSRLAnnotations;
	}

	@Optional
	@RunTime
	@CreoleParameter(comment = "Output extra annotations for PSG", defaultValue = "true")
	public void setOutputPSGAnnotations(Boolean outputPSGAnnotations) {
		this.outputPSGAnnotations = outputPSGAnnotations;
	}

	public Boolean getOutputPSGAnnotations() {
		return outputPSGAnnotations;
	}

}