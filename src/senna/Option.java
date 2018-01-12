package senna;

import senna.mapping.MultiToken;
import senna.mapping.PsgToken;
import senna.mapping.SrlVerbToken;

public class Option<T extends MultiToken> implements Comparable<Option<T>> {

	public static final Option<MultiToken> POS = new Option<>(1);
	public static final Option<MultiToken> CHK = new Option<>(2);
	public static final Option<MultiToken> NER = new Option<>(3);
	public static final Option<SrlVerbToken> SRL = new Option<>(4);
	public static final Option<PsgToken> PSG = new Option<>(5);

	private final int order;

	private Option(int order) {
		this.order = order;
	}

	@Override
	public int compareTo(Option<T> o) {
		return order - o.order;
	}

}