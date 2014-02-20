/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
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
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitState.MemoryLocation;
import org.sosy_lab.cpachecker.cpa.smg.SMGExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg.SMGExpressionEvaluator.LValueAssignmentVisitor;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGUnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;


/**
 * Called communicator and not Evaluator because in the future
 * this will only contain the necessary methods to extract the
 * values and addresses from the respective States, evaluation
 * will be done in another class.
 */
public class SMGExplicitCommunicator {

  private final CFAEdge cfaEdge;
  private final LogManager logger;
  private final MachineModel machineModel;
  private final SMGState smgState;
  private final ExplicitState explicitState;
  private final String functionName;

  public SMGExplicitCommunicator(ExplicitState pExplicitState, String pFunctionName,
      SMGState pSmgState, MachineModel pMachineModel, LogManager pLogger, CFAEdge pCfaEdge) {
    explicitState = pExplicitState;
    smgState = pSmgState;
    machineModel = pMachineModel;
    logger = pLogger;
    cfaEdge = pCfaEdge;
    functionName = pFunctionName;
  }

  public ExplicitValueBase evaluateExpression(CRightHandSide rValue) throws UnrecognizedCCodeException {

    ExplicitExpressionValueVisitor evv =
        new ExplicitExpressionValueVisitor();
    return rValue.accept(evv);
  }

  public SMGSymbolicValue evaluateSMGExpression(CRightHandSide rValue)
      throws CPATransferException {

    SMGExplicitExpressionEvaluator eee =
        new SMGExplicitExpressionEvaluator();
    return eee.evaluateExpressionValue(smgState, cfaEdge, rValue);
  }

  public SMGAddressValue evaluateSMGAddressExpression(CRightHandSide rValue)
      throws CPATransferException {

    SMGExplicitExpressionEvaluator eee =
        new SMGExplicitExpressionEvaluator();
    return eee.evaluateAddress(smgState, cfaEdge, rValue);
  }

  public MemoryLocation evaluateLeftHandSide(CExpression lValue)
      throws UnrecognizedCCodeException {
    ExplicitExpressionValueVisitor evv =
        new ExplicitExpressionValueVisitor();
    return evv.evaluateMemloc(lValue);
  }

  public SMGAddress evaluateSMGLeftHandSide(CExpression lValue)
      throws UnrecognizedCCodeException {

    ExplicitExpressionValueVisitor evv =
        new ExplicitExpressionValueVisitor();
    return evv.evaluateAddress(lValue);
  }

  private class ExplicitExpressionValueVisitor extends org.sosy_lab.cpachecker.cpa.explicit.ExplicitExpressionValueVisitor {

    private final SMGExplicitExpressionEvaluator smgEvaluator;

    public ExplicitExpressionValueVisitor() {
      super(explicitState, functionName, machineModel, logger, cfaEdge);
      smgEvaluator = new SMGExplicitExpressionEvaluator(this);
    }

