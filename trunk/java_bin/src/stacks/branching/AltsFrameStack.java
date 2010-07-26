package stacks.branching;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import stacks.FrameStack;
import stacks.NoBranchFrameStack;
import stacks.StackItem;

public class AltsFrameStack implements FrameStack{
	private List<FrameStack> items;
	
	public AltsFrameStack() {
		items = new ArrayList<FrameStack>();
		items.add(new NoBranchFrameStack());
	}
	
	protected AltsFrameStack(AltsFrameStack copy) {
		items = new ArrayList<FrameStack>();
		for(FrameStack stack : copy.items)
			items.add(stack.getCopy());
	}
	
	@Override
	public AltsFrameStack getCopy() {
		return new AltsFrameStack(this);
	}
	
	@Override
	public StackItem pop() {
		if(!items.isEmpty()) {
			AltsItem.Builder r = new AltsItem.Builder();
			for(FrameStack stack : items)
				r.addAlt(stack.pop());
			return r.build();
		}
		else
			return null;
	}
	
	private void pushAlts(AltsItem alts) {
		Iterator<FrameStack> iter = items.iterator();
		for(StackItem i : alts.getChildren()){
			iter.next().push(i);
		}
	}
	
	@Override
	public void push(StackItem i) {
		if(i instanceof AltsItem) {
			pushAlts((AltsItem)i);
			return;
		}
		for(FrameStack stack : items)
			stack.push(i);
	}
	
	@Override
	public void remove(int n){
		for(FrameStack stack : items)
			for(int i = 0; i < n && !stack.isEmpty(); i++)
				stack.pop();
	}
	
	@Override
	public int numItems() {
		return items.get(0).numItems();
	}
	
	@Override
	public boolean isEmpty() {
		return items.get(0).isEmpty();
	}
	
	@Override
	public void clear() {
		items.clear();
		items.add(new NoBranchFrameStack());
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
		if(old instanceof AltsItem) {
			if(replace instanceof AltsItem) {
				Iterator<FrameStack> stackiter = items.iterator();
				StackItem[] olds = old.getChildren();
				StackItem[] replaces = replace.getChildren();
				int len = olds.length;
				for(int i = 0; i < len; i++)
					stackiter.next().replaceAll(olds[i], replaces[i]);
			}
			return;
		}
		for(FrameStack stack : items)
			stack.replaceAll(old, replace);
	}
	
	public void merge(FrameStack altStack) {
		if(altStack instanceof AltsFrameStack) {
			for(FrameStack stack : ((AltsFrameStack) altStack).items)
				if(!stack.isEmpty())
					items.add(stack);
		} else {
			if(!altStack.isEmpty())
				items.add(altStack);
		}
	}
}
