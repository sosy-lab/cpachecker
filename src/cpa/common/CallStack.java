package cpa.common;

import java.util.Iterator;
import java.util.Stack;

import cpa.common.CallElement;
import cpa.common.CallStack;

public class CallStack {
	
	private Stack<CallElement> stack;
	
	public CallStack(){
		stack = new Stack<CallElement>();
	}
	
	public CallStack(CallStack otherCallStack){
		stack = otherCallStack.stack;
	}

	public Stack<CallElement> getStack() {
		return stack;
	}

	public void setStack(Stack<CallElement> stack) {
		this.stack = stack;
	}

	public CallElement push(CallElement item){
		return stack.push(item);
	}
	
	public CallElement pop(){
		return stack.pop();
	}
	
	public int getSize(){
		return stack.size();
	}
	
	@Override
	public CallStack clone(){
		CallStack ret = new CallStack();
		for(CallElement item:stack){
			ret.push(item);
		}
		return ret;
	}
	
	@Override
	public boolean equals(Object other){
		CallStack otherCs = (CallStack) other;
		if(this.getSize() != otherCs.getSize()){
			return false;
		}
		Iterator<CallElement> thisStackIt = this.stack.iterator();
		Iterator<CallElement> otherStackIt = otherCs.stack.iterator();
		while(thisStackIt.hasNext()){
			CallElement thisCallElement = thisStackIt.next();
			CallElement otherCallElement = otherStackIt.next();
			if(!thisCallElement.equals(otherCallElement)){
				return false;
			}
		}
		return true;
	}

	public CallElement peek() {
		return stack.peek();
	}
	
	public CallElement getSecondTopElement(){
		assert(getStack().size() >= 2);
		return stack.get(stack.size()-2);
	}
}
