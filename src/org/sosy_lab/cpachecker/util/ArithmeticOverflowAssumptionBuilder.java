// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclarationVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.simplification.ExpressionSimplificationVisitor;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.assumptions.genericassumptions.GenericAssumptionBuilder;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/** Generate assumptions related to over/underflow of arithmetic operations */
@Options(prefix = "overflow")
public final class ArithmeticOverflowAssumptionBuilder implements GenericAssumptionBuilder {

  @Option(
      description =
          "Only check live variables for overflow," + " as compiler can remove dead variables.",
      secure = true)
  private boolean useLiveness = true;

  @Option(description = "Track overflows in left-shift operations.")
  private boolean trackLeftShifts = true;

  @Option(description = "Track overflows in additive(+/-) operations.")
  private boolean trackAdditiveOperations = true;

  @Option(description = "Track overflows in multiplication operations.")
  private boolean trackMultiplications = true;

  @Option(description = "Track overflows in division(/ or %) operations.")
  private boolean trackDivisions = true;

  @Option(description = "Track overflows in binary expressions involving pointers.")
  private boolean trackPointers = false;

  @Option(description = "Simplify overflow assumptions.")
  private boolean simplifyExpressions = true;

  private final Map<CType, CLiteralExpression> upperBounds;
  private final Map<CType, CLiteralExpression> lowerBounds;
  private final Map<CType, CLiteralExpression> width;
  private final OverflowAssumptionManager ofmgr;
  private final ExpressionSimplificationVisitor simplificationVisitor;
  private final MachineModel machineModel;
  private final Optional<LiveVariables> liveVariables;
  private final LogManager logger;

  public ArithmeticOverflowAssumptionBuilder(
      CFA cfa, LogManager logger, Configuration pConfiguration)
      throws InvalidConfigurationException {
    this(cfa.getMachineModel(), cfa.getLiveVariables(), logger, pConfiguration);
  }

  public ArithmeticOverflowAssumptionBuilder(
      MachineModel pMachineModel,
      Optional<LiveVariables> pLiveVariables,
      LogManager logger,
      Configuration pConfiguration)
      throws InvalidConfigurationException {
    pConfiguration.inject(this);
    this.logger = logger;
    liveVariables = pLiveVariables;
    machineModel = pMachineModel;
    if (useLiveness) {
      Preconditions.checkState(
          liveVariables.isPresent(), "Liveness information is required for overflow analysis.");
    }

    upperBounds = new HashMap<>();
    lowerBounds = new HashMap<>();
    width = new HashMap<>();

    // TODO: find out if the bare types even occur, or if they are always converted to the SIGNED
    // variants. In that case we could remove the lines with types without the SIGNED_ prefix
    // (though this should really make no difference in performance).
    trackType(CNumericTypes.INT);
    trackType(CNumericTypes.SIGNED_INT);
    trackType(CNumericTypes.LONG_INT);
    trackType(CNumericTypes.SIGNED_LONG_INT);
    trackType(CNumericTypes.LONG_LONG_INT);
    trackType(CNumericTypes.SIGNED_LONG_LONG_INT);

    ofmgr = new OverflowAssumptionManager(machineModel, logger);
    simplificationVisitor =
        new ExpressionSimplificationVisitor(machineModel, new LogManagerWithoutDuplicates(logger));
  }

  /**
   * Returns assumptions required for proving that none of the expressions contained in {@code
   * pEdge} result in overflows.
   *
   * @param pEdge Input CFA edge.
   */
  @Override
  public Set<CExpression> assumptionsForEdge(CFAEdge pEdge) throws UnrecognizedCodeException {
    Set<CExpression> result = new LinkedHashSet<>();

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
        CDeclarationEdge declarationEdge = (CDeclarationEdge) pEdge;
        declarationEdge.getDeclaration().accept(finder);
        break;
      case ReturnStatementEdge:
        CReturnStatementEdge returnEdge = (CReturnStatementEdge) pEdge;
        if (returnEdge.getExpression().isPresent()) {
          returnEdge.getExpression().orElseThrow().accept(finder);
        }
        break;
      case FunctionReturnEdge:
      case CallToReturnEdge:

        // No overflows for summary edges.
        break;
      default:
        throw new UnsupportedOperationException("Unexpected edge type");
    }

