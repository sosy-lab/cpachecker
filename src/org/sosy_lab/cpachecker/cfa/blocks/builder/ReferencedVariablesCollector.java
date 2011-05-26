package org.sosy_lab.cpachecker.cfa.blocks.builder;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.DefaultExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.IASTArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTAssignment;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.RightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.blocks.ReferencedVariable;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;


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
      String varName = declarationEdge.getName();
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
      if (statementEdge.getStatement() instanceof IASTAssignment) {
        IASTAssignment assignment = (IASTAssignment)statementEdge.getStatement();
        String lhsVarName = assignment.getLeftHandSide().getRawSignature();
        ReferencedVariable lhsVar = scoped(new ReferencedVariable(lhsVarName, false, true, null), currentFunction);
        pCollectedVars.add(lhsVar);
      
        collectVars(currentFunction, assignment.getRightHandSide(), lhsVar, pCollectedVars);
      } else {
        // other statements are considered side-effect free, ignore variable occurrences in them
      }
      break;    
    }
  }

  private void collectVars(String pCurrentFunction, IASTRightHandSide pNode, ReferencedVariable lhsVar, Set<ReferencedVariable> pCollectedVars) {    
    pNode.accept(new CollectVariablesVisitor(pCurrentFunction, lhsVar, pCollectedVars)); 
  }  
  
  private class CollectVariablesVisitor extends DefaultExpressionVisitor<Void, RuntimeException>
                                               implements RightHandSideVisitor<Void, RuntimeException> {
    
    private final String currentFunction;
    private final ReferencedVariable lhsVar;
    private final Set<ReferencedVariable> collectedVars;
    
    public CollectVariablesVisitor(String pCurrentFunction,
        ReferencedVariable pLhsVar, Set<ReferencedVariable> pCollectedVars) {
      currentFunction = pCurrentFunction;
      lhsVar = pLhsVar;
      collectedVars = pCollectedVars;
    }

    @Override
    public Void visit(IASTIdExpression pE) {
      String var = pE.getName();
      if(lhsVar == null) {
        collectedVars.add(scoped(new ReferencedVariable(var, true, false, null), currentFunction));
      }
      else {
        collectedVars.add(scoped(new ReferencedVariable(var, false, false, lhsVar), currentFunction));
      }
      return null;
    }
    
    @Override
    public Void visit(IASTArraySubscriptExpression pE) {
      pE.getArrayExpression().accept(this);
      pE.getSubscriptExpression().accept(this);
      return null;
    }
    
    @Override
    public Void visit(IASTBinaryExpression pE) {
      pE.getOperand1().accept(this);
      pE.getOperand2().accept(this);
      return null;
    }
    
    @Override
    public Void visit(IASTCastExpression pE) {
      pE.getOperand().accept(this);
      return null;
    }
    
    @Override
    public Void visit(IASTFieldReference pE) {
      pE.getFieldOwner().accept(this);
      return null;
    }
    
    @Override
    public Void visit(IASTFunctionCallExpression pE) {
      pE.getFunctionNameExpression().accept(this);
      for (IASTExpression param : pE.getParameterExpressions()) {
        param.accept(this);
      }
      return null;
    }

    @Override
    public Void visit(IASTUnaryExpression pE) {
      pE.getOperand().accept(this);
      return null;
    }
    
    @Override
    protected Void visitDefault(IASTExpression pExp) {
      return null;
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
