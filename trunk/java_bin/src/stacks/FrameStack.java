package stacks;


public interface FrameStack {
	public FrameStack getCopy();
	
	public String toString();
	
	public StackItem pop();
	
	public void push(StackItem i);

	public void remove(int n);
	
	public void clear();
	
	public int numItems();
	
	public boolean isEmpty();
	
	/**
	 * any operation that changes the value of an item that effects
	 * any duplicates of that item that might exist in the stack
	 * must be followed by a call to this method if the effects of
	 * dup are to be similated properly
	 * 
	 * @param old		the old StackItem to be replaced
	 * @param replace	the new StackItem to replace the old
	 */
	public void replaceAll(StackItem old, StackItem replace);
	
	public void merge(FrameStack altStack);
}
