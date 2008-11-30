package cfa;

import java.util.ArrayList;
import java.util.List;

public class DOTNodeShapeWriter {

	private List<ShapePair> shapedNodes;

	public DOTNodeShapeWriter() {
		shapedNodes = new ArrayList<ShapePair>();
	}
	
	public void add(int no, String shape){
		ShapePair sp = new ShapePair(no, shape);
		if(!shapedNodes.contains(sp)){
			shapedNodes.add(sp);
		}
	}
	
	public String getDot(){
		String s = "";
		for(ShapePair sp:shapedNodes){
			s = s + "node [shape = " + sp.shape + "]; " + sp.nodeNumber + ";\n";
		}
		return s;
	}
	
	public class ShapePair {
		public int nodeNumber;
		String shape;

		private ShapePair(int nodeNo, String shape){
			nodeNumber = nodeNo;
			this.shape = shape;
		}
	}
}
