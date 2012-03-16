/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.explicit;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.DefaultExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.IASTArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTAssignment;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.RightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.blocks.ReferencedVariable;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;


/**
 * Helper class that collects all "relevant" variables in a given set of nodes, where "relevant" means,
 * that they either appear on the left hand side of an assignment or within an assume edge.
 */
public class AssignedVariablesCollector {
  Set<String> globalVars = new HashSet<String>();

  public AssignedVariablesCollector() {
  }

  public Multimap<CFAEdge, ReferencedVariable> collectVars(Collection<CFAEdge> edges) {
    Multimap<CFAEdge, ReferencedVariable> collectedVars = HashMultimap.create();

    for(CFAEdge edge : edges) {
      collectVars(edge, collectedVars);
    }

    return collectedVars;
  }

  private void collectVars(CFAEdge edge, Multimap<CFAEdge, ReferencedVariable> pCollectedVars) {
    String currentFunction = edge.getPredecessor().getFunctionName();

    switch(edge.getEdgeType()) {
    case BlankEdge:
    case CallToReturnEdge:
    case FunctionCallEdge:
    case ReturnStatementEdge:
      //nothing to do
      break;

    case DeclarationEdge:
      IASTDeclaration declaration = ((DeclarationEdge)edge).getDeclaration();
      if(declaration.getName() != null && declaration.isGlobal()) {
        globalVars.add(declaration.getName());
        pCollectedVars.put(edge, new ReferencedVariable(declaration.getName(), false, false, null));
      }
      break;

    case AssumeEdge:
      AssumeEdge assumeEdge = (AssumeEdge)edge;
      collectVars(currentFunction, assumeEdge, assumeEdge.getExpression(), null, pCollectedVars);
      break;

    case StatementEdge:
      StatementEdge statementEdge = (StatementEdge)edge;
      if (statementEdge.getStatement() instanceof IASTAssignment) {
        IASTAssignment assignment = (IASTAssignment)statementEdge.getStatement();
        String lhsVarName = assignment.getLeftHandSide().toASTString();
        ReferencedVariable lhsVar = scoped(new ReferencedVariable(lhsVarName, false, true, null), currentFunction);
        pCollectedVars.put(edge, lhsVar);
      }
      break;
    }
  }

  private void collectVars(String pCurrentFunction, CFAEdge edge, IASTRightHandSide pNode, ReferencedVariable lhsVar, Multimap<CFAEdge, ReferencedVariable> pCollectedVars) {
    pNode.accept(new CollectVariablesVisitor(edge, pCurrentFunction, lhsVar, pCollectedVars));
  }

  private class CollectVariablesVisitor extends DefaultExpressionVisitor<Void, RuntimeException>
                                               implements RightHandSideVisitor<Void, RuntimeException> {

    private final CFAEdge currentEdge;
    private final String currentFunction;
    private final ReferencedVariable lhsVar;
    private final Multimap<CFAEdge, ReferencedVariable> collectedVars;


    public CollectVariablesVisitor(CFAEdge edge, String pCurrentFunction,
        ReferencedVariable pLhsVar, Multimap<CFAEdge, ReferencedVariable> pCollectedVars) {
      currentEdge     = edge;
      currentFunction = pCurrentFunction;
      lhsVar          = pLhsVar;
      collectedVars   = pCollectedVars;
    }

    private void collectVar(String var) {
      if(lhsVar == null) {
        collectedVars.put(currentEdge, scoped(new ReferencedVariable(var, true, false, null), currentFunction));
      }
      else {
        collectedVars.put(currentEdge, scoped(new ReferencedVariable(var, false, false, lhsVar), currentFunction));
      }
    }

    @Override
    public Void visit(IASTIdExpression pE) {
      collectVar(pE.getName());
      return null;
    }

    @Override
    public Void visit(IASTArraySubscriptExpression pE) {
      collectVar(pE.toASTString());
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
      collectVar(pE.toASTString());
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
      UnaryOperator op = pE.getOperator();

      switch(op) {
      case AMPER:
      case STAR:
        collectVar(pE.toASTString());
      default:
        pE.getOperand().accept(this);
      }

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
