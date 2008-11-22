package cpaplugin.cpa.cpas.interprocedural;

import cpaplugin.cfa.objectmodel.CFANode;

public class CallElement {

	private CFANode callerNode;
	private CFANode returnNode;

	public CallElement(CFANode callerNode, CFANode returnNode) {
		super();
		this.callerNode = callerNode;
		this.returnNode = returnNode;
	}
	public CFANode getCallerNode() {
		return callerNode;
	}
	public void setCallerNode(CFANode callerNode) {
		this.callerNode = callerNode;
	}
	public CFANode getReturnNode() {
		return returnNode;
	}
	public void setReturnNode(CFANode returnNode) {
		this.returnNode = returnNode;
	}
	
}
