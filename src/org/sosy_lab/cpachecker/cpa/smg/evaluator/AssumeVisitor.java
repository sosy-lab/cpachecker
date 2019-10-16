/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.evaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.UnmodifiableSMGState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGZeroValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class AssumeVisitor extends ExpressionValueVisitor {

  private final Map<UnmodifiableSMGState, BinaryRelationResult> relations = new HashMap<>();

  public AssumeVisitor(SMGExpressionEvaluator pSmgExpressionEvaluator, CFAEdge pEdge, SMGState pSmgState) {
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
        SMGSymbolicValue leftSideVal = leftSideValAndState.getObject();
        SMGState newState = leftSideValAndState.getSmgState();

          for (SMGValueAndState rightSideValAndState :
              smgExpressionEvaluator.evaluateExpressionValue(newState, edge, rightSideExpression)) {
          SMGSymbolicValue rightSideVal = rightSideValAndState.getObject();
          newState = rightSideValAndState.getSmgState();

            for (SMGValueAndState resultValueAndState :
                evaluateBinaryAssumption(newState, binaryOperator, leftSideVal, rightSideVal)) {
              newState = resultValueAndState.getSmgState();
              SMGSymbolicValue resultValue = resultValueAndState.getObject();

              //TODO: separate modifiable and unmodifiable visitor
              int leftSideTypeSize = smgExpressionEvaluator.getBitSizeof(edge, leftSideExpression.getExpressionType(), newState);
              int rightSideTypeSize = smgExpressionEvaluator.getBitSizeof(edge, rightSideExpression.getExpressionType(), newState);
              newState.addPredicateRelation(leftSideVal, leftSideTypeSize,
                  rightSideVal, rightSideTypeSize, binaryOperator, edge);
              result.add(SMGValueAndState.of(newState, resultValue));
            }
        }
      }

        return result;
    default:
      return super.visit(pExp);
    }
  }

  private boolean isPointer(UnmodifiableSMGState pNewSmgState, SMGSymbolicValue symVal) {

    if (symVal.isUnknown()) {
      return false;
    }

    if (symVal.isZero()) {
      return true;
    }

    return pNewSmgState.getHeap().isPointer(symVal);
  }

  private boolean isUnequal(
      UnmodifiableSMGState pNewState,
      SMGSymbolicValue pValue1,
      SMGSymbolicValue pValue2,
      boolean isPointerOp1,
      boolean isPointerOp2) {

    if (isPointerOp1 && isPointerOp2) {
      return !pValue1.equals(pValue2);
    } else if ((isPointerOp1 && pValue2.isZero()) || (isPointerOp2 && pValue1.isZero())) {
      return !pValue1.equals(pValue2);
    } else {
      return pNewState.isInNeq(pValue1, pValue2);
    }
  }

  /** returns the comparison of two pointers, i.e. "p1 op p2". */
  private boolean comparePointer(
      SMGKnownAddressValue pV1, SMGKnownAddressValue pV2, BinaryOperator pOp) {

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
      default:
        throw new AssertionError("Impossible case thrown");
      }

    }
    return false;
  }

  private SMGValueAndState evaluateBinaryAssumptionOfConcreteSymbolicValues(
      SMGState pNewState, BinaryOperator pOp, SMGSymbolicValue pV1, SMGSymbolicValue pV2) {

    boolean isPointerOp1 = pV1 instanceof SMGKnownAddressValue;
    boolean isPointerOp2 = pV2 instanceof SMGKnownAddressValue;

    boolean areEqual = pV1.equals(pV2);
    boolean areNonEqual = (isUnequal(pNewState, pV1, pV2, isPointerOp1, isPointerOp2));

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
        if(areEqual) {
          isFalse = true;
        }

        impliesNeqWhenTrue = true;
        break;
      default:
        throw new AssertionError("Impossible case thrown");
      }

        if (isPointerOp1 && isPointerOp2) {
          isTrue = comparePointer((SMGKnownAddressValue) pV1, (SMGKnownAddressValue) pV2, pOp);
          isFalse = !isTrue;
        }
      break;
    default:
      throw new AssertionError(
          "Binary Relation with non-relational operator: " + pOp.toString());
    }

    BinaryRelationResult relationResult = new BinaryRelationResult(isTrue, isFalse, impliesEqWhenFalse, impliesNeqWhenFalse, impliesEqWhenTrue, impliesNeqWhenTrue, pV1, pV2);
    relations.put(pNewState, relationResult);

    if(isTrue) {
      return SMGValueAndState.of(pNewState, SMGKnownSymValue.TRUE);
    } else if(isFalse) {
      return SMGValueAndState.of(pNewState, SMGZeroValue.INSTANCE);
    } else {
      return SMGValueAndState.withUnknownValue(pNewState);
    }
  }

  public List<? extends SMGValueAndState> evaluateBinaryAssumption(
      SMGState pNewState, BinaryOperator pOp, SMGSymbolicValue pV1, SMGSymbolicValue pV2)
      throws SMGInconsistentException {

    // If a value is unknown, we can't make further assumptions about it.
    if (pV2.isUnknown() || pV1.isUnknown()) {
      return Collections.singletonList(SMGValueAndState.withUnknownValue(pNewState));
    }

    List<SMGValueAndState> result = new ArrayList<>(4);

    for (SMGValueAndState operand1AndState : getOperand(pNewState, pV1)) {
      SMGSymbolicValue operand1 = operand1AndState.getObject();

      for (SMGValueAndState operand2AndState : getOperand(pNewState, pV2)) {
        SMGSymbolicValue operand2 = operand2AndState.getObject();
        SMGState newState = operand2AndState.getSmgState();

        SMGValueAndState resultValueAndState = evaluateBinaryAssumptionOfConcreteSymbolicValues(newState, pOp, operand1, operand2);
        result.add(resultValueAndState);
      }
    }

    return result;
  }

  private List<? extends SMGValueAndState> getOperand(SMGState pNewState, SMGSymbolicValue pV)
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

  public SMGSymbolicValue impliesVal1(UnmodifiableSMGState pState) {
    return relations.get(pState).getVal1();
  }

  public SMGSymbolicValue impliesVal2(UnmodifiableSMGState pState) {
    return relations.get(pState).getVal2();
  }

  private static class BinaryRelationResult {

    private final boolean isTrue;
    private final boolean isFalse;

    private final boolean impliesEqWhenTrue;
    private final boolean impliesNeqWhenTrue;

    private final boolean impliesEqWhenFalse;
    private final boolean impliesNeqWhenFalse;

    private final SMGSymbolicValue val1;
    private final SMGSymbolicValue val2;

    /**
     * Creates an object of the BinaryRelationResult. The object is used to
     * determine the relation between two symbolic values in the context of
     * the given smgState and the given binary operator. Note that the given
     * symbolic values, which may also be address values, do not have to be
     * part of the given Smg. The definition of an smg implies conditions for
     * its values, even if they are not part of it.
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
        SMGSymbolicValue pVal1,
        SMGSymbolicValue pVal2) {
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

    SMGSymbolicValue getVal2() {
      return val2;
    }

    SMGSymbolicValue getVal1() {
      return val1;
    }
  }
}