package senna;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import senna.mapping.Document;
import senna.mapping.MultiToken;
import senna.mapping.ResultParser;

public class SennaProcess {

	private ExecutorService executor;
	private ProcessBuilder processBuilder;
	private Collection<Option<? extends MultiToken>> processOptions;
	private Collection<Option<? extends MultiToken>> parseOptions;
	private boolean bracketTags;

	private Process process;
	private Future<Void> writeFuture;
	private Future<Void> readErrorFuture;
	private OutputStream errorStream = System.err;

	protected SennaProcess(ExecutorService executor, ProcessBuilder processBuilder,
			Collection<Option<? extends MultiToken>> processOptions,
			Collection<Option<? extends MultiToken>> parseOptions, boolean bracketTags) {
		this.executor = executor;
		this.processBuilder = processBuilder;
		this.processOptions = processOptions;
		this.parseOptions = parseOptions;
		this.bracketTags = bracketTags;
	}

	public void execute(final Document document) throws IOException, InterruptedException, ExecutionException {
		writeFuture = null;
		readErrorFuture = null;

		try {
			process = processBuilder.start();

			writeFuture = executor.submit(new Callable<Void>() {
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

			readErrorFuture = executor.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					InputStream errorStream = process.getErrorStream();
					Util.copy(errorStream, SennaProcess.this.errorStream);
					errorStream.close();
					return null;
				}
			});

			InputStream inputStream = process.getInputStream();
			ResultParser.parse(document, inputStream, processOptions);
			inputStream.close();

			writeFuture.get();
			readErrorFuture.get();
			process.waitFor();
		} catch (IOException e) {
			cancel();
			throw e;
		} catch (InterruptedException e) {
			cancel();
			throw e;
		} catch (ExecutionException e) {
			cancel();
			throw e;
		}

		for (Option<? extends MultiToken> option : parseOptions) {
			ResultParser.parseAnnotations(document, option, bracketTags);
		}

		process = null;
	}

	public void cancel() {
		if (writeFuture != null) {
			writeFuture.cancel(true);
		}
		if (readErrorFuture != null) {
			readErrorFuture.cancel(true);
		}
		if (process != null && process.isAlive()) {
			process.destroy();
		}
	}

	public void setErrorStream(OutputStream errorStream) {
		this.errorStream = errorStream;
	}

}
