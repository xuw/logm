package stacks;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import stacks.items.LocalVarItem;
import stacks.items.MethodItem;

/**
 * A visitor that keeps track of all byte code operations
 * using a FrameStack class.  It can be told to skip unintereting
 * intructions whith the skip variable.
 * 
 * @author Bruno
 */
public class FrameStackVisitor implements ClassVisitor, MethodVisitor, Opcodes {

	/**keeps track of the current state of the jvm stack**/
	protected FrameStack frameStack;
	/**Map of scopes seen so far by Label and the local variables seen in them*/
	protected Map<Label, LabelScope> labelScopes;
	/**Scopes for future labels that have been seen in jumps but not yet reached*/
	protected Map<Label, LabelScope> futureLabels;
	/**The current scope changed at every label*/
	protected LabelScope currentScope;
	/**Set to true after a jump instruction so that scopes don't get merged*/
	protected boolean jump;
	/**Set to true to skip instructions */
	protected boolean skip;
	/**Use this factory to make StackItems*/
	protected StackItemFactory factory;
	/**Handler to be called when reach method done */
	protected DoneHandler doneHandler;
	/**The name of the current class being visited*/
	protected String className;
	/**The name of the current method being visited*/
	protected String methodName;
	/**The current line number being visited if the information is available */
	protected int lineNumber;
	
	public FrameStackVisitor(StackItemFactory itemFactory, FrameStack stackClass, DoneHandler handler){
		factory = itemFactory;
		frameStack = stackClass;
		doneHandler = handler;
	}
	
	public FrameStackVisitor(StackItemFactory itemFactory, FrameStack stackClass){
		factory = itemFactory;
		frameStack = stackClass;
	}
	
	public FrameStackVisitor(){
		frameStack = new NoBranchFrameStack();
		factory = StackItemFactory.getInstance();
	}
	
	//Visit Class
	
	@Override
	public void visit(int version, int access, String name, String signature,
			String superName, String[] interfaces) {
		//System.out.println("visiting class "+name);
		frameStack.clear();
		futureLabels = new HashMap<Label, LabelScope>();
		labelScopes = new HashMap<Label, LabelScope>();
		className = name;
	}

	@Override
	public void visitSource(String source, String debug) {
		// do nothing
	}
	
	@Override
	public void visitOuterClass(String owner, String name, String desc) {
		// do nothing
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, 
			int access) {
		// do nothing
	}
	
	@Override
	public FieldVisitor visitField(int access, String name, String desc,
			String signature, Object value) {
		return null;
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		//System.out.println("visiting method "+name);
		methodName = name;
		lineNumber = -1;
		return this;
	}
	

	//Visit Method

	@Override
	public void visitCode() {
		frameStack.clear();
		labelScopes.clear();
		futureLabels.clear();
		currentScope = null;
		jump = false;
		skip = false;
	}

