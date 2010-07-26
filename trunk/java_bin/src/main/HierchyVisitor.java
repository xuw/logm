package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import stacks.StackItem;
import stacks.branching.AltsFrameStack;

/**
 * A visitor that uses ClassNode to keep track
 * of class relationships
 * 
 * @author Bruno
 */
public class HierchyVisitor extends StringBuilderVisitor {
	ClassNode thisClass;
	
	public HierchyVisitor(DoneHandler handler) {
		super(StringBuilderFactory.getInstance(), new AltsFrameStack(), handler);
	}
	
	public HierchyVisitor() {
		super(StringBuilderFactory.getInstance(), new AltsFrameStack());
	}
	
	/**
	 * hash of all classes with the internal class name as keys
	 */
	public static Map<String, ClassNode> classes;
	static {
		classes = new HashMap<String, ClassNode>();
		classes.put("java/lang/Object", new ClassNode("java/lang/Object"));
	}
	
	@Override
	public void visit(int version, int access, String name, String signature,
			String superName, String[] interfaces) {
		//check if node for this class already exists and add super to it
		if(classes.containsKey(name)) { 
			thisClass = classes.get(name);
			thisClass.setSuper(superName);
		}
		else {  //otherwise create it
			thisClass = new ClassNode(name, superName);
			classes.put(name, thisClass);
		}
		System.out.println("Hierachy:::  name: "+name+", sig: "+signature+", super: "+superName);
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		//only interested in toStringMethods
		if(name.equals("toString")) {
			//System.out.println("In method "+name);
			super.visitMethod(access, name, desc, signature, exceptions);
			return this;
		}
		return null;
	}
	
	@Override
	public void visitInsn(int opcode) {
		if(opcode== ARETURN) { //set return value as to class string representation
			thisClass.setToStringReturn(frameStack.pop());
		}
		else
			super.visitInsn(opcode);
	}
	
	/**
	 * Stores class relationship information as a tree and string 
	 * representations as a StackItem
	 * Also stores all classes in a lookup table by their internal name
	 * 
	 * @author Bruno
	 */
	public static class ClassNode {
		public Type type;
		private ClassNode baseClass;
		private List<ClassNode> subClasses;
		/** toString method return value of this class */
		public StackItem stringRep;
		/** the external name of this class */
		public String name;
		/** the internal name of this class */
		public String internalName;

		public ClassNode(String n) {
			subClasses = new ArrayList<ClassNode>();;
			internalName = "L"+n+";";
			type = Type.getObjectType(internalName);
			name = n;	
			stringRep = null;
			baseClass = null;
		}

		public ClassNode(String n, String superName) {
			subClasses = new ArrayList<ClassNode>();
			internalName = "L"+n+";";
			type = Type.getObjectType(internalName);
			name = n;	
			stringRep = null;
			this.setSuper(superName);
		}

		/**
		 * sets the super class for this class, creating a ClassNode for
		 * it if it doesn't exist
		 * 
		 * @param superName	the external name of the super class
		 */
		public void setSuper(String superName) {
			if(classes.containsKey(superName))
				baseClass = classes.get(superName);
			else {
				baseClass = new ClassNode(superName);
				classes.put(superName, baseClass);
			}

			baseClass.addSubClass(this);
		}

		/**
		 * adds a subclass to this class
		 * 
		 * @param subClass	ClassNode representing the subclass
		 */
		public void addSubClass(ClassNode subClass) {
			subClasses.add(subClass);
		}

		/**
		 * set the string representation class
		 * @param message	a StackItem representing the return values of the class' to
		 * 					string method
		 */
		public void setToStringReturn(StackItem message) {
			if(message != null)
				stringRep = message;
		}

		public String toString() {
			String buf = name;
			if(baseClass !=null) {
				buf += " extends "+baseClass.name;
			}
			
			buf +="; has children: ";
			for(ClassNode classNode : subClasses){
				buf += classNode.name+", ";
			}

			buf += " toString = "+ stringRep;
			return buf;
		}

		public void printChildren() {
			for(ClassNode child : subClasses) {
				System.out.println(child.toString());
				child.printChildren();
			}
		}
		
		public ClassNode[] getChildren() {
			return subClasses.toArray(new ClassNode[subClasses.size()]);
		}
	}
}
