// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.constraint;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.smg2.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * Factory for creating {@link org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint}
 * objects.
 */
public class ConstraintFactory {

  private final MachineModel machineModel;
  private final LogManagerWithoutDuplicates logger;
  private final SMGState smgState;

  private final SMGOptions options;

  private final SMGCPAExpressionEvaluator evaluator;

  // The edge is only used as failure trace in the value visitor that may be employed by the
  // ExpressionTransformer.
  // May be null for constraint constructions that don't use the visitor (i.e. memory validity
  // checks)
  @Nullable private final CFAEdge edge;

  private SymbolicValueFactory expressionFactory;

  private ConstraintFactory(
      SMGState pSmgState,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger,
      SMGOptions pOptions,
      SMGCPAExpressionEvaluator pEvaluator,
      @Nullable CFAEdge pEdge) {

    machineModel = pMachineModel;
    logger = pLogger;
    smgState = pSmgState;
    expressionFactory = SymbolicValueFactory.getInstance();
    options = pOptions;
    evaluator = pEvaluator;
    edge = pEdge;
  }

  public static ConstraintFactory getInstance(
      SMGState pSmgState,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger,
      SMGOptions pOptions,
      SMGCPAExpressionEvaluator pEvaluator,
      CFAEdge pEdge) {
    return new ConstraintFactory(pSmgState, pMachineModel, pLogger, pOptions, pEvaluator, pEdge);
  }

  public Collection<ConstraintAndSMGState> createNegativeConstraint(CBinaryExpression pExpression)
      throws CPATransferException {
    Collection<ConstraintAndSMGState> positiveConstraint = createPositiveConstraint(pExpression);

    return positiveConstraint.stream()
        .filter(n -> n != null)
        .map(n -> ConstraintAndSMGState.of(createNot(n.getConstraint()), n.getState()))
        .collect(ImmutableList.toImmutableList());
  }

  public Collection<ConstraintAndSMGState> createNegativeConstraint(CIdExpression pExpression)
      throws CPATransferException {

    Collection<ConstraintAndSMGState> positiveConstraintsAndStates =
        createPositiveConstraint(pExpression);
    ImmutableList.Builder<ConstraintAndSMGState> builder = ImmutableList.builder();

    for (ConstraintAndSMGState positiveConstraintAndState : positiveConstraintsAndStates) {
      Constraint positiveConstraint = positiveConstraintAndState.getConstraint();
      SMGState currentState = positiveConstraintAndState.getState();

      if (positiveConstraint == null) {
        builder.add(ConstraintAndSMGState.of(null, currentState));
      } else {
        builder.add(ConstraintAndSMGState.of(createNot(positiveConstraint), currentState));
      }
    }
    return builder.build();
  }

  public Collection<ConstraintAndSMGState> createPositiveConstraint(CBinaryExpression pExpression)
      throws CPATransferException {
    final ExpressionTransformer transformer = getCTransformer();

    assert isConstraint(pExpression);
    return transformedImmutableListCopy(
        transformer.transform(pExpression),
        n -> ConstraintAndSMGState.of((Constraint) n.getSymbolicExpression(), n.getState()));
  }

  private boolean isConstraint(CBinaryExpression pExpression) {
    return switch (pExpression.getOperator()) {
      case EQUALS, NOT_EQUALS, GREATER_EQUAL, GREATER_THAN, LESS_EQUAL, LESS_THAN -> true;
      default -> false;
    };
  }

  public Collection<ConstraintAndSMGState> createPositiveConstraint(CIdExpression pExpression)
      throws CPATransferException {

    ExpressionTransformer transformer = getCTransformer();
    ImmutableList.Builder<ConstraintAndSMGState> builder = ImmutableList.builder();
    for (SymbolicExpressionAndSMGState symbolicExpressionAndState :
        transformer.transform(pExpression)) {
      SymbolicExpression symbolicExpression = symbolicExpressionAndState.getSymbolicExpression();
      SMGState currentState = symbolicExpressionAndState.getState();

      if (symbolicExpression == null) {
        return null;
      } else if (symbolicExpression instanceof Constraint constraint) {
        builder.add(ConstraintAndSMGState.of(constraint, currentState));

      } else {
        builder.add(
            ConstraintAndSMGState.of(
                transformValueToConstraint(symbolicExpression, pExpression.getExpressionType()),
                currentState));
      }
    }
    return builder.build();
  }

