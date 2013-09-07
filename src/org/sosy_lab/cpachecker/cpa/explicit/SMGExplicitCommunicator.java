/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.explicit;

import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.cpalien.SMGExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.cpalien.SMGExpressionEvaluator.LValueAssignmentVisitor;
import org.sosy_lab.cpachecker.cpa.cpalien.SMGState;
import org.sosy_lab.cpachecker.cpa.cpalien.SMGTransferRelation.SMGAddress;
import org.sosy_lab.cpachecker.cpa.cpalien.SMGTransferRelation.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.cpalien.SMGTransferRelation.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.cpalien.SMGTransferRelation.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.cpalien.SMGTransferRelation.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.cpalien.SMGTransferRelation.SMGUnknownValue;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitState.MemoryLocation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;





/**
 * Called communicator and not Evaluator because in the future
 * this will only contain the necessary methods to extract the
 * values and addresses from the respective States, evaluation
 * will be done in another class.
 */
public class SMGExplicitCommunicator {

  public Long evaluateExpression(ExplicitState pState, String pFunctionName,
      SMGState pSmgState, MachineModel machineModel, LogManager pLogger, CFAEdge pCfaEdge,
      CRightHandSide rValue) throws UnrecognizedCCodeException {

    ExplicitExpressionValueVisitor evv =
        new ExplicitExpressionValueVisitor(pState, pFunctionName, pSmgState, machineModel, pLogger, pCfaEdge);
    return rValue.accept(evv);
  }

  public SMGSymbolicValue evaluateSMGExpression(ExplicitState pState, String pFunctionName,
      SMGState pSmgState, MachineModel machineModel, LogManager pLogger, CFAEdge pCfaEdge,
      CRightHandSide rValue) throws CPATransferException {

    SMGExplicitExpressionEvaluator eee =
        new SMGExplicitExpressionEvaluator(pLogger, machineModel, pState, pFunctionName, pSmgState, pCfaEdge);
    return eee.evaluateExpressionValue(pSmgState, pCfaEdge, rValue);
  }

  public SMGAddressValue evaluateSMGAddressExpression(ExplicitState pState, String pFunctionName,
      SMGState pSmgState, MachineModel machineModel, LogManager pLogger, CFAEdge pCfaEdge,
      CRightHandSide rValue) throws CPATransferException {

    SMGExplicitExpressionEvaluator eee =
        new SMGExplicitExpressionEvaluator(pLogger, machineModel, pState, pFunctionName, pSmgState, pCfaEdge);
    return eee.evaluateAddress(pSmgState, pCfaEdge, rValue);
  }

  public MemoryLocation evaluateLeftHandSide(ExplicitState pState, String pFunctionName,
      SMGState pSmgState, MachineModel machineModel, LogManager pLogger, CFAEdge pCfaEdge,
      CExpression lValue) throws UnrecognizedCCodeException {

    ExplicitExpressionValueVisitor evv =
        new ExplicitExpressionValueVisitor(pState, pFunctionName, pSmgState, machineModel, pLogger, pCfaEdge);
    return evv.evaluateMemloc(lValue);
  }

  public SMGAddress evaluateSMGLeftHandSide(ExplicitState pState, String pFunctionName,
      SMGState pSmgState, MachineModel machineModel, LogManager pLogger, CFAEdge pCfaEdge,
      CExpression lValue) throws UnrecognizedCCodeException {

    ExplicitExpressionValueVisitor evv =
        new ExplicitExpressionValueVisitor(pState, pFunctionName, pSmgState, machineModel, pLogger, pCfaEdge);
    return evv.evaluateAddress(lValue);
  }

  private static class ExplicitExpressionValueVisitor extends ExpressionValueVisitor {

    private final SMGExplicitExpressionEvaluator smgEvaluator;
    private final SMGState smgState;
    private final CFAEdge cfaEdge;
    private final LogManager logger;

    public ExplicitExpressionValueVisitor(ExplicitState pState, String pFunctionName,
        SMGState pSmgState, MachineModel machineModel, LogManager pLogger, CFAEdge pCfaEdge) {
      super(pState, pFunctionName, machineModel);
      smgEvaluator = new SMGExplicitExpressionEvaluator(pLogger, machineModel, this);
      smgState = pSmgState;
      cfaEdge = pCfaEdge;
      logger = pLogger;
    }

    public ExplicitExpressionValueVisitor(ExplicitState pState, String pFunctionName,
        SMGState pSmgState, CFAEdge pCfaEdge, SMGExplicitExpressionEvaluator pSmgEvaluator) {
      super(pState, pFunctionName, pSmgEvaluator.getMachineModel());
      smgEvaluator = pSmgEvaluator;
      smgState = pSmgState;
      cfaEdge = pCfaEdge;
      logger = smgEvaluator.getLogger();
    }

