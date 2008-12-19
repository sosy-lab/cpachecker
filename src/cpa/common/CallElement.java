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
import cpa.common.CompositeElement;
import cpa.common.interfaces.AbstractElement;
import cpa.common.CallElement;

public class CallElement {

	private String functionName;
	private CFANode callNode;
	private CompositeElement state;

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

	public void setFunctionName(String functionName) {
		this.functionName = functionName;
	}

	public CFANode getCallNode() {
		return callNode;
	}

	public void setCallNode(CFANode callNode) {
		this.callNode = callNode;
	}

	public CompositeElement getState() {
		return state;
	}

	public void setState(CompositeElement state) {
		this.state = state;
	}

	@Override
	public boolean equals(Object other){
		CallElement otherCallElement = (CallElement) other;

		if(!otherCallElement.functionName.equals(otherCallElement.functionName)){
			return false;
		}

		if(otherCallElement.callNode.getNodeNumber() != this.callNode.getNodeNumber()){
			return false;
		}

		CompositeElement thisCompElement = this.getState();
		CompositeElement otherCompElement = otherCallElement.getState();

		assert(thisCompElement.getNumberofElements() ==
			otherCompElement.getNumberofElements());

		for (int idx = 0; idx < thisCompElement.getNumberofElements(); idx++)
		{
			AbstractElement element1 = thisCompElement.get (idx);
			AbstractElement element2 = otherCompElement.get (idx);

			if (!element1.equals (element2))
				return false;
		}
		return true;
	}

	public boolean isConsistent(String functionName) {
		return (functionName.equals(this.functionName));
	}

	public boolean isConsistent(CFANode node) {
		return(node.getNodeNumber() ==
			this.getCallNode().getLeavingSummaryEdge().getSuccessor().getNodeNumber());
	}

}
