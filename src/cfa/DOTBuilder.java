package cfa;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAErrorNode;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.AssumeEdge;
import cfa.objectmodel.c.FunctionCallEdge;
import cfa.objectmodel.c.ReturnEdge;

import cfa.DOTBuilderInterface;
import cfa.DOTNodeShapeWriter;
import cfa.DOTWriter;

public class DOTBuilder implements DOTBuilderInterface
{
	/**
	 * Class constructor.
	 */
	public DOTBuilder ()
	{

	}

	public void generateDOT (Collection<CFAFunctionDefinitionNode> cfasMapList, CFAFunctionDefinitionNode cfa, String fileName) throws IOException
	{
		Map<String, DOTWriter> subGraphWriters = new HashMap<String, DOTWriter>();
		DOTNodeShapeWriter nodeWriter = new DOTNodeShapeWriter();
		
		DOTWriter dw = new DOTWriter("____Main____Diagram__");
		subGraphWriters.put("____Main____Diagram__", dw);
		
		for(CFAFunctionDefinitionNode fnode:cfasMapList){
			dw = new DOTWriter(fnode.getFunctionName());
			subGraphWriters.put(fnode.getFunctionName(), dw);
		}

		PrintWriter writer = new PrintWriter (fileName);

		generateDotHelper (subGraphWriters, nodeWriter, cfa);

		writer.println ("digraph " + "CFA" + " {");

		writer.print(nodeWriter.getDot());
		writer.print("node [shape = circle];");
		writer.println();
		
		for(CFAFunctionDefinitionNode fnode:cfasMapList){
			dw = subGraphWriters.get(fnode.getFunctionName());
			writer.println ("subgraph cluster_" + fnode.getFunctionName() + " {");
			writer.println ("label = \"" + fnode.getFunctionName() + "()\";");
			writer.print(dw.getSubGraph());
			writer.println ("}");
		}

		dw = subGraphWriters.get("____Main____Diagram__");
		writer.print(dw.getSubGraph());
		writer.println ("}");
		writer.close ();
	}

	private void generateDotHelper (Map<String, DOTWriter> subGraphWriters, DOTNodeShapeWriter nodeWriter, CFAFunctionDefinitionNode cfa)
	{
		Set<CFANode> visitedNodes = new HashSet<CFANode> ();
		Deque<CFANode> waitingNodeList = new ArrayDeque<CFANode> ();
		Set<CFANode> waitingNodeSet = new HashSet<CFANode> ();

		waitingNodeList.add (cfa);
		waitingNodeSet.add (cfa);
		while (!waitingNodeList.isEmpty ())
		{
			CFANode node = waitingNodeList.poll ();
			waitingNodeSet.remove (node);

			visitedNodes.add (node);
			
			// AG - give a shape also to error nodes
			if (node instanceof CFAErrorNode) {
			    nodeWriter.add(node.getNodeNumber(), 
			            "tripleoctagon");
			}
			else if(node.isLoopStart()){
				nodeWriter.add(node.getNodeNumber(), "doublecircle");
			}

			int leavingEdgeCount = node.getNumLeavingEdges ();
			for (int edgeIdx = 0; edgeIdx < leavingEdgeCount; edgeIdx++)
			{
				CFAEdge edge = node.getLeavingEdge (edgeIdx);
				
				if(edge instanceof AssumeEdge){
					nodeWriter.add(node.getNodeNumber(), "diamond");
				}
				
				CFANode successor = edge.getSuccessor ();
				String line = "";

				if ((!visitedNodes.contains (successor)) && (!waitingNodeSet.contains (successor)))
				{
					waitingNodeList.add (successor);
					waitingNodeSet.add (successor);
				}

				line = line + node.getNodeNumber ();
				line = line + " -> ";
				line = line + successor.getNodeNumber ();
				line = line + " [label=\"" ;

				String edgeText = edge.getRawStatement ().replaceAll ("\\\"", "\\\\\\\"").replaceAll("\n", " ");
				line = line + edgeText;
				line = line + "\"];";
				DOTWriter dw;
				if((edge instanceof FunctionCallEdge && !((FunctionCallEdge)edge).isExternalCall()) || 
						edge instanceof ReturnEdge){
					dw = (DOTWriter)subGraphWriters.get("____Main____Diagram__");
				}
				else{
					dw = (DOTWriter)subGraphWriters.get(node.getFunctionName());
				}
				dw.add(line);
			}

			CFAEdge edge = node.getLeavingSummaryEdge();
			if(edge != null){
				CFANode successor = edge.getSuccessor ();
				String line = "";
				
				if ((!visitedNodes.contains (successor)) && (!waitingNodeSet.contains (successor)))
				{
					waitingNodeList.add (successor);
					waitingNodeSet.add (successor);
				}

				line = line + node.getNodeNumber ();
				line = line + " -> ";
				line = line + successor.getNodeNumber ();
				line = line + " [label=\"" ;

				String edgeText = edge.getRawStatement ().replaceAll ("\\\"", "\\\\\\\"").replaceAll("\n", " ");
				line = line + edgeText;
				line = line + "\" style=dotted arrowhead=empty];";
				DOTWriter dw = (DOTWriter)subGraphWriters.get(node.getFunctionName());
				dw.add(line);
			}
		}
	}
}
