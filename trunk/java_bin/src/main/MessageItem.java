package main;


import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Type;

import stacks.ImmutableStackItem;
import stacks.StackItem;
import stacks.StackItemConverter;
import stacks.branching.AltsItem;
import stacks.items.ConstItem;

/**
 * A StackItem that represents  a string. Contains a linked list of
 * StackItems that compromise the pieces of the string
 * 
 * @author Bruno
 */
public class MessageItem extends ImmutableStackItem {
	private final List<StackItem> message;
	
	public static Converter converter = null;
	
	public MessageItem() {
		message = new ArrayList<StackItem>();
	}
	
	private MessageItem(MessageItem m) {
		message = new ArrayList<StackItem>();
		message.addAll(m.message);
	}
	
	public MessageItem addToMessage(ImmutableStackItem i) {
		MessageItem r = new MessageItem(this);
		r.message.add(i);
		return r;
	}
	
	public StackItem addToMessage(AltsItem item) {
		AltsItem.Builder builder = new AltsItem.Builder();
		for(StackItem child : item.getChildren()) {
			builder.addAlt(this.addChild(child));
		}
		return builder.build();
	}
	
	@Override
	public StackItem addChild(StackItem item) {
		if(item instanceof AltsItem) {
			return addToMessage((AltsItem) item);
		} else if(item instanceof ImmutableStackItem) {
			return addToMessage((ImmutableStackItem) item);
		}
		return this;
	}
	
	public String toString() {
		StringBuffer template = new StringBuffer();
		StringBuffer vars = new StringBuffer();
		
		//String buffer = "";
		for(StackItem item : message) {
			if(item instanceof ConstItem) {
				template.append(item);
			} else {
				template.append("@#@");
				vars.append(item).append(";;");
			}
			//buffer += item.toString();
		}
		return "template {\n" + template.toString() +"\n" + vars.toString() +"\n}\n";
	}
	

	@Override
	public Type getType() {
		return Type.getType(String.class);
	}

	@Override
	public StackItem[] getChildren() {
		return message.toArray(new StackItem[message.size()-1]);
	}
	
	@Override
	public Object convert() {
		if(converter != null)
			return converter.convert(message);
		else
			return null;
	}

	public static interface Converter extends StackItemConverter {
		public Object convert(List<StackItem> message);
	}
}
