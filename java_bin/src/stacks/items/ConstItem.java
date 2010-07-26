package stacks.items;

import org.objectweb.asm.Type;

import stacks.ImmutableStackItem;
import stacks.StackItemConverter;

public class ConstItem extends ImmutableStackItem{
	private final Object value;
	private final Type type;
	
	public static Converter converter = null;
	
	public ConstItem(Object v, String t, int l) {
		value = v;
		type = t!=null?Type.getType(t):Type.VOID_TYPE;
		line = l;
	}
	
	public String toString() {
		if(value !=null)
			return value.toString();
		else
			return "null";
	}
	
	@Override
	public Type getType() {
		return type;
	}
	
	@Override
	public Object convert() {
		if(converter != null)
			return converter.convert(value);
		else
			return null;
	}
	
	public static interface Converter extends StackItemConverter {
		public Object convert(Object value);
	}
}
