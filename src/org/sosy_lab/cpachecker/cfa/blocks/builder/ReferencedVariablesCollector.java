/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.blocks.builder;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.blocks.ReferencedVariable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;


/**
 * Helper class that collects all <code>ReferencedVariable</code>s in a given set of nodes.
 */
public class ReferencedVariablesCollector {
  Set<String> globalVars = new HashSet<>();

  public ReferencedVariablesCollector(Collection<CFANode> mainNodes) {
    collectVars(mainNodes);
  }

  public Set<ReferencedVariable> collectVars(Collection<CFANode> nodes) {
    Set<ReferencedVariable> collectedVars = new HashSet<>();

    for (CFANode node : nodes) {
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        CFAEdge leavingEdge = node.getLeavingEdge(i);
        if (nodes.contains(leavingEdge.getSuccessor()) || (leavingEdge instanceof CFunctionCallEdge)) {
          collectVars(leavingEdge, collectedVars);
        }
      }
    }

    return collectedVars;
  }

  private void collectVars(CFAEdge edge, Set<ReferencedVariable> pCollectedVars) {
    String currentFunction = edge.getPredecessor().getFunctionName();

    switch (edge.getEdgeType()) {
    case AssumeEdge:
      CAssumeEdge assumeEdge = (CAssumeEdge)edge;
      collectVars(currentFunction, assumeEdge.getExpression(), null, pCollectedVars);
      break;
    case BlankEdge:
      //nothing to do
      break;
    case CallToReturnEdge:
      //nothing to do
      break;
    case DeclarationEdge:
      CDeclaration declaration = ((CDeclarationEdge)edge).getDeclaration();
      boolean isGlobal = declaration.isGlobal();
      String varName = declaration.getName();
      if (isGlobal) {
        globalVars.add(varName);
      }
      //putVariable(currentFunction, varName, pCollectedVars);
      break;
    case FunctionCallEdge:
      CFunctionCallEdge functionCallEdge = (CFunctionCallEdge)edge;
      for (CExpression argument : functionCallEdge.getArguments()) {
        collectVars(currentFunction, argument, null, pCollectedVars);
      }
      break;
    case ReturnStatementEdge:
      break;
    case StatementEdge:
      CStatementEdge statementEdge = (CStatementEdge)edge;
      if (statementEdge.getStatement() instanceof CAssignment) {
        CAssignment assignment = (CAssignment)statementEdge.getStatement();
        String lhsVarName = assignment.getLeftHandSide().toASTString();
        ReferencedVariable lhsVar = scoped(new ReferencedVariable(lhsVarName, false, true, null), currentFunction);
        pCollectedVars.add(lhsVar);

        collectVars(currentFunction, assignment.getRightHandSide(), lhsVar, pCollectedVars);
      } else {
        // other statements are considered side-effect free, ignore variable occurrences in them
      }
      break;
    }
  }

  private void collectVars(String pCurrentFunction, CRightHandSide pNode, ReferencedVariable lhsVar, Set<ReferencedVariable> pCollectedVars) {
    pNode.accept(new CollectVariablesVisitor(pCurrentFunction, lhsVar, pCollectedVars));
  }

  private class CollectVariablesVisitor extends DefaultCExpressionVisitor<Void, RuntimeException>
                                               implements CRightHandSideVisitor<Void, RuntimeException> {

    private final String currentFunction;
    private final ReferencedVariable lhsVar;
    private final Set<ReferencedVariable> collectedVars;

    public CollectVariablesVisitor(String pCurrentFunction,
        ReferencedVariable pLhsVar, Set<ReferencedVariable> pCollectedVars) {
      currentFunction = pCurrentFunction;
      lhsVar = pLhsVar;
      collectedVars = pCollectedVars;
    }

    private void collectVar(String var) {
      if (lhsVar == null) {
        collectedVars.add(scoped(new ReferencedVariable(var, true, false, null), currentFunction));
      } else {
        collectedVars.add(scoped(new ReferencedVariable(var, false, false, lhsVar), currentFunction));
      }
    }

    @Override
    public Void visit(CIdExpression pE) {
      collectVar(pE.getName());
      return null;
    }

    @Override
    public Void visit(CArraySubscriptExpression pE) {
      collectVar(pE.toASTString());
      pE.getArrayExpression().accept(this);
      pE.getSubscriptExpression().accept(this);
      return null;
    }

    @Override
    public Void visit(CBinaryExpression pE) {
      pE.getOperand1().accept(this);
      pE.getOperand2().accept(this);
      return null;
    }

    @Override
    public Void visit(CCastExpression pE) {
      pE.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(CComplexCastExpression pE) {
      pE.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(CFieldReference pE) {
      collectVar(pE.toASTString());
      pE.getFieldOwner().accept(this);
      return null;
    }

    @Override
    public Void visit(CFunctionCallExpression pE) {
      pE.getFunctionNameExpression().accept(this);
      for (CExpression param : pE.getParameterExpressions()) {
        param.accept(this);
      }
      return null;
    }

    @Override
    public Void visit(CUnaryExpression pE) {
      UnaryOperator op = pE.getOperator();

      switch (op) {
      case AMPER:
        collectVar(pE.toASTString());
        //$FALL-THROUGH$
      default:
        pE.getOperand().accept(this);
      }


      return null;
    }

    @Override
    public Void visit(CPointerExpression pE) {
      collectVar(pE.toASTString());
      pE.getOperand().accept(this);
      return null;
    }

    @Override
    protected Void visitDefault(CExpression pExp) {
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
