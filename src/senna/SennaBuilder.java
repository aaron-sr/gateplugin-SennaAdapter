package senna;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import senna.mapping.MultiToken;

public class SennaBuilder {

	private ExecutorService executor;
	private File sennaFile;
	private Set<CommandOption> commandOptions = new HashSet<>();
	private Set<Option<? extends MultiToken>> processOptions = new HashSet<>();
	private Set<Option<? extends MultiToken>> parseOptions = new HashSet<>();
	private Integer processes;
	private OutputStream errorStream = System.err;

	private static enum CommandOption {

		/**
		 * Display model informations (on the standard error output, so it does not mess
		 * up the tag outputs).
		 */
		VERBOSE("-verbose"),
		/** Do not output tokens (first output column). */
		NO_TOKEN_TAGS("-notokentags"),
		/**
		 * Output start/end character offset (in the sentence), for each token.
		 */
		OFFSET_TAGS("-offsettags"),
		/** Output IOB tags instead of IOBES. */
		IOB_TAGS("-iobtags"),
		/** Output 'bracket' tags instead of IOBES. */
		BRACKET_TAGS("-brackettags"),
		/**
		 * Specify the path to the SENNA data/ and hash/ directories, if you do not run
		 * SENNA in its original directory. The path must end by "/".
		 */
		PATH("-path"),
		/** Use user's tokens (space separated) instead of SENNA tokenizer. */
		USER_TOKENS("-usrtokens"),
		/**
		 * Use verbs outputed by the POS tagger instead of SRL style verbs for SRL task.
		 * You might want to use this, as the SRL training task ignore some verbs (many
		 * "be" and "have") which might be not what you want.
		 */
		POS_VERBS("-posvbs"),
		/**
		 * Use user's verbs (given in <file>) instead of SENNA verbs for SRL task. The
		 * file must contain one line per token, with an empty line between each
		 * sentence. A line which is not a "-" corresponds to a verb. -pos
		 */
		USER_VERBS("-usrvbs"),
		/** Part-of-Speech Tagging */
		POS("-pos"),
		/** Chunking */
		CHK("-chk"),
		/** Named Entity Recognition */
		NER("-ner"),
		/** Semantic Role Labeling */
		SRL("-srl"),
		/** Syntax Tree Parsing */
		PSG("-psg");

		private String command;
		private File file;

		private CommandOption(String command) {
			this.command = command;
		}

		private CommandOption withFile(File file) {
			this.file = file;
			return this;
		}

		private String getCommandString() {
			if (file != null) {
				return command + " " + file.getAbsolutePath();
			} else {
				return command;
			}
		}

	}

	public SennaBuilder(File sennaFile, Integer processes) {
		this.sennaFile = sennaFile;
		this.processes = processes;
	}

	public Senna build() {
		List<String> command = new ArrayList<>();
		command.add(sennaFile.getAbsolutePath());

		commandOptions.add(CommandOption.OFFSET_TAGS);

		for (CommandOption commandOption : Util.sort(commandOptions)) {
			command.add(commandOption.getCommandString());
		}

		ProcessBuilder processBuilder = new ProcessBuilder(command);
		processBuilder.directory(sennaFile.getParentFile());
		parseOptions.retainAll(processOptions);
		boolean bracketTags = commandOptions.contains(CommandOption.BRACKET_TAGS)
				&& !commandOptions.contains(CommandOption.IOB_TAGS);
		ExecutorService executor = this.executor != null ? this.executor : Executors.newCachedThreadPool();
		return new Senna(executor, processes, errorStream, processBuilder, processOptions, parseOptions, bracketTags);
	}

	public SennaBuilder withErrorStream(OutputStream errorStream) {
		this.errorStream = errorStream;
		return this;
	}

	public SennaBuilder withExecutor(ExecutorService executor) {
		this.executor = executor;
		return this;
	}

	public SennaBuilder withIobTags(Boolean iobTags) {
		if (iobTags) {
			commandOptions.add(CommandOption.IOB_TAGS);
		} else {
			commandOptions.remove(CommandOption.IOB_TAGS);
		}
		return this;
	}

	public SennaBuilder withBracketTags(Boolean bracketTags) {
		if (bracketTags) {
			commandOptions.add(CommandOption.BRACKET_TAGS);
		} else {
			commandOptions.remove(CommandOption.BRACKET_TAGS);
		}
		return this;
	}

	public SennaBuilder withUserTokens(Boolean userTokens) {
		if (userTokens) {
			commandOptions.add(CommandOption.USER_TOKENS);
		} else {
			commandOptions.remove(CommandOption.USER_TOKENS);
		}
		return this;
	}

	public SennaBuilder withPosVerbs(Boolean posVerbs) {
		if (posVerbs) {
			commandOptions.add(CommandOption.POS_VERBS);
		} else {
			commandOptions.remove(CommandOption.POS_VERBS);
		}
		return this;
	}

	public SennaBuilder withUserVerbs(File file) {
		if (file != null) {
			commandOptions.add(CommandOption.USER_VERBS.withFile(file));
		} else {
			commandOptions.remove(CommandOption.USER_VERBS);
		}
		return this;
	}

	public SennaBuilder outputPos(Boolean executePos) {
		if (executePos) {
			commandOptions.add(CommandOption.POS);
			processOptions.add(Option.POS);
		} else {
			commandOptions.remove(CommandOption.POS);
			processOptions.remove(Option.POS);
		}
		return this;
	}

	public SennaBuilder outputChk(Boolean executeChk) {
		if (executeChk) {
			commandOptions.add(CommandOption.CHK);
			processOptions.add(Option.CHK);
		} else {
			commandOptions.remove(CommandOption.CHK);
			processOptions.remove(Option.CHK);
		}
		return this;
	}

	public SennaBuilder outputNer(Boolean executeNer) {
		if (executeNer) {
			commandOptions.add(CommandOption.NER);
			processOptions.add(Option.NER);
		} else {
			commandOptions.remove(CommandOption.NER);
			processOptions.remove(Option.NER);
		}
		return this;
	}

	public SennaBuilder outputSrl(Boolean executeSrl) {
		if (executeSrl) {
			commandOptions.add(CommandOption.SRL);
			processOptions.add(Option.SRL);
		} else {
			commandOptions.remove(CommandOption.SRL);
			processOptions.remove(Option.SRL);
		}
		return this;
	}

	public SennaBuilder outputPsg(Boolean executePsg) {
		if (executePsg) {
			commandOptions.add(CommandOption.PSG);
			processOptions.add(Option.PSG);
		} else {
			commandOptions.remove(CommandOption.PSG);
			processOptions.remove(Option.PSG);
		}
		return this;
	}

	public SennaBuilder parseChk(Boolean parseChk) {
		if (parseChk) {
			parseOptions.add(Option.CHK);
		} else {
			parseOptions.remove(Option.CHK);
		}
		return this;
	}

	public SennaBuilder parseNer(Boolean parseNer) {
		if (parseNer) {
			parseOptions.add(Option.NER);
		} else {
			parseOptions.remove(Option.NER);
		}
		return this;
	}

	public SennaBuilder parseSrl(Boolean parseSrl) {
		if (parseSrl) {
			parseOptions.add(Option.SRL);
		} else {
			parseOptions.remove(Option.SRL);
		}
		return this;
	}

	public SennaBuilder parsePsg(Boolean parsePsg) {
		if (parsePsg) {
			parseOptions.add(Option.PSG);
		} else {
			parseOptions.remove(Option.PSG);
		}
		return this;
	}

}
