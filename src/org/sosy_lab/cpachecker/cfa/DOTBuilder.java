/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionReturnEdge;

import com.google.common.base.Joiner;

/**
 * Class for generating a DOT file from a CFA.
 */
public final class DOTBuilder {

  private DOTBuilder() { /* utility class */ }

	private static final String MAIN_GRAPH = "____Main____Diagram__";
  private static final Joiner JOINER_ON_NEWLINE = Joiner.on('\n');


  private static String formatNode(CFANode node, String shape) {
    return "node [shape = " + shape + "]; " + node.getNodeNumber() + ";";
  }

  public static String generateDOT(Collection<CFAFunctionDefinitionNode> cfasMapList, CFAFunctionDefinitionNode cfa) {
		Map<String, List<String>> subGraphs = new HashMap<String, List<String>>();
		List<String> nodeWriter = new ArrayList<String>();

		subGraphs.put(MAIN_GRAPH, new ArrayList<String>());

		for(CFAFunctionDefinitionNode fnode:cfasMapList){
			subGraphs.put(fnode.getFunctionName(), new ArrayList<String>());
		}

		generateDotHelper (subGraphs, nodeWriter, cfa);

		StringBuilder sb = new StringBuilder();
		sb.append("digraph " + "CFA" + " {\n");

		JOINER_ON_NEWLINE.appendTo(sb, nodeWriter);
		sb.append('\n');
		sb.append("node [shape = circle];\n");

		for (CFAFunctionDefinitionNode fnode : cfasMapList) {
			sb.append("subgraph cluster_" + fnode.getFunctionName() + " {\n");
			sb.append("label = \"" + fnode.getFunctionName() + "()\";\n");
			JOINER_ON_NEWLINE.appendTo(sb, subGraphs.get(fnode.getFunctionName()));
			sb.append("}\n");
		}

    JOINER_ON_NEWLINE.appendTo(sb, subGraphs.get(MAIN_GRAPH));
		sb.append("}");
		return sb.toString();
	}

	private static void generateDotHelper(Map<String, List<String>> subGraphWriters, List<String> nodeWriter, CFAFunctionDefinitionNode cfa) {
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

			if(node.isLoopStart()){
				nodeWriter.add(formatNode(node, "doublecircle"));
			}

			int leavingEdgeCount = node.getNumLeavingEdges ();
			for (int edgeIdx = 0; edgeIdx < leavingEdgeCount; edgeIdx++)
			{
				CFAEdge edge = node.getLeavingEdge (edgeIdx);

				if(edge instanceof AssumeEdge){
					nodeWriter.add(formatNode(node, "diamond"));
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
				List<String> graph;
				if ((edge instanceof FunctionCallEdge) || edge instanceof FunctionReturnEdge){
					graph = subGraphWriters.get(MAIN_GRAPH);
				}
				else{
					graph = subGraphWriters.get(node.getFunctionName());
				}
				graph.add(line);
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
				List<String> graph = subGraphWriters.get(node.getFunctionName());
				graph.add(line);
			}
		}
	}
}
