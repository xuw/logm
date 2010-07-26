package stacks.items;

import org.objectweb.asm.Type;

import stacks.ImmutableStackItem;
import stacks.StackItem;
import stacks.StackItemConverter;

public class InstanceOfItem extends ImmutableStackItem {
	private final StackItem object;
	private final Type type;
	
	public static Converter converter = null;
	
	public InstanceOfItem(StackItem o, String t, int l) {
		object = o;
		type = Type.getType(t);
		line = l;
	}
	
	public String toString() {
		return "("+object.toString()+"instanceof"+type.getInternalName()+")";
	}
	
	@Override
	public Type getType() {
		return Type.getType("Z");
	}
	
	@Override
	public StackItem getValue() {
		return object;
	}
	
	@Override
	public Object convert() {
		if(converter != null)
			return converter.convert(object);
		else
			return null;
	}

	public static interface Converter extends StackItemConverter {
		public Object convert(StackItem object);
	}
}
