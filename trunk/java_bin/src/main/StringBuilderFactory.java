package main;

import stacks.ImmutableStackItem;
import stacks.branching.AltsItemFactory;

public class StringBuilderFactory extends AltsItemFactory {
	private static final StringBuilderFactory instance;
	
	static{
		instance = new StringBuilderFactory();
	}
	
	protected StringBuilderFactory() {
		return;
	}

	public static StringBuilderFactory getInstance() {
		return instance;
	}
	
	public ImmutableStackItem newMessage() {
		return new MessageItem();
	}
}
