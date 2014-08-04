package org.sosy_lab.cpachecker.cfa;

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

import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;


/**
 * Helper class that collects all functions referenced by some CFAEdges,
 * not counting those that are called directly.
 * (Only functions that have their address taken (implicitly) are returned.)
 */
public class CReferencedFunctionsCollector {

  private final Set<String> collectedFunctions = new HashSet<>();
  private final CollectFunctionsVisitor collector = new CollectFunctionsVisitor(collectedFunctions);

  public Set<String> getCollectedFunctions() {
    return collectedFunctions;
  }

  public void visitEdge(CFAEdge edge) {
    switch (edge.getEdgeType()) {
    case AssumeEdge:
      CAssumeEdge assumeEdge = (CAssumeEdge)edge;
      assumeEdge.getExpression().accept(collector);
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
          init.accept(collector);
        }
      }
      break;
    case ReturnStatementEdge:
      CReturnStatementEdge returnEdge = (CReturnStatementEdge)edge;
      if (returnEdge.getExpression().isPresent()) {
        returnEdge.getExpression().get().accept(collector);
      }
      break;
    case StatementEdge:
      CStatementEdge statementEdge = (CStatementEdge)edge;
      statementEdge.getStatement().accept(collector);
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

  public void visitDeclaration(CVariableDeclaration decl) {
    if (decl.getInitializer() != null) {
      decl.getInitializer().accept(collector);
    }
  }

  private static class CollectFunctionsVisitor extends DefaultCExpressionVisitor<Void, RuntimeException>
                                               implements CRightHandSideVisitor<Void, RuntimeException>,
                                                          CStatementVisitor<Void, RuntimeException>,
                                                          CInitializerVisitor<Void, RuntimeException> {

    private final Set<String> collectedFunctions;

    public CollectFunctionsVisitor(Set<String> pCollectedVars) {
      collectedFunctions = pCollectedVars;
    }

    @Override
    public Void visit(CIdExpression pE) {
      if (pE.getExpressionType() instanceof CFunctionType) {
        collectedFunctions.add(pE.getName());
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
    public Void visit(CComplexCastExpression pE) {
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
      if (pE.getDeclaration() == null) {
        pE.getFunctionNameExpression().accept(this);
      } else {
        // skip regular function calls
      }

      for (CExpression param : pE.getParameterExpressions()) {
        param.accept(this);
      }
      return null;
    }

    @Override
    public Void visit(CUnaryExpression pE) {
      pE.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(CPointerExpression pE) {
      pE.getOperand().accept(this);
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

    @Override
    public Void visit(CExpressionStatement pIastExpressionStatement) throws RuntimeException {
      pIastExpressionStatement.getExpression().accept(this);
      return null;
    }

    @Override
    public Void visit(CExpressionAssignmentStatement pIastExpressionAssignmentStatement) throws RuntimeException {
      pIastExpressionAssignmentStatement.getLeftHandSide().accept(this);
      pIastExpressionAssignmentStatement.getRightHandSide().accept(this);
      return null;
    }

    @Override
    public Void visit(CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement) throws RuntimeException {
      pIastFunctionCallAssignmentStatement.getLeftHandSide().accept(this);
      pIastFunctionCallAssignmentStatement.getRightHandSide().accept(this);
      return null;
    }

    @Override
    public Void visit(CFunctionCallStatement pIastFunctionCallStatement) throws RuntimeException {
      pIastFunctionCallStatement.getFunctionCallExpression().accept(this);
      return null;
    }
  }
}
