package main;
import java.util.List;

import org.objectweb.asm.Type;

import stacks.StackItem;
import stacks.Visit;
import stacks.FrameStackVisitor;
import stacks.items.*;


public class Main {
	
	static int cnt = 0;
	
	public static void main(final String[] args) throws Exception {
        if (args.length < 1 || args.length > 2) {
        	System.out.println("bad number of arguments");
            return;
        }
        
        setConverters();
        
        HierchyVisitor hVisitor = new HierchyVisitor();
        LoggingVisitor lVisitor = new LoggingVisitor( 
        	new FrameStackVisitor.DoneHandler() {
				@Override
				public void handleDone(FrameStackVisitor visitor) {
					LoggingVisitor lvisitor = (LoggingVisitor) visitor;
					for(StackItem m : lvisitor.getMessages()){
						System.out.println(m.getClass().getSimpleName());
						if(m instanceof MessageItem) {
							cnt +=1;
						}
						System.out.println("template:: " + m.toString());
						//TODO: handle each message here
					}
					lvisitor.clearMessages();
				}
        	}
        );
        lVisitor.addLoggingClass(LoggingVisitor.apacheCommonsLoggerRecognizer);

    	Visit.visit(hVisitor, args[0]);
        //HierchyVisitor.classes.get("java/lang/Object").printChildren();
        
        Visit.visit(lVisitor, args[0]);
  
        System.out.println(LoggingVisitor.messageCount);
        System.out.println(cnt);
    }
	
	private static void setConverters() {
		ArrayIndexItem.converter = new ArrayIndexItem.Converter() {
			@Override
			public Object convert(StackItem array, StackItem index) {
				// TODO Auto-generated method stub
				return null;
			}
        };
        
        ArrayLengthItem.converter = new ArrayLengthItem.Converter() {
			@Override
			public Object convert(StackItem array) {
				// TODO Auto-generated method stub
				return null;
			}
        };
        
        ArthOpItem.converter = new ArthOpItem.Converter() {
			@Override
			public Object convert(StackItem operand1, StackItem operand2,
					String operator) {
				// TODO Auto-generated method stub
				return null;
			}
        };
        
        CastItem.converter = new CastItem.Converter() {
			@Override
			public Object convert(StackItem object) {
				// TODO Auto-generated method stub
				return null;
			}
        };
        
        ConstItem.converter = new ConstItem.Converter() {
			@Override
			public Object convert(Object value) {
				// TODO Auto-generated method stub
				return null;
			}
        };
        
        FieldItem.converter = new FieldItem.Converter() {
			@Override
			public Object convert(String owner, String field, StackItem object) {
				// TODO Auto-generated method stub
				return null;
			}
        };
        
        InstanceOfItem.converter = new InstanceOfItem.Converter() {
			@Override
			public Object convert(StackItem object) {
				// TODO Auto-generated method stub
				return null;
			}
        };
        
        LocalVarItem.converter = new LocalVarItem.Converter() {
			@Override
			public Object convert(String name) {
				// TODO Auto-generated method stub
				return null;
			}
        };
        
        MethodItem.converter = new MethodItem.Converter() {
			@Override
			public Object convert(String owner, String name, Type[] argTypes,
					List<StackItem> args, StackItem object) {
				// TODO Auto-generated method stub
				return null;
			}
        };
        
        ReferenceItem.converter = new ReferenceItem.Converter() {
			@Override
			public Object convert() {
				// TODO Auto-generated method stub
				return null;
			}
        };
        
        MessageItem.converter = new MessageItem.Converter() {
			@Override
			public Object convert(List<StackItem> message) {
				// TODO Auto-generated method stub
				return null;
			}
        };
	}
}
