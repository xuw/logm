package stacks.items;

import org.objectweb.asm.Type;

import stacks.ImmutableStackItem;
import stacks.StackItemConverter;

public class LocalVarItem extends ImmutableStackItem {
	private Type typeDef;
	private String name;
	
	public static Converter converter = null;
	
	public LocalVarItem() {
		name = null;
		typeDef = Type.VOID_TYPE;
	}
	
	public String toString() {
		String buf = "_localvar_";
		if(typeDef != null) {
			buf+=typeDef.getClassName();
		}
		return buf;
	}
	
	public void definition(String t, String n) {
		name = n;
		typeDef = Type.getType(t);
	}

	@Override
	public Type getType() {
		return typeDef;
	}
	
	@Override
	public Object convert() {
		if(converter != null)
			return converter.convert(name);
		else
			return null;
	}

	public static interface Converter extends StackItemConverter {
		public Object convert(String  name);
	}
}
