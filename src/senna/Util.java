package senna;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Util {

	public static List<String> readColumnLine(String line, Pattern columnPattern) {
		List<String> columns = new ArrayList<>();
		readColumnLine(line, columnPattern, columns);
		return columns;
	}

	public static void readColumnLine(String line, Pattern columnPattern, List<String> columns) {
		Matcher matcher = columnPattern.matcher(line);
		if (matcher.matches()) {
			String value = matcher.group(1);
			String restOfLine = matcher.group(2);
			columns.add(value);
			readColumnLine(restOfLine, columnPattern, columns);
		}
	}

	public static <E extends Comparable<? super E>> List<E> sort(Collection<E> collection) {
		List<E> list = new ArrayList<>(collection);
		Collections.sort(list);
		return list;
	}

	public static void copy(InputStream in, OutputStream out) throws IOException {
		int size = 0;
		byte[] buffer = new byte[1024];
		while ((size = in.read(buffer)) != -1)
			out.write(buffer, 0, size);
	}

}
