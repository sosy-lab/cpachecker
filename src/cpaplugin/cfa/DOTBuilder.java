package cpaplugin.cfa;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.CFANode;

public class DOTBuilder
{
	public DOTBuilder ()
	{

	}

	public void generateDOT (CFAFunctionDefinitionNode cfa, String fileName) throws IOException
	{
		PrintWriter writer = new PrintWriter (fileName);

		writer.println ("digraph " + cfa.getFunctionName () + " {");
		generateDotHelper (writer, cfa);
		writer.println ("}");
		writer.close ();
	}

	private void generateDotHelper (PrintWriter writer, CFAFunctionDefinitionNode cfa) throws IOException
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

			int leavingEdgeCount = node.getNumLeavingEdges ();
			for (int edgeIdx = 0; edgeIdx < leavingEdgeCount; edgeIdx++)
			{
				CFAEdge edge = node.getLeavingEdge (edgeIdx);
				CFANode successor = edge.getSuccessor ();

				if ((!visitedNodes.contains (successor)) && (!waitingNodeSet.contains (successor)))
				{
					waitingNodeList.add (successor);
					waitingNodeSet.add (successor);
				}

				writer.print (node.getNodeNumber ());
				writer.print (" -> ");
				writer.print (successor.getNodeNumber ());
				writer.print (" [label=\"");

				String edgeText = edge.getRawStatement ().replaceAll ("\\\"", "\\\\\\\"");
				writer.print (edgeText);
				writer.println ("\"];");
			}
		}
	}
}