	@Override
	public void visitFrame(int type, int nLocal, Object[] local, int nStack,
			Object[] stack) {
		//System.out.print("nStack = "+ nStack+ "stack.length = " +stack.length);
		switch(type) {
		case F_NEW:
		case F_FULL:
			currentScope.clearScope();
			break;
		case F_CHOP:
			currentScope.chop(nLocal);
		case F_APPEND:
		case F_SAME:
			frameStack.clear();
			break;
		case F_SAME1:
			break;
		}
	}

	
	/**
	 * 		  NOP, ACONST_NULL, ICONST_M1, ICONST_0, ICONST_1, ICONST_2,
     *        ICONST_3, ICONST_4, ICONST_5, LCONST_0, LCONST_1, FCONST_0,
     *        FCONST_1, FCONST_2, DCONST_0, DCONST_1, IALOAD, LALOAD, FALOAD,
     *        DALOAD, AALOAD, BALOAD, CALOAD, SALOAD, IASTORE, LASTORE, FASTORE,
     *        DASTORE, AASTORE, BASTORE, CASTORE, SASTORE, POP, POP2, DUP,
     *        DUP_X1, DUP_X2, DUP2, DUP2_X1, DUP2_X2, SWAP, IADD, LADD, FADD,
     *        DADD, ISUB, LSUB, FSUB, DSUB, IMUL, LMUL, FMUL, DMUL, IDIV, LDIV,
     *        FDIV, DDIV, IREM, LREM, FREM, DREM, INEG, LNEG, FNEG, DNEG, ISHL,
     *        LSHL, ISHR, LSHR, IUSHR, LUSHR, IAND, LAND, IOR, LOR, IXOR, LXOR,
     *        I2L, I2F, I2D, L2I, L2F, L2D, F2I, F2L, F2D, D2I, D2L, D2F, I2B,
     *        I2C, I2S, LCMP, FCMPL, FCMPG, DCMPL, DCMPG, IRETURN, LRETURN,
     *        FRETURN, DRETURN, ARETURN, RETURN, ARRAYLENGTH, ATHROW,
     *        MONITORENTER, or MONITOREXIT.
	 */
	@Override
	public void visitInsn(int opcode) {
		if(skip)
			return;
		
		switch(opcode){
		case NOP: 
		case ACONST_NULL:
			frameStack.push(factory.newConst(null, null, lineNumber));
			break;
		case ICONST_M1:
			frameStack.push(factory.newConst(new Integer(-1), Type.INT_TYPE.getDescriptor(), lineNumber));
			break;
		case ICONST_0:
			frameStack.push(factory.newConst(new Integer(0), Type.INT_TYPE.getDescriptor(), lineNumber));
			break;
		case ICONST_1:
			frameStack.push(factory.newConst(new Integer(1), Type.INT_TYPE.getDescriptor(), lineNumber));
			break;
		case ICONST_2:
			frameStack.push(factory.newConst(new Integer(2), Type.INT_TYPE.getDescriptor(), lineNumber));
			break;
		case ICONST_3:
			frameStack.push(factory.newConst(new Integer(3), Type.INT_TYPE.getDescriptor(), lineNumber));
			break;
		case ICONST_4:
			frameStack.push(factory.newConst(new Integer(4), Type.INT_TYPE.getDescriptor(), lineNumber));
			break;
		case ICONST_5:
			frameStack.push(factory.newConst(new Integer(5), Type.INT_TYPE.getDescriptor(), lineNumber));
			break;
		case LCONST_0:
			frameStack.push(factory.newConst(new Long(0), Type.LONG_TYPE.getDescriptor(), lineNumber));
			break;
		case LCONST_1:
			frameStack.push(factory.newConst(new Long(1), Type.LONG_TYPE.getDescriptor(), lineNumber));
			break;
		case FCONST_0:
			frameStack.push(factory.newConst(new Float(0), Type.FLOAT_TYPE.getDescriptor(), lineNumber));
			break;
		case FCONST_1: 
			frameStack.push(factory.newConst(new Float(1), Type.FLOAT_TYPE.getDescriptor(), lineNumber));
			break;
		case FCONST_2:
			frameStack.push(factory.newConst(new Float(2), Type.FLOAT_TYPE.getDescriptor(), lineNumber));
			break;
		case DCONST_0:
			frameStack.push(factory.newConst(new Double(0), Type.DOUBLE_TYPE.getDescriptor(), lineNumber));
			break;
		case DCONST_1:
			frameStack.push(factory.newConst(new Double(1), Type.DOUBLE_TYPE.getDescriptor(), lineNumber));
			break;
		case IALOAD:
		case LALOAD:
		case FALOAD:
		case DALOAD: 
		case AALOAD: 
		case BALOAD: 
		case CALOAD: 
		case SALOAD:
			StackItem index = frameStack.pop();
			StackItem array = frameStack.pop();
			frameStack.push(factory.newArrayIndex(array, index, lineNumber));
			break;
		case IASTORE: 
		case LASTORE:
		case FASTORE:
		case DASTORE:
		case AASTORE:
		case BASTORE:
		case CASTORE:
		case SASTORE:
			frameStack.remove(3);
			break;
		case ARRAYLENGTH:
			frameStack.push(factory.newArrayLength(frameStack.pop(), lineNumber));
			break;
		case I2L: 
		case F2L: 
		case D2L:
			frameStack.push(factory.newCast(Type.LONG_TYPE.getDescriptor(), frameStack.pop(), lineNumber));
			break;
		case I2F: 
		case L2F: 
		case D2F: 
			frameStack.push(factory.newCast(Type.FLOAT_TYPE.getDescriptor(), frameStack.pop(), lineNumber));
			break;
		case I2D: 
		case F2D: 
		case L2D:
			frameStack.push(factory.newCast(Type.DOUBLE_TYPE.getDescriptor(), frameStack.pop(), lineNumber));
			break;
		case L2I: 
		case D2I: 
		case F2I: 
			frameStack.push(factory.newCast(Type.INT_TYPE.getDescriptor(), frameStack.pop(), lineNumber));
			break;
		case I2B:
			frameStack.push(factory.newCast(Type.BYTE_TYPE.getDescriptor(), frameStack.pop(), lineNumber));
			break;
		case I2C:
			frameStack.push(factory.newCast(Type.CHAR_TYPE.getDescriptor(), frameStack.pop(), lineNumber));
			break;
		case I2S:
			frameStack.push(factory.newCast(Type.SHORT_TYPE.getDescriptor(), frameStack.pop(), lineNumber));
			break;
		case POP:
			frameStack.pop();
			break;
		case POP2:
			StackItem top = frameStack.pop();
			if(!(top.getType().equals(Type.LONG_TYPE) || top.getType().equals(Type.DOUBLE_TYPE)))
				frameStack.pop();
			break;
		case DUP:
			top = frameStack.pop();
			if(top == null)
				break;
			frameStack.push(top);
			frameStack.push(top);
			break;
		case DUP_X1:
			StackItem v2 = frameStack.pop();
			StackItem v1 = frameStack.pop();
			frameStack.push(v2);
			frameStack.push(v1);
			frameStack.push(v2);
			break;
		case DUP_X2:
			StackItem v3 = frameStack.pop();
			v2 = frameStack.pop();
			if(v2.getType().equals(Type.LONG_TYPE) || v2.getType().equals(Type.DOUBLE_TYPE))
				v1 = null;
			else
				v1 = frameStack.pop();
			frameStack.push(v3);
			if(v1!=null)
				frameStack.push(v1);
			frameStack.push(v2);
			frameStack.push(v3);
			break;
		case DUP2:
			v2 = frameStack.pop();
			if(v2.getType().equals(Type.LONG_TYPE) || v2.getType().equals(Type.DOUBLE_TYPE)) {
				frameStack.push(v2);
				frameStack.push(v2);
			}
			else {
				v1 = frameStack.pop();
				frameStack.push(v2);
				frameStack.push(v1);
				frameStack.push(v2);
			}
			break;
		case DUP2_X1:
			v3 = frameStack.pop();
			if(v3.getType().equals(Type.LONG_TYPE) || v3.getType().equals(Type.DOUBLE_TYPE))
				v2 = null;
			else
				v2 = frameStack.pop();
			v1 = frameStack.pop();
			if(v2!=null)
				frameStack.push(v2);
			frameStack.push(v3);
			frameStack.push(v1);
			if(v2!=null)
				frameStack.push(v2);
			frameStack.push(v3);
			break;
		case DUP2_X2:
			StackItem v4 = frameStack.pop();
			if(v4.getType().equals(Type.LONG_TYPE) || v4.getType().equals(Type.DOUBLE_TYPE))
				v3 = null;
			else
				v3 = frameStack.pop();
			v2 = frameStack.pop();
			if(v2.getType().equals(Type.LONG_TYPE) || v2.getType().equals(Type.DOUBLE_TYPE))
				v1 = null;
			else
				v1 = frameStack.pop();
			if(v3!=null)
				frameStack.push(v3);
			frameStack.push(v4);
			if(v1!=null)
				frameStack.push(v1);
			frameStack.push(v2);
			if(v3!=null)
				frameStack.push(v3);
			frameStack.push(v4);
			break;
		case SWAP:
			StackItem item1 = frameStack.pop();
			StackItem item2 = frameStack.pop();
			frameStack.push(item1);
			frameStack.push(item2);
			break;
		case IADD:
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), "+", Type.INT_TYPE.getDescriptor(), lineNumber));
			break;
		case LADD:
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), "+", Type.LONG_TYPE.getDescriptor(), lineNumber));
			break;
		case FADD:
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), "+", Type.FLOAT_TYPE.getDescriptor(), lineNumber));
			break;
		case DADD:
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), "+", Type.DOUBLE_TYPE.getDescriptor(), lineNumber));
			break;
		case ISUB:
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), "-", Type.INT_TYPE.getDescriptor(), lineNumber));
			break;
		case LSUB:
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), "-", Type.LONG_TYPE.getDescriptor(), lineNumber));
			break;
		case FSUB:
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), "-", Type.FLOAT_TYPE.getDescriptor(), lineNumber));
			break;
		case DSUB:
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), "-", Type.DOUBLE_TYPE.getDescriptor(), lineNumber));
			break;
		case IMUL:
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), "*", Type.INT_TYPE.getDescriptor(), lineNumber));
			break;
		case LMUL:
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), "*", Type.LONG_TYPE.getDescriptor(), lineNumber));
			break;
		case FMUL:
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), "*", Type.FLOAT_TYPE.getDescriptor(), lineNumber));
			break;
		case DMUL:
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), "*", Type.DOUBLE_TYPE.getDescriptor(), lineNumber));
			break;
		case IDIV:
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), "/", Type.INT_TYPE.getDescriptor(), lineNumber));
			break;
		case LDIV:
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), "/", Type.LONG_TYPE.getDescriptor(), lineNumber));
			break;
		case FDIV:
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), "/", Type.FLOAT_TYPE.getDescriptor(), lineNumber));
			break;
		case DDIV:
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), "/", Type.DOUBLE_TYPE.getDescriptor(), lineNumber));
			break;
		case IREM:
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), "%", Type.INT_TYPE.getDescriptor(), lineNumber));
			break;
		case LREM:
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), "%", Type.LONG_TYPE.getDescriptor(), lineNumber));
			break;
		case FREM: 
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), "%", Type.FLOAT_TYPE.getDescriptor(), lineNumber));
			break;
		case DREM:
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), "%", Type.DOUBLE_TYPE.getDescriptor(), lineNumber));
			break;
		case INEG: 
			frameStack.push(factory.newArthOp(factory.newConst(new Integer(0), Type.INT_TYPE.getDescriptor(), lineNumber), frameStack.pop(), "-", Type.INT_TYPE.getDescriptor(), lineNumber));
			break;
		case LNEG: 
			frameStack.push(factory.newArthOp(factory.newConst(new Integer(0), Type.INT_TYPE.getDescriptor(), lineNumber), frameStack.pop(), "-", Type.LONG_TYPE.getDescriptor(), lineNumber));
			break;
		case FNEG: 
			frameStack.push(factory.newArthOp(factory.newConst(new Integer(0), Type.INT_TYPE.getDescriptor(), lineNumber), frameStack.pop(), "-", Type.FLOAT_TYPE.getDescriptor(), lineNumber));
			break;
		case DNEG: 
			frameStack.push(factory.newArthOp(factory.newConst(new Integer(0), Type.INT_TYPE.getDescriptor(), lineNumber), frameStack.pop(), "-", Type.DOUBLE_TYPE.getDescriptor(), lineNumber));
			break;
		case ISHL:
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), "<<", Type.INT_TYPE.getDescriptor(), lineNumber));
			break;
		case LSHL:
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), "<<", Type.LONG_TYPE.getDescriptor(), lineNumber));
			break;
		case IUSHR: 
		case ISHR:
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), ">>", Type.INT_TYPE.getDescriptor(), lineNumber));
			break;
		case LSHR: 
		case LUSHR:
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), ">>", Type.LONG_TYPE.getDescriptor(), lineNumber));
			break;
		case IAND: 
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), "&", Type.INT_TYPE.getDescriptor(), lineNumber));
			break;
		case LAND:
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), "&", Type.LONG_TYPE.getDescriptor(), lineNumber));
			break;
		case IOR: 
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), "|", Type.INT_TYPE.getDescriptor(), lineNumber));
			break;
		case LOR:
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), "|", Type.LONG_TYPE.getDescriptor(), lineNumber));
			break;
		case IXOR: 
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), "^", Type.INT_TYPE.getDescriptor(), lineNumber));
			break;
		case LXOR:
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), "^", Type.LONG_TYPE.getDescriptor(), lineNumber));
			break;
		case LCMP: 
		case FCMPL: 
		case FCMPG: 
		case DCMPL: 
		case DCMPG: 
			frameStack.push(factory.newArthOp(frameStack.pop(), frameStack.pop(), "cmp", Type.INT_TYPE.getDescriptor(), lineNumber));
			break;
		case IRETURN: 
		case LRETURN:
		case FRETURN: 
		case DRETURN: 
		case ARETURN: 
		case RETURN: 
		case ATHROW:
			jump = true;
			frameStack.clear();
			break;
		case MONITORENTER:
	    case MONITOREXIT:
	    	frameStack.pop();
	    	break;
	    default:
	    	frameStack.clear();	
		}
	}
	
	/**
	 *       BIPUSH, SIPUSH or NEWARRAY
	 */
	@Override
	public void visitIntInsn(int opcode, int operand) {
		if(skip)
			return;
		
		switch(opcode){
		case BIPUSH:
		case SIPUSH:
			frameStack.push(factory.newConst(new Integer(operand), Type.INT_TYPE.getDescriptor(), lineNumber));
			break;
		
		}
	}
	
	/**
	 *        ILOAD, LLOAD, FLOAD, DLOAD, ALOAD, ISTORE,
     *        LSTORE, FSTORE, DSTORE, ASTORE or RET
	 */
	@Override
	public void visitVarInsn(int opcode, int var) {
		if(skip)
			return;
		
		switch(opcode){
		case ILOAD:
		case LLOAD:
		case FLOAD:
		case DLOAD:
		case ALOAD:
			frameStack.push(currentScope.getVar(var));
			break;
		case LSTORE:
		case ISTORE:
		case FSTORE:
		case DSTORE:
		case ASTORE:
			frameStack.pop();
			break;
		case RET:
			jump = true;
			frameStack.clear();
			break;
		}
	}
	
	/**
	 *        NEW, ANEWARRAY, CHECKCAST or INSTANCEOF
	 */
	@Override
	public void visitTypeInsn(int opcode, String desc) {
		if(skip)
			return;
		
		switch(opcode) {
		case NEW:
			frameStack.push(factory.newReference("L"+desc+";", lineNumber));
			break;
		case NEWARRAY:
			frameStack.pop();
			frameStack.push(factory.newReference("["+desc+";", lineNumber));
			break;
		case ANEWARRAY:
			frameStack.pop();
			frameStack.push(factory.newReference("[L"+desc+";", lineNumber));
			break;
		case CHECKCAST:
			frameStack.push( factory.newCast("L"+desc+";", frameStack.pop(), lineNumber));
			break;
		case INSTANCEOF:
			frameStack.push(factory.newInstanceOf(frameStack.pop(), "L"+desc+";", lineNumber));
		}

	}

	/**
	 *       GETSTATIC, PUTSTATIC, GETFIELD or PUTFIELD
	 */
	@Override
	public void visitFieldInsn(int opcode, String owner, String name, 
			String desc) {
		if(skip)
			return;
		
		switch(opcode) {
		case GETSTATIC:
			frameStack.push(factory.newField(owner, name, desc, null, lineNumber));
			break;
		case GETFIELD:
			frameStack.push(factory.newField(owner, name, desc, frameStack.pop(), lineNumber));
			break;
		case PUTFIELD:
			frameStack.pop();
			break;
		case PUTSTATIC:
			frameStack.pop();
			break;
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
		
		MethodItem.MethodItemBuilder method = factory.newMethodItemBuilder(owner, name, lineNumber);
		int numArgs = method.parseDesc(desc);
		for(int i = 0; i < numArgs; i++) {
			method.addArg(frameStack.pop());
		}
		if(opcode != INVOKESTATIC) {
			method.setInstance(frameStack.pop());
		}
		if(!desc.endsWith("V"))
			frameStack.push(method.build());


	}

	/**
	 *        IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ,
     *        IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ,
     *        IF_ACMPNE, GOTO, JSR, IFNULL or IFNONNULL.
	 */
	@Override
	public void visitJumpInsn(int opcode, Label label) {
		if(skip)
			return;
		
		switch(opcode) {
		case IFEQ:
		case IFNE: 
		case IFLT:
		case IFGE:
		case IFGT:
		case IFLE:
		case IFNULL:
		case IFNONNULL:
			frameStack.pop();
			break;
		case IF_ICMPEQ:
		case IF_ICMPNE:
		case IF_ICMPLT:
		case IF_ICMPGE:
		case IF_ICMPGT:
		case IF_ICMPLE:
		case IF_ACMPEQ:
		case IF_ACMPNE:
			frameStack.remove(2);
			break;
		case GOTO:
			break;
		case JSR:
			return;
		}
		if(!labelScopes.containsKey(label)) {
			LabelScope newLabel = new LabelScope(label);
			if(frameStack != null)
				newLabel.backupStack = frameStack.getCopy();
			futureLabels.put(label, newLabel);
			jump = true;
		}
	}

	@Override
	public void visitLdcInsn(Object constant) {
		if(skip)
			return;
		
		frameStack.push(factory.newConst(constant, Type.getType(constant.getClass()).getDescriptor(), lineNumber));
	}
	
	@Override
	public void visitIincInsn(int var, int increment) {
		//does nothing to the stack
		return;
	}
	
	@Override
	public void visitTableSwitchInsn(int min, int max, Label dflt, 
			Label[] labels) {
		if(skip)
			return;
		
		// TODO only necessary for supporting more complex cases
		frameStack.clear();
	}
	
	@Override
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
		if(skip)
			return;
		
		// TODO only necessary for supporting more complex cases
	}
	
	@Override
	public void visitMultiANewArrayInsn(String desc, int dims) {
		if(skip)
			return;
		
		frameStack.remove(dims);
		desc = "L"+desc+";";
		for(int i = 0; i < dims; i++) {
			desc = "["+desc;
		}
		frameStack.push(factory.newReference(desc, lineNumber));
	}


	@Override
	public void visitLocalVariable(
			String name,
	        String desc,
	        String signature,
	        Label start,
	        Label end,
	        int index) {
		LabelScope scope = labelScopes.get(start);
		do {
			LocalVarItem var = scope.getVar(index);
			if(var != null)
				var.definition(desc, name);
			scope = scope.getNext();
		} while(scope != null && !scope.label.equals(end));
	}

	@Override
	public void visitMaxs(int maxStack, int maxLocals) {
		// do nothing
	}

	@Override
	public void visitTryCatchBlock(Label start, Label end, Label handler, 
			String type) {
		if(skip)
			return;
		if(!labelScopes.containsKey(handler)) {
			LabelScope newLabel = new LabelScope(handler);
			if(frameStack != null) {
				newLabel.backupStack = frameStack.getCopy();
				newLabel.backupStack.clear();
				newLabel.backupStack.push(factory.newReference("L"+type+";", lineNumber));
			}
			futureLabels.put(handler, newLabel);
		}
	}

	@Override
	public void visitLabel(Label label) {
		if(currentScope == null) {
			currentScope = new LabelScope(label);
			labelScopes.put(label, currentScope);
			return;
		}
		LabelScope scope;
		if((scope = futureLabels.get(label))!=null) {
			if(scope.backupStack != null) {
				if(jump)
					frameStack = scope.backupStack;
				else
					frameStack.merge(scope.backupStack);
			}
			futureLabels.remove(label);
		}
		else {
			scope = new LabelScope(label);
		}
		labelScopes.put(label, scope);
		currentScope.setNext(scope);
		currentScope = scope;
		currentScope.copyScope();
		jump = false;
	}

	@Override
	public void visitLineNumber(int line, Label start) {
		lineNumber = line;
	}

	@Override
	public void visitEnd() {
		if(doneHandler != null)
			doneHandler.handleDone(this);
	}
	
	//useless for now
	@Override
	public void visitAttribute(Attribute attr) {
		// do nothing
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		// do nothing
		return null;
	}
	
	@Override
	public AnnotationVisitor visitAnnotationDefault() {
		return null;
	}

	@Override
	public AnnotationVisitor visitParameterAnnotation(int arg0, String arg1,
			boolean arg2) {
		return null;
	}
	
	//Signature Visitor
	//not implementing for now
	
	public static interface DoneHandler {
		public void handleDone(FrameStackVisitor visitor);
	}
}
