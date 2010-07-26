package stacks;

import org.objectweb.asm.Type;

public interface StackItem {
	public Type getType();
	public StackItem getCopy();
	public StackItem[] getChildren();
	public StackItem addChild(StackItem item);
	public StackItem getValue();
	public int getLine();
	public Object convert();
}
