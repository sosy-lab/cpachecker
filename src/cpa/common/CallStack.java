/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
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
	
	public boolean stacksContextEqual(CallStack other){
    if(this.getSize() != other.getSize()){
      return false;
    }
    Iterator<CallElement> thisStackIt = this.stack.iterator();
    Iterator<CallElement> otherStackIt = other.stack.iterator();
    while(thisStackIt.hasNext()){
      CallElement thisCallElement = thisStackIt.next();
      CallElement otherCallElement = otherStackIt.next();
      if(!thisCallElement.areContextEqual(otherCallElement)){
        return false;
      }
    }
    return true;
  
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
