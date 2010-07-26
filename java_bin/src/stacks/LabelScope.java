package stacks;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Label;

import stacks.items.LocalVarItem;

/**
 * Contains a list of local variables seen under this label
 * variables are updated at the end of each method with their
 * name and type.  Can store the next and previous LabelScope
 * to act somewhat like a doubly linked list.
 * 
 * @author Bruno
 */
public class LabelScope {
	public Label label;
	private List<LocalVarItem> localVars;
	public FrameStack backupStack;
	private LabelScope next;
	private LabelScope previous;
	static private StackItemFactory factory = StackItemFactory.getInstance();
	
	public LabelScope(Label l) {
		label = l;
		localVars = new ArrayList<LocalVarItem>();
		backupStack = null;
		next = previous = null;
	}
	
	/**
	 * Returns the variable at the specified index creating
	 * it if its the first time it is encountered under this
	 * label.
	 * 
	 * @param index	the local variable index
	 * @return the local variable at the specified index for this label
	 */
	public LocalVarItem getVar(int index) {
		if(localVars.size()<index+1 || 
				localVars.get(index)==null) {
			while(localVars.size() != index)
				localVars.add((LocalVarItem)factory.newLocalVar(localVars.size()-1));
			localVars.add((LocalVarItem)factory.newLocalVar(index));
		}
		return localVars.get(index);
	}
	
	/**
	 * copy the local variables from the previous scope and
	 * chop the last few
	 * 
	 * @param chop 	the number of variables to chop off the end
	 * 				of the variable list from the previous scope
	 */
	public void copyScope() {
		if(previous != null) {
			localVars.addAll(previous.localVars);
		}
	}
	
	public void chop(int chop) {
		for(int i = 0; i < chop; i++) {
			if(!localVars.isEmpty())
				localVars.remove(localVars.size()-1);
		}
	}
	
	public void clearScope() {
		localVars.clear();
	}
	
	/**
	 * sets the next LabelScope for this LabelScope and the previous
	 * LabelScope of the passed in LabelScope as this LabelScope. Should
	 * Only be used for adding to the end of a list.
	 * 
	 * @param scope  the LabelScope to set as the new end of the list
	 */
	public void setNext(LabelScope scope) {
		this.next = scope;
		scope.previous = this;
	}
	
	public LabelScope getNext() {
		return next;
	}
	
	public LabelScope getPrevious() {
		return previous;
	}
}
