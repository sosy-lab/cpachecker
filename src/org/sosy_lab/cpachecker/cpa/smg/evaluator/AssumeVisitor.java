// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.evaluator;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.UnmodifiableSMGState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGType;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGNullObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGZeroValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class AssumeVisitor extends ExpressionValueVisitor {

  private final Map<UnmodifiableSMGState, BinaryRelationResult> relations = new HashMap<>();

  public AssumeVisitor(
      SMGExpressionEvaluator pSmgExpressionEvaluator, CFAEdge pEdge, SMGState pSmgState) {
    super(pSmgExpressionEvaluator, pEdge, pSmgState);
  }

  @Override
  public List<? extends SMGValueAndState> visit(CBinaryExpression pExp)
      throws CPATransferException {
    BinaryOperator binaryOperator = pExp.getOperator();

    switch (binaryOperator) {
      case EQUALS:
      case NOT_EQUALS:
      case LESS_EQUAL:
      case LESS_THAN:
      case GREATER_EQUAL:
      case GREATER_THAN:
        List<SMGValueAndState> result = new ArrayList<>(4);

        CExpression leftSideExpression = pExp.getOperand1();
        CExpression rightSideExpression = pExp.getOperand2();

        CFAEdge edge = getCfaEdge();

        for (SMGValueAndState leftSideValAndState :
            smgExpressionEvaluator.evaluateExpressionValue(
                getInitialSmgState(), edge, leftSideExpression)) {
          SMGValue leftSideVal = leftSideValAndState.getObject();
          SMGState newState = leftSideValAndState.getSmgState();

          for (SMGValueAndState rightSideValAndState :
              smgExpressionEvaluator.evaluateExpressionValue(newState, edge, rightSideExpression)) {
            SMGValue rightSideVal = rightSideValAndState.getObject();
            newState = rightSideValAndState.getSmgState();

            // if we already know the value, we should use it.
            // TODO why does the visitor above create the symbolic value in the first place?
            if (leftSideVal instanceof SMGKnownSymbolicValue) {
              SMGKnownSymbolicValue expValue = (SMGKnownSymbolicValue) leftSideVal;
              if (newState.isExplicit(expValue)) {
                leftSideVal = newState.getExplicit(expValue);
              }
            }
            if (rightSideVal instanceof SMGKnownSymbolicValue) {
              SMGKnownSymbolicValue expValue = (SMGKnownSymbolicValue) rightSideVal;
              if (newState.isExplicit(expValue)) {
                rightSideVal = newState.getExplicit(expValue);
              }
            }

            for (SMGValueAndState resultValueAndState :
                evaluateBinaryAssumption(newState, binaryOperator, leftSideVal, rightSideVal)) {
              newState = resultValueAndState.getSmgState();
              SMGValue resultValue = resultValueAndState.getObject();

              // TODO: separate modifiable and unmodifiable visitor
              CType leftSideType = leftSideExpression.getExpressionType();
              SMGType leftSideSMGType =
                  SMGType.constructSMGType(leftSideType, newState, edge, smgExpressionEvaluator);
              while (leftSideExpression instanceof CCastExpression) {
                // TODO: rewrite as list of castings
                CCastExpression leftSideCastExpression = (CCastExpression) leftSideExpression;
                leftSideExpression = leftSideCastExpression.getOperand();
                CType leftSideOriginType = leftSideExpression.getExpressionType();
                SMGType leftSideOriginSMGType =
                    SMGType.constructSMGType(
                        leftSideOriginType, newState, edge, smgExpressionEvaluator);
                leftSideSMGType = new SMGType(leftSideSMGType, leftSideOriginSMGType);
              }

              CType rightSideType = rightSideExpression.getExpressionType();
              SMGType rightSideSMGType =
                  SMGType.constructSMGType(rightSideType, newState, edge, smgExpressionEvaluator);
              while (rightSideExpression instanceof CCastExpression) {
                CCastExpression rightSideCastExpression = (CCastExpression) rightSideExpression;
                rightSideExpression = rightSideCastExpression.getOperand();
                CType rightSideOriginType = rightSideExpression.getExpressionType();
                SMGType rightSideOriginSMGType =
                    SMGType.constructSMGType(
                        rightSideOriginType, newState, edge, smgExpressionEvaluator);
                rightSideSMGType = new SMGType(leftSideSMGType, rightSideOriginSMGType);
              }

              // TODO
              // The following predicate relation is a completely unsound assumption,
              // because we know nothing about the calling context, not even, if we are in a negated
              // (!) expression.
              // This might clearly be a bug, but I could currently not find a better way to solve
              // this.
              // The code works well for expressions that are not nested, like "a==b" or "a!=b",
              // but is invalid for "(a==b)==c".
              // There exists code in SMGTransferRelation.strenghten
              // that even needs to negate an edge to get correct results.

              // FIXME: require calculate cast on integer promotions
              newState.addPredicateRelation(
                  // next line: use the symbolic value here and not the potential explicit one.
                  leftSideVal,
                  leftSideSMGType,
                  // next line: use the symbolic value here and not the potential explicit one.
                  rightSideVal,
                  rightSideSMGType,
                  binaryOperator,
                  edge);
              result.add(SMGValueAndState.of(newState, resultValue));
            }
          }
        }

        return result;
      default:
        return super.visit(pExp);
    }
  }

  private boolean isPointer(UnmodifiableSMGState pNewSmgState, SMGValue symVal) {

    if (symVal.isUnknown()) {
      return false;
    }

    if (symVal.isZero()) {
      return true;
    }

    return pNewSmgState.getHeap().isPointer(symVal);
  }

  /** returns the comparison of two pointers, i.e. "p1 op p2". */
  private boolean comparePointer(SMGAddressValue pV1, SMGAddressValue pV2, BinaryOperator pOp) {

    SMGObject object1 = pV1.getObject();
    SMGObject object2 = pV2.getObject();

    // there can be more precise comparison when pointer point to the same object.
    if (object1 == object2) {
      long offset1 = pV1.getOffset().getAsLong();
      long offset2 = pV2.getOffset().getAsLong();

      switch (pOp) {
        case GREATER_EQUAL:
          return offset1 >= offset2;
        case GREATER_THAN:
          return offset1 > offset2;
        case LESS_EQUAL:
          return offset1 <= offset2;
        case LESS_THAN:
          return offset1 < offset2;
        case EQUALS:
          return offset1 == offset2;
        case NOT_EQUALS:
          return offset1 != offset2;
        default:
          throw new AssertionError("Impossible case thrown");
      }
    } else if (pOp == BinaryOperator.NOT_EQUALS
        && (getInitialSmgState().getHeap().isObjectValid(object1)
            || SMGNullObject.INSTANCE.equals(object1))
        && (getInitialSmgState().getHeap().isObjectValid(object2)
            || SMGNullObject.INSTANCE.equals(object2))) {
      // We can't evaluate whether new object is not the same as freed object
      return true;
    }
    return false;
  }

  private SMGValueAndState evaluateBinaryAssumptionOfConcreteSymbolicValues(
      SMGState pNewState, BinaryOperator pOp, SMGValue pV1, SMGValue pV2) {

    boolean isPointerOp1 = pV1 instanceof SMGKnownAddressValue;
    boolean isPointerOp2 = pV2 instanceof SMGKnownAddressValue;

    boolean areEqual = pV1.equals(pV2);
    boolean areNonEqual = pNewState.areNonEqual(pV1, pV2);

    boolean isTrue = false;
    boolean isFalse = false;
    boolean impliesEqWhenFalse = false;
    boolean impliesNeqWhenTrue = false;
    boolean impliesEqWhenTrue = false;
    boolean impliesNeqWhenFalse = false;

    switch (pOp) {
      case NOT_EQUALS:
        isTrue = areNonEqual;
        isFalse = areEqual;
        impliesEqWhenFalse = true;
        impliesNeqWhenTrue = true;
        break;
      case EQUALS:
        isTrue = areEqual;
        isFalse = areNonEqual;
        impliesEqWhenTrue = true;
        impliesNeqWhenFalse = true;
        break;
      case GREATER_EQUAL:
      case LESS_EQUAL:
      case LESS_THAN:
      case GREATER_THAN:
        switch (pOp) {
          case LESS_EQUAL:
          case GREATER_EQUAL:
            if (areEqual) {
              isTrue = true;
              impliesEqWhenTrue = true;
              impliesNeqWhenFalse = true;
            } else {
              impliesNeqWhenFalse = true;
            }
            break;
          case GREATER_THAN:
          case LESS_THAN:
            if (areEqual) {
              isFalse = true;
            }

            impliesNeqWhenTrue = true;
            break;
          default:
            throw new AssertionError("Impossible case thrown");
        }
        break;
      default:
        throw new AssertionError("Binary Relation with non-relational operator: " + pOp);
    }

    if (isPointerOp1 && isPointerOp2) {
      isTrue = comparePointer((SMGKnownAddressValue) pV1, (SMGKnownAddressValue) pV2, pOp);
      isFalse =
          comparePointer(
              (SMGKnownAddressValue) pV1,
              (SMGKnownAddressValue) pV2,
              pOp.getOppositLogicalOperator());
    } else if (isPointerOp1 && !pV2.isUnknown()) {
      SMGKnownExpValue explicit2 = pNewState.getExplicit(pV2);
      if (explicit2 != null) {
        isTrue =
            comparePointer(
                (SMGKnownAddressValue) pV1,
                SMGKnownAddressValue.valueOf(
                    (SMGKnownSymbolicValue) pV2, SMGNullObject.INSTANCE, explicit2),
                pOp);
        isFalse =
            comparePointer(
                (SMGKnownAddressValue) pV1,
                SMGKnownAddressValue.valueOf(
                    (SMGKnownSymbolicValue) pV2, SMGNullObject.INSTANCE, explicit2),
                pOp.getOppositLogicalOperator());
      }
    } else if (isPointerOp2 && !pV1.isUnknown()) {
      SMGKnownExpValue explicit1 = pNewState.getExplicit(pV1);
      if (explicit1 != null) {
        isTrue =
            comparePointer(
                SMGKnownAddressValue.valueOf(
                    (SMGKnownSymbolicValue) pV1, SMGNullObject.INSTANCE, explicit1),
                (SMGKnownAddressValue) pV2,
                pOp);
        isFalse =
            comparePointer(
                SMGKnownAddressValue.valueOf(
                    (SMGKnownSymbolicValue) pV1, SMGNullObject.INSTANCE, explicit1),
                (SMGKnownAddressValue) pV2,
                pOp.getOppositLogicalOperator());
      }
    }

    BinaryRelationResult relationResult =
        new BinaryRelationResult(
            isTrue,
            isFalse,
            impliesEqWhenFalse,
            impliesNeqWhenFalse,
            impliesEqWhenTrue,
            impliesNeqWhenTrue,
            pV1,
            pV2);
    relations.put(pNewState, relationResult);

    if (isTrue) {
      return SMGValueAndState.of(pNewState, SMGKnownSymValue.TRUE);
    } else if (isFalse) {
      return SMGValueAndState.of(pNewState, SMGZeroValue.INSTANCE);
    } else {
      return SMGValueAndState.withUnknownValue(pNewState);
    }
  }

  public List<? extends SMGValueAndState> evaluateBinaryAssumption(
      SMGState pNewState, BinaryOperator pOp, SMGValue pV1, SMGValue pV2)
      throws SMGInconsistentException {

    // If a value is unknown, we can't make further assumptions about it.
    if (pV2.isUnknown() || pV1.isUnknown()) {
      return Collections.singletonList(SMGValueAndState.withUnknownValue(pNewState));
    }

    ImmutableList.Builder<SMGValueAndState> result = ImmutableList.builderWithExpectedSize(4);

    for (SMGValueAndState operand1AndState : getOperand(pNewState, pV1)) {
      SMGValue operand1 = operand1AndState.getObject();

      for (SMGValueAndState operand2AndState : getOperand(pNewState, pV2)) {
        SMGValue operand2 = operand2AndState.getObject();
        SMGState newState = operand2AndState.getSmgState();

        SMGValueAndState resultValueAndState =
            evaluateBinaryAssumptionOfConcreteSymbolicValues(newState, pOp, operand1, operand2);
        result.add(resultValueAndState);
      }
    }

    return result.build();
  }

  private List<? extends SMGValueAndState> getOperand(SMGState pNewState, SMGValue pV)
      throws SMGInconsistentException {
    if (isPointer(pNewState, pV)) {
      return smgExpressionEvaluator.getAddressFromSymbolicValue(SMGValueAndState.of(pNewState, pV));
    } else {
      return Collections.singletonList(SMGValueAndState.of(pNewState, pV));
    }
  }

  public boolean impliesEqOn(boolean pTruth, UnmodifiableSMGState pState) {
    if (!relations.containsKey(pState)) {
      return false;
    }
    return relations.get(pState).impliesEq(pTruth);
  }

  public boolean impliesNeqOn(boolean pTruth, UnmodifiableSMGState pState) {
    if (!relations.containsKey(pState)) {
      return false;
    }
    return relations.get(pState).impliesNeq(pTruth);
  }

  public SMGValue impliesVal1(UnmodifiableSMGState pState) {
    return relations.get(pState).getVal1();
  }

  public SMGValue impliesVal2(UnmodifiableSMGState pState) {
    return relations.get(pState).getVal2();
  }

  private static class BinaryRelationResult {

    private final boolean isTrue;
    private final boolean isFalse;

    private final boolean impliesEqWhenTrue;
    private final boolean impliesNeqWhenTrue;

    private final boolean impliesEqWhenFalse;
    private final boolean impliesNeqWhenFalse;

    private final SMGValue val1;
    private final SMGValue val2;

    /**
     * Creates an object of the BinaryRelationResult. The object is used to determine the relation
     * between two symbolic values in the context of the given smgState and the given binary
     * operator. Note that the given symbolic values, which may also be address values, do not have
     * to be part of the given Smg. The definition of an smg implies conditions for its values, even
     * if they are not part of it.
     *
     * @param pIsTrue boolean expression is true.
     * @param pIsFalse boolean expression is false
     * @param pImpliesEqWhenFalse if boolean expression is false, operands are equal
     * @param pImpliesNeqWhenFalse if boolean expression is false, operands are unequal
     * @param pImpliesEqWhenTrue if boolean expression is true, operands are equal
     * @param pImpliesNeqWhenTrue if boolean expression is true, operands are unequal
     * @param pVal1 operand 1 of boolean expression
     * @param pVal2 operand 2 of boolean expression
     */
    BinaryRelationResult(
        boolean pIsTrue,
        boolean pIsFalse,
        boolean pImpliesEqWhenFalse,
        boolean pImpliesNeqWhenFalse,
        boolean pImpliesEqWhenTrue,
        boolean pImpliesNeqWhenTrue,
        SMGValue pVal1,
        SMGValue pVal2) {
      isTrue = pIsTrue;
      isFalse = pIsFalse;
      impliesEqWhenFalse = pImpliesEqWhenFalse;
      impliesNeqWhenFalse = pImpliesNeqWhenFalse;
      impliesEqWhenTrue = pImpliesEqWhenTrue;
      impliesNeqWhenTrue = pImpliesNeqWhenTrue;
      val1 = pVal1;
      val2 = pVal2;
    }

    @SuppressWarnings("unused")
    public boolean isTrue() {
      return isTrue;
    }

    @SuppressWarnings("unused")
    public boolean isFalse() {
      return isFalse;
    }

    boolean impliesEq(boolean pTruth) {
      return pTruth ? impliesEqWhenTrue : impliesEqWhenFalse;
    }

    boolean impliesNeq(boolean pTruth) {
      return pTruth ? impliesNeqWhenTrue : impliesNeqWhenFalse;
    }

    SMGValue getVal2() {
      return val2;
    }

    SMGValue getVal1() {
      return val1;
    }
  }
}
