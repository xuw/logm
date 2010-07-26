package stacks;

import java.util.ListIterator;
import java.util.Stack;

/**
 * A linked list of StackItems used to keep track of the current
 * state of the stack
 * 
 * @author Bruno
 */
public class NoBranchFrameStack implements FrameStack{
	private Stack<StackItem> items;
	
	public NoBranchFrameStack() {
		items = new Stack<StackItem>();
	}
	
	public NoBranchFrameStack getCopy() {
		NoBranchFrameStack r = new NoBranchFrameStack();
		for(StackItem item: items)
			r.items.add(item.getCopy());
		return r;
	}
	
	public String toString() {
		String buffer = "";
		for(StackItem item : items)
			buffer += item.toString()+"\n";
		return buffer;
	}
	
	public StackItem pop() {
		if(!items.isEmpty())
			return items.pop();
		else
			return null;
	}
	
	public void push(StackItem i) {
		items.push(i);
	}

	public void remove(int n){
		for(int i = 0; i < n && !items.isEmpty(); i++) {
			items.pop();
		}
	}
	
	public void clear() {
		items.clear();
	}
	
	public int numItems() {
		return items.size();
	}
	
	public boolean isEmpty() {
		return items.empty();
	}
	
	/**
	 * any operation that changes the value of an item that effects
	 * any duplicates of that item that might exist in the stack
	 * must be followed by a call to this method if the effects of
	 * dup are to be similated properly
	 * 
	 * @param old		the old StackItem to be replaced
	 * @param replace	the new StackItem to replace the old
	 */
	public void replaceAll(StackItem old, StackItem replace) {
		for(ListIterator<StackItem> iter = items.listIterator(); iter.hasNext();) {
			if(iter.next()==old)
				iter.set(replace);
		}
	}
	
	public void merge(FrameStack altStack) {
		if(this.numItems() != altStack.numItems()) {
			System.err.println("\t\talternates stack not same size");
			return;
		}
		return;
	}
}