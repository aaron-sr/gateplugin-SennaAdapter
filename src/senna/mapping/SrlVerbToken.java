package senna.mapping;

import java.util.ArrayList;
import java.util.List;

import senna.Option;

public class SrlVerbToken extends MultiToken {

	protected List<SrlArgumentToken> arguments = new ArrayList<>();

	protected SrlVerbToken(Sentence sentence, String type, Token startToken, Token endToken) {
		super(sentence, Option.SRL, type, startToken, endToken);
	}

	public List<SrlArgumentToken> getArguments() {
		return arguments;
	}

	protected void addArguments(List<SrlArgumentToken> arguments) {
		this.arguments.addAll(arguments);
		for (SrlArgumentToken argument : arguments) {
			argument.verbToken = this;
		}
	}

}