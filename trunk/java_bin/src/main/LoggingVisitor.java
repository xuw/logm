package main;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import stacks.StackItem;
import stacks.branching.AltsFrameStack;
import stacks.branching.AltsItem;

/**
 * A visitor that keeps track of logging messages
 * 
 * @author Bruno
 */
public class LoggingVisitor extends StringBuilderVisitor {

	/** list of messages found in this method*/
	private List<StackItem> messages;
	/** list of recognized logging classes and methods*/
	private List<LoggingClass> loggers;
	
	/**recognizes System.out.println calls as a logging method */
	public final static LoggingClass sysOutRecognizer;
	/**recognizes the Java SDK logger methods as logging methods */
	public final static LoggingClass javaLoggerRecognizer;
	/**recognizes apache.commons.logging logging methods*/
	public final static LoggingClass apacheCommonsLoggerRecognizer;
	
	public static int messageCount = 0;
	
	static {
		sysOutRecognizer = new LoggingClass("java/io/PrintStream");
		sysOutRecognizer.addMethod("println", 0);
		
		javaLoggerRecognizer = new LoggingClass("java/util/logging/Logger");
		javaLoggerRecognizer.addMethod("log", 1);
		javaLoggerRecognizer.addMethod("logp", 3);
		javaLoggerRecognizer.addMethod("logrb", 4);
		
		apacheCommonsLoggerRecognizer = new LoggingVisitor.LoggingClass("org/apache/commons/logging/Log");
		apacheCommonsLoggerRecognizer.addMethod("fatal", 0);
		apacheCommonsLoggerRecognizer.addMethod("info", 0);
		apacheCommonsLoggerRecognizer.addMethod("debug", 0);
		apacheCommonsLoggerRecognizer.addMethod("warn", 0);
		apacheCommonsLoggerRecognizer.addMethod("error", 0);
		apacheCommonsLoggerRecognizer.addMethod("trace", 0);
	}
	
	public LoggingVisitor(DoneHandler handler) {
		super(StringBuilderFactory.getInstance(), new AltsFrameStack(), handler);
		messages = new ArrayList<StackItem>();			
		loggers = new ArrayList<LoggingClass>();
	}
	
	public LoggingVisitor() {
		super(StringBuilderFactory.getInstance(), new AltsFrameStack());
		messages = new ArrayList<StackItem>();			
		loggers = new ArrayList<LoggingClass>();
	}
	
	/**
	 * add a logging class to be recognized by this visitor
	 * 
	 * @param alogger
	 */
	public void addLoggingClass(LoggingClass alogger) {
		loggers.add(alogger);
	}
	
	public StackItem[] getMessages() {
		return messages.toArray(new StackItem[messages.size()]);
	}
	
	public void clearMessages() {
		messages.clear();
	}
	
	//Visit Class
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc,
			String signature, String[] exceptions) {
		messages.clear();
		return super.visitMethod(access, name, desc, signature, exceptions);
	}

	//Visit Method

	/**
	 *        INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC or
     *        INVOKEINTERFACE.
	 */
	@Override
	public void visitMethodInsn(int opcode, String owner, String name, 
			String desc) {
		for(LoggingClass alogger : loggers) {
			if(alogger.isThisLogger(owner)) { 
				int numArgs = Type.getArgumentTypes(desc).length;
				StackItem[] args = new StackItem[numArgs];
				for(int i = 0; i < numArgs; i++){
					args[i] = frameStack.pop();
				}

				StackItem message = alogger.checkForMessage(name, args);
				//if found a message add it to messages and clear stack
				if(message!=null) {	
					if(message instanceof AltsItem) {
						System.err.println("AltsItem!!!");
						for(StackItem i :message.getChildren()) {
							System.err.println(i.getClass() +" " + i);
							messages.add(i);
						}
						messageCount++;
					} else {
						messages.add(message);
						messageCount++;
					}
					frameStack.clear();
					return;
				}
				//if its not a message add args back to stack and let super deal with it
				for(int i = 0; i < numArgs; i++) {
					frameStack.push(args[i]);
				}
			}
		}
		super.visitMethodInsn(opcode, owner, name, desc);
	}

	/** 
	 * represents a class whose LoggingMethods are to be recognized 
	 * as writing to the log
	 * 
	 * @author Bruno
	 *
	 */
	public static class LoggingClass {
		private String desc;
		private List<LoggingMethod> methods;
		
		public LoggingClass(String type) {
			desc = type;
			methods = new ArrayList<LoggingMethod>();
		}
		
		/**
		 * adds a method to be recognized as writting to the log
		 * 
		 * @param name			name of the method to be recognized
		 * @param messageIndex	arg index of the log message
		 */
		public void addMethod(String name, int messageIndex) {
			methods.add(new LoggingMethod(name, messageIndex));
		}
		
		/**
		 * checks if the given class is an instance of this logger
		 * 
		 * @param 	owner	the class type to be checked
		 * @return	true if it is a logger of this type
		 */
		public boolean isThisLogger(String owner) {
			return owner.equals(desc);
		}
		
		/**
		 * checks if the given method is a logging method and returns the 
		 * associated string buffer StackItem if it is
		 * 
		 * @param name	the name of the method to be checked
		 * @param args	the argument array in the order popped from the stack
		 * @return		the stack item corresponding to the log message or null if not
		 * 				a logging method
		 */
		public StackItem checkForMessage(String name, StackItem[] args) {
			for(LoggingMethod method : methods) {
				if(method.isThisMethod(name)) {
					return args[method.messageIndex(args.length)];
				}
			}

			return null;
		}
		
		private static class LoggingMethod {
			public String name;
			public int messageArgIndex;
			
			public LoggingMethod(String n, int messageIndex) {
				name = n;
				messageArgIndex = messageIndex;
			}
			
			int messageIndex(int numArgs) {
				return numArgs-messageArgIndex-1;
			}
			
			public boolean isThisMethod(String name) {
				return this.name.equals(name);
			}
		}
	}
}
