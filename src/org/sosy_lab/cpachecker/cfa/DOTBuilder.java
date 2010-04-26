/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAErrorNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.ReturnEdge;

import com.google.common.base.Joiner;

/**
 * Class for generating a DOT file from a CFA.
 */
public class DOTBuilder {

	private static final String MAIN_GRAPH = "____Main____Diagram__";
  private static final Joiner JOINER_ON_NEWLINE = Joiner.on('\n');

  private static class ShapePair {
    private final int nodeNumber;
    private final String shape;

    private ShapePair(int nodeNo, String shape){
      this.nodeNumber = nodeNo;
      this.shape = shape;
    }

    @Override
    public String toString() {
      return "node [shape = " + shape + "]; " + nodeNumber + ";";
    }
  }

  private static class DOTNodeShapeWriter extends ArrayList<ShapePair> {

    private static final long serialVersionUID = -595748260228384806L;

    public void add(int no, String shape){
      add(new ShapePair(no, shape));
    }

    public String getDot(){
      return JOINER_ON_NEWLINE.join(this);
    }

  }

  private static class DOTWriter extends ArrayList<String> {

    private static final long serialVersionUID = -3086512411642445646L;

    public String getSubGraph(){
      return JOINER_ON_NEWLINE.join(this);
    }
  }

  public void generateDOT (Collection<CFAFunctionDefinitionNode> cfasMapList, CFAFunctionDefinitionNode cfa, File fileName) throws IOException
	{
		Map<String, DOTWriter> subGraphWriters = new HashMap<String, DOTWriter>();
		DOTNodeShapeWriter nodeWriter = new DOTNodeShapeWriter();

		DOTWriter dw = new DOTWriter();
		subGraphWriters.put(MAIN_GRAPH, dw);

		for(CFAFunctionDefinitionNode fnode:cfasMapList){
			dw = new DOTWriter();
			subGraphWriters.put(fnode.getFunctionName(), dw);
		}

		generateDotHelper (subGraphWriters, nodeWriter, cfa);

    PrintWriter writer = new PrintWriter(fileName);
		writer.println ("digraph " + "CFA" + " {");

		writer.println(nodeWriter.getDot());
		writer.println("node [shape = circle];");

		for(CFAFunctionDefinitionNode fnode:cfasMapList){
			dw = subGraphWriters.get(fnode.getFunctionName());
			writer.println ("subgraph cluster_" + fnode.getFunctionName() + " {");
			writer.println ("label = \"" + fnode.getFunctionName() + "()\";");
			writer.print(dw.getSubGraph());
			writer.println ("}");
		}

		dw = subGraphWriters.get(MAIN_GRAPH);
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

				//the first call to replaceAll replaces \" with \ " to prevent a bug in dotty.
				//future updates of dotty may make this obsolete.
				String edgeText = edge.getRawStatement().replaceAll("\\Q\\\"\\E", "\\ \"")
				                                        .replaceAll ("\\\"", "\\\\\\\"")
				                                        .replaceAll("\n", " ");

				line = line + edgeText;
				line = line + "\"];";
				DOTWriter dw;
				if((edge instanceof FunctionCallEdge && !((FunctionCallEdge)edge).isExternalCall()) ||
						edge instanceof ReturnEdge){
					dw = subGraphWriters.get(MAIN_GRAPH);
				}
				else{
					dw = subGraphWriters.get(node.getFunctionName());
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
				DOTWriter dw = subGraphWriters.get(node.getFunctionName());
				dw.add(line);
			}
		}
	}
}
