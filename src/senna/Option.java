package senna;

import senna.mapping.MultiToken;
import senna.mapping.PsgToken;
import senna.mapping.SrlVerbToken;

public class Option<T extends MultiToken> implements Comparable<Option<T>> {

	public static final Option<MultiToken> POS = new Option<MultiToken>(1);
	public static final Option<MultiToken> CHK = new Option<MultiToken>(2);
	public static final Option<MultiToken> NER = new Option<MultiToken>(3);
	public static final Option<SrlVerbToken> SRL = new Option<SrlVerbToken>(4);
	public static final Option<PsgToken> PSG = new Option<PsgToken>(5);

	private int order;

	private Option(int order) {
		this.order = order;
	}

	@Override
	public int compareTo(Option<T> o) {
		return order - o.order;
	}

}