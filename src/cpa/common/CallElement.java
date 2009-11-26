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

import cfa.objectmodel.CFANode;

import compositeCPA.CompositeElement;

public class CallElement {

	private final String functionName;
	private final CFANode callNode;
	private final CompositeElement state;

	public CallElement(String functionName, CFANode callNode,
			CompositeElement state) {
		this.functionName = functionName;
		this.callNode = callNode;
		this.state = state;
	}

	public CallElement(CallElement ce) {
		this.functionName = ce.getFunctionName();
		this.callNode = ce.getCallNode();
		this.state = ce.getState();
	}

	public String getFunctionName() {
		return functionName;
	}

	public CFANode getCallNode() {
		return callNode;
	}

	public CompositeElement getState() {
		return state;
	}

	@Override
	public boolean equals(Object other) {
	  if (other == this) {
	    return true;
	  }
	  if (other == null || !(other instanceof CallElement)) {
	    return false;
	  }
		CallElement otherCallElement = (CallElement) other;

		return otherCallElement.functionName.equals(this.functionName)
		    && otherCallElement.callNode.equals(this.callNode)
		    && otherCallElement.state.equals(this.state);
	}

	@Override
	public int hashCode() {
	  return functionName.hashCode() + 17 * callNode.hashCode();
	}
	
	public boolean isConsistent(String functionName) {
		return (functionName.equals(this.functionName));
	}

	public boolean isConsistent(CFANode node) {
		return(node.getNodeNumber() ==
			this.getCallNode().getLeavingSummaryEdge().getSuccessor().getNodeNumber());
	}

}
