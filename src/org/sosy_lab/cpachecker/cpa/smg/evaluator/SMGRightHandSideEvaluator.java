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

import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGExplicitValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGNullObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

/**
 * The class {@link SMGExpressionEvaluator} is meant to evaluate
 * a expression using an arbitrary SMGState. Thats why it does not
 * permit semantic changes of the state it uses. This class implements
 * additionally the changes that occur while calculating the next smgState
 * in the Transfer Relation. These mainly include changes when evaluating
 * functions. They also contain code that should only be executed during
 * the calculation of the next SMG State, e.g. logging.
 */
public class SMGRightHandSideEvaluator extends SMGExpressionEvaluator {

  final SMGTransferRelation smgTransferRelation;
  final SMGOptions options;

  public SMGRightHandSideEvaluator(SMGTransferRelation pSmgTransferRelation,
      LogManagerWithoutDuplicates pLogger, MachineModel pMachineModel, SMGOptions pOptions) {
    super(pLogger, pMachineModel);
    smgTransferRelation = pSmgTransferRelation;
    options = pOptions;
  }

  public SMGExplicitValueAndState forceExplicitValue(SMGState smgState,
      CFAEdge pCfaEdge, CRightHandSide rVal)
      throws UnrecognizedCCodeException {

    ForceExplicitValueVisitor v =
        new ForceExplicitValueVisitor(
            this, this, smgState, null, machineModel, logger, pCfaEdge, options);

    Value val = rVal.accept(v);

    if (val.isUnknown()) {
      return SMGExplicitValueAndState.of(v.getNewState());
    }

    return SMGExplicitValueAndState.of(v.getNewState(),
        SMGKnownExpValue.valueOf(val.asNumericValue().longValue()));
  }

  public SMGState deriveFurtherInformation(SMGState pNewState, boolean pTruthValue, CFAEdge pCfaEdge, CExpression rValue)
      throws CPATransferException {
    AssigningValueVisitor v = new AssigningValueVisitor(this, pNewState, pTruthValue, pCfaEdge);

    rValue.accept(v);
    return v.getAssignedState();
  }

  @Override
  public SMGValueAndState readValue(SMGState pSmgState, SMGObject pObject,
      SMGExplicitValue pOffset, CType pType, CFAEdge pEdge)
      throws SMGInconsistentException, UnrecognizedCCodeException {

    if (pOffset.isUnknown() || pObject == null) {
      SMGState errState = pSmgState.setInvalidRead();
      errState.setErrorDescription("Can't evaluate offset or object");
      return SMGValueAndState.of(errState);
    }

    long fieldOffset = pOffset.getAsLong();
    int typeBitSize = getBitSizeof(pEdge, pType, pSmgState);
    int objectBitSize = pObject.getSize();

    //FIXME Does not work with variable array length.
    boolean doesNotFitIntoObject = fieldOffset < 0
        || fieldOffset + typeBitSize > objectBitSize;

    if (doesNotFitIntoObject) {
      SMGState errState = pSmgState.setInvalidRead();
      // Field does not fit size of declared Memory
      logger.log(Level.INFO, pEdge.getFileLocation(), ":", "Field ", "(",
           fieldOffset, ", ", pType.toASTString(""), ")",
          " does not fit object ", pObject, ".");
      if (!pObject.equals(SMGNullObject.INSTANCE)) {
        if (typeBitSize % 8 != 0 || fieldOffset % 8 != 0 || objectBitSize % 8 != 0) {
          errState.setErrorDescription("Field with " + typeBitSize
              + " bit size can't be read from offset " + fieldOffset + " bit of "
              + "object " + objectBitSize + " bit size");
        } else {
          errState.setErrorDescription("Field with " + typeBitSize / 8
              + " byte size can't be read from offset " + fieldOffset / 8 + " byte of "
              + "object " + objectBitSize / 8 + " byte size");

        }
        errState.addInvalidObject(pObject);
      } else {
        errState.setErrorDescription("NULL pointer dereference on read");
      }
      return SMGValueAndState.of(errState);
    }

    return pSmgState.forceReadValue(pObject, fieldOffset, pType);
  }

