package cpa.octagon;

import java.util.HashMap;

import cpa.location.LocationElement;

public class OctWideningControl {

	HashMap<Integer, LoopNode> loopNodeList = new HashMap<Integer, LoopNode>();

	public class LoopNode{
		@SuppressWarnings("unused")
		private int nodeId;
		private int iterationCount = 0;
		private boolean isWideningUsed = false;

		public LoopNode(int id){
			nodeId = id;
		}

		public LoopNode() {
			// TODO Auto-generated constructor stub
		}

		public void incrementIteration(){
			iterationCount++;
		}

		public boolean exceedThreshold(){
			return iterationCount > OctConstants.wideningThreshold;
		}

		public boolean isWideningUsed(){
			if(isWideningUsed) {
				return true;
			}
			else {
				incrementIteration();
				if(exceedThreshold()){
					switchToWideningUsed();
				}
			}
			return isWideningUsed;
		}

		public void switchToWideningUsed(){
			isWideningUsed = true;
		}
	}

	public boolean isWideningUsed(LocationElement le){
		int nodeId = le.getLocationNode().getNodeNumber();
		Integer nodeIdIntObj = new Integer(nodeId);
		LoopNode ln = new LoopNode();
		if(loopNodeList.containsKey(nodeIdIntObj)){
			ln = loopNodeList.get(nodeIdIntObj);
			return ln.isWideningUsed();
		}
		else{
			ln = new LoopNode(nodeIdIntObj);
			loopNodeList.put(nodeIdIntObj, ln);
			return 	ln.isWideningUsed();
		}
	}
}
