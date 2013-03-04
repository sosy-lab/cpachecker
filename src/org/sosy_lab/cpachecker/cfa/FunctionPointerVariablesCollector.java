package org.sosy_lab.cpachecker.cfa;

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

import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;


/**
 * Helper class that collects all <code>ReferencedVariable</code>s in a given set of nodes.
 */
public class FunctionPointerVariablesCollector {

  public static Set<String> collectVars(FunctionEntryNode initialNode) {
    // we use a worklist algorithm
    Deque<CFANode> workList = new ArrayDeque<>();
    Set<CFANode> processed = new HashSet<>();

    workList.addLast(initialNode);
    Set<String> collectedVars = new HashSet<>();


    while (!workList.isEmpty()) {
      CFANode node = workList.pollFirst();
      if (!processed.add(node)) {
        // already handled
        continue;
      }

      for (CFAEdge edge : leavingEdges(node).toList()) {
        collectVars(edge, collectedVars);

        // if successor node is not on a different CFA, add it to the worklist
        CFANode successorNode = edge.getSuccessor();
        if (node.getFunctionName().equals(successorNode.getFunctionName())) {
          workList.add(successorNode);
        }
      }
    }

    return collectedVars;
  }

  private static void collectVars(CFAEdge edge, Set<String> pCollectedVars) {
    switch (edge.getEdgeType()) {
    case AssumeEdge:
      CAssumeEdge assumeEdge = (CAssumeEdge)edge;
      collectVars(assumeEdge.getExpression(), pCollectedVars);
      break;
    case BlankEdge:
      //nothing to do
      break;
    case CallToReturnEdge:
      //nothing to do
      assert false;
      break;
    case DeclarationEdge:
      CDeclaration declaration = ((CDeclarationEdge)edge).getDeclaration();
      if (declaration instanceof CVariableDeclaration) {
        //CVariableDeclaration v = (CVariableDeclaration)declaration;
        //TODO?:collectVars(v.getInitializer(), pCollectedVars);
      }
      break;
    case FunctionCallEdge:
      assert false;
      //for (CExpression argument : functionCallEdge.getArguments()) {
      //  collectVars(argument, pCollectedVars);
      //}
      break;
    case ReturnStatementEdge:
      CReturnStatementEdge returnEdge = (CReturnStatementEdge)edge;
      if (returnEdge.getExpression()!=null) {
        collectVars(returnEdge.getExpression(), pCollectedVars);
      }
      break;
    case StatementEdge:
      CStatementEdge statementEdge = (CStatementEdge)edge;
      CStatement s = statementEdge.getStatement();
      if (s instanceof CAssignment) {
        CAssignment assignment = (CAssignment)s;
        collectVars(assignment.getLeftHandSide(), pCollectedVars);
        collectVars(assignment.getRightHandSide(), pCollectedVars);
      } else if (s instanceof CExpressionStatement) {
        CExpressionStatement expr = (CExpressionStatement)s;
        collectVars(expr.getExpression(), pCollectedVars);
      } else if (s instanceof CFunctionCallStatement) {
        CFunctionCallStatement call = (CFunctionCallStatement)s;
        collectVars(call.getFunctionCallExpression(), pCollectedVars);
      }
      break;
    case FunctionReturnEdge:
      //TODO
      assert false;
      break;
    case MultiEdge:
      //TODO
      assert false;
      break;
    default:
      assert false;
      break;
    }
  }

  private static void collectVars(CRightHandSide pNode, Set<String> pCollectedVars) {
    pNode.accept(new CollectVariablesVisitor(pCollectedVars));
  }

  private static class CollectVariablesVisitor extends DefaultCExpressionVisitor<Void, RuntimeException>
                                               implements CRightHandSideVisitor<Void, RuntimeException> {

    private final Set<String> collectedVars;

    public CollectVariablesVisitor(Set<String> pCollectedVars) {
      collectedVars = pCollectedVars;
    }

    private void collectVar(String var) {
      collectedVars.add(var);
    }

    @Override
    public Void visit(CIdExpression pE) {
      if (pE.getExpressionType() instanceof CFunctionType) {
        collectVar(pE.getName());
      }
      return null;
    }

    @Override
    public Void visit(CArraySubscriptExpression pE) {
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
    public Void visit(CFieldReference pE) {
      pE.getFieldOwner().accept(this);
      return null;
    }

    @Override
    public Void visit(CFunctionCallExpression pE) {

      if (CFASecondPassBuilderComplete.isRegularCall(pE)) {
        //skip regular calls;
      } else {
        pE.getFunctionNameExpression().accept(this);
      }
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
      case STAR:
        pE.getOperand().accept(this);
        break;
      default:
        pE.getOperand().accept(this);
      }


      return null;
    }

    @Override
    protected Void visitDefault(CExpression pExp) {
      return null;
    }
  }
}
