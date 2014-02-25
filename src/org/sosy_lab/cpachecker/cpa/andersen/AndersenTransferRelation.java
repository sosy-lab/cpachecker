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
package org.sosy_lab.cpachecker.cpa.andersen;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.andersen.util.BaseConstraint;
import org.sosy_lab.cpachecker.cpa.andersen.util.ComplexConstraint;
import org.sosy_lab.cpachecker.cpa.andersen.util.SimpleConstraint;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

import com.google.common.collect.Iterables;

@Options(prefix = "cpa.pointerA")
public class AndersenTransferRelation implements TransferRelation {

  private final LogManager logger;

  public AndersenTransferRelation(Configuration pConfig, LogManager pLogger) throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;
  }

  @Override
  public Collection<AbstractState> getAbstractSuccessors(AbstractState pElement, Precision pPrecision,
      CFAEdge pCfaEdge)
      throws CPATransferException {

    AbstractState successor = null;
    AndersenState andersenState = (AndersenState) pElement;

    // check the type of the edge
    switch (pCfaEdge.getEdgeType()) {

    // if edge is a statement edge, e.g. a = b + c
    case StatementEdge:
      CStatementEdge statementEdge = (CStatementEdge) pCfaEdge;
      successor = handleStatement(andersenState, statementEdge.getStatement(), pCfaEdge);
      break;

    // edge is a declaration edge, e.g. int a;
    case DeclarationEdge:
      CDeclarationEdge declarationEdge = (CDeclarationEdge) pCfaEdge;
      successor = handleDeclaration(andersenState, declarationEdge);
      break;

    // this is an assumption, e.g. if (a == b)
    case AssumeEdge:
      successor = andersenState;
      break;

    case BlankEdge:
      successor = andersenState;
      break;
    case MultiEdge:
      successor = andersenState;
      Iterator<CFAEdge> edgeIterator = ((MultiEdge) pCfaEdge).iterator();
      while (pElement != null && edgeIterator.hasNext()) {
        successor = Iterables.getFirst(getAbstractSuccessors(successor, pPrecision, edgeIterator.next()), null);
      }
      break;

    case CallToReturnEdge:
    case FunctionCallEdge:
    case ReturnStatementEdge:
    case FunctionReturnEdge:
    default:
      printWarning(pCfaEdge);
    }

    if (successor == null) {
      return Collections.emptySet();
    } else {
      return Collections.singleton(successor);
    }
  }

  @Override
  public Collection<? extends AbstractState> strengthen(AbstractState pElement, List<AbstractState> pElements,
      CFAEdge pCfaEdge, Precision pPrecision)
      throws UnrecognizedCCodeException {

    return null;
  }

  private AndersenState handleStatement(AndersenState pElement, CStatement pExpression, CFAEdge pCfaEdge)
      throws UnrecognizedCCodeException {

    // e.g. a = b;
    if (pExpression instanceof CAssignment) {
      return handleAssignment(pElement, (CAssignment) pExpression, pCfaEdge);
    } else if (pExpression instanceof CFunctionCallStatement) {
      return pElement;
    } else if (pExpression instanceof CExpressionStatement) {
      return pElement;
    } else {
      throw new UnrecognizedCCodeException("unknown statement", pCfaEdge, pExpression);
    }
  }

  private AndersenState handleAssignment(AndersenState pElement, CAssignment pAssignExpression, CFAEdge pCfaEdge)
      throws UnrecognizedCCodeException {

    CExpression op1 = pAssignExpression.getLeftHandSide();
    CRightHandSide op2 = pAssignExpression.getRightHandSide();

    if (op1 instanceof CIdExpression) {

      // a = ...

      return handleAssignmentTo(op1.toASTString(), op2, pElement, pCfaEdge);

    } else if (op1 instanceof CPointerExpression
        && op2 instanceof CIdExpression) {

      // *a = b; complex constraint

      op1 = ((CPointerExpression) op1).getOperand();

      if (op1 instanceof CIdExpression) {

        return pElement.addConstraint(new ComplexConstraint(op2.toASTString(), op1.toASTString(), false));

      } else {
        throw new UnrecognizedCCodeException("not supported", pCfaEdge, op2);
      }

    } else {
      throw new UnrecognizedCCodeException("not supported", pCfaEdge, op1);
    }
  }

  /**
   * Handles an assignement of the form <code>op1 = ...</code> to a given variable <code>op1</code>.
   *
   * @param pOp1
   *        Name of the lefthandside variable in the assignement <code>op1 = ...</code>.
   * @param pOp2
   *        Righthandside of the assignement.
   * @param pElement
   *        Predecessor of this assignement's AndersonElement.
   * @param pCfaEdge
  *          Corresponding edge of the CFA.
   * @return <code>element</code>'s successor.
   *
   * @throws UnrecognizedCCodeException
   */
  private AndersenState handleAssignmentTo(String pOp1, CRightHandSide pOp2, AndersenState pElement, CFAEdge pCfaEdge)
      throws UnrecognizedCCodeException {

    // unpack cast if necessary
    while (pOp2 instanceof CCastExpression) {
      pOp2 = ((CCastExpression) pOp2).getOperand();
    }

    if (pOp2 instanceof CIdExpression) {

      // a = b; simple constraint

      return pElement.addConstraint(new SimpleConstraint(pOp2.toASTString(), pOp1));

    } else if (pOp2 instanceof CUnaryExpression && ((CUnaryExpression) pOp2).getOperator() == UnaryOperator.AMPER) {

      // a = &b; base constraint

      pOp2 = ((CUnaryExpression) pOp2).getOperand();

      if (pOp2 instanceof CIdExpression) {

        return pElement.addConstraint(new BaseConstraint(pOp2.toASTString(), pOp1));

      } else {
        throw new UnrecognizedCCodeException("not supported", pCfaEdge, pOp2);
      }

    } else if (pOp2 instanceof CPointerExpression) {

      // a = *b; complex constraint

      pOp2 = ((CPointerExpression) pOp2).getOperand();

      if (pOp2 instanceof CIdExpression) {

        return pElement.addConstraint(new ComplexConstraint(pOp2.toASTString(), pOp1, true));

      } else {
        throw new UnrecognizedCCodeException("not supported", pCfaEdge, pOp2);
      }

    } else if (pOp2 instanceof CFunctionCallExpression
        && "malloc".equals(((CFunctionCallExpression) pOp2).getFunctionNameExpression().toASTString())) {

      return pElement.addConstraint(new BaseConstraint("malloc-" + pCfaEdge.getLineNumber(), pOp1));

    }

    // not implemented, or not interessing
    printWarning(pCfaEdge);
    return pElement;
  }

  private AndersenState handleDeclaration(AndersenState pElement, CDeclarationEdge pDeclarationEdge)
      throws UnrecognizedCCodeException {

    if (!(pDeclarationEdge.getDeclaration() instanceof CVariableDeclaration)) {
      // nothing interesting to see here, please move along
      return pElement;
    }

    CVariableDeclaration decl = (CVariableDeclaration) pDeclarationEdge.getDeclaration();

    // get the variable name in the declarator
    String varName = decl.getName();

    // get initial value
    CInitializer init = decl.getInitializer();
    if (init instanceof CInitializerExpression) {

      CRightHandSide exp = ((CInitializerExpression) init).getExpression();

      return handleAssignmentTo(varName, exp, pElement, pDeclarationEdge);
    }

    return pElement;
  }

  /**
   * Prints a warning to System.err that the statement corresponding to the given
   * <code>cfaEdge</code> was not handled.
   */
  private void printWarning(CFAEdge pCfaEdge) {
    logger.log(Level.WARNING, "Warning! CFA Edge \"" + pCfaEdge.getRawStatement() + "\" (line: " + pCfaEdge.getLineNumber()
        + ") not handled.");
  }
}
