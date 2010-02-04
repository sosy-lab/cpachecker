/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package cfa;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;

import cpa.common.CPAchecker;


import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAEdgeType;
import cfa.objectmodel.CFAFunctionDefinitionNode;
import cfa.objectmodel.CFANode;
import cfa.objectmodel.c.DeclarationEdge;
import cfa.objectmodel.c.MultiDeclarationEdge;
import cfa.objectmodel.c.MultiStatementEdge;
import cfa.objectmodel.c.StatementEdge;


/**
 * Used to simplify CPA by removing blank edges and combining block statements
 * @author erkan
 *
 */
public class CFASimplifier
{
	/**
	 * Class constructor.
	 */
	public CFASimplifier ()
	{

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

			// The actual simplification part
			simplifyNode (node);
		}
	}

	/**
	 * Takes the node and run simplifications on it.
	 * @param node This node's edges will be simplified
	 */
	private void simplifyNode (CFANode node)
	{
		// Remove leading blank edge if useless
		removeUselessBlankEdge (node);

		if (CPAchecker.config.getBooleanValue("cfa.combineBlockStatements"))
		{
			makeMultiStatement (node);
			makeMultiDeclaration (node);
		}

		if (CPAchecker.config.getBooleanValue("cfa.removeDeclarations"))
		{
			removeDeclarations(node);
		}
	}

	/**
	 * Removes declaration edges when cfa.combineBlockStatements is set
	 * to true.
	 * @param node
	 */
	private void removeDeclarations(CFANode node) {
		int numOfEnteringEdges = node.getNumEnteringEdges();

		if (numOfEnteringEdges == 0 || node.getNumLeavingEdges () != 1)
			return;

		CFAEdge leavingEdge = node.getLeavingEdge (0);
		CFANode successor = leavingEdge.getSuccessor ();

		if (leavingEdge.getEdgeType () != CFAEdgeType.DeclarationEdge)
			return;

		for(int idx=0; idx<numOfEnteringEdges; idx++){
			CFAEdge enteringEdge = node.getEnteringEdge (0);
			successor.removeEnteringEdge (leavingEdge);
			node.removeEnteringEdge (enteringEdge);
			node.removeLeavingEdge (leavingEdge);
			enteringEdge.setSuccessor (successor);
		}
	}

	/**
	 * Removes blank edges.
	 * @param node
	 */
	private void removeUselessBlankEdge (CFANode node)
	{
		int numOfEnteringEdges = node.getNumEnteringEdges();

		if (numOfEnteringEdges == 0 || node.getNumLeavingEdges () != 1)
			return;

		CFAEdge leavingEdge = node.getLeavingEdge (0);
		CFANode successor = leavingEdge.getSuccessor ();

		if ((leavingEdge.getEdgeType () != CFAEdgeType.BlankEdge) || !("".equals (leavingEdge.getRawStatement ())))
			return;

		for(int idx=0; idx<numOfEnteringEdges; idx++){
			CFAEdge enteringEdge = node.getEnteringEdge (0);
			successor.removeEnteringEdge (leavingEdge);
			node.removeEnteringEdge (enteringEdge);
			node.removeLeavingEdge (leavingEdge);
			enteringEdge.setSuccessor (successor);
		}
	}

	private void makeMultiStatement (CFANode cfa)
	{
		if ((cfa.getNumEnteringEdges () != 1) || (cfa.getNumLeavingEdges () != 1) || (cfa.hasJumpEdgeLeaving ()))
			return;

		CFAEdge leavingEdge = cfa.getLeavingEdge (0);
		if (leavingEdge.getEdgeType () != CFAEdgeType.StatementEdge)
			return;

		StatementEdge leavingStatementEdge = (StatementEdge) leavingEdge;

		CFAEdge enteringEdge = cfa.getEnteringEdge (0);
		if (enteringEdge.getEdgeType () == CFAEdgeType.StatementEdge)
		{
			List<IASTExpression> expressions = new ArrayList<IASTExpression> ();
			expressions.add (((StatementEdge)enteringEdge).getExpression ());
			expressions.add (leavingStatementEdge.getExpression ());

			CFANode priorNode = enteringEdge.getPredecessor ();
			CFANode afterNode = leavingEdge.getSuccessor ();

			priorNode.removeLeavingEdge (enteringEdge);
			afterNode.removeEnteringEdge (leavingEdge);

			MultiStatementEdge msEdge = new MultiStatementEdge ("multi-statement edge", expressions);
			msEdge.initialize (priorNode, afterNode);
		}
		else if (enteringEdge.getEdgeType () == CFAEdgeType.MultiStatementEdge)
		{
			MultiStatementEdge msEdge = (MultiStatementEdge) enteringEdge;
			List<IASTExpression> expressions = msEdge.getExpressions ();
			expressions.add (leavingStatementEdge.getExpression ());

			CFANode afterNode = leavingEdge.getSuccessor ();
			afterNode.removeEnteringEdge (leavingEdge);

			msEdge.setSuccessor (afterNode);
		}
	}

	private void makeMultiDeclaration (CFANode cfa)
	{
		if ((cfa.getNumEnteringEdges () != 1) || (cfa.getNumLeavingEdges () != 1) || (cfa.hasJumpEdgeLeaving ()))
			return;

		CFAEdge leavingEdge = cfa.getLeavingEdge (0);
		if (leavingEdge.getEdgeType () != CFAEdgeType.DeclarationEdge)
			return;

		DeclarationEdge leavingDeclarationEdge = (DeclarationEdge) leavingEdge;

		CFAEdge enteringEdge = cfa.getEnteringEdge (0);
		if (enteringEdge.getEdgeType () == CFAEdgeType.DeclarationEdge)
		{
			List<IASTDeclarator[]> declarators = new ArrayList<IASTDeclarator[]> ();
			List<String> rawStatements = new ArrayList<String> ();

			declarators.add (((DeclarationEdge)enteringEdge).getDeclarators ());
			declarators.add (leavingDeclarationEdge.getDeclarators ());

			rawStatements.add (enteringEdge.getRawStatement ());
			rawStatements.add (leavingDeclarationEdge.getRawStatement ());

			CFANode priorNode = enteringEdge.getPredecessor ();
			CFANode afterNode = leavingEdge.getSuccessor ();

			priorNode.removeLeavingEdge (enteringEdge);
			afterNode.removeEnteringEdge (leavingEdge);

			MultiDeclarationEdge mdEdge = new MultiDeclarationEdge ("multi-declaration edge", declarators, rawStatements);
			mdEdge.initialize (priorNode, afterNode);
		}
		else if (enteringEdge.getEdgeType () == CFAEdgeType.MultiDeclarationEdge)
		{
			MultiDeclarationEdge mdEdge = (MultiDeclarationEdge) enteringEdge;

			List<IASTDeclarator[]> declarators = mdEdge.getDeclarators ();
			declarators.add (leavingDeclarationEdge.getDeclarators ());

			List<String> rawStatements = mdEdge.getRawStatements ();
			rawStatements.add (leavingDeclarationEdge.getRawStatement ());

			CFANode afterNode = leavingEdge.getSuccessor ();
			afterNode.removeEnteringEdge (leavingEdge);

			mdEdge.setSuccessor (afterNode);
		}
	}
}
