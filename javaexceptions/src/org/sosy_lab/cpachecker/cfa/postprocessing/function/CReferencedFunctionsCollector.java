// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.function;

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
import org.sosy_lab.cpachecker.exceptions.NoException;

/**
 * Helper class that collects all functions referenced by some CFAEdges, not counting those that are
 * called directly. (Only functions that have their address taken (implicitly) are returned.)
 */
class CReferencedFunctionsCollector {

  final Set<String> collectedFunctions = new HashSet<>();
  protected CollectFunctionsVisitor collector;

  public CReferencedFunctionsCollector() {
    collector = new CollectFunctionsVisitor(collectedFunctions);
  }

  public Set<String> getCollectedFunctions() {
    return collectedFunctions;
  }

  public void visitEdge(CFAEdge edge) {
    switch (edge.getEdgeType()) {
      case AssumeEdge:
        CAssumeEdge assumeEdge = (CAssumeEdge) edge;
        assumeEdge.getExpression().accept(collector);
        break;
      case BlankEdge:
        // nothing to do
        break;
      case DeclarationEdge:
        CDeclaration declaration = ((CDeclarationEdge) edge).getDeclaration();
        if (declaration instanceof CVariableDeclaration) {
          visitDeclaration((CVariableDeclaration) declaration);
        }
        break;
      case ReturnStatementEdge:
        CReturnStatementEdge returnEdge = (CReturnStatementEdge) edge;
        if (returnEdge.getExpression().isPresent()) {
          returnEdge.getExpression().orElseThrow().accept(collector);
        }
        break;
      case StatementEdge:
        CStatementEdge statementEdge = (CStatementEdge) edge;
        statementEdge.getStatement().accept(collector);
        break;

      default:
        throw new AssertionError();
    }
  }

  public void visitDeclaration(CVariableDeclaration decl) {
    if (decl.getInitializer() != null) {
      decl.getInitializer().accept(collector);
    }
  }

  static class CollectFunctionsVisitor extends DefaultCExpressionVisitor<Void, NoException>
      implements CRightHandSideVisitor<Void, NoException>,
          CStatementVisitor<Void, NoException>,
          CInitializerVisitor<Void, RuntimeException> {

    final Set<String> collectedFunctions;

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
    public Void visit(CInitializerExpression pInitializerExpression) {
      pInitializerExpression.getExpression().accept(this);
      return null;
    }

    @Override
    public Void visit(CInitializerList pInitializerList) {
      for (CInitializer init : pInitializerList.getInitializers()) {
        init.accept(this);
      }
      return null;
    }

    @Override
    public Void visit(CDesignatedInitializer pCStructInitializerPart) {
      pCStructInitializerPart.getRightHandSide().accept(this);
      return null;
    }

    @Override
    public Void visit(CExpressionStatement pIastExpressionStatement) {
      pIastExpressionStatement.getExpression().accept(this);
      return null;
    }

    @Override
    public Void visit(CExpressionAssignmentStatement pIastExpressionAssignmentStatement) {
      pIastExpressionAssignmentStatement.getLeftHandSide().accept(this);
      pIastExpressionAssignmentStatement.getRightHandSide().accept(this);
      return null;
    }

    @Override
    public Void visit(CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement) {
      pIastFunctionCallAssignmentStatement.getLeftHandSide().accept(this);
      pIastFunctionCallAssignmentStatement.getRightHandSide().accept(this);
      return null;
    }

    @Override
    public Void visit(CFunctionCallStatement pIastFunctionCallStatement) {
      pIastFunctionCallStatement.getFunctionCallExpression().accept(this);
      return null;
    }
  }
}
