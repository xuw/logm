package stacks.items;

import org.objectweb.asm.Type;

import stacks.ImmutableStackItem;
import stacks.StackItem;
import stacks.StackItemConverter;

public class FieldItem extends ImmutableStackItem {
	private final String owner;
	private final String field;
	private final StackItem object;
	private final Type type;
	
	public static Converter converter = null;
	
	public FieldItem(String c, String f, String t, 
			StackItem o, int l) {
		object = o;
		owner = c;
		field = f;
		type = Type.getType(t);
		line = l;
	}
	
	public String toString() {
		if(object==null) {
			return owner+"."+field;
		}
		else {
			return object.toString()+"."+field;
		}
	}

	@Override
	public Type getType() {
		return type;
	}
	
	@Override
	public StackItem getValue() {
		return object;
	}
	
	@Override
	public Object convert() {
		if(converter != null)
			return converter.convert(owner, field, object);
		else
			return null;
	}
	
	public static interface Converter extends StackItemConverter {
		public Object convert(String owner, String field, StackItem object);
	}
}
