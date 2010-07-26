package stacks.items;

import org.objectweb.asm.Type;

import stacks.ImmutableStackItem;
import stacks.StackItem;
import stacks.StackItemConverter;

public class CastItem extends ImmutableStackItem{
	private final StackItem object;
	private final Type type;
	
	public static Converter converter = null;
	
	public CastItem(String def, StackItem obj, int l){
		type = Type.getType(def);
		object = obj;
		line = l;
	}
	
	public String toString() {
		return "[cast: ("+type.getClassName()+")"+object+"]";
	}

	@Override
	public StackItem getValue() {
		return object;
	}

	@Override
	public Type getType() {
		return type;
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
