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
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
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

  private final CFAEdge edge;

  private SymbolicValueFactory expressionFactory;

  private ConstraintFactory(
      SMGState pSmgState,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger,
      SMGOptions pOptions,
      SMGCPAExpressionEvaluator pEvaluator,
      CFAEdge pEdge) {

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
    switch (pExpression.getOperator()) {
      case EQUALS:
      case NOT_EQUALS:
      case GREATER_EQUAL:
      case GREATER_THAN:
      case LESS_EQUAL:
      case LESS_THAN:
        return true;
      default:
        return false;
    }
  }

  public Collection<ConstraintAndSMGState> createPositiveConstraint(CIdExpression pExpression)
      throws CPATransferException {

    ExpressionTransformer transformer =
        new ExpressionTransformer(edge, smgState, machineModel, logger, options, evaluator);
    ImmutableList.Builder<ConstraintAndSMGState> builder = ImmutableList.builder();
    for (SymbolicExpressionAndSMGState symbolicExpressionAndState :
        transformer.transform(pExpression)) {
      SymbolicExpression symbolicExpression = symbolicExpressionAndState.getSymbolicExpression();
      SMGState currentState = symbolicExpressionAndState.getState();

      if (symbolicExpression == null) {
        return null;
      } else if (symbolicExpression instanceof Constraint) {
        builder.add(ConstraintAndSMGState.of((Constraint) symbolicExpression, currentState));

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
    if (pType instanceof CType) {
      CType canonicalType = ((CType) pType).getCanonicalType();
      if (canonicalType instanceof CSimpleType) {
        switch (((CSimpleType) canonicalType).getType()) {
          case FLOAT:
          case INT:
            return true;
          default:
            // DO NOTHING, false is returned below
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
}