    if (simplifyExpressions) {
      return transformedImmutableSetCopy(result, x -> x.accept(simplificationVisitor));
    }
    return ImmutableSet.copyOf(result);
  }

  private void trackType(CSimpleType type) {
    CIntegerLiteralExpression typeMinValue =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY, type, machineModel.getMinimalIntegerValue(type));
    CIntegerLiteralExpression typeMaxValue =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY, type, machineModel.getMaximalIntegerValue(type));
    CIntegerLiteralExpression typeWidth =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY,
            type,
            OverflowAssumptionManager.getWidthForMaxOf(machineModel.getMaximalIntegerValue(type)));

    upperBounds.put(type, typeMaxValue);
    lowerBounds.put(type, typeMinValue);
    width.put(type, typeWidth);
  }

  /**
   * Compute assumptions whose conjunction states that the expression does not overflow the allowed
   * bound of its type.
   */
  private void addAssumptionOnBounds(CExpression exp, Set<CExpression> result, CFANode node)
      throws UnrecognizedCodeException {
    if (useLiveness) {
      Set<CSimpleDeclaration> referencedDeclarations =
          CFAUtils.getIdExpressionsOfExpression(exp)
              .transform(CIdExpression::getDeclaration)
              .toSet();

      Set<ASimpleDeclaration> liveVars = liveVariables.orElseThrow().getLiveVariablesForNode(node);
      if (Sets.intersection(referencedDeclarations, liveVars).isEmpty()) {
        logger.log(Level.FINE, "No live variables found in expression", exp, "skipping");
        return;
      }
    }

    if (isBinaryExpressionThatMayOverflow(exp)) {
      CBinaryExpression binexp = (CBinaryExpression) exp;
      BinaryOperator binop = binexp.getOperator();
      CType calculationType = binexp.getCalculationType();
      CExpression op1 = binexp.getOperand1();
      CExpression op2 = binexp.getOperand2();
      if (trackAdditiveOperations
          && (binop.equals(BinaryOperator.PLUS) || binop.equals(BinaryOperator.MINUS))) {
        if (lowerBounds.get(calculationType) != null) {
          result.add(ofmgr.getLowerAssumption(op1, op2, binop, lowerBounds.get(calculationType)));
        }
        if (upperBounds.get(calculationType) != null) {
          result.add(ofmgr.getUpperAssumption(op1, op2, binop, upperBounds.get(calculationType)));
        }
      } else if (trackMultiplications && binop.equals(BinaryOperator.MULTIPLY)) {
        if (lowerBounds.get(calculationType) != null && upperBounds.get(calculationType) != null) {
          result.addAll(
              ofmgr.addMultiplicationAssumptions(
                  op1, op2, lowerBounds.get(calculationType), upperBounds.get(calculationType)));
        }
      } else if (trackDivisions
          && (binop.equals(BinaryOperator.DIVIDE) || binop.equals(BinaryOperator.MODULO))) {
        if (lowerBounds.get(calculationType) != null) {
          ofmgr.addDivisionAssumption(op1, op2, lowerBounds.get(calculationType), result);
        }
      } else if (trackLeftShifts && binop.equals(BinaryOperator.SHIFT_LEFT)) {
        if (upperBounds.get(calculationType) != null && width.get(calculationType) != null) {
          ofmgr.addLeftShiftAssumptions(op1, op2, upperBounds.get(calculationType), result);
        }
      }
    } else if (exp instanceof CUnaryExpression) {
      CType calculationType = exp.getExpressionType();
      CUnaryExpression unaryexp = (CUnaryExpression) exp;
      if (unaryexp.getOperator().equals(CUnaryExpression.UnaryOperator.MINUS)
          && lowerBounds.get(calculationType) != null) {

        CExpression operand = unaryexp.getOperand();
        result.add(ofmgr.getNegationAssumption(operand, lowerBounds.get(calculationType)));
      }
    } else {
      // TODO: check out and implement in case this happens
    }
  }

  private boolean isBinaryExpressionThatMayOverflow(CExpression pExp) {
    if (pExp instanceof CBinaryExpression) {
      CBinaryExpression binexp = (CBinaryExpression) pExp;
      CExpression op1 = binexp.getOperand1();
      CExpression op2 = binexp.getOperand2();
      if (op1.getExpressionType() instanceof CPointerType
          || op2.getExpressionType() instanceof CPointerType) {
        // There are no classical arithmetic overflows in binary operations involving pointers,
        // since pointer types are not necessarily signed integer types as far as ISO/IEC 9899:2018
        // (C17) is concerned. So we do not track this by default, but make it configurable:
        return trackPointers;
      } else {
        return true;
      }
    } else {
      return false;
    }
  }

  private class AssumptionsFinder extends DefaultCExpressionVisitor<Void, UnrecognizedCodeException>
      implements CStatementVisitor<Void, UnrecognizedCodeException>,
          CSimpleDeclarationVisitor<Void, UnrecognizedCodeException>,
          CInitializerVisitor<Void, UnrecognizedCodeException> {

    private final Set<CExpression> assumptions;
    private final CFANode node;

    private AssumptionsFinder(Set<CExpression> pAssumptions, CFANode node) {
      assumptions = pAssumptions;
      this.node = node;
    }

    @Override
    public Void visit(CBinaryExpression pIastBinaryExpression) throws UnrecognizedCodeException {
      if (resultCanOverflow(pIastBinaryExpression)) {
        addAssumptionOnBounds(pIastBinaryExpression, assumptions, node);
      }
      pIastBinaryExpression.getOperand1().accept(this);
      pIastBinaryExpression.getOperand2().accept(this);
      return null;
    }

    @Override
    protected Void visitDefault(CExpression exp) throws UnrecognizedCodeException {
      return null;
    }

    @Override
    public Void visit(CArraySubscriptExpression pIastArraySubscriptExpression)
        throws UnrecognizedCodeException {
      return pIastArraySubscriptExpression.getSubscriptExpression().accept(this);
    }

    @Override
    public Void visit(CPointerExpression pointerExpression) throws UnrecognizedCodeException {
      return pointerExpression.getOperand().accept(this);
    }

    @Override
    public Void visit(CComplexCastExpression complexCastExpression)
        throws UnrecognizedCodeException {
      return complexCastExpression.getOperand().accept(this);
    }

    @Override
    public Void visit(CUnaryExpression pIastUnaryExpression) throws UnrecognizedCodeException {
      if (resultCanOverflow(pIastUnaryExpression)) {
        addAssumptionOnBounds(pIastUnaryExpression, assumptions, node);
      }
      return pIastUnaryExpression.getOperand().accept(this);
    }

    @Override
    public Void visit(CCastExpression pIastCastExpression) throws UnrecognizedCodeException {
      // TODO: can cast itself cause overflows?
      return pIastCastExpression.getOperand().accept(this);
    }

    @Override
    public Void visit(CExpressionStatement pIastExpressionStatement)
        throws UnrecognizedCodeException {
      return pIastExpressionStatement.getExpression().accept(this);
    }

    @Override
    public Void visit(CExpressionAssignmentStatement pIastExpressionAssignmentStatement)
        throws UnrecognizedCodeException {
      return pIastExpressionAssignmentStatement.getRightHandSide().accept(this);
    }

    @Override
    public Void visit(CFunctionCallAssignmentStatement pIastFunctionCallAssignmentStatement)
        throws UnrecognizedCodeException {
      for (CExpression arg :
          pIastFunctionCallAssignmentStatement.getRightHandSide().getParameterExpressions()) {
        arg.accept(this);
      }
      return null;
    }

    @Override
    public Void visit(CFunctionCallStatement pIastFunctionCallStatement)
        throws UnrecognizedCodeException {
      for (CExpression arg :
          pIastFunctionCallStatement.getFunctionCallExpression().getParameterExpressions()) {
        arg.accept(this);
      }
      return null;
    }

    @Override
    public Void visit(CFunctionDeclaration pDecl) throws UnrecognizedCodeException {
      // no overflows in CFunctionDeclaration
      return null;
    }

    @Override
    public Void visit(CComplexTypeDeclaration pDecl) throws UnrecognizedCodeException {
      // no overflows in CComplexTypeDeclaration
      return null;
    }

    @Override
    public Void visit(CTypeDefDeclaration pDecl) throws UnrecognizedCodeException {
      // no overflows in CTypeDefDeclaration
      return null;
    }

    @Override
    public Void visit(CVariableDeclaration pDecl) throws UnrecognizedCodeException {
      // rhs of CVariableDeclaration can contain overflows!
      if (pDecl.getInitializer() != null) {
        pDecl.getInitializer().accept(this);
      }
      return null;
    }

    @Override
    public Void visit(CParameterDeclaration pDecl) throws UnrecognizedCodeException {
      // no overflows in CParameterDeclaration
      return null;
    }

    @Override
    public Void visit(CEnumerator pDecl) throws UnrecognizedCodeException {
      // no overflows in CEnumerator
      return null;
    }

    @Override
    public Void visit(CInitializerExpression pInitializerExpression)
        throws UnrecognizedCodeException {
      // CInitializerExpression has a CExpression that can contain an overflow:
      pInitializerExpression.getExpression().accept(this);
      return null;
    }

    @Override
    public Void visit(CInitializerList pInitializerList) throws UnrecognizedCodeException {
      // check each CInitializer for overflow:
      for (CInitializer initializer : pInitializerList.getInitializers()) {
        initializer.accept(this);
      }
      return null;
    }

    @Override
    public Void visit(CDesignatedInitializer pCStructInitializerPart)
        throws UnrecognizedCodeException {
      // CDesignatedInitializer has a CInitializer on the rhs that can contain an overflow:
      pCStructInitializerPart.getRightHandSide().accept(this);
      return null;
    }
  }

  /** Whether the given operator can create new expression. */
  private boolean resultCanOverflow(CExpression expr) {
    if (expr instanceof CBinaryExpression) {
      switch (((CBinaryExpression) expr).getOperator()) {
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
    } else if (expr instanceof CUnaryExpression) {
      switch (((CUnaryExpression) expr).getOperator()) {
        case MINUS:
          return true;
        default:
          return false;
      }
    }
    return false;
  }
}
