package senna;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
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
		int sentencesPerSubdocument = (int) Math.ceil((double) document.getSentences().size() / processes);
		Set<Future<?>> futures = new HashSet<>();

		for (int subDocument = 0; subDocument < processes; subDocument++) {
			Future<Void> future = executeDocument(document, sentencesPerSubdocument, subDocument);
			futures.add(future);
		}
		for (Future<?> future : futures) {
			future.get();
		}
	}

	private Future<Void> executeDocument(final Document document, int sentencesPerSubdocument, int subDocument) {
		Future<Void> future = executor.submit(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				int startSentenceIndex = sentencesPerSubdocument * subDocument;
				int endSentenceIndex = (sentencesPerSubdocument * (subDocument + 1)) - 1;
				if (endSentenceIndex >= document.getSentences().size()) {
					endSentenceIndex = document.getSentences().size() - 1;
				}
				Sentence startSentence = document.getSentences().get(startSentenceIndex);
				Sentence endSentence = document.getSentences().get(endSentenceIndex);
				DocumentBuilder.buildFrom(document, startSentence.getDocumentStart(), endSentence.getDocumentEnd());
				executeProcess(document);
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

		for (Option<? extends MultiToken> option : parseOptions) {
			ResultParser.parseAnnotations(document, option, bracketTags);
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