    public SMGAddress evaluateAddress(CExpression pOperand) throws UnrecognizedCCodeException {
      SMGAddress value;

      LValueAssignmentVisitor visitor = smgEvaluator.getLValueAssignmentVisitor(cfaEdge, smgState);

      try {
        value = pOperand.accept(visitor);
      } catch (CPATransferException e) {
        if (e instanceof UnrecognizedCCodeException) {
          throw (UnrecognizedCCodeException) e;
        } else {
          logger.logDebugException(e);
          throw new UnrecognizedCCodeException("Could not evluate Address of " + pOperand.toASTString(),
              cfaEdge, pOperand);
        }
      }

      return value;
    }

    @Override
    public Long visit(CPointerExpression pPointerExpression) throws UnrecognizedCCodeException {

      MemoryLocation memloc = evaluateMemloc(pPointerExpression);
      return getValueFromLocation(memloc, pPointerExpression);
    }

    @Override
    public Long visit(CFieldReference pFieldReferenceExpression) throws UnrecognizedCCodeException {

      MemoryLocation memloc = evaluateMemloc(pFieldReferenceExpression);
      return getValueFromLocation(memloc, pFieldReferenceExpression);
    }

    @Override
    public Long visit(CUnaryExpression pUnaryExpression) throws UnrecognizedCCodeException {

      CExpression unaryOperand = pUnaryExpression.getOperand();

      CType unaryOperandType = unaryOperand.getExpressionType().getCanonicalType();

      if (unaryOperandType instanceof CArrayType || unaryOperandType instanceof CPointerType) {

        UnaryOperator unaryOperator = pUnaryExpression.getOperator();

        switch (unaryOperator) {
        case NOT:

          SMGSymbolicValue address;

          try {
            address = smgEvaluator.evaluateAddress(smgState, cfaEdge, unaryOperand);
          } catch (CPATransferException e) {
            logger.logUserException(Level.SEVERE, e, e.getMessage());
            throw new UnrecognizedCCodeException("Could not evaluate address of "
                + unaryOperand.toASTString(),
                unaryOperand);
          }

          if (address.isUnknown()) {
            return null;
          } else if (address.getAsInt() == 0) {
            return 1L;
          } else {
            return 0L;
          }
        default:
          return super.visit(pUnaryExpression);
        }

      } else {
        return super.visit(pUnaryExpression);
      }
    }

    @Override
    public Long visit(CArraySubscriptExpression pE) throws UnrecognizedCCodeException {
      MemoryLocation memloc = evaluateMemloc(pE);
      return getValueFromLocation(memloc, pE);
    }

    private Long getValueFromLocation(MemoryLocation pMemloc, CExpression rValue) throws UnrecognizedCCodeException {

      if(pMemloc == null) {
        return null;
      }

      ExplicitState explState = getState();

      if (explState.contains(pMemloc) ) {
        return explState.getValueFor(pMemloc);
      } else {
        // In rare cases, through reinterpretations of nullified blocks, smgState can ecaluate an
        // explicit Value while explicit State cannot.
        // TODO Erase when null reinterpretations an memset is implemented for explicit state
        SMGSymbolicValue value;

        try {
          value = smgEvaluator.evaluateExpressionValue(smgState, cfaEdge, rValue);
        } catch (CPATransferException e) {
          logger.logDebugException(e);
          throw new UnrecognizedCCodeException("Rvalue Could not be evaluated by smgEvaluator", cfaEdge,rValue);
        }

         if(value.isUnknown() || value.getAsInt() != 0) {
           return null;
         } else {
           return 0L;
         }
      }
    }

    public MemoryLocation evaluateMemloc(CExpression pOperand) throws UnrecognizedCCodeException {

      SMGAddress value = evaluateAddress(pOperand);

      if(value == null || value.isUnknown()) {
        return null;
      } else {
        return smgState.resolveMemLoc(value, getFunctionName());
      }
    }
  }

  private static class SMGExplicitExpressionEvaluator extends SMGExpressionEvaluator {

    ExplicitExpressionValueVisitor evv;

    public SMGExplicitExpressionEvaluator(LogManager pLogger, MachineModel pMachineModel,
        ExplicitExpressionValueVisitor pEvv) {
      super(pLogger, pMachineModel);
      evv = pEvv;
    }

    @Override
    public MachineModel getMachineModel() {
      return super.getMachineModel();
    }

    public SMGExplicitExpressionEvaluator(LogManager pLogger, MachineModel pMachineModel, ExplicitState pExplicitState,
        String pFunctionName, SMGState pSmgState, CFAEdge pCfaEdge) {
      super(pLogger, pMachineModel);
      evv = new ExplicitExpressionValueVisitor(pExplicitState, pFunctionName, pSmgState, pCfaEdge, this);
    }

    @Override
    public SMGExplicitValue evaluateExplicitValue(SMGState pSmgState, CFAEdge pCfaEdge, CRightHandSide pRValue)
        throws CPATransferException {
      Long value = pRValue.accept(evv);

      if (value == null) {
        return SMGUnknownValue.getInstance();
      } else {
        return SMGKnownExpValue.valueOf(value);
      }
    }
  }
}
