package org.sosy_lab.cpachecker.cfa;

/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;


/**
 * Helper class that collects all <code>ReferencedVariable</code>s in a given set of nodes.
 */
public class FunctionPointerVariablesCollector extends CFATraversal.DefaultCFAVisitor {

  private final Set<String> collectedVars = new HashSet<>();

  public static Set<String> collectVars(FunctionEntryNode initialNode) {
    // we use a worklist algorithm
    FunctionPointerVariablesCollector collector = new FunctionPointerVariablesCollector();
    CFATraversal.dfs().ignoreFunctionCalls().traverseOnce(initialNode, collector);
    return collector.collectedVars;
  }

  @Override
  public TraversalProcess visitEdge(CFAEdge edge) {
    switch (edge.getEdgeType()) {
    case AssumeEdge:
      CAssumeEdge assumeEdge = (CAssumeEdge)edge;
      collectVars(assumeEdge.getExpression(), collectedVars);
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
        CInitializer init = ((CVariableDeclaration)declaration).getInitializer();
        if (init != null) {
          init.accept(new CollectVariablesVisitor(collectedVars));
        }
      }
      break;
    case ReturnStatementEdge:
      CReturnStatementEdge returnEdge = (CReturnStatementEdge)edge;
      if (returnEdge.getExpression()!=null) {
        collectVars(returnEdge.getExpression(), collectedVars);
      }
      break;
    case StatementEdge:
      CStatementEdge statementEdge = (CStatementEdge)edge;
      CStatement s = statementEdge.getStatement();
      if (s instanceof CAssignment) {
        CAssignment assignment = (CAssignment)s;
        collectVars(assignment.getLeftHandSide().getExpression(), collectedVars);
        collectVars(assignment.getRightHandSide(), collectedVars);
      } else if (s instanceof CExpressionStatement) {
        CExpressionStatement expr = (CExpressionStatement)s;
        collectVars(expr.getExpression(), collectedVars);
      } else if (s instanceof CFunctionCallStatement) {
        CFunctionCallStatement call = (CFunctionCallStatement)s;
        collectVars(call.getFunctionCallExpression(), collectedVars);
      }
      break;
    case MultiEdge:
      //TODO
      assert false;
      break;
    default:
      assert false;
      break;
    }

    return TraversalProcess.CONTINUE;
  }

  private static void collectVars(CRightHandSide pNode, Set<String> pCollectedVars) {
    pNode.accept(new CollectVariablesVisitor(pCollectedVars));
  }

  private static class CollectVariablesVisitor extends DefaultCExpressionVisitor<Void, RuntimeException>
                                               implements CRightHandSideVisitor<Void, RuntimeException>,
                                                           CInitializerVisitor<Void, RuntimeException> {

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

    @Override
    public Void visit(CInitializerExpression pInitializerExpression) throws RuntimeException {
      pInitializerExpression.getExpression().accept(this);
      return null;
    }

    @Override
    public Void visit(CInitializerList pInitializerList) throws RuntimeException {
      for (CInitializer init : pInitializerList.getInitializers()) {
        init.accept(this);
      }
      return null;
    }

    @Override
    public Void visit(CDesignatedInitializer pCStructInitializerPart) throws RuntimeException {
      pCStructInitializerPart.getRightHandSide().accept(this);
      return null;
    }
  }
}
