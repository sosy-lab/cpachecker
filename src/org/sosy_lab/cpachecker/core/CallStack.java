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
package org.sosy_lab.cpachecker.core;

import java.util.ArrayList;

public class CallStack implements Cloneable {

	private final ArrayList<CallElement> stack;

	public CallStack(){
		stack = new ArrayList<CallElement>();
	}

	public void push(CallElement item){
		stack.add(item);
	}

	public CallElement pop(){
		return stack.remove(stack.size()-1);
	}

	public int size(){
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
	  if (other == this) {
	    return true;
	  }
	  if (other == null || !(other instanceof CallStack)) {
	    return false;
	  }
		CallStack otherCs = (CallStack) other;
		return stack.equals(otherCs.stack);
	}

	@Override
	public int hashCode() {
	  return stack.hashCode();
	}
	
	public CallElement peek() {
		return stack.get(stack.size()-1);
	}

	public CallElement getSecondTopElement(){
		assert(stack.size() >= 2);
		return stack.get(stack.size()-2);
	}
}
