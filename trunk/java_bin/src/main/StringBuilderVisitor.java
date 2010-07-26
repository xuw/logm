package main;

import org.objectweb.asm.Type;

import stacks.FrameStack;
import stacks.FrameStackVisitor;
import stacks.StackItem;

/**
 * An extended FrameStackVisitor that adds special
 * handling for strings so that their contents are kept
 * track off
 * 
 * @author Bruno
 */
public class StringBuilderVisitor extends FrameStackVisitor {
	
	public StringBuilderVisitor(StringBuilderFactory itemFactory, FrameStack stackClass, DoneHandler handler){
		super(itemFactory, stackClass, handler);
	}
	
	public StringBuilderVisitor(StringBuilderFactory itemFactory, FrameStack stackClass){
		super(itemFactory, stackClass);
	}
	
	public StringBuilderVisitor(){
		super();
	}
	
	/**
	 *        NEW, ANEWARRAY, CHECKCAST or INSTANCEOF
	 */
	@Override
	public void visitTypeInsn(int opcode, String desc) {
		if(skip)
			return;
		//check for string creation
		if(opcode ==NEW && desc.equals("java/lang/StringBuilder")) {
			frameStack.push(((StringBuilderFactory)factory).newMessage());
		}
		else {
			super.visitTypeInsn(opcode, desc);
		}
	}
	
	/**
	 *        INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC or
     *        INVOKEINTERFACE.
	 */
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, 
			String desc) {
		if(skip)
			return;
		//check for string initialization or appending
		if(owner.equals("java/lang/StringBuilder")) {
			Type[] argTypes = Type.getArgumentTypes(desc);
			int numArgs = argTypes.length;
			
			if((name.equals("<init>") && numArgs==1) || name.equals("append")) {
				StackItem item = frameStack.pop();
				StackItem buffer = frameStack.pop();
				StackItem newMessage = buffer.addChild(item);
				frameStack.replaceAll(buffer, newMessage);
				if(name.equals("append")) //if appending need to add result back to the stack
					frameStack.push(newMessage);
				return;
			}
			if(name.equals("toString")) {
				return;
			}
		}
		super.visitMethodInsn(opcode, owner, name, desc);
	}
}
