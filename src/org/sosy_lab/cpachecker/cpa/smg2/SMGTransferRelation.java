// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.collect.Collections3;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
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
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMG2Exception;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAValueExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.ConstantSymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;

public class SMGTransferRelation
    extends ForwardingTransferRelation<Collection<SMGState>, SMGState, SMGPrecision> {

  private final SMGOptions options;
  private final SMGCPAExportOptions exportSMGOptions;

  private final MachineModel machineModel;

  @SuppressWarnings("unused")
  private final ShutdownNotifier shutdownNotifier;

  private final LogManagerWithoutDuplicates logger;

  private final SMGCPAValueExpressionEvaluator evaluator;

  // Collection of tracked symbolic boolean variables that get used when learning assumptions
  // (see SMGCPAAssigningValueVisitor)
  private final Collection<String> booleanVariables;

  // Ignored variables (declarations)
  // TODO: ignore the declarations using these variables
  @SuppressWarnings("unused")
  private final Collection<String> addressedVariables;

  public SMGTransferRelation(
      LogManager pLogger,
      SMGOptions pOptions,
      SMGCPAExportOptions pExportSMGOptions,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa) {
    logger = new LogManagerWithoutDuplicates(pLogger);
    options = pOptions;
    exportSMGOptions = pExportSMGOptions;
    machineModel = pCfa.getMachineModel();
    shutdownNotifier = pShutdownNotifier;
    evaluator = new SMGCPAValueExpressionEvaluator(machineModel, logger, exportSMGOptions, options);

    if (pCfa.getVarClassification().isPresent()) {
      addressedVariables = pCfa.getVarClassification().orElseThrow().getAddressedVariables();
      booleanVariables = pCfa.getVarClassification().orElseThrow().getIntBoolVars();
    } else {
      addressedVariables = ImmutableSet.of();
      booleanVariables = ImmutableSet.of();
    }
  }

  /* For tests only. */
  protected SMGTransferRelation(
      LogManager pLogger,
      SMGOptions pOptions,
      SMGCPAExportOptions pExportSMGOptions,
      ShutdownNotifier pShutdownNotifier,
      MachineModel pMachineModel,
      Collection<String> pAddressedVariables,
      Collection<String> pBooleanVariables) {
    logger = new LogManagerWithoutDuplicates(pLogger);
    options = pOptions;
    exportSMGOptions = pExportSMGOptions;
    machineModel = pMachineModel;
    shutdownNotifier = pShutdownNotifier;
    evaluator = new SMGCPAValueExpressionEvaluator(machineModel, logger, exportSMGOptions, options);
    addressedVariables = pAddressedVariables;
    booleanVariables = pBooleanVariables;
  }

  @Override
  protected Collection<SMGState> postProcessing(Collection<SMGState> pSuccessors, CFAEdge edge) {
    Set<CSimpleDeclaration> outOfScopeVars = edge.getSuccessor().getOutOfScopeVariables();
    return transformedImmutableSetCopy(
        pSuccessors,
        successorState -> {
          return successorState.copyAndPruneOutOfScopeVariables(outOfScopeVars);
        });
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
   * Similar to this, we need to handle leaks at any program exit point (abort, etc.).
   * TODO: how to do this?
   * Is it sufficient to check the successor states with the exception of the returned stuff?
   */
  @Override
  protected Collection<SMGState> handleReturnStatementEdge(CReturnStatementEdge returnEdge)
      throws CPATransferException {
    ImmutableList.Builder<SMGState> successorsBuilder = ImmutableList.builder();
    // Check that there is a return object and if there is one we can write the return to it
    if (state.getMemoryModel().hasReturnObjectForCurrentStackFrame()) {
      // value 0 is the default return value in C
      CExpression returnExp = returnEdge.getExpression().orElse(CIntegerLiteralExpression.ZERO);
      CType retType = SMGCPAValueExpressionEvaluator.getCanonicalType(returnExp);
      Optional<CAssignment> returnAssignment = returnEdge.asAssignment();
      if (returnAssignment.isPresent()) {
        retType = returnAssignment.orElseThrow().getLeftHandSide().getExpressionType();
      }
      SMGCPAValueVisitor valueVisitor =
          new SMGCPAValueVisitor(evaluator, state, returnEdge, logger);
      for (ValueAndSMGState returnValueAndState : returnExp.accept(valueVisitor)) {
        // We get the size per state as it could theoretically change per state (abstraction)
        BigInteger sizeInBits = evaluator.getBitSizeof(state, retType);
        successorsBuilder.add(
            returnValueAndState
                .getState()
                .writeToReturn(sizeInBits, returnValueAndState.getValue()));
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
          SMGCPAValueExpressionEvaluator.getCanonicalType(funcCallExpr.getRightHandSide());
      BigInteger sizeInBits = evaluator.getBitSizeof(state, rightValueType);
      Optional<SMGObject> returnObject =
          state.getMemoryModel().getReturnObjectForCurrentStackFrame();
      // There should always be a return memory object in the case of a
      // CFunctionCallAssignmentStatement!
      Preconditions.checkArgument(returnObject.isPresent());
      // Read the return object with its type
      ValueAndSMGState readValueAndState =
          state.readValue(returnObject.orElseThrow(), BigInteger.ZERO, sizeInBits);

      // Now we can drop the stack frame as we left the function and have the return value
      SMGState currentState = readValueAndState.getState().dropStackFrame();
      // Get the memory for the left hand side variable
      return evaluator.writeValueToExpression(
          summaryEdge, leftValue, readValueAndState.getValue(), currentState);
    } else {
      return ImmutableList.of(state);
    }
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
    } else {
      // TODO Parameter with varArgs
      throw new SMG2Exception("TODO: handle this correctly with the example just run!");
    }

    return ImmutableList.of(handleFunctionCall(state, callEdge, arguments, paramDecl));
  }

  /**
   * Creates a new stack frame for the function call, then creates the local variables for the
   * parameters and fills them using the value visitor with the values given. This should only be
   * called if the number of arguments and paramDecls entered match.
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
    // Add the new stack frame based on the function def
    SMGState currentState =
        initialState.copyAndAddStackFrame(callEdge.getSuccessor().getFunctionDefinition());

    // Create a variable for each parameter, then evaluate all given parameters and assign them to
    // their variables
    for (int i = 0; i < arguments.size(); i++) {
      String varName = paramDecl.get(i).getName();
      CExpression cParamExp = arguments.get(i);
      CType cParamType = SMGCPAValueExpressionEvaluator.getCanonicalType(paramDecl.get(i));
      BigInteger paramSizeInBits = evaluator.getBitSizeof(currentState, cParamType);

      // Evaluator the CExpr into a Value
      SMGCPAValueVisitor valueVisitor =
          new SMGCPAValueVisitor(evaluator, currentState, callEdge, logger);
      List<ValueAndSMGState> valuesAndStates = cParamExp.accept(valueVisitor);

      // If this ever fails; we need to take all states/values into account, meaning we would neet
      // to proceed from this point onwards with all of them with all following operations
      Preconditions.checkArgument(valuesAndStates.size() == 1);
      ValueAndSMGState valueAndState = valuesAndStates.get(0);
      Value paramValue = valueAndState.getValue();

      // Create the new local variable
      currentState = valueAndState.getState().copyAndAddLocalVariable(paramSizeInBits, varName);
      // Write the value into it
      currentState =
          currentState.writeToStackOrGlobalVariable(
              varName, BigInteger.ZERO, paramSizeInBits, paramValue);
    }

    return currentState;
  }

  @Override
  protected void setInfo(
      AbstractState abstractState, Precision abstractPrecision, CFAEdge cfaEdge) {
    super.setInfo(abstractState, abstractPrecision, cfaEdge);
  }

  @Override
  protected Collection<SMGState> handleAssumption(
      CAssumeEdge cfaEdge, CExpression expression, boolean truthAssumption)
      throws CPATransferException, InterruptedException {
    // Assumptions are essentially all value analysis in nature. We might get the values from the
    // SMGs
    // though.
    // Assumptions are for example all comparisons like ==, !=, <.... and should always be a
    // CBinaryExpression
    // We also might learn something by assuming symbolic or unknown values based on known values
    return handleAssumption(cfaEdge, (AExpression) expression, truthAssumption);
  }

  private Collection<SMGState> handleAssumption(
      CAssumeEdge cfaEdge, AExpression expression, boolean truthValue) throws CPATransferException {

    // TODO: statistics
    /*
    if (stats != null) {
      stats.incrementAssumptions();
    }
    */

    // We know it has to be a CExpression as this analysis only supports C
    Pair<AExpression, Boolean> simplifiedExpression = simplifyAssumption(expression, truthValue);
    CExpression cExpression = (CExpression) simplifiedExpression.getFirst();
    truthValue = simplifiedExpression.getSecond();

    final SMGCPAValueVisitor vv = new SMGCPAValueVisitor(evaluator, state, cfaEdge, logger);

    ImmutableList.Builder<SMGState> resultStateBuilder = ImmutableList.builder();
    // Get the value of the expression (either true[1L], false[0L], or unknown[null])
    List<ValueAndSMGState> valuesAndStates = cExpression.accept(vv);
    for (ValueAndSMGState valueAndState : valuesAndStates) {
      Value value = valueAndState.getValue();
      SMGState currentState = valueAndState.getState();

      // TODO: statistics
      /*
      if (value.isExplicitlyKnown() && stats != null) {
        stats.incrementDeterministicAssumptions();
      }
      */

      if (!value.isExplicitlyKnown()) {
        SMGCPAAssigningValueVisitor avv =
            new SMGCPAAssigningValueVisitor(
                evaluator, state, cfaEdge, logger, truthValue, options, booleanVariables);

        for (ValueAndSMGState newValueAndUpdatedState : cExpression.accept(avv)) {
          SMGState updatedState = newValueAndUpdatedState.getState();

          // TODO: track missing information needed to succeed with the analysis
          /*
          if (isMissingCExpressionInformation(vv, cExpression)) {
            missingInformationList.add(new MissingInformation(truthValue, cExpression));
          }
          */

          // If we now learned something from the SMGCPAAssigningValueVisitor the branch we are in
          // (either if(expression) or the else which is !expression) condition is fulfilled.
          // TODO: is it possible to learn something in such a way that the assumption is NOT
          // fulfilled?
          resultStateBuilder.add(updatedState);
        }

      } else if (representsBoolean(value, truthValue)) {
        // We do not know more than before, and the assumption is fulfilled, so return the state
        // from
        // the value visitor (we don't need a copy as every state operation generates a new state
        // and
        // never modifies the old state)
        resultStateBuilder.add(currentState);

      } else {
        // Assumption not fulfilled
        Preconditions.checkArgument(valuesAndStates.size() == 1);
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
      return value.equals(new NumericValue(bool ? 1L : 0L));

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

      return handleAssignment(state, pCfaEdge, lValue, rValue);

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
      // Fallthrough for unhandled cases
      // TODO: log
      return ImmutableList.of(state);
    }
  }

  /*
   * Function calls without assignment only. The split up of the methods used helps with better errors.
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
        List<ValueAndSMGState> uselessValuesAndnewStates =
            builtins.evaluateConfigurableAllocationFunction(cFCExpression, pState, pCfaEdge);
        for (ValueAndSMGState valueAndState : uselessValuesAndnewStates) {
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
      uselessValuesAndStates =
          builtins.handleUnknownFunction(pCfaEdge, cFCExpression, calledFunctionName, pState);
    }
    return Collections3.transformedImmutableListCopy(
        uselessValuesAndStates, valAndState -> valAndState.getState());
  }

  @Override
  protected List<SMGState> handleDeclarationEdge(CDeclarationEdge edge, CDeclaration cDecl)
      throws CPATransferException {
    if (cDecl instanceof CFunctionDeclaration) {
      // TODO:
    } else if (cDecl instanceof CComplexTypeDeclaration) {
      // TODO:
    } else if (cDecl instanceof CTypeDefDeclaration) {
      // TODO:
    } else if (cDecl instanceof CVariableDeclaration) {
      return handleVariableDeclaration(state, (CVariableDeclaration) cDecl, edge);
    }
    // Fall through
    // TODO: log that declaration failed
    return ImmutableList.of(state);
  }

  /**
   * Creates (or re-uses) a variable for the name given. The variable is either on the stack, global
   * or externally allocated.
   *
   * @param pState current {@link SMGState}
   * @param pVarDecl declaration of the variable declared.
   * @param pEdge current CFAEdge
   * @return a new state with the variable declared and initialized.
   * @throws CPATransferException TODO
   */
  private List<SMGState> handleVariableDeclaration(
      SMGState pState, CVariableDeclaration pVarDecl, CDeclarationEdge pEdge)
      throws CPATransferException {
    String varName = pVarDecl.getName();
    CType cType = SMGCPAValueExpressionEvaluator.getCanonicalType(pVarDecl);
    boolean isExtern = pVarDecl.getCStorageClass().equals(CStorageClass.EXTERN);

    if (cType.isIncomplete() && cType instanceof CElaboratedType) {
      // for incomplete types, we do not add variables.
      // we are not allowed to read or write them, dereferencing is possible.
      // example: "struct X; extern struct X var; void main() { }"
      // TODO currently we assume that only CElaboratedTypes are unimportant when incomplete.
      return ImmutableList.of(pState);
    }

    /*
     *  If the variable exists it does so because of loops etc.
     *  Invalid declarations should be already caught by the parser.
     */
    SMGState newState = pState;
    if (!pState.checkVariableExists(pState, varName)
        && (!isExtern || options.getAllocateExternalVariables())) {
      int typeSize = evaluator.getBitSizeof(pState, cType).intValueExact();

      // Handle incomplete type of external variables as externally allocated
      if (options.isHandleIncompleteExternalVariableAsExternalAllocation()
          && cType.isIncomplete()
          && isExtern) {
        typeSize = options.getExternalAllocationSize();
      }
      if (pVarDecl.isGlobal()) {
        newState = pState.copyAndAddGlobalVariable(typeSize, varName);
      } else {
        newState = pState.copyAndAddLocalVariable(typeSize, varName);
      }
    }

    return handleInitializerForDeclaration(newState, varName, pVarDecl, cType, pEdge);
  }

  /**
   * This method expects that there is a variable (global or otherwise) existing under the name
   * entered with the corect size allocated. This also expects that the type is correct. This method
   * will write globals to 0 and handle futher initialization of variables if necessary.
   *
   * @param pState current {@link SMGState}.
   * @param pVarName name of the variable to be initialized. This var should be present on the
   *     memory model with the correct size.
   * @param pVarDecl {@link CVariableDeclaration} for the variable.
   * @param cType {@link CType} of the variable.
   * @param pEdge {@link CDeclarationEdge} for the declaration.
   * @return a list of states with the variable initialized.
   * @throws CPATransferException if something goes wrong
   */
  private List<SMGState> handleInitializerForDeclaration(
      SMGState pState,
      String pVarName,
      CVariableDeclaration pVarDecl,
      CType cType,
      CDeclarationEdge pEdge)
      throws CPATransferException {
    CInitializer newInitializer = pVarDecl.getInitializer();
    SMGState currentState = pState;

    if (pVarDecl.isGlobal()) {
      // Global vars are always initialized to 0
      // Don't nullify external variables
      if (pVarDecl.getCStorageClass().equals(CStorageClass.EXTERN)) {
        if (options.isHandleIncompleteExternalVariableAsExternalAllocation()) {
          currentState = currentState.setExternallyAllocatedFlag(pVarName);
        }
      } else {
        // Global variables (but not extern) without initializer are nullified in C
        currentState = currentState.writeToStackOrGlobalVariableToZero(pVarName);
      }
    }

    if (newInitializer != null) {
      return handleInitializer(
          currentState, pVarDecl, pEdge, pVarName, BigInteger.ZERO, cType, newInitializer);
    }

    return ImmutableList.of(currentState);
  }

  /*
   * Handles initializing of just declared variables. I.e. int bla = 5; This expects global vars to be already written to 0.
   */
  private List<SMGState> handleInitializer(
      SMGState pNewState,
      CVariableDeclaration pVarDecl,
      CFAEdge pEdge,
      String variableName,
      BigInteger pOffset,
      CType pLValueType,
      CInitializer pInitializer)
      throws CPATransferException {

    if (pInitializer instanceof CInitializerExpression) {
      CExpression expression = ((CInitializerExpression) pInitializer).getExpression();
      // string literal handling
      if (expression instanceof CStringLiteralExpression) {
        return handleStringInitializer(
            pNewState,
            pVarDecl,
            pEdge,
            variableName,
            pOffset,
            pLValueType,
            pInitializer.getFileLocation(),
            (CStringLiteralExpression) expression);
      } else if (expression instanceof CCastExpression) {
        // handle casting on initialization like 'char *str = (char *)"string";'
        return handleCastInitializer(
            pNewState,
            pVarDecl,
            pEdge,
            variableName,
            pOffset,
            pLValueType,
            pInitializer.getFileLocation(),
            (CCastExpression) expression);
      } else {
        return writeCExpressionToLocalOrGlobalVariable(
            pNewState, pEdge, variableName, pOffset, pLValueType, expression);
      }
    } else if (pInitializer instanceof CInitializerList) {
      CInitializerList pNewInitializer = ((CInitializerList) pInitializer);
      CType realCType = pLValueType.getCanonicalType();

      if (realCType instanceof CArrayType) {
        CArrayType arrayType = (CArrayType) realCType;
        return handleInitializerList(
            pNewState, pVarDecl, pEdge, variableName, pOffset, arrayType, pNewInitializer);

      } else if (realCType instanceof CCompositeType) {
        CCompositeType structType = (CCompositeType) realCType;
        return handleInitializerList(
            pNewState, pVarDecl, pEdge, variableName, pOffset, structType, pNewInitializer);
      }

      // Type cannot be resolved
      logger.log(
          Level.INFO,
          () ->
              String.format(
                  "Type %s cannot be resolved sufficiently to handle initializer %s",
                  realCType.toASTString(""), pNewInitializer));
      return ImmutableList.of(pNewState);

    } else if (pInitializer instanceof CDesignatedInitializer) {
      throw new AssertionError(
          "Error in handling initializer, designated Initializer "
              + pInitializer.toASTString()
              + " should not appear at this point.");

    } else {
      throw new UnrecognizedCodeException("Did not recognize Initializer", pInitializer);
    }
  }

  /*
   * Handles castings when initializing variables. I.e. = (char) 55;
   */
  private List<SMGState> handleCastInitializer(
      SMGState pNewState,
      CVariableDeclaration pVarDecl,
      CFAEdge pEdge,
      String variableName,
      BigInteger pOffset,
      CType pLValueType,
      FileLocation pFileLocation,
      CCastExpression pExpression)
      throws CPATransferException {
    CExpression expression = pExpression.getOperand();
    if (expression instanceof CStringLiteralExpression) {
      return handleStringInitializer(
          pNewState,
          pVarDecl,
          pEdge,
          variableName,
          pOffset,
          pLValueType,
          pFileLocation,
          (CStringLiteralExpression) expression);
    } else if (expression instanceof CCastExpression) {
      return handleCastInitializer(
          pNewState,
          pVarDecl,
          pEdge,
          variableName,
          pOffset,
          pLValueType,
          pFileLocation,
          (CCastExpression) expression);
    } else {
      return writeCExpressionToLocalOrGlobalVariable(
          pNewState, pEdge, variableName, pOffset, pLValueType, expression);
    }
  }

  /*
   * Handles and inits, to the variable given, the given CInitializerList initializers. In this case composite types like structs and unions.
   */
  private List<SMGState> handleInitializerList(
      SMGState pState,
      CVariableDeclaration pVarDecl,
      CFAEdge pEdge,
      String variableName,
      BigInteger pOffset,
      CCompositeType pLValueType,
      CInitializerList pNewInitializer)
      throws CPATransferException {

    int listCounter = 0;

    List<CCompositeType.CCompositeTypeMemberDeclaration> memberTypes = pLValueType.getMembers();
    // Member -> offset map
    Map<CCompositeType.CCompositeTypeMemberDeclaration, BigInteger> offsetAndPosition =
        machineModel.getAllFieldOffsetsInBits(pLValueType);

    // ImmutableList.Builder<SMGState> finalStates = ImmutableList.builder();
    SMGState currentState = pState;
    // Just to be sure, should never trigger
    Preconditions.checkArgument(pNewInitializer.getInitializers().size() == memberTypes.size());
    for (CInitializer initializer : pNewInitializer.getInitializers()) {
      // TODO: this has to be checked with a test!!!!
      if (initializer instanceof CDesignatedInitializer) {
        initializer = ((CDesignatedInitializer) initializer).getRightHandSide();
      }

      CType memberType = memberTypes.get(listCounter).getType();
      // The offset is the base offset given + the current offset
      BigInteger offset = pOffset.add(offsetAndPosition.get(memberTypes.get(listCounter)));

      List<SMGState> newStates =
          handleInitializer(
              currentState, pVarDecl, pEdge, variableName, offset, memberType, initializer);

      // If this ever fails: branch into the new states and perform the rest of the loop on both!
      Preconditions.checkArgument(newStates.size() == 1);
      currentState = newStates.get(0);
      // finalStates.addAll(newStates);
      listCounter++;
    }
    return ImmutableList.of(currentState);
    // return finalStates.build();
  }

  /*
   * Handles and inits, to the variable given, the given CInitializerList initializers. In this case arrays.
   */
  private List<SMGState> handleInitializerList(
      SMGState pState,
      CVariableDeclaration pVarDecl,
      CFAEdge pEdge,
      String variableName,
      BigInteger pOffset,
      CArrayType pLValueType,
      CInitializerList pNewInitializer)
      throws CPATransferException {

    CType memberType = SMGCPAValueExpressionEvaluator.getCanonicalType(pLValueType.getType());
    BigInteger memberTypeSize = evaluator.getBitSizeof(pState, memberType);

    // ImmutableList.Builder<SMGState> finalStates = ImmutableList.builder();
    SMGState currentState = pState;
    BigInteger offset = pOffset;
    for (CInitializer initializer : pNewInitializer.getInitializers()) {
      // TODO: this has to be checked with a test!!!!
      if (initializer instanceof CDesignatedInitializer) {
        initializer = ((CDesignatedInitializer) initializer).getRightHandSide();
      }

      List<SMGState> newStates =
          handleInitializer(
              currentState, pVarDecl, pEdge, variableName, offset, memberType, initializer);

      offset = offset.add(memberTypeSize);

      // If this ever fails we have to split the rest of the initializer such that all states are
      // treated the same from this point onwards
      Preconditions.checkArgument(newStates.size() == 1);
      currentState = newStates.get(0);
      // finalStates.addAll(newStates);
    }

    return ImmutableList.of(currentState);
    // return finalStates.build();
  }

  /*
   * Handle string literal expression initializer:
   * if a string initializer is used with a pointer:
   * - create a new memory for string expression (temporary array)
   * - call #handleInitializer for new region and string expression
   * - create pointer for new region and initialize pointer with it
   * else
   *  - create char array from string and call list init for given memory
   */
  private List<SMGState> handleStringInitializer(
      SMGState pState,
      CVariableDeclaration pVarDecl,
      CFAEdge pEdge,
      String variableName,
      BigInteger pOffset,
      CType pCurrentExpressionType,
      FileLocation pFileLocation,
      CStringLiteralExpression pExpression)
      throws CPATransferException {

    // If this is a pointer (i.e. char * name = "iAmAString";) we actually have not yet initialized
    // the memory for the String, just the pointer. So we need to create new memory for the String,
    // write the String into it, make a pointer to the beginning and save that in the char *.
    if (pCurrentExpressionType instanceof CPointerType) {
      // create a new memory region for the string (right hand side)
      CType stringArrayType = pExpression.transformTypeToArrayType();
      String stringVarName = "_" + pExpression.getContentString() + "_STRING_LITERAL";
      // If the var exists we change the name and create a new one
      // (Don't reuse an old variable! They might be different than the new one!)
      int num = 0;
      while (pState.isGlobalVariablePresent(stringVarName + num)) {
        num++;
      }
      stringVarName += num;

      BigInteger sizeOfString = evaluator.getBitSizeof(pState, stringArrayType);
      SMGState currentState = pState.copyAndAddGlobalVariable(sizeOfString, stringVarName);
      List<SMGState> initedStates =
          transformStringToArrayAndInitialize(
              currentState,
              pVarDecl,
              pEdge,
              stringVarName,
              BigInteger.ZERO,
              pFileLocation,
              pExpression);

      ImmutableList.Builder<SMGState> stateBuilder = ImmutableList.builder();
      for (SMGState initedState : initedStates) {
        // Now create a pointer to the String memory and save that in the original variable
        ValueAndSMGState addressAndState =
            evaluator.createAddressForLocalOrGlobalVariable(stringVarName, initedState);
        SMGState addressState = addressAndState.getState();
        stateBuilder.add(
            addressState.writeToStackOrGlobalVariable(
                variableName,
                pOffset,
                evaluator.getBitSizeof(addressState, pCurrentExpressionType),
                addressAndState.getValue()));
      }
      return stateBuilder.build();
    }

    return transformStringToArrayAndInitialize(
        pState, pVarDecl, pEdge, variableName, pOffset, pFileLocation, pExpression);
  }

  private List<SMGState> transformStringToArrayAndInitialize(
      SMGState pState,
      CVariableDeclaration pVarDecl,
      CFAEdge pEdge,
      String variableName,
      BigInteger pOffset,
      FileLocation pFileLocation,
      CStringLiteralExpression pExpression)
      throws CPATransferException {
    // Create a char array from string and call list init
    ImmutableList.Builder<CInitializer> charArrayInitialziersBuilder = ImmutableList.builder();
    CArrayType arrayType = pExpression.transformTypeToArrayType();
    for (CCharLiteralExpression charLiteralExp : pExpression.expandStringLiteral(arrayType)) {
      charArrayInitialziersBuilder.add(new CInitializerExpression(pFileLocation, charLiteralExp));
    }
    return handleInitializerList(
        pState,
        pVarDecl,
        pEdge,
        variableName,
        pOffset,
        arrayType,
        new CInitializerList(pFileLocation, charArrayInitialziersBuilder.build()));
  }

  /**
   * (non-Javadoc)
   *
   * @see
   *     org.sosy_lab.cpachecker.core.interfaces.TransferRelation#strengthen(org.sosy_lab.cpachecker.core.interfaces.AbstractState,
   *     java.lang.Iterable, org.sosy_lab.cpachecker.cfa.model.CFAEdge,
   *     org.sosy_lab.cpachecker.core.interfaces.Precision)
   */
  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState element,
      Iterable<AbstractState> elements,
      CFAEdge cfaEdge,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {

    return null;
  }

  /*
   * Writes valueToWrite (Some CExpression that does not lead to multiple values) into the
   * variable with the name given at the offset given. The type given is used for the size.
   */
  private List<SMGState> writeCExpressionToLocalOrGlobalVariable(
      SMGState pState,
      CFAEdge cfaEdge,
      String variableName,
      BigInteger pOffsetInBits,
      CType pLFieldType,
      CRightHandSide exprToWrite)
      throws CPATransferException {
    // This can't handle types leading to multiple values!
    Preconditions.checkArgument(!(exprToWrite instanceof CStringLiteralExpression));

    BigInteger sizeOfType = evaluator.getBitSizeof(pState, pLFieldType);
    SMGCPAValueVisitor valueVisitor = new SMGCPAValueVisitor(evaluator, pState, cfaEdge, logger);
    ImmutableList.Builder<SMGState> resultStatesBuilder = ImmutableList.builder();
    for (ValueAndSMGState valueAndState : exprToWrite.accept(valueVisitor)) {
      Value valueToAssign = valueAndState.getValue();
      SMGState currentState = valueAndState.getState();

      resultStatesBuilder.add(
          currentState.writeToStackOrGlobalVariable(
              variableName, pOffsetInBits, sizeOfType, valueToAssign));
    }
    return resultStatesBuilder.build();
  }

  /*
   * Handles any form of assignments a = b; a = foo();. The lValue is transformed into its memory (SMG) counterpart in which the rValue, evaluated by the value visitor, is then saved.
   * TODO: move this method as it uses SMGObject!
   */
  private List<SMGState> handleAssignment(
      SMGState pState, CFAEdge cfaEdge, CExpression lValue, CRightHandSide rValue)
      throws CPATransferException {

    ImmutableList.Builder<SMGState> returnStateBuilder = ImmutableList.builder();
    SMGCPAAddressVisitor leftHandSidevisitor =
        new SMGCPAAddressVisitor(evaluator, pState, cfaEdge, logger);
    SMGCPAValueVisitor rightHandSideVisitor =
        new SMGCPAValueVisitor(evaluator, pState, cfaEdge, logger);

    List<Optional<SMGObjectAndOffset>> maybeAddresses = lValue.accept(leftHandSidevisitor);
    List<ValueAndSMGState> valuesAndStates = rValue.accept(rightHandSideVisitor);
    for (Optional<SMGObjectAndOffset> maybeAddress : maybeAddresses) {
      if (maybeAddress.isEmpty()) {
        // No memory for the left hand side -> can't write
        // TODO: move the exception into the visitor! Then we can actually see more details as to
        // what
        // kind of variable we tried to find the memory for.
        throw new SMG2Exception("Invalid write to variable.");
      }
      SMGObjectAndOffset addressAndOffsetToWriteTo = maybeAddress.orElseThrow();
      SMGObject addressToWriteTo = addressAndOffsetToWriteTo.getSMGObject();
      BigInteger offsetToWriteTo = addressAndOffsetToWriteTo.getOffsetForObject();

      // The right hand side either returns Values representing values or a AddressExpression. In
      // the
      // later case this means the entire structure behind it needs to be copied as C is
      // pass-by-value.
      for (ValueAndSMGState valueAndState : rValue.accept(rightHandSideVisitor)) {
        Value valueToWrite = valueAndState.getValue();
        SMGState currentState = valueAndState.getState();
        if (valueToWrite instanceof ConstantSymbolicExpression
            && ((ConstantSymbolicExpression) valueToWrite).getValue() == null) {
          // A ConstantSymbolicValue without Value is used to copy entire variable structures (i.e.
          // arrays/structs etc.)
          String rightHandSideIdentifier =
              ((ConstantSymbolicExpression) valueToWrite)
                  .getRepresentedLocation()
                  .orElseThrow()
                  .getIdentifier();
          // Get the SMGObject for the memory region on the right hand side and copy the entire
          // region
          // into the left hand
          // side
          Optional<SMGObject> maybeRightHandSideMemory =
              currentState.getMemoryModel().getObjectForVisibleVariable(rightHandSideIdentifier);

          if (maybeRightHandSideMemory.isEmpty()) {
            // Write to unknown variable
            throw new SMG2Exception(
                currentState.withWriteToUnknownVariable(rightHandSideIdentifier));
          }

          SMGObject rightHandSideMemory = maybeRightHandSideMemory.orElseThrow();
          // copySMGObjectContentToSMGObject checks for sizes etc.
          returnStateBuilder.add(
              currentState.copySMGObjectContentToSMGObject(
                  rightHandSideMemory,
                  rightHandSideMemory.getOffset(),
                  addressToWriteTo,
                  offsetToWriteTo,
                  addressToWriteTo.getSize().subtract(offsetToWriteTo)));
        } else {
          // All other cases should return such that the value can be written directly to the left
          // hand side!
          BigInteger sizeInBits = evaluator.getBitSizeof(currentState, rValue);
          currentState =
              currentState.writeValueTo(
                  addressToWriteTo, offsetToWriteTo, sizeInBits, valueToWrite);
          returnStateBuilder.add(currentState);
        }
      }
    }

    return returnStateBuilder.build();
  }

  /*
   * Preliminary options. Copied and modified from value + old SMG CPA!
   */
  @Options(prefix = "cpa.smg2")
  public static class ValueTransferOptions {

    @Option(
        secure = true,
        description =
            "if there is an assumption like (x!=0), "
                + "this option sets unknown (uninitialized) variables to 1L, "
                + "when the true-branch is handled.")
    private boolean initAssumptionVars = false;

    @Option(
        secure = true,
        description =
            "Assume that variables used only in a boolean context are either zero or one.")
    private boolean optimizeBooleanVariables = true;

    @Option(secure = true, description = "Track or not function pointer values")
    private boolean ignoreFunctionValue = true;

    @Option(
        secure = true,
        description =
            "If 'ignoreFunctionValue' is set to true, this option allows to provide a fixed set of"
                + " values in the TestComp format. It is used for function-calls to calls of"
                + " VERIFIER_nondet_*. The file is provided via the option"
                + " functionValuesForRandom ")
    private boolean ignoreFunctionValueExceptRandom = false;

    @Option(
        secure = true,
        description =
            "Fixed set of values for function calls to VERIFIER_nondet_*. Does only work, if"
                + " ignoreFunctionValueExceptRandom is enabled ")
    @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
    private Path functionValuesForRandom = null;

    @Option(
        secure = true,
        description = "Use equality assumptions to assign values (e.g., (x == 0) => x = 0)")
    private boolean assignEqualityAssumptions = true;

    @Option(
        secure = true,
        description =
            "Allow the given extern functions and interpret them as pure functions"
                + " although the value analysis does not support their semantics"
                + " and this can produce wrong results.")
    private Set<String> allowedUnsupportedFunctions = ImmutableSet.of();

    public ValueTransferOptions(Configuration config) throws InvalidConfigurationException {
      config.inject(this);
    }

    boolean isInitAssumptionVars() {
      return initAssumptionVars;
    }

    boolean isAssignEqualityAssumptions() {
      return assignEqualityAssumptions;
    }

    boolean isOptimizeBooleanVariables() {
      return optimizeBooleanVariables;
    }

    boolean isIgnoreFunctionValue() {
      return ignoreFunctionValue;
    }

    public boolean isIgnoreFunctionValueExceptRandom() {
      return ignoreFunctionValueExceptRandom;
    }

    public Path getFunctionValuesForRandom() {
      return functionValuesForRandom;
    }

    boolean isAllowedUnsupportedOption(String func) {
      return allowedUnsupportedFunctions.contains(func);
    }
  }
}
