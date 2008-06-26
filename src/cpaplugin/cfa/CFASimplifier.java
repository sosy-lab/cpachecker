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

public class CFASimplifier
{
    private boolean combineBlockStatements;

    public CFASimplifier (boolean combineBlockStatements)
    {
        this.combineBlockStatements = combineBlockStatements;
    }

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

    private void simplifyNode (CFANode cfa)
    {
        // Remove leading blank edge if useless
        removeUselessBlankEdge (cfa);

        if (combineBlockStatements)       
        {
            makeMultiStatement (cfa);
            makeMultiDeclaration (cfa);
        }
    }

    private void removeUselessBlankEdge (CFANode cfa)
    {
        if (cfa.getNumEnteringEdges () != 1 || cfa.getNumLeavingEdges () != 1)
            return;
        
        CFAEdge enteringEdge = cfa.getEnteringEdge (0);
        CFAEdge leavingEdge = cfa.getLeavingEdge (0);
        
        if ((leavingEdge.getEdgeType () != CFAEdgeType.BlankEdge) || !("".equals (leavingEdge.getRawStatement ())))
            return;
        
        CFANode successor = leavingEdge.getSuccessor ();
        
        successor.removeEnteringEdge (leavingEdge);
        cfa.removeEnteringEdge (enteringEdge);
        cfa.removeLeavingEdge (leavingEdge);
        
        enteringEdge.setSuccessor (successor);
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
