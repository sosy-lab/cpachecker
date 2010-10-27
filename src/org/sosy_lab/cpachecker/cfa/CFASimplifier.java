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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.BlankEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

/**
 * Used to simplify CPA by removing declarations
 * @author erkan
 */
public class CFASimplifier {

  private final boolean removeDeclarations;

	public CFASimplifier(boolean removeDeclarations) {
	  this.removeDeclarations = removeDeclarations;
	}

	/**
	 * Run the simplification algorithm on a given CFA. Uses BFS approach.
	 * @param cfa CFA to be simplified
	 */
	public void simplify (CFAFunctionDefinitionNode cfa)
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
			}

	    if (removeDeclarations) {
	      removeDeclarations(node);
	    }
	  }
	}

	/**
	 * Removes declaration edges when cfa.removeDeclarations is set
	 * to true.
	 * @param node
	 */
	private void removeDeclarations(CFANode node) {

		if (node.getNumLeavingEdges() != 1) {
			return;
		}

		CFAEdge leavingEdge = node.getLeavingEdge(0);
		if (leavingEdge.getEdgeType() != CFAEdgeType.DeclarationEdge) {
			return;
		}
    CFANode successor = leavingEdge.getSuccessor ();

		node.removeLeavingEdge(leavingEdge);
		successor.removeEnteringEdge(leavingEdge);

		BlankEdge be = new BlankEdge("removed declaration", leavingEdge.getLineNumber(), node, successor);
		be.addToCFA(null);
	}
}
