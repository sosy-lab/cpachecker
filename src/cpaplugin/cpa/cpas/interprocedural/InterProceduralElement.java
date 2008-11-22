package cpaplugin.cpa.cpas.interprocedural;

import java.util.Stack;

import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cpa.common.interfaces.AbstractElement;

public class InterProceduralElement implements AbstractElement
{
	private Stack<CallElement> callStack;
	
	public InterProceduralElement ()
	{
		this.callStack = new Stack<CallElement>();
	}

	public InterProceduralElement (Stack<CallElement> stack)
	{
		this.callStack = stack;
	}

	public Stack<CallElement> getCallStack ()
	{
		return callStack;
	}

	public boolean equals (Object other)
	{
		if (!(other instanceof InterProceduralElement)){
			return false;
		}
		
		InterProceduralElement ipe = (InterProceduralElement) other;
		if (ipe.getStackSize() != this.getStackSize()){
			return false;
		}

		for(int i=0; i<this.getStackSize(); i++){
			if(!this.callStack.get(i).equals(ipe.callStack.get(i))){
				return false;
			}
		}
		return true;
	}

//	public boolean containsCallElement (CallElement elem){
//		return (this.callStack.contains(elem));
//	}

//	public boolean containsCall(String fName){
//		Iterator<CallElement> it = this.callStack.iterator();
//		while(it.hasNext()){
//			CallElement ce = it.next();
//			if(ce.getFunctionName().compareTo(fName) == 0){
//				return true;
//			}
//		}
//		return false;
//	}

	public String toString ()
	{
		String s = "Call Stack: \n ============ \n";
		for(CallElement ce:callStack){
			s = s + ce + "\n";
		}
		return s;
	}
	
	public int getStackSize(){
		return this.callStack.size();
	}
	
	public InterProceduralElement clone(){
		Stack<CallElement> stack = new Stack<CallElement>();
		for(CallElement ce:this.callStack){
			stack.add(ce);
		}
		return new InterProceduralElement(stack);
	}
	
	public void push(CallElement ce){
		this.callStack.push(ce);
	}
	
	public CallElement peek(){
		return this.callStack.peek();
	}
	
	public CallElement pop(){
		return this.callStack.pop();
	}
	
//	
//	public CallElement getCallElement(String pfName) {
//		for(CallElement ce:callStack){
//			if(elem.getFunctionName().compareTo(pfName) == 0){
//				return elem;
//			}
//		}
//		return null;
//	}

//	public void removeCallElement(String pfName) {
//		this.callStack.remove(getCallElement(pfName));
//	}
}