    public ExplicitExpressionValueVisitor(SMGExplicitExpressionEvaluator pSmgEvaluator) {
      super(explicitState, functionName, machineModel, logger, cfaEdge);
      smgEvaluator = pSmgEvaluator;
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
    public ExplicitValueBase visit(CPointerExpression pPointerExpression) throws UnrecognizedCCodeException {

      MemoryLocation memloc = evaluateMemloc(pPointerExpression);
      return getValueFromLocation(memloc, pPointerExpression);
    }

    @Override
    public ExplicitValueBase visit(CFieldReference pFieldReferenceExpression) throws UnrecognizedCCodeException {

      MemoryLocation memloc = evaluateMemloc(pFieldReferenceExpression);
      return getValueFromLocation(memloc, pFieldReferenceExpression);
    }

    @Override
    public ExplicitValueBase visit(CUnaryExpression pUnaryExpression) throws UnrecognizedCCodeException {

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
            return ExplicitValueBase.ExplicitUnknownValue.getInstance();
          } else if (address.getAsInt() == 0) {
            return new ExplicitNumericValue(1L);
          } else {
            return new ExplicitNumericValue(0L);
          }
        default:
          return super.visit(pUnaryExpression);
        }

      } else {
        return super.visit(pUnaryExpression);
      }
    }

    @Override
    public ExplicitValueBase visit(CBinaryExpression pE) throws UnrecognizedCCodeException {
      ExplicitValueBase result = super.visit(pE);
      if (!result.isUnknown()) {
        return result;
      }

      CExpression op1 = pE.getOperand1();
      CExpression op2 = pE.getOperand2();

      if (!(op1.getExpressionType().getCanonicalType() instanceof CPointerType)) {
        return ExplicitValueBase.ExplicitUnknownValue.getInstance();
      }
      if (!(op2.getExpressionType().getCanonicalType() instanceof CPointerType)) {
        return ExplicitValueBase.ExplicitUnknownValue.getInstance();
      }

      try {
        SMGAddressValue op1Value = evaluateSMGAddressExpression(op1);
        SMGAddressValue op2Value = evaluateSMGAddressExpression(op2);
        if (op1Value.isUnknown() || op2Value.isUnknown()) {
          return ExplicitValueBase.ExplicitUnknownValue.getInstance();
        }

        if (op1Value.getOffset().isUnknown() || op2Value.getOffset().isUnknown()) {
          return ExplicitValueBase.ExplicitUnknownValue.getInstance();
        }
        long op1Offset = op1Value.getOffset().getAsLong();
        long op2Offset = op2Value.getOffset().getAsLong();

        switch (pE.getOperator()) {
        case EQUALS:
          return booleanAsLong(op1Value.getObject().equals(op2Value.getObject())
                              && op1Offset == op2Offset);

        case NOT_EQUALS:
          return booleanAsLong(!(op1Value.getObject().equals(op2Value.getObject())
                              && op1Offset == op2Offset));

        case GREATER_EQUAL:
        case GREATER_THAN:
        case LESS_EQUAL:
        case LESS_THAN:
          if (!op1Value.getObject().equals(op2Value.getObject())) {
            return ExplicitValueBase.ExplicitUnknownValue.getInstance();
          }
          switch (pE.getOperator()) {
          case GREATER_EQUAL:
            return booleanAsLong(op1Offset >= op2Offset);
          case GREATER_THAN:
            return booleanAsLong(op1Offset > op2Offset);
          case LESS_EQUAL:
            return booleanAsLong(op1Offset <= op2Offset);
          case LESS_THAN:
            return booleanAsLong(op1Offset < op2Offset);
          default:
            throw new AssertionError();
          }

        default:
          return ExplicitValueBase.ExplicitUnknownValue.getInstance();
        }
      } catch (CPATransferException e) {
        UnrecognizedCCodeException e2 = new UnrecognizedCCodeException("Could not symbolically evaluate expression", pE);
        e2.initCause(e);
        throw e2;
      }
    }

    private ExplicitValueBase booleanAsLong(boolean b) {
      return b ? new ExplicitNumericValue(1L) : new ExplicitNumericValue(0L);
    }

    @Override
    public ExplicitValueBase visit(CArraySubscriptExpression pE) throws UnrecognizedCCodeException {
      MemoryLocation memloc = evaluateMemloc(pE);
      return getValueFromLocation(memloc, pE);
    }

    private ExplicitValueBase getValueFromLocation(MemoryLocation pMemloc, CExpression rValue) throws UnrecognizedCCodeException {

      if(pMemloc == null) {
        return ExplicitValueBase.ExplicitUnknownValue.getInstance();
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
           return ExplicitValueBase.ExplicitUnknownValue.getInstance();
         } else {
           return new ExplicitNumericValue(0L);
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

  private class SMGExplicitExpressionEvaluator extends SMGExpressionEvaluator {

    ExplicitExpressionValueVisitor evv;

    public SMGExplicitExpressionEvaluator(ExplicitExpressionValueVisitor pEvv) {
      super(logger, machineModel);
      evv = pEvv;
    }

    @Override
    public MachineModel getMachineModel() {
      return super.getMachineModel();
    }

    public SMGExplicitExpressionEvaluator() {
      super(logger, machineModel);
      evv = new ExplicitExpressionValueVisitor(this);
    }

    @Override
    public SMGExplicitValue evaluateExplicitValue(SMGState pSmgState, CFAEdge pCfaEdge, CRightHandSide pRValue)
        throws CPATransferException {
      Long value = pRValue.accept(evv).asLong(pRValue.getExpressionType());

      if (value == null) {
        return SMGUnknownValue.getInstance();
      } else {
        return SMGKnownExpValue.valueOf(value);
      }
    }
  }
}
