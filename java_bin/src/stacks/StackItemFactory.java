package stacks;

import stacks.items.ArrayIndexItem;
import stacks.items.ArrayLengthItem;
import stacks.items.ArthOpItem;
import stacks.items.CastItem;
import stacks.items.ConstItem;
import stacks.items.FieldItem;
import stacks.items.InstanceOfItem;
import stacks.items.LocalVarItem;
import stacks.items.MethodItem;
import stacks.items.ReferenceItem;

public class StackItemFactory {
	
	private static final StackItemFactory instance;
	
	static{
		instance = new StackItemFactory();
	}

	protected StackItemFactory() {
		return;
	}

	public static StackItemFactory getInstance() {
		return instance;
	}

	public ImmutableStackItem newConst(Object value, String type, int line) {
		return new ConstItem(value, type, line);
	}

	public ImmutableStackItem newLocalVar(int index) {
		return new LocalVarItem();
	}

	public ImmutableStackItem newReference(String type, int line) {
		return new ReferenceItem(type, line);
	}

	public StackItem newArrayIndex(StackItem array, StackItem index, int line){
		return new ArrayIndexItem(array, index, line);
	}

	public StackItem newArrayLength(StackItem array, int line) {
		return new ArrayLengthItem(array, line);
	}

	public StackItem newArthOp(StackItem operand2, StackItem operand1, String operator, String type, int line) {
		return new ArthOpItem(operand1, operand2, operator, type, line);
	}

	public StackItem newCast(String type, StackItem object, int line) {
		return new CastItem(type, object, line);
	}

	public StackItem newField(String ownerClass, String fieldName, String type, StackItem object, int line) {
		return new FieldItem(ownerClass, fieldName, type, object, line);
	}

	public StackItem newInstanceOf(StackItem object, String typeCheck, int line) {
		return new InstanceOfItem(object, typeCheck, line);
	}
	
	public MethodItem.MethodItemBuilder newMethodItemBuilder(String owner, String name, int lineNumber) {
		return new MethodItem.MethodItemBuilder(owner, name, lineNumber);
	}
}
