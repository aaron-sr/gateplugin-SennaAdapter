package senna;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import senna.mapping.Document;
import senna.mapping.DocumentBuilder;
import senna.mapping.MultiToken;
import senna.mapping.ResultParser;
import senna.mapping.Sentence;
import senna.mapping.SubDocument;

public class Senna {

	private ExecutorService executor;
	private Integer processes;
	private ProcessBuilder processBuilder;
	private Collection<Option<? extends MultiToken>> processOptions;
	private Collection<Option<? extends MultiToken>> parseOptions;
	private boolean bracketTags;
	private OutputStream errorStream;

	private Set<Process> currentProcesses = new HashSet<>();

	protected Senna(ExecutorService executor, Integer processes, OutputStream errorStream,
			ProcessBuilder processBuilder, Collection<Option<? extends MultiToken>> processOptions,
			Collection<Option<? extends MultiToken>> parseOptions, boolean bracketTags) {
		this.executor = executor;
		this.processes = processes;
		this.errorStream = errorStream;
		this.processBuilder = processBuilder;
		this.processOptions = processOptions;
		this.parseOptions = parseOptions;
		this.bracketTags = bracketTags;
	}

	public void execute(final Document document) throws IOException, InterruptedException, ExecutionException {
		if (processes == 1) {
			executeProcess(document);
		} else {
			Set<Future<?>> futures = new HashSet<>();
			List<Integer> sentencesCountList = splitIntoParts(document.getSentences().size(), processes);
			Integer startSentenceIndex = 0;
			for (int subDocument = 0; subDocument < processes; subDocument++) {
				Integer sentencesCount = sentencesCountList.get(subDocument);
				if (sentencesCount > 0) {
					int endSentenceIndex = startSentenceIndex + sentencesCount - 1;
					Future<Void> future = executeDocument(document, startSentenceIndex, endSentenceIndex);
					futures.add(future);
					startSentenceIndex = endSentenceIndex + 1;
				}
			}
			for (Future<?> future : futures) {
				future.get();
			}
		}
		for (Option<? extends MultiToken> option : parseOptions) {
			ResultParser.parseAnnotations(document, option, bracketTags);
		}
	}

	private static List<Integer> splitIntoParts(Integer x, Integer n) {
		List<Integer> list = new ArrayList<>();
		int f = x / n;
		int r = x % n;
		for (int i = 0; i < n; i++) {
			if (i < n - r) {
				list.add(f);
			} else {
				list.add(f + 1);
			}
		}
		return list;
	}

	private Future<Void> executeDocument(final Document document, int startSentenceIndex, int endSentenceIndex) {
		Future<Void> future = executor.submit(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				Sentence startSentence = document.getSentences().get(startSentenceIndex);
				Sentence endSentence = document.getSentences().get(endSentenceIndex);
				SubDocument subDocument = DocumentBuilder.buildSubDocument(document, startSentence, endSentence);
				executeProcess(subDocument);
				subDocument.mergeToOriginal();
				return null;
			}

		});
		return future;
	}

	private void executeProcess(final Document document) throws IOException, InterruptedException, ExecutionException {
		try {
			Process process = processBuilder.start();
			currentProcesses.add(process);

			executor.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					OutputStream outputStream = process.getOutputStream();
					ByteArrayInputStream in = new ByteArrayInputStream(document.getSennaText().getBytes());
					Util.copy(in, outputStream);
					in.close();
					outputStream.close();
					return null;
				}
			});

			executor.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					InputStream errorStream = process.getErrorStream();
					Util.copy(errorStream, Senna.this.errorStream);
					errorStream.close();
					return null;
				}
			});

			InputStream inputStream = process.getInputStream();
			ResultParser.parse(document, inputStream, processOptions);
			inputStream.close();

			process.waitFor();
			currentProcesses.remove(process);
		} catch (IOException e) {
			cancel();
			throw e;
		} catch (InterruptedException e) {
			cancel();
			throw e;
		}
	}

	public void cancel() {
		executor.shutdownNow();
		for (Process process : currentProcesses) {
			if (process.isAlive()) {
				process.destroy();
			}
		}
	}

}
