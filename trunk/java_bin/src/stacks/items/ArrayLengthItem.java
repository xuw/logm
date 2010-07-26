package stacks.items;

import org.objectweb.asm.Type;

import stacks.ImmutableStackItem;
import stacks.StackItem;
import stacks.StackItemConverter;

public class ArrayLengthItem extends ImmutableStackItem {
	private StackItem array;
	
	public static Converter converter = null;
	
	public ArrayLengthItem(StackItem a, int l) {
		array = a;
		line = l;
	}
	
	public String toString() {
		return array.toString()+".length";
	}
	
	@Override
	public Type getType() {
		return Type.getType("I");
	}

	@Override
	public StackItem getValue() {
		return array;
	}

	@Override
	public Object convert() {
		if(converter != null)
			return converter.convert(array);
		else
			return null;
	}
	
	public static interface Converter extends StackItemConverter {
		public Object convert(StackItem array);
	}
}
