// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.cpa.rtt.RTTState;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMG2Exception;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGStateAndOptionalSMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.ConstraintsStrengthenOperator;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class SMGTransferRelation
    extends ForwardingTransferRelation<Collection<SMGState>, SMGState, SMGPrecision> {

  private final SMGOptions options;
  private final SMGCPAExportOptions exportSMGOptions;

  private final MachineModel machineModel;

  private final LogManagerWithoutDuplicates logger;

  private final SMGCPAExpressionEvaluator evaluator;

  @SuppressWarnings("unused")
  private final ConstraintsStrengthenOperator constraintsStrengthenOperator;

  // Collection of tracked symbolic boolean variables that get used when learning assumptions
  // (see SMGCPAAssigningValueVisitor). Used to assign boolean concrete values.
  private final Collection<String> booleanVariables;

  private final @Nullable SMGCPAStatistics stats;

  public SMGTransferRelation(
      LogManager pLogger,
      SMGOptions pOptions,
      SMGCPAExportOptions pExportSMGOptions,
      CFA pCfa,
      ConstraintsStrengthenOperator pConstraintsStrengthenOperator,
      SMGCPAStatistics pStats) {
    logger = new LogManagerWithoutDuplicates(pLogger);
    options = pOptions;
    exportSMGOptions = pExportSMGOptions;
    machineModel = pCfa.getMachineModel();

    if (pCfa.getVarClassification().isPresent()) {
      booleanVariables = pCfa.getVarClassification().orElseThrow().getIntBoolVars();
    } else {
      booleanVariables = ImmutableSet.of();
    }

    evaluator = new SMGCPAExpressionEvaluator(machineModel, logger, exportSMGOptions, options);
    constraintsStrengthenOperator = pConstraintsStrengthenOperator;
    stats = pStats;
  }

  /* For tests only. */
  protected SMGTransferRelation(
      LogManager pLogger,
      SMGOptions pOptions,
      SMGCPAExportOptions pExportSMGOptions,
      MachineModel pMachineModel,
      Collection<String> pBooleanVariables,
      @Nullable ConstraintsStrengthenOperator pConstraintsStrengthenOperator) {
    logger = new LogManagerWithoutDuplicates(pLogger);
    options = pOptions;
    exportSMGOptions = pExportSMGOptions;
    machineModel = pMachineModel;
    evaluator = new SMGCPAExpressionEvaluator(machineModel, logger, exportSMGOptions, options);
    booleanVariables = pBooleanVariables;
    constraintsStrengthenOperator = pConstraintsStrengthenOperator;
    stats = null;
  }

  @Override
  protected Collection<SMGState> postProcessing(Collection<SMGState> pSuccessors, CFAEdge edge) {
    // This handles variables that went out of scope in a function through { }
    if (pSuccessors == null) {
      return super.postProcessing(pSuccessors, edge);
    }
    ImmutableList.Builder<SMGState> successors = ImmutableList.builder();
    for (SMGState s : pSuccessors) {
      for (CSimpleDeclaration variable : edge.getSuccessor().getOutOfScopeVariables()) {
        s = s.invalidateVariable(MemoryLocation.forDeclaration(variable));
      }
      successors.add(s);
    }
    return successors.build();
  }

  @Override
  protected Set<SMGState> handleBlankEdge(BlankEdge cfaEdge) throws CPATransferException {
    if (cfaEdge.getSuccessor() instanceof FunctionExitNode) {
      if (isEntryFunction(cfaEdge)) {
        // Entry functions need special handling as they don't have a return edge
        // (i.e. check for memory leaks)
        return handleReturnEntryFunction(Collections.singleton(state));
      }
    }

    return Collections.singleton(state);
  }

  /**
   * If the option isHandleNonFreedMemoryInMainAsMemLeak() is true, the given states are first
   * checked for memory leaks and then the stack frame is dropped before unreachables are pruned and
   * finally the states are returned.
   *
   * @param pSuccessors {@link SMGState}s to process.
   * @return a Collection of SMGStates that are processed. May include memory leak error states.
   */
  private Set<SMGState> handleReturnEntryFunction(Collection<SMGState> pSuccessors) {
    return pSuccessors.stream()
        .map(
            pState -> {
              if (options.isHandleNonFreedMemoryInMainAsMemLeak()) {
                pState = pState.dropStackFrame();
              }
              // Pruning checks for memory leaks and updates the error state if one is found!
              return pState.copyAndPruneUnreachable();
            })
        .collect(ImmutableSet.toImmutableSet());
  }

  private boolean isEntryFunction(CFAEdge pCfaEdge) {
    return pCfaEdge.getSuccessor().getNumLeavingEdges() == 0;
  }

  /* (non-Javadoc)
   * Returns a collection of SMGStates that are the successors of the handled edge.
   * If there is no returned data, the current state is the successor state.
   * If there is returned data we assign the returned statement to the field of the state,
   * returning the successor states.
   * This assignment is further explained in its method.
   * In the case that this is an entry function, there is no function return edge,
   * meaning we have to check for memory leaks!
   * This means that every successor-state has to be checked for memory that is not freed
   * (if the option for that is enabled) and then the unreachables need to be pruned.
   */
  @Override
  protected Collection<SMGState> handleReturnStatementEdge(CReturnStatementEdge returnEdge)
      throws CPATransferException {
    ImmutableList.Builder<SMGState> successorsBuilder = ImmutableList.builder();
    // Check that there is a return object and if there is one we can write the return to it
    if (state.getMemoryModel().hasReturnObjectForCurrentStackFrame()) {
      // value 0 is the default return value in C
      CExpression returnExp = returnEdge.getExpression().orElse(CIntegerLiteralExpression.ZERO);
      CType retType = SMGCPAExpressionEvaluator.getCanonicalType(returnExp);
      Optional<CAssignment> returnAssignment = returnEdge.asAssignment();
      if (returnAssignment.isPresent()) {
        retType = returnAssignment.orElseThrow().getLeftHandSide().getExpressionType();
      }

      SMGCPAValueVisitor vv = new SMGCPAValueVisitor(evaluator, state, returnEdge, logger);
      for (ValueAndSMGState returnValueAndState : vv.evaluate(returnExp, retType)) {
        // We get the size per state as it could theoretically change per state (abstraction)
        BigInteger sizeInBits = evaluator.getBitSizeof(state, retType);
        ValueAndSMGState valueAndStateToWrite =
            evaluator.unpackAddressExpression(
                returnValueAndState.getValue(), returnValueAndState.getState());

        successorsBuilder.add(
            valueAndStateToWrite
                .getState()
                .writeToReturn(
                    sizeInBits, valueAndStateToWrite.getValue(), returnExp.getExpressionType()));
      }
    } else {
      successorsBuilder.add(state);
    }

    // Handle entry function return (check for mem leaks)
    if (isEntryFunction(returnEdge)) {
      return handleReturnEntryFunction(successorsBuilder.build());
    }
    return successorsBuilder.build();
  }

  @Override
  protected Collection<SMGState> handleFunctionReturnEdge(
      CFunctionReturnEdge functionReturnEdge,
      CFunctionSummaryEdge fnkCall,
      CFunctionCall summaryExpr,
      String callerFunctionName)
      throws CPATransferException {

    Collection<SMGState> successors = handleFunctionReturn(functionReturnEdge);
    if (options.isCheckForMemLeaksAtEveryFrameDrop()) {
      ImmutableList.Builder<SMGState> prunedSuccessors = ImmutableList.builder();
      for (SMGState successor : successors) {
        // Pruning checks for memory leaks and updates the error state if one is found!
        prunedSuccessors.add(successor.copyAndPruneUnreachable());
      }
      successors = prunedSuccessors.build();
    }
    return successors;
  }

  private List<SMGState> handleFunctionReturn(CFunctionReturnEdge functionReturnEdge)
      throws CPATransferException {
    CFunctionSummaryEdge summaryEdge = functionReturnEdge.getSummaryEdge();
    CFunctionCall summaryExpr = summaryEdge.getExpression();

    Preconditions.checkArgument(
        state.getMemoryModel().getStackFrames().peek().getFunctionDefinition()
            == summaryEdge.getFunctionEntry().getFunctionDefinition());

    if (summaryExpr instanceof CFunctionCallAssignmentStatement) {
      CFunctionCallAssignmentStatement funcCallExpr =
          (CFunctionCallAssignmentStatement) summaryExpr;
      CExpression leftValue = funcCallExpr.getLeftHandSide();
      CType rightValueType =
          SMGCPAExpressionEvaluator.getCanonicalType(funcCallExpr.getRightHandSide());
      BigInteger sizeInBits = evaluator.getBitSizeof(state, rightValueType);
      Optional<SMGObject> returnObject =
          state.getMemoryModel().getReturnObjectForCurrentStackFrame();
      // There should always be a return memory object in the case of a
      // CFunctionCallAssignmentStatement!
      Preconditions.checkArgument(returnObject.isPresent());
      // Read the return object with its type
      ImmutableList.Builder<SMGState> stateBuilder = ImmutableList.builder();
      for (ValueAndSMGState readValueAndState :
          state.readValue(
              returnObject.orElseThrow(), BigInteger.ZERO, sizeInBits, rightValueType)) {

        // Now we can drop the stack frame as we left the function and have the return value
        SMGState currentState = readValueAndState.getState().dropStackFrame();
        // The memory on the left hand side might not exist because of CEGAR
        for (SMGState stateWithNewVar : createVariableOnTheSpot(leftValue, currentState)) {
          stateBuilder.addAll(
              evaluator.writeValueToExpression(
                  summaryEdge, leftValue, readValueAndState.getValue(), stateWithNewVar));
        }
      }
      return stateBuilder.build();
    } else {
      return ImmutableList.of(state.dropStackFrame());
    }
  }

  /**
   * Creates the variable used in leftHandSideExpr in the returned {@link SMGState}. This does check
   * if the variable already exists and if it is invalidated it is validated again. (i.e. out of
   * scope variables that are declared again)
   *
   * @param leftHandSideExpr some left hand side {@link CExpression}.
   * @param pState current {@link SMGState}
   * @return a new SMGState with the variable added.
   * @throws CPATransferException for errors and unhandled cases
   */
  private List<SMGState> createVariableOnTheSpot(CExpression leftHandSideExpr, SMGState pState)
      throws CPATransferException {
    if (leftHandSideExpr instanceof CIdExpression) {
      CIdExpression leftCIdExpr = (CIdExpression) leftHandSideExpr;
      CSimpleDeclaration decl = leftCIdExpr.getDeclaration();
      String varName = decl.getQualifiedName();
      SMGState currentState = pState;
      // The orElse(true) skips the call on purpose!
      if (pState.isLocalOrGlobalVariablePresent(varName)
          && !pState.isLocalOrGlobalVariableValid(varName).orElse(true)) {
        currentState = pState.copyAndRemoveStackVariable(varName);
      }
      if (!currentState.isLocalOrGlobalVariablePresent(varName)) {
        if (decl instanceof CVariableDeclaration) {
          return evaluator.handleVariableDeclarationWithoutInizializer(
              currentState, (CVariableDeclaration) decl);
        } else if (decl instanceof CParameterDeclaration) {
          return evaluator.handleVariableDeclarationWithoutInizializer(
              currentState, ((CParameterDeclaration) decl).asVariableDeclaration());
        }
      }
      return ImmutableList.of(pState);

    } else if (leftHandSideExpr instanceof CArraySubscriptExpression) {
      CExpression arrayExpr = ((CArraySubscriptExpression) leftHandSideExpr).getArrayExpression();
      return createVariableOnTheSpot(arrayExpr, pState);

    } else if (leftHandSideExpr instanceof CFieldReference) {
      CExpression fieldOwn = ((CFieldReference) leftHandSideExpr).getFieldOwner();
      return createVariableOnTheSpot(fieldOwn, pState);

    } else if (leftHandSideExpr instanceof CPointerExpression) {
      CExpression operand = ((CPointerExpression) leftHandSideExpr).getOperand();
      return createVariableOnTheSpot(operand, pState);

    } else if (leftHandSideExpr instanceof CUnaryExpression) {
      CExpression operand = ((CUnaryExpression) leftHandSideExpr).getOperand();
      return createVariableOnTheSpot(operand, pState);
    } else if (leftHandSideExpr instanceof CCastExpression) {
      CExpression operand = ((CCastExpression) leftHandSideExpr).getOperand();
      return createVariableOnTheSpot(operand, pState);
    }
    return ImmutableList.of(pState);
  }

  @Override
  protected Collection<SMGState> handleFunctionCallEdge(
      CFunctionCallEdge callEdge,
      List<CExpression> arguments,
      List<CParameterDeclaration> paramDecl,
      String calledFunctionName)
      throws CPATransferException {

    if (!callEdge.getSuccessor().getFunctionDefinition().getType().takesVarArgs()) {
      if (paramDecl.size() != arguments.size()) {
        throw new SMG2Exception(
            "The number of arguments expected and given do not match for function call "
                + callEdge.getDescription()
                + ".");
      }
    }

    return ImmutableList.of(handleFunctionCall(state, callEdge, arguments, paramDecl));
  }

  /**
   * Creates a new stack frame for the function call, then creates the local variables for the
   * parameters and fills them using the value visitor with the values given. This should only be
   * called if the number of arguments and paramDecls entered match. This function also checks for
   * variable arguments and saves them in an array in the order they appear.
   *
   * @param initialState the current state.
   * @param callEdge the edge of the function call.
   * @param arguments the function call arguments {@link CExpression}s.
   * @param paramDecl the {@link CParameterDeclaration} for the arguments.
   * @return a state with a new stack frame and all parameters evaluated to values and assigned to
   *     new local variables.
   * @throws CPATransferException in case of a critical error.
   */
  private SMGState handleFunctionCall(
      SMGState initialState,
      CFunctionCallEdge callEdge,
      List<CExpression> arguments,
      List<CParameterDeclaration> paramDecl)
      throws CPATransferException {
    // Create a variable for each parameter, then evaluate all given parameters and assign them to
    // their variables
    // TODO: check if the FunctionPointerCPA would be a better option instead of doing it myself
    SMGState currentState = initialState;
    ImmutableList.Builder<Value> readValuesInOrderBuilder = ImmutableList.builder();
    BigInteger overallVarArgsSizeInBits = BigInteger.ZERO;
    CType parameterType = null;

    for (int i = 0; i < arguments.size(); i++) {
      CExpression cParamExp = arguments.get(i);
      CType argumentType =
          SMGCPAExpressionEvaluator.getCanonicalType(cParamExp.getExpressionType());

      if (paramDecl.size() > i) {
        // The last type is the type for the following arguments
        parameterType = SMGCPAExpressionEvaluator.getCanonicalType(paramDecl.get(i));
      } else {
        // Remember overall size of varArgs
        overallVarArgsSizeInBits =
            overallVarArgsSizeInBits.add(evaluator.getBitSizeof(currentState, cParamExp));
      }

      ValueAndSMGState valueAndState;
      if (parameterType instanceof CPointerType && argumentType instanceof CArrayType) {
        // Implicit & on the array expr
        List<ValueAndSMGState> addressesAndStates =
            evaluator.createAddress(cParamExp, currentState, callEdge);

        Preconditions.checkArgument(addressesAndStates.size() == 1);
        valueAndState = addressesAndStates.get(0);
      } else {
        // Evaluator the CExpr into a Value
        // Note: this evaluates local arrays into pointers!!!!!
        List<ValueAndSMGState> valuesAndStates =
            cParamExp.accept(new SMGCPAValueVisitor(evaluator, currentState, callEdge, logger));

        // If this ever fails; we need to take all states/values into account, meaning we would need
        // to proceed from this point onwards with all of them with all following operations
        Preconditions.checkArgument(valuesAndStates.size() == 1);
        valueAndState = valuesAndStates.get(0);
      }
      readValuesInOrderBuilder.add(valueAndState.getValue());
      currentState = valueAndState.getState();
    }

    ImmutableList<Value> readValuesInOrder = readValuesInOrderBuilder.build();
    // Add the new stack frame based on the function def, but only after we read the values from the
    // old stack frame
    CFunctionDeclaration funcDecl = callEdge.getSuccessor().getFunctionDefinition();
    if (funcDecl.getType().takesVarArgs()) {
      // Get the var args and save them in the stack frame
      ImmutableList.Builder<Value> varArgsBuilder = ImmutableList.builder();
      for (int i = paramDecl.size(); i < arguments.size(); i++) {
        varArgsBuilder.add(readValuesInOrder.get(i));
      }
      currentState = currentState.copyAndAddStackFrame(funcDecl, varArgsBuilder.build());
    } else {
      currentState = currentState.copyAndAddStackFrame(funcDecl);
    }

    for (int i = 0; i < paramDecl.size(); i++) {
      Value paramValue = readValuesInOrder.get(i);
      CType valueType = SMGCPAExpressionEvaluator.getCanonicalType(arguments.get(i));

      // Normal variable with a name
      String varName = paramDecl.get(i).getQualifiedName();
      CType cParamType = SMGCPAExpressionEvaluator.getCanonicalType(paramDecl.get(i));

      currentState =
          handleFunctionParameterAssignments(
              varName, valueType, paramValue, cParamType, callEdge, currentState);
    }
    return currentState;
  }

  /*
   * Assigns the value with type valueType to the variable behind the name given with the type cParamType.
   * Note: the action depends on the types. See method for more details.
   */
  private SMGState handleFunctionParameterAssignments(
      String varName,
      CType valueType,
      Value paramValue,
      CType cParamType,
      CFAEdge callEdge,
      SMGState pCurrentState)
      throws SMG2Exception {
    SMGState currentState = pCurrentState;
    if (valueType instanceof CArrayType && cParamType instanceof CArrayType) {
      // Take the pointer to the local array and get the memory area, then associate this memory
      // area with the variable name
      List<SMGStateAndOptionalSMGObjectAndOffset> knownMemoriesAndStates =
          currentState.dereferencePointer(paramValue);
      Preconditions.checkArgument(knownMemoriesAndStates.size() == 1);
      SMGStateAndOptionalSMGObjectAndOffset knownMemoryAndState = knownMemoriesAndStates.get(0);
      currentState = knownMemoryAndState.getSMGState();
      if (!knownMemoryAndState.hasSMGObjectAndOffset()) {
        throw new SMG2Exception("Could not associate a local array in a new function.");
      } else if (knownMemoryAndState.getOffsetForObject().compareTo(BigInteger.ZERO) != 0) {
        throw new SMG2Exception("Could not associate a local array in a new function.");
      }
      // arrays don't get copied! They are handled via pointers.
      SMGObject knownMemory = knownMemoryAndState.getSMGObject();
      return currentState.copyAndAddLocalVariable(knownMemory, varName, cParamType);

    } else if (SMGCPAExpressionEvaluator.isStructOrUnionType(valueType)
        && SMGCPAExpressionEvaluator.isStructOrUnionType(cParamType)) {

      if (paramValue.isUnknown()) {
        return currentState;
      }

      Preconditions.checkArgument(paramValue instanceof SymbolicIdentifier);
      Preconditions.checkArgument(
          ((SymbolicIdentifier) paramValue).getRepresentedLocation().isPresent());
      MemoryLocation locationInPrevStackFrame =
          ((SymbolicIdentifier) paramValue).getRepresentedLocation().orElseThrow();
      Optional<SMGObject> maybeKnownMemory =
          currentState
              .getMemoryModel()
              .getObjectForVisibleVariableFromPreviousStackframe(
                  locationInPrevStackFrame.getQualifiedName());
      if (maybeKnownMemory.isEmpty()) {
        throw new SMG2Exception("Usage of unknown variable in function " + callEdge);
      }
      // Structs get copies
      BigInteger offsetSource = BigInteger.valueOf(locationInPrevStackFrame.getOffset());
      currentState =
          currentState.copyAndAddLocalVariable(
              maybeKnownMemory.orElseThrow().getSize().subtract(offsetSource), varName, cParamType);
      SMGObject newMemory =
          currentState.getMemoryModel().getObjectForVisibleVariable(varName).orElseThrow();

      return currentState.copySMGObjectContentToSMGObject(
          maybeKnownMemory.orElseThrow(),
          offsetSource,
          newMemory,
          BigInteger.ZERO,
          newMemory.getSize().subtract(offsetSource));
    } else {
      return evaluator.writeValueToNewVariableBasedOnTypes(
          paramValue, cParamType, valueType, varName, currentState);
    }
  }

  @Override
  protected void setInfo(
      AbstractState abstractState, Precision abstractPrecision, CFAEdge cfaEdge) {
    super.setInfo(abstractState, abstractPrecision, cfaEdge);
    if (stats != null) {
      stats.incrementIterations();
    }
  }

  @Override
  protected Collection<SMGState> handleAssumption(
      CAssumeEdge cfaEdge, CExpression expression, boolean truthAssumption)
      throws CPATransferException, InterruptedException {
    // Assumptions are essentially all value analysis in nature. We might get the values from the
    // SMGs though.  Assumptions are for example all comparisons like ==, !=, <.... and should
    // always be a CBinaryExpression.
    // We also might learn something by assuming symbolic or unknown values based on known values
    return handleAssumption(expression, cfaEdge, truthAssumption);
  }

  private @Nullable Collection<SMGState> handleAssumption(
      AExpression expression, CFAEdge cfaEdge, boolean truthValue) throws CPATransferException {

    if (stats != null) {
      stats.incrementAssumptions();
    }

    // We know it has to be a CExpression as this analysis only supports C
    Pair<AExpression, Boolean> simplifiedExpression = simplifyAssumption(expression, truthValue);
    CExpression cExpression = (CExpression) simplifiedExpression.getFirst();
    truthValue = simplifiedExpression.getSecond();

    ImmutableList.Builder<SMGState> resultStateBuilder = ImmutableList.builder();
    // Get the value of the expression (either true[1L], false[0L], or unknown[null])
    SMGCPAValueVisitor vv = new SMGCPAValueVisitor(evaluator, state, cfaEdge, logger);
    for (ValueAndSMGState valueAndState :
        vv.evaluate(
            cExpression, SMGCPAExpressionEvaluator.getCanonicalType((CExpression) expression))) {
      Value value = valueAndState.getValue();
      SMGState currentState = valueAndState.getState();

      if (value.isExplicitlyKnown() && stats != null) {
        stats.incrementDeterministicAssumptions();
      }

      if (!value.isExplicitlyKnown()) {
        SMGCPAAssigningValueVisitor avv =
            new SMGCPAAssigningValueVisitor(
                evaluator, state, cfaEdge, logger, truthValue, options, booleanVariables);

        for (ValueAndSMGState newValueAndUpdatedState : cExpression.accept(avv)) {
          SMGState updatedState = newValueAndUpdatedState.getState();

          resultStateBuilder.add(updatedState);
        }

      } else if (representsBoolean(value, truthValue)) {
        // We do not know more than before, and the assumption is fulfilled, so return the state
        // from the value visitor (we don't need a copy as every state operation generates a new
        // state and never modifies the old state)
        resultStateBuilder.add(currentState);

      } else {
        // Assumption not fulfilled
        return null;
      }
    }
    return resultStateBuilder.build();
  }

  /*
   *  returns 'true' if the given value represents the specified boolean bool.
   *  A return of 'false' does not necessarily mean that the given value represents !bool,
   *  but only that it does not represent bool.
   *
   *  For example:
   *    * representsTrue(BooleanValue.valueOf(true), true)  = true
   *    * representsTrue(BooleanValue.valueOf(false), true) = false
   *  but:
   *    * representsTrue(NullValue.getInstance(), true)     = false
   *    * representsTrue(NullValue.getInstance(), false)    = false
   *
   */
  private boolean representsBoolean(Value value, boolean bool) {
    if (value instanceof BooleanValue) {
      return ((BooleanValue) value).isTrue() == bool;

    } else if (value.isNumericValue()) {
      if (bool) {
        return value.asNumericValue().longValue() == 1L;
      } else {
        return value.asNumericValue().longValue() == 0L;
      }

    } else {
      return false;
    }
  }

  @Override
  protected Collection<SMGState> handleStatementEdge(CStatementEdge pCfaEdge, CStatement cStmt)
      throws CPATransferException {
    // Either assignments a = b; or function calls foo(..);
    if (cStmt instanceof CAssignment) {
      // Assignments, evaluate the right hand side value using the value visitor and write it into
      // the address returned by the address evaluator for the left hand side.
      CAssignment cAssignment = (CAssignment) cStmt;
      CExpression lValue = cAssignment.getLeftHandSide();
      CRightHandSide rValue = cAssignment.getRightHandSide();
      ImmutableList.Builder<SMGState> stateBuilder = ImmutableList.builder();
      for (SMGState currentState : createVariableOnTheSpot(lValue, state)) {
        stateBuilder.addAll(handleAssignment(currentState, pCfaEdge, lValue, rValue));
      }
      return stateBuilder.build();

    } else if (cStmt instanceof CFunctionCallStatement) {
      // Check the arguments for the function, then simply execute the function
      CFunctionCallStatement cFCall = (CFunctionCallStatement) cStmt;
      CFunctionCallExpression cFCExpression = cFCall.getFunctionCallExpression();
      CExpression fileNameExpression = cFCExpression.getFunctionNameExpression();
      String calledFunctionName = fileNameExpression.toASTString();

      ImmutableList.Builder<SMGState> resultStatesBuilder = ImmutableList.builder();

      // function calls without assignments
      resultStatesBuilder.addAll(
          handleFunctionCallWithoutBody(state, pCfaEdge, cFCExpression, calledFunctionName));

      return resultStatesBuilder.build();
    } else {
      // Fall through for unneeded cases
      return ImmutableList.of(state);
    }
  }

  /*
   * Function calls without assignment only. Checks the arguments for validity!
   * The split up of the methods used helps with better errors.
   */
  private Collection<SMGState> handleFunctionCallWithoutBody(
      SMGState pState,
      CStatementEdge pCfaEdge,
      CFunctionCallExpression cFCExpression,
      String calledFunctionName)
      throws CPATransferException {
    SMGCPABuiltins builtins = evaluator.getBuiltinFunctionHandler();
    List<ValueAndSMGState> uselessValuesAndStates;
    if (builtins.isABuiltIn(calledFunctionName)) {
      if (builtins.isConfigurableAllocationFunction(calledFunctionName)) {
        ImmutableList.Builder<SMGState> newStatesBuilder = ImmutableList.builder();
        String errorMSG =
            "Calling " + functionName + " and not using the return value results in a memory leak.";
        logger.logf(Level.INFO, "Error in %s: %s", errorMSG, pCfaEdge.getFileLocation());
        List<ValueAndSMGState> uselessValuesAndNewStates =
            builtins.evaluateConfigurableAllocationFunction(cFCExpression, pState, pCfaEdge);
        for (ValueAndSMGState valueAndState : uselessValuesAndNewStates) {
          newStatesBuilder.add(
              valueAndState
                  .getState()
                  .withMemoryLeak(errorMSG, ImmutableList.of(valueAndState.getValue())));
        }
        return newStatesBuilder.build();
      }
      if (builtins.isDeallocationFunction(calledFunctionName)) {
        return builtins.evaluateFree(cFCExpression, pState, pCfaEdge);
      } else {
        uselessValuesAndStates =
            builtins.handleBuiltinFunctionCall(pCfaEdge, cFCExpression, calledFunctionName, pState);
      }
    } else {
      // Check arguments for unknown functions without body
      return builtins.checkAllParametersForValidity(cFCExpression, pState, pCfaEdge);
    }
    return Collections3.transformedImmutableListCopy(
        uselessValuesAndStates, valAndState -> valAndState.getState());
  }

  @Override
  protected List<SMGState> handleDeclarationEdge(CDeclarationEdge edge, CDeclaration cDecl)
      throws CPATransferException {
    SMGState currentState = state;
    // CEGAR checks inside the ifs! Else we check every typedef!

    if (cDecl instanceof CFunctionDeclaration) {
      CFunctionDeclaration cFuncDecl = (CFunctionDeclaration) cDecl;
      if (cFuncDecl.getQualifiedName().equals("main")) {
        if (cFuncDecl.getParameters() != null) {
          // Init main parameters of there are any
          for (CParameterDeclaration parameters : cFuncDecl.getParameters()) {
            CType paramType = SMGCPAExpressionEvaluator.getCanonicalType(parameters.getType());
            BigInteger paramSizeInBits = evaluator.getBitSizeof(currentState, paramType);
            currentState =
                currentState.copyAndAddLocalVariable(
                    paramSizeInBits, parameters.getQualifiedName(), paramType);
          }
        }
      }
    } else if (cDecl instanceof CComplexTypeDeclaration) {
      // don't handle, just let pass through
    } else if (cDecl instanceof CTypeDefDeclaration) {
      // don't handle, just let pass through
    } else if (cDecl instanceof CVariableDeclaration) {
      try {
        return evaluator.handleVariableDeclaration(
            currentState, (CVariableDeclaration) cDecl, edge);
      } catch (UnsupportedOperationException e) {
        // Since we lose the cfa edge (and the CExpression for other cases) we can not throw this in
        // the method directly
        throw new UnsupportedCodeException(e.getMessage(), edge);
      }
    }
    // Fall through
    return ImmutableList.of(currentState);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState element,
      Iterable<AbstractState> elements,
      CFAEdge cfaEdge,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {
    Preconditions.checkArgument(element instanceof SMGState);

    List<SMGState> toStrengthen = new ArrayList<>();
    List<SMGState> result = new ArrayList<>();
    toStrengthen.add((SMGState) element);
    result.add((SMGState) element);

    for (AbstractState ae : elements) {
      if (ae instanceof RTTState) {
        result.clear();
        for (SMGState stateToStrengthen : toStrengthen) {
          super.setInfo(element, pPrecision, cfaEdge);
          result.add(stateToStrengthen);
        }
        toStrengthen.clear();
        toStrengthen.addAll(result);
      } else if (ae instanceof AbstractStateWithAssumptions) {
        result.clear();
        for (SMGState stateToStrengthen : toStrengthen) {
          super.setInfo(element, pPrecision, cfaEdge);
          AbstractStateWithAssumptions stateWithAssumptions = (AbstractStateWithAssumptions) ae;
          result.addAll(
              strengthenWithAssumptions(stateWithAssumptions, stateToStrengthen, cfaEdge));
        }
        toStrengthen.clear();
        toStrengthen.addAll(result);
      } else if (ae instanceof ConstraintsState) {
        throw new CPATransferException("ConstraintsCPA not compatible with SMGCPA!.");
      } else if (ae instanceof PointerState) {
        throw new CPATransferException("Don't use the pointer CPA with the SMGCPA!");
      }
    }
    // Do post-processing
    final Collection<AbstractState> postProcessedResult = new ArrayList<>(result.size());
    for (SMGState rawResult : result) {
      // The original state has already been post-processed
      if (rawResult == element) {
        postProcessedResult.add(element);
      } else {
        postProcessedResult.addAll(postProcessing(ImmutableList.of(rawResult), cfaEdge));
      }
    }

    super.resetInfo();

    return postProcessedResult;
  }

  /*
   * Used e.g. for witness validation through this CPA.
   */
  private @NonNull Collection<SMGState> strengthenWithAssumptions(
      AbstractStateWithAssumptions pStateWithAssumptions, SMGState pState, CFAEdge pCfaEdge)
      throws CPATransferException {

    Collection<SMGState> newStates = ImmutableList.of(pState);

    for (AExpression assumption : pStateWithAssumptions.getAssumptions()) {
      newStates = handleAssumption(assumption, pCfaEdge, true);

      if (newStates == null) {
        break;
      } else {
        for (SMGState newState : newStates) {
          setInfo(newState, precision, pCfaEdge);
        }
      }
    }

    return Objects.requireNonNullElseGet(newStates, ImmutableList::of);
  }

  /*
   * Handles any form of assignments a = b; a = foo();.
   * The lValue is transformed into its memory (SMG) counterpart in which the rValue,
   * evaluated by the value visitor, is then saved.
   */
  private List<SMGState> handleAssignment(
      SMGState pState, CFAEdge cfaEdge, CExpression lValue, CRightHandSide rValue)
      throws CPATransferException {

    CType rightHandSideType = SMGCPAExpressionEvaluator.getCanonicalType(rValue);
    CType leftHandSideType = SMGCPAExpressionEvaluator.getCanonicalType(lValue);

    ImmutableList.Builder<SMGState> returnStateBuilder = ImmutableList.builder();
    SMGState currentState = pState;
    for (SMGStateAndOptionalSMGObjectAndOffset targetAndOffsetAndState :
        lValue.accept(new SMGCPAAddressVisitor(evaluator, currentState, cfaEdge, logger))) {
      currentState = targetAndOffsetAndState.getSMGState();

      if (!targetAndOffsetAndState.hasSMGObjectAndOffset()) {
        // No memory for the left hand side found -> UNKNOWN
        // We still evaluate the right hand side to find errors though
        List<ValueAndSMGState> listOfStates =
            rValue.accept(new SMGCPAValueVisitor(evaluator, currentState, cfaEdge, logger));
        returnStateBuilder.addAll(Lists.transform(listOfStates, ValueAndSMGState::getState));
        continue;
      }

      SMGObject addressToWriteTo = targetAndOffsetAndState.getSMGObject();
      BigInteger offsetToWriteTo = targetAndOffsetAndState.getOffsetForObject();

      if (leftHandSideType instanceof CPointerType && rightHandSideType instanceof CArrayType) {
        // Implicit & on the array expr
        for (ValueAndSMGState addressAndState :
            evaluator.createAddress(rValue, currentState, cfaEdge)) {

          BigInteger sizeOfTypeLeft = evaluator.getBitSizeof(currentState, leftHandSideType);
          Value addressToAssign = addressAndState.getValue();
          currentState = addressAndState.getState();

          returnStateBuilder.add(
              currentState.writeValueTo(
                  addressToWriteTo,
                  offsetToWriteTo,
                  sizeOfTypeLeft,
                  addressToAssign,
                  leftHandSideType));
          continue;
        }
        continue;
      }

      // The right hand side either returns Values representing values or a AddressExpression. In
      // the later case this means the entire structure behind it needs to be copied as C is
      // pass-by-value.
      SMGCPAValueVisitor vv = new SMGCPAValueVisitor(evaluator, currentState, cfaEdge, logger);
      for (ValueAndSMGState valueAndState : vv.evaluate(rValue, leftHandSideType)) {
        returnStateBuilder.add(
            handleAssignmentOfValueTo(
                valueAndState.getValue(),
                leftHandSideType,
                addressToWriteTo,
                offsetToWriteTo,
                rightHandSideType,
                valueAndState.getState()));
      }
    }

    return returnStateBuilder.build();
  }

  /*
   * Handles the concrete assignment of the value to its destination based on the types given.
   */
  private SMGState handleAssignmentOfValueTo(
      Value valueToWrite,
      CType leftHandSideType,
      SMGObject addressToWriteTo,
      BigInteger offsetToWriteTo,
      CType rightHandSideType,
      SMGState pCurrentState)
      throws SMG2Exception {
    SMGState currentState = pCurrentState;

    // Size of the left hand side as vv.evaluate() casts automatically to this type
    BigInteger sizeInBits = evaluator.getBitSizeof(currentState, leftHandSideType);

    if (valueToWrite instanceof SymbolicIdentifier
        && ((SymbolicIdentifier) valueToWrite).getRepresentedLocation().isPresent()) {
      Preconditions.checkArgument(SMGCPAExpressionEvaluator.isStructOrUnionType(rightHandSideType));
      // A SymbolicIdentifier with location is used to copy entire variable structures (i.e.
      // arrays/structs etc.)
      return evaluator.copyStructOrArrayFromValueTo(
          valueToWrite, leftHandSideType, addressToWriteTo, offsetToWriteTo, currentState);

    } else if (valueToWrite instanceof AddressExpression) {
      if (SMGCPAExpressionEvaluator.isStructOrUnionType(rightHandSideType)) {
        Preconditions.checkArgument(
            rightHandSideType.equals(leftHandSideType)
                || rightHandSideType.canBeAssignedFrom(leftHandSideType));
        // This is a copy based on a pointer
        AddressExpression addressInValue = (AddressExpression) valueToWrite;
        Value pointerOffset = addressInValue.getOffset();
        if (!pointerOffset.isNumericValue()) {
          // Write unknown to left
          return currentState.writeValueTo(
              addressToWriteTo,
              offsetToWriteTo,
              sizeInBits,
              UnknownValue.getInstance(),
              leftHandSideType);
        }
        BigInteger baseOffsetFromPointer = pointerOffset.asNumericValue().bigInteger();

        Value properPointer;
        // We need a true pointer without AddressExpr
        if (baseOffsetFromPointer.compareTo(BigInteger.ZERO) == 0) {
          // offset == 0 -> known pointer
          properPointer = addressInValue.getMemoryAddress();
        } else {
          // Offset known but not 0, search for/create the correct address
          List<ValueAndSMGState> newAddressesAndStates =
              evaluator.findOrcreateNewPointer(
                  addressInValue.getMemoryAddress(),
                  addressInValue.getOffset().asNumericValue().bigInteger(),
                  currentState);

          // Very unlikely that a 0+ list abstraction gets materialized here
          Preconditions.checkArgument(newAddressesAndStates.size() == 1);
          ValueAndSMGState newAddressAndState = newAddressesAndStates.get(0);
          currentState = newAddressAndState.getState();
          properPointer = newAddressAndState.getValue();
        }

        Optional<SMGObjectAndOffset> maybeRightHandSideMemoryAndOffset =
            currentState.getPointsToTarget(properPointer);

        if (maybeRightHandSideMemoryAndOffset.isEmpty()) {
          return currentState;
        }
        SMGObjectAndOffset rightHandSideMemoryAndOffset =
            maybeRightHandSideMemoryAndOffset.orElseThrow();
        // copySMGObjectContentToSMGObject checks for sizes etc.
        return currentState.copySMGObjectContentToSMGObject(
            rightHandSideMemoryAndOffset.getSMGObject(),
            rightHandSideMemoryAndOffset.getOffsetForObject(),
            addressToWriteTo,
            offsetToWriteTo,
            addressToWriteTo.getSize().subtract(offsetToWriteTo));

      } else {
        // Genuine pointer that needs to be written
        // Retranslate into a pointer and write the pointer
        AddressExpression addressInValue = (AddressExpression) valueToWrite;
        if (addressInValue.getOffset().isNumericValue()
            && addressInValue.getOffset().asNumericValue().longValue() == 0) {
          // offset == 0 -> write the value directly (known pointer)
          valueToWrite = addressInValue.getMemoryAddress();
        } else if (addressInValue.getOffset().isNumericValue()) {
          // Offset known but not 0, search for/create the correct address
          List<ValueAndSMGState> newAddressesAndStates =
              evaluator.findOrcreateNewPointer(
                  addressInValue.getMemoryAddress(),
                  addressInValue.getOffset().asNumericValue().bigInteger(),
                  currentState);

          // Very unlikely that a 0+ list abstraction gets materialized here
          Preconditions.checkArgument(newAddressesAndStates.size() == 1);
          ValueAndSMGState newAddressAndState = newAddressesAndStates.get(0);
          currentState = newAddressAndState.getState();
          valueToWrite = newAddressAndState.getValue();
        } else {
          // Offset unknown/symbolic. This is not usable!
          valueToWrite = UnknownValue.getInstance();
        }
        Preconditions.checkArgument(
            sizeInBits.compareTo(evaluator.getBitSizeof(currentState, leftHandSideType)) == 0);

        return currentState.writeValueTo(
            addressToWriteTo, offsetToWriteTo, sizeInBits, valueToWrite, leftHandSideType);
      }

    } else {
      // All other cases should return such that the value can be written directly to the left
      // hand side!
      return currentState.writeValueTo(
          addressToWriteTo, offsetToWriteTo, sizeInBits, valueToWrite, leftHandSideType);
    }
  }
}
