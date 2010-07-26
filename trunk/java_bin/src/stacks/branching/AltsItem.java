package stacks.branching;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Type;

import stacks.StackItem;

public class AltsItem implements StackItem {
	private final StackItem[] alts;
	
	public AltsItem(StackItem[] children) {
		alts = children;
	}
	
	protected AltsItem(AltsItem copy) {
		alts = copy.alts.clone();
	}

	@Override
	public StackItem addChild(StackItem item) {
		Builder r = new Builder();
		if(item instanceof AltsItem) {
			StackItem[] children = item.getChildren();
			int len = alts.length;
			for(int i = 0; i < len; i++)
				r.addAlt(alts[i].addChild(children[i]));
		} else {
			for(StackItem i : alts)
				r.addAlt(i.addChild(item));
		}
		return r.build();
	}

	/**
	 * not immutable. don't mess with array, direct reference to internals
	 */
	@Override
	public StackItem[] getChildren() {
		return alts;
	}

	@Override
	public AltsItem getCopy() {
		return this;
	}

	@Override
	public Type getType() {
		return alts[0].getType();
	}

	@Override
	public StackItem getValue() {
		return null;
	}
	
	@Override
	public int getLine() {
		return -1;
	}

	public static class Builder{
		private List<StackItem> children;
		
		public Builder() {
			children = new ArrayList<StackItem>();
		}
		
		public void addAlt(StackItem alt) {
			children.add(alt);
		}
		
		public StackItem build() {
			int s = children.size();
			if(s == 0) {
				return null;
			} else if(s == 1) {
				return children.get(0);
			} else
				return new AltsItem(children.toArray(new StackItem[s]));
		}
	}

	@Override
	public Object convert() {
		return null;
	}
}
