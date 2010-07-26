package stacks;

import org.objectweb.asm.Type;

public abstract class ImmutableStackItem implements StackItem {
	protected int line;
	
	@Override
	public StackItem[] getChildren() {
		return null;
	}

	@Override
	public StackItem getCopy() {
		return this;
	}

	@Override
	public Type getType() {
		return null;
	}

	@Override
	public StackItem getValue() {
		return null;
	}
	
	@Override
	public StackItem addChild(StackItem item) {
		return this;
	}

	@Override
	public int getLine() {
		return line;
	}
}
