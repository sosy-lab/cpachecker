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
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.interval.NumberInterface;
import org.sosy_lab.cpachecker.cpa.smg.SMGBuiltins;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelationKind;
import org.sosy_lab.cpachecker.cpa.smg.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressValueAndStateList;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGExplicitValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGValueAndStateList;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.smgvalue.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.smgvalue.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.smgvalue.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.smgvalue.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.smgvalue.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
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

  private final SMGTransferRelation smgTransferRelation;
  private final SMGOptions options;

  public SMGRightHandSideEvaluator(SMGTransferRelation pSmgTransferRelation,
      LogManagerWithoutDuplicates pLogger, MachineModel pMachineModel, SMGOptions pOptions) {
    super(pLogger, pMachineModel);
    smgTransferRelation = pSmgTransferRelation;
    options = pOptions;
  }

  public SMGExplicitValueAndState forceExplicitValue(SMGState smgState,
      CFAEdge pCfaEdge, CRightHandSide rVal)
      throws UnrecognizedCCodeException {

    ForceExplicitValueVisitor v = new ForceExplicitValueVisitor(this, smgState,
        null, machineModel, logger, pCfaEdge);

    NumberInterface val = rVal.accept(v);

    if (val.isUnknown()) {
      return SMGExplicitValueAndState.of(v.getNewState());
    }

    return SMGExplicitValueAndState.of(v.getNewState(),
        SMGKnownExpValue.valueOf(val.asNumericValue().longValue()));
  }

  public SMGState deriveFurtherInformation(SMGState pNewState, boolean pTruthValue, CFAEdge pCfaEdge, CExpression rValue)
      throws CPATransferException {
    AssigningValueVisitor v = new AssigningValueVisitor(pNewState, pTruthValue, pCfaEdge);

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

  /**
   * Visitor that derives further information from an assume edge
   */
  private class AssigningValueVisitor extends DefaultCExpressionVisitor<Void, CPATransferException> {

    private SMGState assignableState;
    private boolean truthValue = false;
    private CFAEdge edge;

    public AssigningValueVisitor(SMGState pSMGState, boolean pTruthvalue, CFAEdge pEdge) {
      assignableState = pSMGState;
      truthValue = pTruthvalue;
      edge = pEdge;
    }

    public SMGState getAssignedState() {
      return assignableState;
    }

    @Override
    protected Void visitDefault(CExpression pExp) throws CPATransferException {
      return null;
    }

    @Override
    public Void visit(CPointerExpression pointerExpression) throws CPATransferException {
      deriveFurtherInformation(pointerExpression);
      return null;
    }

    @Override
    public Void visit(CIdExpression pExp) throws CPATransferException {
      deriveFurtherInformation(pExp);
      return null;
    }

    @Override
    public Void visit(CArraySubscriptExpression pExp) throws CPATransferException {
      deriveFurtherInformation(pExp);
      return null;
    }

    @Override
    public Void visit(CFieldReference pExp) throws CPATransferException {
      deriveFurtherInformation(pExp);
      return null;
    }

    @Override
    public Void visit(CCastExpression pE) throws CPATransferException {
      // TODO cast reinterpretations
      return pE.getOperand().accept(this);
    }

    @Override
    public Void visit(CCharLiteralExpression pE) throws CPATransferException {
      throw new AssertionError();
    }

    @Override
    public Void visit(CFloatLiteralExpression pE) throws CPATransferException {
      throw new AssertionError();
    }

    @Override
    public Void visit(CIntegerLiteralExpression pE) throws CPATransferException {
      throw new AssertionError();
    }


    @Override
    public Void visit(CBinaryExpression binExp) throws CPATransferException {
      //TODO More precise

      CExpression operand1 = unwrap(binExp.getOperand1());
      CExpression operand2 = unwrap(binExp.getOperand2());
      BinaryOperator op = binExp.getOperator();

      if (operand1 instanceof CLeftHandSide) {
        deriveFurtherInformation((CLeftHandSide) operand1, operand2, op);
      }

      if (operand2 instanceof CLeftHandSide) {
        BinaryOperator resultOp = op;

        switch (resultOp) {
          case EQUALS:
          case NOT_EQUALS:
            break;
          default:
            resultOp = resultOp.getOppositLogicalOperator();
        }

        deriveFurtherInformation((CLeftHandSide) operand2, operand1, resultOp);
      }

      return null;
    }

    private void deriveFurtherInformation(CLeftHandSide lValue, CExpression exp, BinaryOperator op) throws CPATransferException {

      SMGExplicitValue rValue = evaluateExplicitValueV2(assignableState, edge, exp);

      if (rValue.isUnknown()) {
        // no further information can be inferred
        return;
      }

      SMGSymbolicValue rSymValue = evaluateExpressionValueV2(assignableState, edge, lValue);

      if(rSymValue.isUnknown()) {

        rSymValue = SMGKnownSymValue.valueOf(SMGValueFactory.getNewValue());

        LValueAssignmentVisitor visitor = getLValueAssignmentVisitor(edge, assignableState);

        List<SMGAddressAndState> addressOfFields = lValue.accept(visitor);

        if (addressOfFields.size() != 1) {
          return;
        }

        SMGAddress addressOfField = addressOfFields.get(0).getObject();

        if (addressOfField.isUnknown()) {
          return;
        }

        assignableState = smgTransferRelation.writeValue(assignableState, addressOfField.getObject(),
            addressOfField.getOffset().getAsInt(), getRealExpressionType(lValue), rSymValue, edge);
      }
      int size = getBitSizeof(edge, getRealExpressionType(lValue), assignableState);
      if (truthValue) {
        if (op == BinaryOperator.EQUALS) {
          assignableState.addPredicateRelation(rSymValue, size, rValue, size, BinaryOperator.EQUALS, edge);
          assignableState.putExplicit((SMGKnownSymValue) rSymValue, (SMGKnownExpValue) rValue);
        } else {
          assignableState.addPredicateRelation(rSymValue, size, rValue, size, op, edge);
        }
      } else {
        if (op == BinaryOperator.NOT_EQUALS) {
          assignableState.addPredicateRelation(rSymValue, size, rValue, size, BinaryOperator.EQUALS, edge);
          assignableState.putExplicit((SMGKnownSymValue) rSymValue, (SMGKnownExpValue) rValue);
          //TODO more precise
        } else {
          assignableState.addPredicateRelation(rSymValue, size, rValue, size, op, edge);
        }
      }
    }

    @Override
    public Void visit(CUnaryExpression pE) throws CPATransferException {

      UnaryOperator op = pE.getOperator();

      CExpression operand = pE.getOperand();

      switch (op) {
      case AMPER:
        throw new AssertionError("In this case, the assume should be able to be calculated");
      case MINUS:
      case TILDE:
        // don't change the truth value
        return operand.accept(this);
      case SIZEOF:
        throw new AssertionError("At the moment, this case should be able to be calculated");
      default:
        // TODO alignof is not handled
      }

      return null;
    }

    private void deriveFurtherInformation(CLeftHandSide lValue) throws CPATransferException {

      if (truthValue == true) {
        return; // no further explicit Information can be derived
      }

      LValueAssignmentVisitor visitor = getLValueAssignmentVisitor(edge, assignableState);

      List<SMGAddressAndState> addressOfFields = lValue.accept(visitor);

      if(addressOfFields.size() != 1) {
        return;
      }

      SMGAddress addressOfField = addressOfFields.get(0).getObject();

      if (addressOfField.isUnknown()) {
        return;
      }

      // If this value is known, the assumption can be evaluated, therefore it should be unknown
      assert evaluateExplicitValueV2(assignableState, edge, lValue).isUnknown();

      SMGSymbolicValue value = evaluateExpressionValueV2(assignableState, edge, lValue);

      // This symbolic value should have been added when evaluating the assume
      assert !value.isUnknown();

      assignableState.putExplicit((SMGKnownSymValue)value, SMGKnownExpValue.ZERO);

    }

    private CExpression unwrap(CExpression expression) {
      // is this correct for e.g. [!a != !(void*)(int)(!b)] !?!?!

      if (expression instanceof CCastExpression) {
        CCastExpression exp = (CCastExpression) expression;
        expression = exp.getOperand();

        expression = unwrap(expression);
      }

      return expression;
    }
  }

  private class RHSLValueAssignmentVisitor extends LValueAssignmentVisitor {

    public RHSLValueAssignmentVisitor(SMGExpressionEvaluator pSmgExpressionEvaluator, CFAEdge pEdge, SMGState pSmgState) {
      super(pSmgExpressionEvaluator, pEdge, pSmgState);
    }

    @Override
    public List<SMGAddressAndState> visit(CIdExpression variableName) throws CPATransferException {
      logger.log(Level.ALL, ">>> Handling statement: variable assignment");

      // a = ...
      return super.visit(variableName);
    }

    @Override
    public List<SMGAddressAndState> visit(CPointerExpression pLValue)
        throws CPATransferException {
      logger.log(Level.ALL, ">>> Handling statement: assignment to dereferenced pointer");

      List<SMGAddressAndState> addresses = super.visit(pLValue);

      List<SMGAddressAndState> results = new ArrayList<>(addresses.size());

      for (SMGAddressAndState address : addresses) {
        if (address.getObject().isUnknown()) {
          SMGState newState = address.getSmgState().setUnknownDereference();
          results.add(SMGAddressAndState.of(newState));
        } else {
          results.add(address);
        }
      }
      return results;
    }

    @Override
    public List<SMGAddressAndState> visit(CFieldReference lValue) throws CPATransferException {
      logger.log(Level.ALL, ">>> Handling statement: assignment to field reference");

      return super.visit(lValue);
    }

    @Override
    public List<SMGAddressAndState> visit(CArraySubscriptExpression lValue) throws CPATransferException {
      logger.log(Level.ALL, ">>> Handling statement: assignment to array Cell");

      return super.visit(lValue);
    }
  }

  private class RHSExpressionValueVisitor extends ExpressionValueVisitor {

    private final SMGBuiltins builtins;

    public RHSExpressionValueVisitor(SMGExpressionEvaluator pSmgExpressionEvaluator,
        CFAEdge pEdge, SMGState pSmgState, SMGBuiltins pBuiltins) {
      super(pSmgExpressionEvaluator, pEdge, pSmgState);
      builtins = pBuiltins;
    }

    @Override
    public SMGValueAndStateList visit(CFunctionCallExpression pIastFunctionCallExpression)
        throws CPATransferException {

      CExpression fileNameExpression = pIastFunctionCallExpression.getFunctionNameExpression();
      String functionName = fileNameExpression.toASTString();

      //TODO extreme code sharing ...

      // If Calloc and Malloc have not been properly declared,
      // they may be shown to return void
      if (builtins.isABuiltIn(functionName)) {
        if (builtins.isConfigurableAllocationFunction(functionName)) {
          smgTransferRelation.possibleMallocFail = true;
          SMGAddressValueAndStateList configAllocEdge = builtins.evaluateConfigurableAllocationFunction(
              pIastFunctionCallExpression, getInitialSmgState(), getCfaEdge());
          return configAllocEdge;
        }
        if (builtins.isExternalAllocationFunction(functionName)) {
          SMGAddressValueAndStateList extAllocEdge = builtins.evaluateExternalAllocation(
              pIastFunctionCallExpression, getInitialSmgState());
          return extAllocEdge;
        }
        switch (functionName) {
        case "__VERIFIER_BUILTIN_PLOT":
          builtins.evaluateVBPlot(pIastFunctionCallExpression, getInitialSmgState());
          break;
        case "__builtin_alloca":
          smgTransferRelation.possibleMallocFail = true;
          SMGAddressValueAndStateList allocEdge = builtins.evaluateAlloca(pIastFunctionCallExpression, getInitialSmgState(), getCfaEdge());
          return allocEdge;
        case "printf":
          return SMGValueAndStateList.of(getInitialSmgState());
        default:
          if (builtins.isNondetBuiltin(functionName)) {
            return SMGValueAndStateList.of(getInitialSmgState());
          } else {
            throw new AssertionError("Unexpected function handled as a builtin: " + functionName);
          }
        }
      } else {
        switch (options.getHandleUnknownFunctions()) {
        case STRICT:
          throw new CPATransferException("Unknown function '" + functionName + "' may be unsafe. See the cpa.smg.handleUnknownFunction option.");
        case ASSUME_SAFE:
          return SMGValueAndStateList.of(getInitialSmgState());
        default:
          throw new AssertionError("Unhandled enum value in switch: " + options.getHandleUnknownFunctions());
        }
      }

      return SMGValueAndStateList.of(getInitialSmgState());
    }
  }

  private class ForceExplicitValueVisitor extends
      ExplicitValueVisitor {

    private final SMGKnownExpValue GUESS = SMGKnownExpValue.valueOf(2);

    public ForceExplicitValueVisitor(SMGExpressionEvaluator pSmgExpressionEvaluator,
        SMGState pSmgState, String pFunctionName, MachineModel pMachineModel,
        LogManagerWithoutDuplicates pLogger, CFAEdge pEdge) {
      super(pSmgExpressionEvaluator, pSmgState, pFunctionName, pMachineModel, pLogger, pEdge);
    }

    @Override
    protected NumberInterface evaluateCArraySubscriptExpression(CArraySubscriptExpression pLValue)
        throws UnrecognizedCCodeException {
      NumberInterface result = super.evaluateCArraySubscriptExpression(pLValue);

      if (result.isUnknown()) {
        return guessLHS(pLValue);
      } else {
        return result;
      }
    }

    @Override
    protected NumberInterface evaluateCIdExpression(CIdExpression pCIdExpression)
        throws UnrecognizedCCodeException {

      NumberInterface result = super.evaluateCIdExpression(pCIdExpression);

      if (result.isUnknown()) {
        return guessLHS(pCIdExpression);
      } else {
        return result;
      }
    }

    private NumberInterface guessLHS(CLeftHandSide exp)
        throws UnrecognizedCCodeException {

      SMGValueAndState symbolicValueAndState;

      try {
        SMGValueAndStateList symbolicValueAndStates = evaluateExpressionValue(getNewState(),
            getEdge(), exp);

        if(symbolicValueAndStates.size() != 1) {
          throw new SMGInconsistentException("Found abstraction where non should exist,due to the expression " + exp.toASTString() + "already being evaluated once in this transferrelation step.");
        } else {
          symbolicValueAndState = symbolicValueAndStates.getValueAndStateList().get(0);
        }

      } catch (CPATransferException e) {
        UnrecognizedCCodeException e2 = new UnrecognizedCCodeException(
            "SMG cannot get symbolic value of : " + exp.toASTString(), exp);
        e2.initCause(e);
        throw e2;
      }

      SMGSymbolicValue value = symbolicValueAndState.getObject();
      setSmgState(symbolicValueAndState.getSmgState());

      if (value.isUnknown()) {
        return NumberInterface.UnknownValue.getInstance();
      }

      getNewState().putExplicit((SMGKnownSymValue) value, GUESS);

      return new NumericValue(GUESS.getValue());
    }

    @Override
    protected NumberInterface evaluateCFieldReference(CFieldReference pLValue) throws UnrecognizedCCodeException {
      NumberInterface result = super.evaluateCFieldReference(pLValue);

      if (result.isUnknown()) {
        return guessLHS(pLValue);
      } else {
        return result;
      }
    }

    @Override
    protected NumberInterface evaluateCPointerExpression(CPointerExpression pCPointerExpression)
        throws UnrecognizedCCodeException {
      NumberInterface result = super.evaluateCPointerExpression(pCPointerExpression);

      if (result.isUnknown()) {
        return guessLHS(pCPointerExpression);
      } else {
        return result;
      }
    }
  }

  private class PointerAddressVisitor extends PointerVisitor {

    private final SMGBuiltins builtins;

    public PointerAddressVisitor(SMGExpressionEvaluator pSmgExpressionEvaluator, CFAEdge pEdge, SMGState pSmgState, SMGBuiltins pBuiltins) {
      super(pSmgExpressionEvaluator, pEdge, pSmgState);
      builtins = pBuiltins;
    }

    @Override
    protected SMGAddressValueAndStateList createAddressOfFunction(CIdExpression pIdFunctionExpression)
        throws SMGInconsistentException {
      SMGState state = getInitialSmgState();

      CFunctionDeclaration functionDcl = (CFunctionDeclaration) pIdFunctionExpression.getDeclaration();

      SMGObject functionObject =
          state.getObjectForFunction(functionDcl);

      if (functionObject == null) {
        functionObject = state.createObjectForFunction(functionDcl);
      }

      return createAddress(state, functionObject, SMGKnownExpValue.ZERO);
    }

    @Override
    public SMGAddressValueAndStateList visit(CFunctionCallExpression pIastFunctionCallExpression)
        throws CPATransferException {
      CExpression fileNameExpression = pIastFunctionCallExpression.getFunctionNameExpression();
      String functionName = fileNameExpression.toASTString();

      if (builtins.isABuiltIn(functionName)) {
        if (builtins.isConfigurableAllocationFunction(functionName)) {
          smgTransferRelation.possibleMallocFail = true;
          SMGAddressValueAndStateList configAllocEdge = builtins.evaluateConfigurableAllocationFunction(pIastFunctionCallExpression, getInitialSmgState(), getCfaEdge());
          return configAllocEdge;
        }
        if (builtins.isExternalAllocationFunction(functionName)) {
          SMGAddressValueAndStateList extAllocEdge = builtins.evaluateExternalAllocation(pIastFunctionCallExpression, getInitialSmgState());
          return extAllocEdge;
        }
        switch (functionName) {
        case "__builtin_alloca":
          SMGAddressValueAndStateList allocEdge = builtins.evaluateAlloca(pIastFunctionCallExpression, getInitialSmgState(), getCfaEdge());
          return allocEdge;
        case "memset":
          SMGAddressValueAndStateList memsetTargetEdge = builtins.evaluateMemset(pIastFunctionCallExpression, getInitialSmgState(), getCfaEdge());
          return memsetTargetEdge;
        case "memcpy":
          SMGAddressValueAndStateList memcpyTargetEdge = builtins.evaluateMemcpy(pIastFunctionCallExpression, getInitialSmgState(), getCfaEdge());
          return memcpyTargetEdge;
        case "printf":
          return SMGAddressValueAndStateList.of(getInitialSmgState());
        default:
          if (builtins.isNondetBuiltin(functionName)) {
            return SMGAddressValueAndStateList.of(getInitialSmgState());
          } else {
            throw new AssertionError("Unexpected function handled as a builtin: " + functionName);
          }
        }
      } else {
        switch (options.getHandleUnknownFunctions()) {
        case STRICT:
          throw new CPATransferException(
              "Unknown function '" + functionName + "' may be unsafe. See the cpa.smg.handleUnknownFunction option.");
        case ASSUME_SAFE:
          return SMGAddressValueAndStateList.of(getInitialSmgState());
        default:
          throw new AssertionError("Unhandled enum value in switch: " + options.getHandleUnknownFunctions());
        }
      }
    }
  }

  private class RHSCSizeOfVisitor extends CSizeOfVisitor {

    public RHSCSizeOfVisitor(MachineModel pModel, CFAEdge pEdge, SMGState pState,
        LogManagerWithoutDuplicates pLogger, Optional<CExpression> pExpression) {
      super(pModel, pEdge, pState, pLogger, pExpression);
    }

    @Override
    protected int handleUnkownArrayLengthValue(CArrayType pArrayType) {
      if (smgTransferRelation.kind == SMGTransferRelationKind.REFINEMENT) {
        return 0;
      } else {
        return super.handleUnkownArrayLengthValue(pArrayType);
      }
    }
  }

  @Override
  public org.sosy_lab.cpachecker.cpa.smg.evaluator.PointerVisitor getPointerVisitor(
      CFAEdge pCfaEdge, SMGState pNewState) {
    return new PointerAddressVisitor(this, pCfaEdge, pNewState, smgTransferRelation.builtins);
  }

  @Override
  public org.sosy_lab.cpachecker.cpa.smg.evaluator.ExpressionValueVisitor getExpressionValueVisitor(
      CFAEdge pCfaEdge, SMGState pNewState) {
    return new RHSExpressionValueVisitor(this, pCfaEdge, pNewState, smgTransferRelation.builtins);
  }

  @Override
  public org.sosy_lab.cpachecker.cpa.smg.evaluator.LValueAssignmentVisitor getLValueAssignmentVisitor(
      CFAEdge pCfaEdge, SMGState pNewState) {
    return new RHSLValueAssignmentVisitor(this, pCfaEdge, pNewState);
  }

  @Override
  protected RHSCSizeOfVisitor getSizeOfVisitor(CFAEdge pEdge, SMGState pState, Optional<CExpression> pExpression) {
    return new RHSCSizeOfVisitor(machineModel, pEdge, pState, logger, pExpression);
  }

  @Override
  protected SMGValueAndState handleUnknownDereference(SMGState pSmgState,
      CFAEdge pEdge) {

    SMGState newState = pSmgState.setUnknownDereference();
    return super.handleUnknownDereference(newState, pEdge);
  }
}