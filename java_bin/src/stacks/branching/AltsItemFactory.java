package stacks.branching;

import stacks.StackItem;
import stacks.StackItemFactory;
import stacks.items.ArrayIndexItem;
import stacks.items.ArrayLengthItem;
import stacks.items.ArthOpItem;
import stacks.items.CastItem;
import stacks.items.FieldItem;
import stacks.items.InstanceOfItem;

public class AltsItemFactory extends StackItemFactory {
	
	private static final AltsItemFactory instance;
	
	static{
		instance = new AltsItemFactory();
	}
	
	protected AltsItemFactory() {
		return;
	}

	public static StackItemFactory getInstance() {
		return instance;
	}

	@Override
	public StackItem newArrayIndex(StackItem array, StackItem index, int line){
		if(array instanceof AltsItem) {
			AltsItem.Builder builder = new AltsItem.Builder();
			if(index instanceof AltsItem) {
				StackItem[] alts1 = array.getChildren();
				StackItem[] alts2 = index.getChildren();
				int len = alts1.length;
				if(len != alts2.length)
					return null;
				for(int i = 0; i < len; i++) {
					builder.addAlt(newArrayIndex(alts1[i], alts2[i], line));
				}
				return builder.build();
			} else {
				for(StackItem alt : array.getChildren())
					builder.addAlt(newArrayIndex(alt, index, line));
				return builder.build();
			}
		} else if(index instanceof AltsItem) {
			AltsItem.Builder builder = new AltsItem.Builder();
			for(StackItem alt : index.getChildren())
				builder.addAlt(newArrayIndex(array, alt, line));
			return builder.build();
		}
		return new ArrayIndexItem(array, index, line);
	}
	
	@Override
	public StackItem newArrayLength(StackItem array, int line) {
		if(array instanceof AltsItem) {
			AltsItem.Builder builder = new AltsItem.Builder();
			for(StackItem alt : array.getChildren())
				builder.addAlt(newArrayLength(alt, line));
			return builder.build();
		}
		return new ArrayLengthItem(array, line);
	}
	
	@Override
	public StackItem newArthOp(StackItem operand2, StackItem operand1, String operator, String type, int line) {
		
		if(operand1 instanceof AltsItem) {
			AltsItem.Builder builder = new AltsItem.Builder();
			if(operand2 instanceof AltsItem) {
				StackItem[] alts1 = operand1.getChildren();
				StackItem[] alts2 = operand2.getChildren();
				int len = alts1.length;
				if(len != alts2.length)
					return null;
				for(int i = 0; i < len; i++) {
					builder.addAlt(newArthOp(alts1[i], alts2[i], operator, type, line));
				}
				return builder.build();
			} else {
				for(StackItem alt : operand1.getChildren())
					builder.addAlt(newArthOp(alt, operand2, operator, type, line));
				return builder.build();
			}
		} else if(operand2 instanceof AltsItem) {
			AltsItem.Builder builder = new AltsItem.Builder();
			for(StackItem alt : operand2.getChildren())
				builder.addAlt(newArthOp(operand1, alt, operator, type, line));
			return builder.build();
		}
		return new ArthOpItem(operand1, operand2, operator, type, line);
	}
	
	@Override
	public StackItem newCast(String type, StackItem object, int line) {
		if(object instanceof AltsItem) {
			AltsItem.Builder builder = new AltsItem.Builder();
			for(StackItem alt : object.getChildren())
				builder.addAlt(newCast(type, alt, line));
			return builder.build();
		}
		return new CastItem(type, object, line);
	}

	@Override
	public StackItem newField(String ownerClass, String fieldName, String type, StackItem object, int line) {
		if(object instanceof AltsItem) {
			AltsItem.Builder builder = new AltsItem.Builder();
			for(StackItem alt : object.getChildren())
				builder.addAlt(newField(ownerClass, fieldName, type, alt, line));
			return builder.build();
		}
		return new FieldItem(ownerClass, fieldName, type, object, line);
	}
	
	@Override
	public StackItem newInstanceOf(StackItem object, String typeCheck, int line) {
		if(object instanceof AltsItem) {
			AltsItem.Builder builder = new AltsItem.Builder();
			for(StackItem alt : object.getChildren())
				builder.addAlt(newInstanceOf(alt, typeCheck, line));
			return builder.build();
		}
		return new InstanceOfItem(object, typeCheck, line);
	}
}