  private Constraint transformValueToConstraint(
      SymbolicExpression pExpression, Type expressionType) {

    if (isNumeric(expressionType)) {
      // 1 == pExpression
      // We do not have to cast the values to a specific calculation type, as every type can
      // represent 1 and 0.
      return createEqual(
          getOneConstant(expressionType), pExpression, expressionType, pExpression.getType());

    } else {
      throw new AssertionError("Unexpected type " + expressionType);
    }
  }

  private ExpressionTransformer getCTransformer() {
    return new ExpressionTransformer(edge, smgState, machineModel, logger, options, evaluator);
  }

  private boolean isNumeric(Type pType) {
    if (pType instanceof CType cType) {
      CType canonicalType = cType.getCanonicalType();
      if (canonicalType instanceof CSimpleType cSimpleType) {
        switch (cSimpleType.getType()) {
          case FLOAT, INT -> {
            return true;
          }
          default -> {
            // DO NOTHING, false is returned below
          }
        }
      }

      return false;
    } else {
      throw new AssertionError("Unexpected type " + pType);
    }
  }

  private SymbolicExpression getOneConstant(Type pType) {
    return expressionFactory.asConstant(new NumericValue(1L), pType);
  }

  private Constraint createNot(Constraint pConstraint) {
    // We use ConstraintExpression as Constraints, so this should be possible
    return createNot((SymbolicExpression) pConstraint);
  }

  private Constraint createNot(SymbolicExpression pSymbolicExpression) {
    return (Constraint)
        expressionFactory.logicalNot(pSymbolicExpression, pSymbolicExpression.getType());
  }

  private Constraint createEqual(
      SymbolicExpression pLeftOperand,
      SymbolicExpression pRightOperand,
      Type pExpressionType,
      Type pCalculationType) {

    return expressionFactory.equal(pLeftOperand, pRightOperand, pExpressionType, pCalculationType);
  }

  public Collection<Constraint> checkValidMemoryAccess(
      Value offsetInBits,
      Value readSizeInBits,
      Value memoryRegionSizeInBits,
      CType comparisonType,
      SMGState currentState) {
    final ExpressionTransformer transformer = getCTransformer();
    return transformer.checkValidMemoryAccess(
        offsetInBits, readSizeInBits, memoryRegionSizeInBits, comparisonType, currentState);
  }

  /** Those constraints need to be kept on the stack as long as their assignments are needed. */
  public List<Constraint> checkForConcreteMemoryAccessAssignmentWithSolver(
      Value offsetInBits,
      Value readSizeInBits,
      Value memoryRegionSizeInBits,
      CType comparisonType,
      SMGState currentState) {
    final ExpressionTransformer transformer = getCTransformer();
    return transformer.getValidMemoryAccessConstraints(
        offsetInBits, readSizeInBits, memoryRegionSizeInBits, comparisonType, currentState);
  }

  public Constraint getUnequalConstraint(
      SymbolicValue symbolicValueUnequalTo,
      Value valueUnequalTo,
      CType typeOfValueToBlock,
      SMGState currentState) {
    final ExpressionTransformer transformer = getCTransformer();
    return transformer.getUnequalConstraint(
        symbolicValueUnequalTo, valueUnequalTo, typeOfValueToBlock, currentState);
  }

  public Constraint getEqualConstraint(
      Value symbolicValueEqualTo, Value valueEqualTo, CType typeOfValue, SMGState currentState) {
    final ExpressionTransformer transformer = getCTransformer();
    return transformer.getEqualConstraint(
        symbolicValueEqualTo, valueEqualTo, typeOfValue, currentState);
  }

  public Constraint getMemorySizeInBitsEqualsZeroConstraint(
      Value memoryRegionSizeInBits, CType calculationType, SMGState currentState) {
    final ExpressionTransformer transformer = getCTransformer();
    return transformer.checkMemorySizeEqualsZero(
        memoryRegionSizeInBits, calculationType, currentState);
  }

  public Constraint getNotEqualsZeroConstraint(
      Value valueNotEqZero, CType calculationType, SMGState currentState) {
    final ExpressionTransformer transformer = getCTransformer();
    // Yes this does add a != 0 constraint on the value correctly.
    return transformer.getNotEqualsZeroConstraint(valueNotEqZero, calculationType, currentState);
  }
}
