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

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cpa.fsmbdd.interfaces.DomainIntervalProvider;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;


public class FsmSyntaxAnalizer implements DomainIntervalProvider{

  private final Map<String,Integer> literalIndexMap;
  private int literalSequence;
  private CFA cfa;

  /**
   * Constructor.
   */
  public FsmSyntaxAnalizer(CFA pCfa) {
    this.cfa = pCfa;
    this.literalIndexMap = new HashMap<String, Integer>();
    this.literalSequence = 0;
  }

  @Override
  public void printLiteralIndexMap(PrintStream pOut) {
    for (String lit : literalIndexMap.keySet()) {
      pOut.println(String.format("%10s --> %d", lit, literalIndexMap.get(lit)));
    }
  }

  @Override
  public int getIntervalMaximum() throws CPATransferException {
    if (literalSequence == 0) {
      extractLiterals(cfa);
    }

    return literalSequence + 1;
  }

  /**
   * Map the given literal to an integer.
   */
  @Override
  public int mapLiteralToIndex(CExpression pLiteral) throws CPATransferException {
    Integer index = literalIndexMap.get(pLiteral.toASTString());
    if (index == null) {
      throw new CPATransferException("Cannot map literal to index: " + pLiteral.getClass().getSimpleName() + ": " + pLiteral.toASTString());
    }

    return index;
  }

  /**
   * Assign an sequence number to a given literal.
   * @param literal
   */
  private void registerLiteral(CExpression literal) {
    if (!literalIndexMap.containsKey(literal.toASTString())) {
      literalIndexMap.put(literal.toASTString(), literalSequence++);
    }
  }

  /**
   * Find all literals that are defined in the program
   * and assign them an unique sequence number (without any gaps).
   */
  private void extractLiterals(CFA pCfa) throws CPATransferException {

    final DefaultCExpressionVisitor<Void, UnsupportedCCodeException> exprVisitor = new DefaultCExpressionVisitor<Void, UnsupportedCCodeException>() {
      @Override
      protected Void visitDefault(CExpression pExp) throws UnsupportedCCodeException {
        return null;
      }

      @Override
      public Void visit(CCharLiteralExpression pE) throws UnsupportedCCodeException {
        registerLiteral(pE);
        return null;
      }

      @Override
      public Void visit(CIntegerLiteralExpression pE) throws UnsupportedCCodeException {
        registerLiteral(pE);
        return null;
      }

      @Override
      public Void visit(CStringLiteralExpression pE) throws UnsupportedCCodeException {
        registerLiteral(pE);
        return null;
      }

      @Override
      public Void visit(CUnaryExpression pE) throws UnsupportedCCodeException {
        if (pE.getOperand() instanceof CUnaryExpression
            || pE.getOperand() instanceof CBinaryExpression) {
          pE.getOperand().accept(this);
        } else {
          registerLiteral(pE);
        }
        return null;
      }

      @Override
      public Void visit(CBinaryExpression pE) throws UnsupportedCCodeException {
        pE.getOperand1().accept(this);
        pE.getOperand2().accept(this);
        return null;
      }

    };

    final CStatementVisitor<Void, UnsupportedCCodeException> stmtVisitor = new CStatementVisitor<Void, UnsupportedCCodeException>() {
      @Override
      public Void visit(CFunctionCallStatement pS) throws UnsupportedCCodeException {
        return null;
      }

      @Override
      public Void visit(CFunctionCallAssignmentStatement pS)
          throws UnsupportedCCodeException {
        pS.getLeftHandSide().accept(exprVisitor);
        return null;
      }

      @Override
      public Void visit(CExpressionAssignmentStatement pS) throws UnsupportedCCodeException {
        pS.getLeftHandSide().accept(exprVisitor);
        pS.getRightHandSide().accept(exprVisitor);
        return null;
      }

      @Override
      public Void visit(CExpressionStatement pS) throws UnsupportedCCodeException {
        pS.getExpression().accept(exprVisitor);
        return null;
      }
    };

    for (CFANode n: pCfa.getAllNodes()) {
      Deque<CFAEdge> leavingEdges = new ArrayDeque<CFAEdge>();
      leavingEdges.addAll(CFAUtils.leavingEdges(n).toImmutableList());

      while (!leavingEdges.isEmpty()) {
        CFAEdge e = leavingEdges.pop();

        switch (e.getEdgeType()) {
        case AssumeEdge:
          CAssumeEdge assumeEdge = (CAssumeEdge) e;
          assumeEdge.getExpression().accept(exprVisitor);
          break;
        case DeclarationEdge:
          CDeclarationEdge declEdge = (CDeclarationEdge) e;
          if (declEdge.getDeclaration() instanceof CVariableDeclaration) {
            CVariableDeclaration varDecl = (CVariableDeclaration) declEdge.getDeclaration();
            if (varDecl.getInitializer() != null) {
              if (varDecl.getInitializer() instanceof CInitializerExpression) {
                ((CInitializerExpression) varDecl.getInitializer()).getExpression().accept(exprVisitor);
              }
            }
          }
          break;
        case StatementEdge:
          CStatementEdge stmtEdge = (CStatementEdge) e;
          stmtEdge.getStatement().accept(stmtVisitor);
          break;
        case ReturnStatementEdge:
          CReturnStatementEdge retEdge = (CReturnStatementEdge) e;
          retEdge.getExpression().accept(exprVisitor);
          break;
        case MultiEdge:
          MultiEdge multiEdge = (MultiEdge) e;
          leavingEdges.addAll(multiEdge.getEdges());
        }
      }
    }
  }


}
