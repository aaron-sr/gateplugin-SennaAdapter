package senna.mapping;

import java.util.ArrayList;
import java.util.List;

import senna.Option;

public class PsgToken extends MultiToken {
	private static final long serialVersionUID = 1L;

	protected PsgToken parent;
	protected List<PsgToken> children = new ArrayList<>();

	protected PsgToken(Sentence sentence, String type, Token startToken, PsgToken parent) {
		super(sentence, Option.PSG, type, startToken, null);
		this.parent = parent;
		if (parent != null) {
			parent.children.add(this);
		}
	}

	public PsgToken getParent() {
		return parent;
	}

	public List<PsgToken> getChildren() {
		return children;
	}

}
