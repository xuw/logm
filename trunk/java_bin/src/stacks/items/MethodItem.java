package stacks.items;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Type;

import stacks.ImmutableStackItem;
import stacks.StackItem;
import stacks.StackItemConverter;
import stacks.branching.AltsItem;

public class MethodItem extends ImmutableStackItem {
	private final String owner;
	private final String name;
	private final Type rtype;
	private final Type[] argTypes;
	private final List<StackItem> args;
	private final StackItem object;
	
	public static Converter converter = null;
	
	
	public MethodItem(String c, String m, Type t, Type[] argts, StackItem o, int l)
	{
		owner = c;
		name = m;
		args = new ArrayList<StackItem>();
		rtype = t;
		argTypes = argts;
		object = o;
		line = l;
	}
	
	public String toString() {
		String buffer;
		if(object != null)
			buffer = object.toString();
		else
			buffer = owner;
		buffer+="."+name+"(";
		for(StackItem arg : args)
			buffer += arg + ", ";
		buffer+=")";
		return buffer;
	}

	@Override
	public Type getType() {
		return rtype;
	}

	@Override
	public StackItem[] getChildren() {
		return args.toArray(new StackItem[args.size()-1]);
	}
	
	@Override
	public StackItem getValue() {
		return object;
	}
	
	@Override
	public Object convert() {
		if(converter != null)
			return converter.convert(owner, name, argTypes, args, object);
		else
			return null;
	}

	public static interface Converter extends StackItemConverter {
		public Object convert(String owner, String name, Type[] argTypes, List<StackItem> args, StackItem object);
	}
	
	public static class MethodItemBuilder {
		private String bowner;
		private String bname;
		private Type brtype;
		private Type[] bargTypes;
		private List<StackItem> bargs;
		private StackItem bobject;
		private int bline;
		
		
		public MethodItemBuilder(String c, String m, int l)
		{
			bowner = c;
			bname = m;
			bargs = new ArrayList<StackItem>();
			bobject = null;
			bline = l;
		}
		
		public void setInstance(StackItem inst) {
			bobject = inst;
		}
		
		/**
		 * parses the function description for the return type
		 * and the argument types
		 * 
		 * @param desc	the byte code method descrition
		 * @return		the number of arguments the method has
		 */
		public int parseDesc(String desc){
			brtype = Type.getReturnType(desc);
			bargTypes = Type.getArgumentTypes(desc);
			return bargTypes.length;
		}
		
		public void addArg(StackItem i) {
			bargs.add(i);
		}
		
		public StackItem build() {
			if(bobject instanceof AltsItem) {
				AltsItem.Builder r = new AltsItem.Builder();
				StackItem[] objects = bobject.getChildren();
				int len = objects.length;
				for(int i = 0; i < len; i++) {
					MethodItemBuilder b = new MethodItemBuilder(bowner, bname, bline);
					b.setInstance(objects[i]);
					b.brtype = brtype;
					b.bargTypes = bargTypes;
					for(StackItem altsarg : bargs) {
						b.addArg(altsarg.getChildren()[i]);
					}
					r.addAlt(b.build());
				}
				return r.build();
			} else {
				MethodItem m = new MethodItem(bowner, bname, brtype, bargTypes, bobject, bline);
				m.args.addAll(bargs);
				return m;
			}
		}
	}
}
