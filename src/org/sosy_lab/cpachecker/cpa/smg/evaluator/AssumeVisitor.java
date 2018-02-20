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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGValueAndStateList;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownAddVal;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGSymbolicValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class AssumeVisitor extends ExpressionValueVisitor {

  private Map<SMGState,BinaryRelationResult> relations = new HashMap<>();

  public AssumeVisitor(SMGExpressionEvaluator pSmgExpressionEvaluator, CFAEdge pEdge, SMGState pSmgState) {
    super(pSmgExpressionEvaluator, pEdge, pSmgState);
  }

  @Override
  public SMGValueAndStateList visit(CBinaryExpression pExp)
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

      SMGValueAndStateList leftSideValAndStates = smgExpressionEvaluator.evaluateExpressionValue(getInitialSmgState(),
          edge, leftSideExpression);

      for (SMGValueAndState leftSideValAndState : leftSideValAndStates.getValueAndStateList()) {
        SMGSymbolicValue leftSideVal = leftSideValAndState.getObject();
        SMGState newState = leftSideValAndState.getSmgState();

        SMGValueAndStateList rightSideValAndStates = smgExpressionEvaluator.evaluateExpressionValue(
            newState, edge, rightSideExpression);

        for (SMGValueAndState rightSideValAndState : rightSideValAndStates.getValueAndStateList()) {
          SMGSymbolicValue rightSideVal = rightSideValAndState.getObject();
          newState = rightSideValAndState.getSmgState();

            SMGValueAndStateList resultValueAndStates = evaluateBinaryAssumption(newState,
                binaryOperator, leftSideVal, rightSideVal);

            for (SMGValueAndState resultValueAndState : resultValueAndStates.getValueAndStateList()) {
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

      return SMGValueAndStateList.copyOf(result);
    default:
      return super.visit(pExp);
    }
  }

  private boolean isPointer(SMGState pNewSmgState, SMGSymbolicValue symVal) {

    if (symVal.isUnknown()) {
      return false;
    }

    if (symVal instanceof SMGAddressValue) {
      return true;
    }

    if (pNewSmgState.isPointer(symVal.getAsInt())) {
      return true;
    } else {
      return false;
    }
  }

  private boolean isUnequal(SMGState pNewState, SMGSymbolicValue pValue1,
      SMGSymbolicValue pValue2, boolean isPointerOp1,
      boolean isPointerOp2) {

    int value1 = pValue1.getAsInt();
    int value2 = pValue2.getAsInt();

    if (isPointerOp1 && isPointerOp2) {

      return value1 != value2;
    } else if ((isPointerOp1 && value2 == 0) || (isPointerOp2 && value1 == 0)) {
      return value1 != value2;
    } else {
      return pNewState.isInNeq(pValue1, pValue2);
    }
  }

  private PointerComparisonResult comparePointer(SMGKnownAddVal pV1, SMGKnownAddVal pV2, BinaryOperator pOp) {

    SMGObject object1 = pV1.getObject();
    SMGObject object2 = pV2.getObject();

    boolean isTrue = false;
    boolean isFalse = true;

    // there can be more precise comparison when pointer point to the same object.
    if (object1 == object2) {
      int offset1 = pV1.getOffset().getAsInt();
      int offset2 = pV2.getOffset().getAsInt();

      switch (pOp) {
      case GREATER_EQUAL:
        isTrue = offset1 >= offset2;
        isFalse = !isTrue;
        break;
      case GREATER_THAN:
        isTrue = offset1 > offset2;
        isFalse = !isTrue;
        break;
      case LESS_EQUAL:
        isTrue = offset1 <= offset2;
        isFalse = !isTrue;
        break;
      case LESS_THAN:
        isTrue = offset1 < offset2;
        isFalse = !isTrue;
        break;
      default:
        throw new AssertionError("Impossible case thrown");
      }

    }
    return PointerComparisonResult.valueOf(isTrue, isFalse);
  }

  private SMGValueAndState evaluateBinaryAssumptionOfConcreteSymbolicValues(SMGState pNewState, BinaryOperator pOp, SMGKnownSymValue pV1, SMGKnownSymValue pV2) {

    boolean isPointerOp1 = pV1 instanceof SMGKnownAddVal;
    boolean isPointerOp2 = pV2 instanceof SMGKnownAddVal;

    int v1 = pV1.getAsInt();
    int v2 = pV2.getAsInt();

    boolean areEqual = (v1 == v2);
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
          SMGKnownAddVal p1 = (SMGKnownAddVal) pV1;
          SMGKnownAddVal p2 = (SMGKnownAddVal) pV2;
          PointerComparisonResult result = comparePointer(p1, p2, pOp);
          isFalse = result.isFalse();
          isTrue = result.isTrue();
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
      return SMGValueAndState.of(pNewState, SMGKnownSymValue.FALSE);
    } else {
      return SMGValueAndState.of(pNewState);
    }
  }

  public SMGValueAndStateList evaluateBinaryAssumption(SMGState pNewState, BinaryOperator pOp, SMGSymbolicValue pV1, SMGSymbolicValue pV2) throws SMGInconsistentException {

    // If a value is unknown, we can't make further assumptions about it.
    if (pV2.isUnknown() || pV1.isUnknown()) {
      return SMGValueAndStateList.of(pNewState);
    }

    boolean isPointerOp1 = isPointer(pNewState, pV1);
    boolean isPointerOp2 = isPointer(pNewState, pV2);

    SMGValueAndStateList operand1AndStates;

    if(isPointerOp1) {
      operand1AndStates = smgExpressionEvaluator.getAddressFromSymbolicValue(SMGValueAndState.of(pNewState, pV1));
    } else {
      operand1AndStates = SMGValueAndStateList.of(pNewState, pV1);
    }

    List<SMGValueAndState> result = new ArrayList<>(4);

    for(SMGValueAndState operand1AndState : operand1AndStates.getValueAndStateList()) {

      SMGKnownSymValue operand1 = (SMGKnownSymValue) operand1AndState.getObject();
      SMGState newState = operand1AndState.getSmgState();

      SMGValueAndStateList operand2AndStates;

      if(isPointerOp2) {
        operand2AndStates = smgExpressionEvaluator.getAddressFromSymbolicValue(SMGValueAndState.of(newState, pV2));
      } else {
        operand2AndStates = SMGValueAndStateList.of(pNewState, pV2);
      }

      for (SMGValueAndState operand2AndState : operand2AndStates.getValueAndStateList()) {

        SMGKnownSymValue operand2 = (SMGKnownSymValue) operand2AndState.getObject();
        newState = operand2AndState.getSmgState();

        SMGValueAndState resultValueAndState = evaluateBinaryAssumptionOfConcreteSymbolicValues(newState, pOp, operand1, operand2);
        result.add(resultValueAndState);
      }
    }

    return SMGValueAndStateList.copyOf(result);
  }

  public boolean impliesEqOn(boolean pTruth, SMGState pState) {
    if (!relations.containsKey(pState)) {
      return false;
    }
    return relations.get(pState).impliesEq(pTruth);
  }

  public boolean impliesNeqOn(boolean pTruth, SMGState pState) {
    if (!relations.containsKey(pState)) {
      return false;
    }
    return relations.get(pState).impliesNeq(pTruth);
  }

  public SMGSymbolicValue impliesVal1(SMGState pState) {
    return relations.get(pState).getVal1();
  }

  public SMGSymbolicValue impliesVal2(SMGState pState) {
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
     *
     */
    public BinaryRelationResult(boolean pIsTrue, boolean pIsFalse, boolean pImpliesEqWhenFalse,
        boolean pImpliesNeqWhenFalse, boolean pImpliesEqWhenTrue, boolean pImpliesNeqWhenTrue,
        SMGSymbolicValue pVal1, SMGSymbolicValue pVal2) {
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

    public boolean impliesEq(boolean pTruth) {
      return pTruth ? impliesEqWhenTrue : impliesEqWhenFalse;
    }

    public boolean impliesNeq(boolean pTruth) {
      return pTruth ? impliesNeqWhenTrue : impliesNeqWhenFalse;
    }

    public SMGSymbolicValue getVal2() {
      return val2;
    }

    public SMGSymbolicValue getVal1() {
      return val1;
    }
  }

  private static class PointerComparisonResult {

    private final boolean isTrue;
    private final boolean isFalse;

    private PointerComparisonResult(boolean pIsTrue, boolean pIsFalse) {
      isTrue = pIsTrue;
      isFalse = pIsFalse;
    }

    public static PointerComparisonResult valueOf(boolean pIsTrue, boolean pIsFalse) {
      return new PointerComparisonResult(pIsTrue, pIsFalse);
    }

    public boolean isTrue() {
      return isTrue;
    }

    public boolean isFalse() {
      return isFalse;
    }
  }
}