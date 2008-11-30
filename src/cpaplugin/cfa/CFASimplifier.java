package cpaplugin.cfa;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.IASTExpression;

import cpaplugin.cfa.objectmodel.CFAEdge;
import cpaplugin.cfa.objectmodel.CFAEdgeType;
import cpaplugin.cfa.objectmodel.CFAFunctionDefinitionNode;
import cpaplugin.cfa.objectmodel.CFANode;
import cpaplugin.cfa.objectmodel.c.DeclarationEdge;
import cpaplugin.cfa.objectmodel.c.MultiDeclarationEdge;
import cpaplugin.cfa.objectmodel.c.MultiStatementEdge;
import cpaplugin.cfa.objectmodel.c.StatementEdge;
import cpaplugin.cmdline.CPAMain;

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

		if (CPAMain.cpaConfig.getBooleanValue("cfa.combineBlockStatements"))       
		{
			makeMultiStatement (node);
			makeMultiDeclaration (node);
		}

		if (CPAMain.cpaConfig.getBooleanValue("cfa.removeDeclarations"))       
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
