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
package org.sosy_lab.cpachecker.cpa.fsmbdd;

import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cpa.fsmbdd.interfaces.DomainIntervalProvider;


public class FsmSyntaxAnalizer implements DomainIntervalProvider{

  private final Map<CExpression,Integer> literalIndexMap;

//  public Multiset<String> extractIdExpressionFrquency(CFA pCfa) throws UnsupportedCCodeException {
//    final Multiset<String> result = HashMultiset.create();
//    final DefaultCExpressionVisitor<Void, UnsupportedCCodeException> visitor = new DefaultCExpressionVisitor<Void, UnsupportedCCodeException>() {
//      @Override
//      protected Void visitDefault(CExpression pExp) throws UnsupportedCCodeException {
//        return null;
//      }
//
//      @Override
//      public Void visit(CIdExpression pE) throws UnsupportedCCodeException {
//        result.add(pE.getName());
//        return null;
//      }
//    };
//
//    CStatementVisitor<Void, UnsupportedCCodeException> stmtVisitor = new CStatementVisitor<Void, UnsupportedCCodeException>() {
//
//      @Override
//      public Void visit(CFunctionCallStatement pIastFunctionCallStatement) throws UnsupportedCCodeException {
//
//        return null;
//      }
//
//      @Override
//      public Void visit(CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement) throws UnsupportedCCodeException {
//        return null;
//      }
//
//      @Override
//      public Void visit(CExpressionAssignmentStatement pIastExpressionAssignmentStatement) throws UnsupportedCCodeException {
//        pIastExpressionAssignmentStatement.getLeftHandSide().accept(visitor);
//        pIastExpressionAssignmentStatement.getRightHandSide().accept(visitor);
//        return null;
//      }
//
//      @Override
//      public Void visit(CExpressionStatement pIastExpressionStatement) throws UnsupportedCCodeException {
//        pIastExpressionStatement.getExpression().accept(visitor);
//        return null;
//      }
//    };
//
//
//    for (CFANode n: pCfa.getAllNodes()) {
//      for (CFAEdge e: CFAUtils.leavingEdges(n)) {
//        switch (e.getEdgeType()) {
//        case AssumeEdge:
//          CAssumeEdge assumeEdge = (CAssumeEdge) e;
//          assumeEdge.getExpression().accept(visitor);
//          break;
//        case DeclarationEdge:
//          CDeclarationEdge declEdge = (CDeclarationEdge) e;
//          result.add(declEdge.getDeclaration().getName());
//          break;
//        case StatementEdge:
//          CStatementEdge stmtEdge = (CStatementEdge) e;
//          stmtEdge.getStatement().accept(stmtVisitor);
//          break;
//        case ReturnStatementEdge:
//          CReturnStatementEdge retEdge = (CReturnStatementEdge) e;
//          retEdge.getExpression().accept(visitor);
//          break;
//        default:
//          throw new UnsupportedCCodeException("Edge not supported: " + e.getEdgeType(), e);
//        }
//      }
//    }
//
//    return result;
//  }
//
//  public Set<String> extractLiterals(CFA pCfa) {
//    final Set<String> result = new HashSet<String>();
//    final DefaultCExpressionVisitor<Void, UnsupportedCCodeException> visitor = new DefaultCExpressionVisitor<Void, UnsupportedCCodeException>() {
//      @Override
//      protected Void visitDefault(CExpression pExp) throws UnsupportedCCodeException {
//        return null;
//      }
//
//      @Override
//      public Void visit(CCharLiteralExpression pE) throws UnsupportedCCodeException {
//        return super.visit(pE);
//      }
//
//      @Override
//      public Void visit(CIntegerLiteralExpression pE) throws UnsupportedCCodeException {
//        return super.visit(pE);
//      }
//
//      @Override
//      public Void visit(CStringLiteralExpression pE) throws UnsupportedCCodeException {
//        return super.visit(pE);
//      }
//
//    };
//
//    for (CFANode n: pCfa.getAllNodes()) {
//      for (CFAEdge e: CFAUtils.leavingEdges(n)) {
//        switch (e.getEdgeType()) {
//        case AssumeEdge:
//          CAssumeEdge assumeEdge = (CAssumeEdge) e;
//          assumeEdge.getExpression().accept(visitor);
//          break;
//        case DeclarationEdge:
//          CDeclarationEdge declEdge = (CDeclarationEdge) e;
//          result.add(declEdge.getDeclaration().getName());
//          break;
//        case StatementEdge:
//          CStatementEdge stmtEdge = (CStatementEdge) e;
//          stmtEdge.getStatement().accept(stmtVisitor);
//          break;
//        case ReturnStatementEdge:
//          CReturnStatementEdge retEdge = (CReturnStatementEdge) e;
//          retEdge.getExpression().accept(visitor);
//          break;
//        default:
//          throw new UnsupportedCCodeException("Edge not supported: " + e.getEdgeType(), e);
//        }
//      }
//    }
//  }


  public FsmSyntaxAnalizer() {
    this.literalIndexMap = new HashMap<CExpression, Integer>();
  }

  @Override
  public int getIntervalMaximum() {
    // TODO
    return 30;
  }

  @Override
  public int mapLiteralToIndex(CExpression pLiteral) {
    Integer index = literalIndexMap.get(pLiteral);
    if (index == null) {
      if (pLiteral instanceof CIntegerLiteralExpression) {
        index = (int) ((CIntegerLiteralExpression) pLiteral).asLong();
      } else if ((pLiteral instanceof CUnaryExpression
          && ((CUnaryExpression)pLiteral).getOperator() == UnaryOperator.MINUS
          && ((CUnaryExpression)pLiteral).getOperand() instanceof CLiteralExpression)) {
        index = (int) ((CIntegerLiteralExpression) ((CUnaryExpression)pLiteral).getOperand()).asLong();
      } else {
        throw new RuntimeException("Type of literal not (yet) supported: " + pLiteral.toASTString());
      }

      literalIndexMap.put(pLiteral, index);
    }

    return index;
  }
}
