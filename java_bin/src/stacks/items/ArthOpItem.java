package stacks.items;

import org.objectweb.asm.Type;

import stacks.ImmutableStackItem;
import stacks.StackItem;
import stacks.StackItemConverter;

public class ArthOpItem extends ImmutableStackItem {
	private final StackItem operand1;
	private final StackItem operand2;
	private final String operator;
	private final Type type;
	
	public static Converter converter = null;
	
	public ArthOpItem(StackItem op1, StackItem op2, String op, String t, int l) {
		operand1 = op1;
		operand2 = op2;
		operator = op;
		type = Type.getType(t);
		line = l;
	}
	
	public String toString() {
		return "arth("+operand1.toString()+operator+operand2.toString()+")";
	}
	
	@Override
	public Type getType() {
		return type;
	}

	@Override
	public StackItem[] getChildren() {
		return new StackItem[] {operand1, operand2};
	}
	
	@Override
	public Object convert() {
		if(converter != null)
			return converter.convert(operand1, operand2, operator);
		else
			return null;
	}
	
	public static interface Converter extends StackItemConverter {
		public Object convert(StackItem operand1, StackItem operand2, String operator);
	}
}