  /**
   * Write a value into the SMG.
   *
   * @param pState state with the SMG to be modified.
   * @param pMemoryOfField target-memory where to be written to.
   * @param pFieldOffset offset in pMemoryOfField.
   * @param pRValueType the type of the data to be written (should match into pMemoryOfField minus
   *     pFieldOffset.
   * @param pValue the new value
   * @param pEdge edge for logging
   */
  public SMGState writeValue(
      SMGState pState,
      SMGObject pMemoryOfField,
      long pFieldOffset,
      CType pRValueType,
      SMGSymbolicValue pValue,
      CFAEdge pEdge)
      throws SMGInconsistentException, UnrecognizedCCodeException {

    // FIXME Does not work with variable array length.
    // TODO: write value with bit precise size
    int memoryBitSize = pMemoryOfField.getSize();
    int rValueTypeBitSize = getBitSizeof(pEdge, pRValueType, pState);
    boolean doesNotFitIntoObject =
        pFieldOffset < 0 || pFieldOffset + rValueTypeBitSize > memoryBitSize;

    if (doesNotFitIntoObject) {
      // Field does not fit size of declared Memory
      logger.log(
          Level.INFO,
          () ->
              String.format(
                  "%s: Field (%d, %s) does not fit object %s.",
                  pEdge.getFileLocation(),
                  pFieldOffset,
                  pRValueType.toASTString(""),
                  pMemoryOfField));
      SMGState newState = pState.setInvalidWrite();
      if (!pMemoryOfField.equals(SMGNullObject.INSTANCE)) {
        if (rValueTypeBitSize % 8 != 0 || pFieldOffset % 8 != 0 || memoryBitSize % 8 != 0) {
          newState.setErrorDescription(
              "Field with size "
                  + rValueTypeBitSize
                  + " bit can't be written at offset "
                  + pFieldOffset
                  + " bit of object "
                  + memoryBitSize
                  + " bit size");
        } else {
          newState.setErrorDescription(
              "Field with size "
                  + rValueTypeBitSize / 8
                  + " byte can't "
                  + "be written at offset "
                  + pFieldOffset / 8
                  + " byte of object "
                  + memoryBitSize / 8
                  + " byte size");
        }
        newState.addInvalidObject(pMemoryOfField);
      } else {
        newState.setErrorDescription("NULL pointer dereference on write");
      }
      return newState;
    }

    if (pValue.isUnknown()) {
      return pState;
    }

    if (pRValueType instanceof CPointerType
        && !(pValue instanceof SMGAddressValue)
        && pValue instanceof SMGKnownSymValue) {
        SMGExplicitValue explicit = pState.getExplicit((SMGKnownSymValue) pValue);
        if (!explicit.isUnknown()) {
          pValue =
              SMGKnownAddressValue.valueOf(
                  SMGNullObject.INSTANCE, (SMGKnownExpValue) explicit, (SMGKnownSymValue) pValue);
      }
    }
    return pState.writeValue(pMemoryOfField, pFieldOffset, pRValueType, pValue).getState();
  }

  @Override
  PointerVisitor getPointerVisitor(CFAEdge pCfaEdge, SMGState pNewState) {
    return new RHSPointerAddressVisitor(this, pCfaEdge, pNewState);
  }

  @Override
  ExpressionValueVisitor getExpressionValueVisitor(CFAEdge pCfaEdge, SMGState pNewState) {
    return new RHSExpressionValueVisitor(this, pCfaEdge, pNewState);
  }

  @Override
  public LValueAssignmentVisitor getLValueAssignmentVisitor(CFAEdge pCfaEdge, SMGState pNewState) {
    return new RHSLValueAssignmentVisitor(this, pCfaEdge, pNewState);
  }

  @Override
  RHSCSizeOfVisitor getSizeOfVisitor(
      CFAEdge pEdge, SMGState pState, Optional<CExpression> pExpression) {
    return new RHSCSizeOfVisitor(this, pEdge, pState, pExpression);
  }

  @Override
  protected SMGValueAndState handleUnknownDereference(SMGState pSmgState, CFAEdge pEdge) {
    return super.handleUnknownDereference(pSmgState.setUnknownDereference(), pEdge);
  }
}
