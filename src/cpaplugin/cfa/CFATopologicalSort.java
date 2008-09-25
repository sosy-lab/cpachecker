package cpaplugin.cfa;

import java.util.ArrayList;

import cpaplugin.cfa.objectmodel.CFANode;

public class CFATopologicalSort {

	ArrayList<Integer> visited;
	private int topSortId = 0;

	public CFATopologicalSort() {
		visited = new ArrayList<Integer>();
	}

	public void topologicalSort(CFANode node){

		visited.add(node.getNodeNumber());
		for(int i=0; i<node.getNumLeavingEdges(); i++){
			CFANode successor = node.getLeavingEdge(i).getSuccessor();
			if(!visited.contains(successor.getNodeNumber())){
				topologicalSort(successor);
			}
		}
		node.setTopologicalSortId(topSortId);
		topSortId++;
	}
}
