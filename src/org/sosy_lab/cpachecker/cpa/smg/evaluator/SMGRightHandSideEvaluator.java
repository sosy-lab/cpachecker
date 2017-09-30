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
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.interval.NumberInterface;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGExplicitValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
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

    ForceExplicitValueVisitor v = new ForceExplicitValueVisitor(this, this, smgState,
        null, machineModel, logger, pCfaEdge);

    NumberInterface val = rVal.accept(v);

    if (val.isUnknown()) {
      return SMGExplicitValueAndState.of(v.getNewState());
    }

    return SMGExplicitValueAndState.of(v.getNewState(),
        SMGKnownExpValue.valueOf(val.getNumber().longValue()));
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
      return SMGValueAndState.of(pSmgState.setInvalidRead());
    }

    int fieldOffset = pOffset.getAsInt();

    //FIXME Does not work with variable array length.
    boolean doesNotFitIntoObject = fieldOffset < 0
        || fieldOffset + getBitSizeof(pEdge, pType, pSmgState) > pObject.getSize();

    if (doesNotFitIntoObject) {
      // Field does not fit size of declared Memory
      logger.log(Level.INFO, pEdge.getFileLocation(), ":", "Field ", "(",
           fieldOffset, ", ", pType.toASTString(""), ")",
          " does not fit object ", pObject, ".");

      return SMGValueAndState.of(pSmgState.setInvalidRead());
    }

    return pSmgState.forceReadValue(pObject, fieldOffset, pType);
  }

  @Override
  public PointerVisitor getPointerVisitor(CFAEdge pCfaEdge, SMGState pNewState) {
    return new RHSPointerAddressVisitor(this, pCfaEdge, pNewState);
  }

  @Override
  public ExpressionValueVisitor getExpressionValueVisitor(CFAEdge pCfaEdge, SMGState pNewState) {
    return new RHSExpressionValueVisitor(this, pCfaEdge, pNewState);
  }

  @Override
  public LValueAssignmentVisitor getLValueAssignmentVisitor(CFAEdge pCfaEdge, SMGState pNewState) {
    return new RHSLValueAssignmentVisitor(this, pCfaEdge, pNewState);
  }

  @Override
  protected RHSCSizeOfVisitor getSizeOfVisitor(CFAEdge pEdge, SMGState pState, Optional<CExpression> pExpression) {
    return new RHSCSizeOfVisitor(this, pEdge, pState, pExpression);
  }

  @Override
  protected SMGValueAndState handleUnknownDereference(SMGState pSmgState, CFAEdge pEdge) {
    return super.handleUnknownDereference(pSmgState.setUnknownDereference(), pEdge);
  }
}