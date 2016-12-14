/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.assumptions.genericassumptions.GenericAssumptionBuilder;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Generate assumptions related to over/underflow
 * of arithmetic operations
 */
@Options(prefix="overflow")
public final class ArithmeticOverflowAssumptionBuilder implements
                                                 GenericAssumptionBuilder {

  @Option(description = "Only check live variables for overflow,"
      + " as compiler can remove dead variables.", secure=true)
  private boolean useLiveness = true;

  @Option(description = "Track overflow for signed integers.")
  private boolean trackSignedIntegers = true;

  private final Map<CType, CLiteralExpression> upperBounds;
  private final Map<CType, CLiteralExpression> lowerBounds;
  private final CBinaryExpressionBuilder cBinaryExpressionBuilder;
  private final CFA cfa;
  private final LogManager logger;

  public ArithmeticOverflowAssumptionBuilder(
      CFA cfa,
      LogManager logger,
      Configuration pConfiguration) throws InvalidConfigurationException {
    pConfiguration.inject(this);
    this.logger = logger;
    this.cfa = cfa;
    if (useLiveness) {
      Preconditions.checkState(cfa.getLiveVariables().isPresent(),
          "Liveness information is required for overflow analysis.");
    }

    CIntegerLiteralExpression INT_MIN = new CIntegerLiteralExpression(
        FileLocation.DUMMY,
        CNumericTypes.INT,
        cfa.getMachineModel().getMinimalIntegerValue(CNumericTypes.INT));
    CIntegerLiteralExpression INT_MAX = new CIntegerLiteralExpression(
        FileLocation.DUMMY,
        CNumericTypes.INT,
        cfa.getMachineModel().getMaximalIntegerValue(CNumericTypes.INT));

    ImmutableMap.Builder<CType, CLiteralExpression> upperBoundsBuilder =
        ImmutableMap.builder();
    ImmutableMap.Builder<CType, CLiteralExpression> lowerBoundsBuilder =
        ImmutableMap.builder();

    if (trackSignedIntegers) {
      upperBoundsBuilder.put(CNumericTypes.INT, INT_MAX);
      upperBoundsBuilder.put(CNumericTypes.SIGNED_INT, INT_MAX);
      lowerBoundsBuilder.put(CNumericTypes.INT, INT_MIN);
      lowerBoundsBuilder.put(CNumericTypes.SIGNED_INT, INT_MIN);
    }
    upperBounds = upperBoundsBuilder.build();
    lowerBounds = lowerBoundsBuilder.build();
    cBinaryExpressionBuilder = new CBinaryExpressionBuilder(
        cfa.getMachineModel(),
        logger);
  }

  /**
   *
   * @param pEdge Input CFA edge.
   * @return Assumptions required for proving that none of the expressions
   * contained in {@code pEdge} result in overflows.
   */
  @Override
  public List<CExpression> assumptionsForEdge(CFAEdge pEdge)
      throws UnrecognizedCCodeException {
    Set<CExpression> result = new HashSet<>();

    // Node is used for liveness calculation, and predecessor will contain
    // the live variables of the successor.
    CFANode node = pEdge.getPredecessor();
    AssumptionsFinder finder = new AssumptionsFinder(result, node);


    switch (pEdge.getEdgeType()) {
      case BlankEdge:

        // Can't be an overflow if we don't do anything.
        break;
      case AssumeEdge:
        CAssumeEdge assumeEdge = (CAssumeEdge) pEdge;
        assumeEdge.getExpression().accept(finder);
        break;
      case FunctionCallEdge:
        CFunctionCallEdge fcallEdge = (CFunctionCallEdge) pEdge;

        // Overflows in argument parameters.
        for (CExpression e : fcallEdge.getArguments()) {
          e.accept(finder);
        }
        break;
      case StatementEdge:
        CStatementEdge stmtEdge = (CStatementEdge) pEdge;
        stmtEdge.getStatement().accept(finder);
        break;
      case DeclarationEdge:

        // No overflows in declaration.
        break;
      case ReturnStatementEdge:
        CReturnStatementEdge returnEdge = (CReturnStatementEdge) pEdge;
        if (returnEdge.getExpression().isPresent()) {
          returnEdge.getExpression().get().accept(finder);
        }
        break;
      case FunctionReturnEdge:
      case CallToReturnEdge:

        // No overflows for summary edges.
        break;
      default:
        throw new UnsupportedOperationException("Unexpected edge type");
    }
    return ImmutableList.copyOf(result);
  }

  /**
   * Compute and conjunct the assumption for the given expression,
   * stating that it does not overflow the allowed bound of its type.
   */
  private void addAssumptionOnBounds(
      CExpression exp,
      Set<CExpression> result,
      CFANode node)
      throws UnrecognizedCCodeException {
    CType typ = exp.getExpressionType();

    if (useLiveness) {
      Set<CSimpleDeclaration> referencedDeclarations =
          CFAUtils.getIdExpressionsOfExpression(exp)
              .transform(CIdExpression::getDeclaration)
              .toSet();

      Set<ASimpleDeclaration> liveVars =
          cfa.getLiveVariables().get().getLiveVariablesForNode(node).toSet();
      if (Sets.intersection(referencedDeclarations, liveVars).isEmpty()) {
        logger.log(Level.FINE, "No live variables found in expression", exp,
            "skipping");
        return;
      }
    }

    if (lowerBounds.get(typ) != null) {
      result.add(cBinaryExpressionBuilder.buildBinaryExpression(
          exp,
          lowerBounds.get(typ),
          BinaryOperator.GREATER_EQUAL
      ));
    }

    if (upperBounds.get(typ) != null) {
      result.add(cBinaryExpressionBuilder.buildBinaryExpression(
          exp,
          upperBounds.get(typ),
          BinaryOperator.LESS_EQUAL
      ));
    }
  }

  private class AssumptionsFinder
      extends DefaultCExpressionVisitor<Void,UnrecognizedCCodeException>
      implements CStatementVisitor<Void, UnrecognizedCCodeException> {

    private final Set<CExpression> assumptions;
    private final CFANode node;

    private AssumptionsFinder(Set<CExpression> pAssumptions, CFANode node) {
      assumptions = pAssumptions;
      this.node = node;
    }

    @Override
    public Void visit(CBinaryExpression pIastBinaryExpression)
        throws UnrecognizedCCodeException {
      if (resultCanOverflow(pIastBinaryExpression)) {
        addAssumptionOnBounds(pIastBinaryExpression, assumptions, node);
      }
      pIastBinaryExpression.getOperand1().accept(this);
      pIastBinaryExpression.getOperand2().accept(this);
      return null;
    }

    @Override
    protected Void visitDefault(CExpression exp)
        throws UnrecognizedCCodeException {
      return null;
    }

    @Override
    public Void visit(CArraySubscriptExpression pIastArraySubscriptExpression)
        throws UnrecognizedCCodeException {
      return pIastArraySubscriptExpression.getSubscriptExpression().accept(this);
    }

    @Override
    public Void visit(CPointerExpression pointerExpression)
        throws UnrecognizedCCodeException {
      return pointerExpression.getOperand().accept(this);
    }

    @Override
    public Void visit(CComplexCastExpression complexCastExpression)
        throws UnrecognizedCCodeException {
      return complexCastExpression.getOperand().accept(this);
    }

    @Override
    public Void visit(CUnaryExpression pIastUnaryExpression)
        throws UnrecognizedCCodeException {
      return pIastUnaryExpression.getOperand().accept(this);
    }

    @Override
    public Void visit(CCastExpression pIastCastExpression)
        throws UnrecognizedCCodeException {
      // TODO: can cast itself cause overflows?
      return pIastCastExpression.getOperand().accept(this);
    }

    @Override
    public Void visit(CExpressionStatement pIastExpressionStatement)
        throws UnrecognizedCCodeException {
      return pIastExpressionStatement.getExpression().accept(this);
    }

    @Override
    public Void visit(CExpressionAssignmentStatement pIastExpressionAssignmentStatement)
        throws UnrecognizedCCodeException {
      return pIastExpressionAssignmentStatement.getRightHandSide().accept(this);
    }

    @Override
    public Void visit(CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement)
        throws UnrecognizedCCodeException {
      for (CExpression arg : pIastFunctionCallAssignmentStatement
          .getRightHandSide().getParameterExpressions()) {
        arg.accept(this);
      }
      return null;
    }

    @Override
    public Void visit(CFunctionCallStatement pIastFunctionCallStatement)
        throws UnrecognizedCCodeException {
      for (CExpression arg : pIastFunctionCallStatement
          .getFunctionCallExpression()
          .getParameterExpressions()) {
        arg.accept(this);
      }
      return null;
    }
  }

  /**
   * Whether the given operator can create new expression.
   */
  private boolean resultCanOverflow(CBinaryExpression expr) {
    switch (expr.getOperator()) {
      case MULTIPLY:
      case DIVIDE:
      case PLUS:
      case MINUS:
      case SHIFT_LEFT:
      case SHIFT_RIGHT:
        return true;
      case LESS_THAN:
      case GREATER_THAN:
      case LESS_EQUAL:
      case GREATER_EQUAL:
      case BINARY_AND:
      case BINARY_XOR:
      case BINARY_OR:
      case EQUALS:
      case NOT_EQUALS:
      default:
        return false;
    }
  }

}
