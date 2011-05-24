package de.upb.agw.cpachecker.cpa.abm.sa;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.IASTExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;

import de.upb.agw.cpachecker.cpa.abm.util.ReferencedVariable;

/**
 * Helper class that collects all <code>ReferencedVariable</code>s in a given set of nodes.
 * @author dwonisch
 *
 */
public class ReferencedVariablesCollector {
  Set<String> globalVars = new HashSet<String>();
  
  public ReferencedVariablesCollector(Collection<CFANode> mainNodes) {
    collectVars(mainNodes);
  }
  
  public Set<ReferencedVariable> collectVars(Collection<CFANode> nodes) {
    Set<ReferencedVariable> collectedVars = new HashSet<ReferencedVariable>();
    
    for(CFANode node : nodes) {
      for(int i = 0; i < node.getNumLeavingEdges(); i++) {
        CFAEdge leavingEdge = node.getLeavingEdge(i);
        if(nodes.contains(leavingEdge.getSuccessor())) {
          collectVars(leavingEdge, collectedVars);      
        }
      }
    }
    
    return collectedVars;
  }
  
  private void collectVars(CFAEdge edge, Set<ReferencedVariable> pCollectedVars) {
    String currentFunction = edge.getPredecessor().getFunctionName();    
    switch(edge.getEdgeType()) {
    case AssumeEdge:
      AssumeEdge assumeEdge = (AssumeEdge)edge;
      collectVars(currentFunction, assumeEdge.getExpression(), null, pCollectedVars);
      break;
    case BlankEdge:
      //nothing to do
      break;
    case CallToReturnEdge:
      //nothing to do
      break;
    case DeclarationEdge:
      DeclarationEdge declarationEdge = (DeclarationEdge)edge;
      boolean isGlobal = declarationEdge.isGlobal();      
      String varName = declarationEdge.getName().getRawSignature();
      if(isGlobal) {
        globalVars.add(varName);
      }
      //putVariable(currentFunction, varName, pCollectedVars);      
      break;
    case FunctionCallEdge:      
      break;
    case ReturnStatementEdge:
      break;
    case StatementEdge:
      StatementEdge statementEdge = (StatementEdge)edge;
      if(statementEdge.getStatement() instanceof IASTExpressionAssignmentStatement) {
        IASTExpressionAssignmentStatement assignment = (IASTExpressionAssignmentStatement) statementEdge.getStatement();
        String lhsVarName = assignment.getLeftHandSide().getRawSignature();
        ReferencedVariable lhsVar = scoped(new ReferencedVariable(lhsVarName, false, true, null), currentFunction);
        pCollectedVars.add(lhsVar);
      
        collectVars(currentFunction, assignment.getRightHandSide(), lhsVar, pCollectedVars);
      }
      else {
        IASTFunctionCallAssignmentStatement assignment = (IASTFunctionCallAssignmentStatement) statementEdge.getStatement();
        String lhsVarName = assignment.getLeftHandSide().getRawSignature();
        ReferencedVariable lhsVar = scoped(new ReferencedVariable(lhsVarName, false, true, null), currentFunction);
        pCollectedVars.add(lhsVar);
      
        collectVars(currentFunction, assignment.getRightHandSide(), lhsVar, pCollectedVars);
      }
      break;    
    }
  }

  private void collectVars(String pCurrentFunction, IASTNode pNode, ReferencedVariable lhsVar, Set<ReferencedVariable> pCollectedVars) {    
    if(pNode instanceof IASTIdExpression) {
      IASTIdExpression idExpression = (IASTIdExpression)pNode;
      putVariable(pCurrentFunction, new String(idExpression.getName().getRawSignature()), lhsVar, pCollectedVars);
    }
    else {
      if(pNode != null && pNode.getChildren() != null) {
        for(IASTNode node : pNode.getChildren()) {
          collectVars(pCurrentFunction, node, lhsVar, pCollectedVars);
        }
      }
    }    
  }  
  
  private void putVariable(String function, String var, ReferencedVariable lhsVar, Set<ReferencedVariable> pCollectedVars) {
    if(lhsVar == null) {
      pCollectedVars.add(scoped(new ReferencedVariable(var, true, false, null), function));
    }
    else {
      pCollectedVars.add(scoped(new ReferencedVariable(var, false, false, lhsVar), function));
    }
    
  }
  
  private ReferencedVariable scoped(ReferencedVariable var, String function) {
    if (globalVars.contains(var.getName())) {
      return var;
    } else {
      return new ReferencedVariable(function + "::" + var, var.occursInCondition(), var.occursOnLhs(), var.getLhsVariable());
    }
  }
}
