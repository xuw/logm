package stacks.items;

import org.objectweb.asm.Type;

import stacks.ImmutableStackItem;
import stacks.StackItemConverter;

public class ReferenceItem extends ImmutableStackItem {
	private final Type type;
	
	public static Converter converter;
	
	public ReferenceItem(String t, int l) {
		type = Type.getType(t);
		line = l;
	}
	
	public String toString() {
		return "[reference: "+type.getClassName()+"]";
	}

	@Override
	public Type getType() {
		return type;
	}
	
	@Override
	public Object convert() {
		if(converter != null)
			return converter.convert();
		else
			return null;
	}

	public static interface Converter extends StackItemConverter {
		public Object convert();
	}
}
