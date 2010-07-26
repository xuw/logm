package stacks.items;

import org.objectweb.asm.Type;

import stacks.ImmutableStackItem;
import stacks.StackItem;
import stacks.StackItemConverter;

public class ArrayIndexItem extends ImmutableStackItem {
	private final StackItem index;
	private final StackItem array;
	
	public static Converter converter = null;
	
	public ArrayIndexItem(StackItem a, StackItem i, int l) {
		array = a;
		index = i;
		line = l;
	}
	
	public String toString() {
		return array.toString()+"["+index.toString()+"]";
	}

	@Override
	public Type getType() {
		return Type.getType(array.getType().getInternalName().substring(1));
	}

	@Override
	public StackItem[] getChildren() {
		return new StackItem[] {index, array};
	}
	
	@Override
	public Object convert() {
		if(converter != null)
			return converter.convert(array, index);
		else
			return null;
	}

	public static interface Converter extends StackItemConverter {
		public Object convert(StackItem array, StackItem index);
	}
}
