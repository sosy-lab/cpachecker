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
