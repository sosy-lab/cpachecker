// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
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
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAValueExpressionEvaluator;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

public class SMGTransferRelation
    extends ForwardingTransferRelation<Collection<SMGState>, SMGState, SMGPrecision> {

  private final SMGOptions options;

  @SuppressWarnings("unused")
  private final MachineModel machineModel;

  @SuppressWarnings("unused")
  private final ShutdownNotifier shutdownNotifier;

  private final LogManagerWithoutDuplicates logger;

  public SMGTransferRelation(
      LogManager pLogger,
      SMGOptions pOptions,
      MachineModel pMachineModel,
      ShutdownNotifier pShutdownNotifier) {
    logger = new LogManagerWithoutDuplicates(pLogger);
    options = pOptions;
    machineModel = pMachineModel;
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  protected Collection<SMGState> postProcessing(Collection<SMGState> pSuccessors, CFAEdge edge) {
    Set<CSimpleDeclaration> outOfScopeVars = edge.getSuccessor().getOutOfScopeVariables();
    return transformedImmutableSetCopy(
        pSuccessors,
        successorState -> {
          SMGState prunedState = successorState.copyAndPruneOutOfScopeVariables(outOfScopeVars);
          return checkAndSetErrorRelation(prunedState);
        });
  }

  @SuppressWarnings("unused")
  private SMGState checkAndSetErrorRelation(SMGState pPrunedState) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  protected Set<SMGState> handleBlankEdge(BlankEdge cfaEdge) throws CPATransferException {
    if (cfaEdge.getSuccessor() instanceof FunctionExitNode) {
      if (isEntryFunction(cfaEdge)) {
        return handleReturnEntryFunction(Collections.singleton(state));
      }
    }

    return Collections.singleton(state);
  }

  private Set<SMGState> handleReturnEntryFunction(Collection<SMGState> pSuccessors) {
    return pSuccessors.stream()
        .map(
            pState -> {
              if (options.isHandleNonFreedMemoryInMainAsMemLeak()) {
                pState = pState.dropStackFrame();
              }
              return pState.copyAndPruneUnreachable();
            })
        .collect(ImmutableSet.toImmutableSet());
  }

  private boolean isEntryFunction(CFAEdge pCfaEdge) {
    return pCfaEdge.getSuccessor().getNumLeavingEdges() == 0;
  }

  /* (non-Javadoc)
   * Returns a collection of SMGStates that are the successors of the handled edge.
   * This method will
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
    // First get the (SMG)Object that is returned if possible
    Optional<SMGObject> returnObjectOptional =
        state.getMemoryModel().getReturnObjectForCurrentStackFrame();
    Collection<SMGState> successors = Collections.singleton(state);
    // If there is an (SMG)Object returned, assign it to the successor state
    if (returnObjectOptional.isPresent()) {
      successors = assignStatementToField(state, returnObjectOptional.orElseThrow(), returnEdge);
    }

    // Handle entry function return (check for mem leaks)
    if (isEntryFunction(returnEdge)) {
      return handleReturnEntryFunction(successors);
    }
    return successors;
  }

  /**
   * Evaluates the value of the given expression (i.e. a return statement) and assigns the value to
   * given state at the given region.
   *
   * @param pState - The current {@link SMGState}.
   * @param pRegion - The {@link SMGObject} that is the return object on the heap of the function
   *     just returned.
   * @param pReturnEdge - The {@link CReturnStatementEdge} that models the return of the function
   *     that just returned.
   * @return A collection of {@link SMGState}s that represents the successor states.
   * @throws CPATransferException is thrown if TODO:?
   */
  @SuppressWarnings("unused")
  private Collection<SMGState> assignStatementToField(
      SMGState pState, SMGObject pRegion, CReturnStatementEdge pReturnEdge)
      throws CPATransferException {
    // If there is no concrete value use 0 as that is the C default value
    CExpression returnExp = pReturnEdge.getExpression().orElse(CIntegerLiteralExpression.ZERO);
    SMGCPAValueExpressionEvaluator valueExpressionVisitor =
        new SMGCPAValueExpressionEvaluator(machineModel, logger);
    // TODO: the rest, because this makes no sense
    return valueExpressionVisitor.evaluateValues(pState, pReturnEdge, returnExp);
  }

  @Override
  protected Collection<SMGState> handleFunctionReturnEdge(
      CFunctionReturnEdge functionReturnEdge,
      CFunctionSummaryEdge fnkCall,
      CFunctionCall summaryExpr,
      String callerFunctionName)
      throws CPATransferException {
    return null;
  }

  @Override
  protected Collection<SMGState> handleFunctionCallEdge(
      CFunctionCallEdge callEdge,
      List<CExpression> arguments,
      List<CParameterDeclaration> paramDecl,
      String calledFunctionName)
      throws CPATransferException {

    return null;
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
    // Assumptions are essentially all value analysis in nature. We get the values from the SMGs
    // though.
    // Assumptions are for example all comparisons like ==, !=, <.... and should always be a
    // CBinaryExpression
    return null;
  }

  @Override
  protected Collection<SMGState> handleStatementEdge(CStatementEdge pCfaEdge, CStatement cStmt)
      throws CPATransferException {

    return null;
  }

  @Override
  protected List<SMGState> handleDeclarationEdge(CDeclarationEdge edge, CDeclaration cDecl)
      throws CPATransferException {
    return null;
  }

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
   * Handles any form of assignments. Takes the C/CFA arguments, transforms them into SMG arguments (reads SMG, gets SMG representatives of C values/objects or creates them if they are unknown) and then assigns them to the SMG counterparts, updating the state in the process.
   */
  @SuppressWarnings("unused")
  private List<SMGState> handleAssignment(
      SMGState pState, CFAEdge cfaEdge, CExpression lValue, CRightHandSide rValue)
      throws CPATransferException {
    /* old/ just reference
    List<SMGState> result = new ArrayList<>(4);
    LValueAssignmentVisitor visitor =
        expressionEvaluator.getLValueAssignmentVisitor(cfaEdge, pState);
    // Create ?visitor
    // For all values in lValue do:
    //  Get the SMGState, SMGValue etc.
    //  Get the correct type (bit size) for the value
    //  If the object to be assigned to ?
    for (SMGAddressAndState addressOfFieldAndState : lValue.accept(visitor)) {
      SMGAddress addressOfField = addressOfFieldAndState.getObject();
      pState = addressOfFieldAndState.getSmgState();

      CType fieldType = TypeUtils.getRealExpressionType(lValue);

      if (addressOfField.isUnknown()) {
        // Check for dereference errors in rValue
        List<SMGState> newStates =
            asSMGStateList(readValueToBeAssiged(pState.copyOf(), cfaEdge, rValue));
        newStates.forEach(smgState -> smgState.unknownWrite());
        result.addAll(newStates);
      } else {
        result.addAll(
            assignFieldToState(
                pState.copyOf(),
                cfaEdge,
                addressOfField.getObject(),
                addressOfField.getOffset().getAsLong(),
                fieldType,
                rValue));
      }
    }

    return result;
    */
    return null;
  }

  @SuppressWarnings("unused")
  public SMGState assignFieldToState(
      SMGState currentState,
      CFAEdge cfaEdge,
      SMGObject memoryOfField,
      BigInteger valueOffset,
      SMGValue pValue,
      CType rValueType)
      throws UnrecognizedCodeException {

    // TODO: getSizeof() method does not cover variable array length in C. Develop ideas for that!
    // For variable length stuff we need to read the SMG (for values) but also information from the
    // CFAEdge to determin which SMG object to read correctly! So this method needs to be
    // re-thought.

    BigInteger valueSize = machineModel.getSizeof(rValueType);

    // write values depending on the type of values
    if (valueOffset.compareTo(BigInteger.ZERO) < 0
        || memoryOfField.getSize().compareTo(valueOffset.add(valueSize)) < 0) {
      // Out of range does not mean failure just yet, it might be that
      // Log out of range info
      logOutOfRangeInformation(cfaEdge, memoryOfField, valueOffset, valueSize);

      if (memoryOfField.isZero()) {
        // Try to dereference a null pointer / all null pointers should be out of range
        return currentState.withNullPointerDereferenceWhenWriting(memoryOfField);
      } else {
        // Non null memory object but out of range write
        return currentState.withOutOfRangeWrite(memoryOfField, valueOffset, valueSize, pValue);
      }
    } else if (SMGCPAValueExpressionEvaluator.isStructOrUnionType(rValueType)) {
      // Write the struct
      // return assignStruct(currentState, memoryOfField, fieldOffset, rValueType, value, cfaEdge);
    } else {
      // Write non-struct value
      // return writeValue(currentState, memoryOfField, fieldOffset, rValueType, value, cfaEdge);
    }
    return null;
  }

  /**
   * TODO: move this. Structs get a seperate assignment method because we need to potentially copy
   * from one struct to another. TODO: Do we have to do more? They might have pointers in them.
   * (might even have methods)
   */
  @SuppressWarnings("unused")
  private SMGState assignStruct(
      SMGState pNewState,
      SMGObject pMemoryOfField,
      long pFieldOffset,
      CType pRValueType,
      SMGValue pValue,
      CFAEdge pCfaEdge)
      throws UnrecognizedCodeException {
    // If the value is a known address of a struct do:
    // Get the object for the (value address) struct
    // Write the information of the struct at the value address into the new struct at the given
    // offset/size
    // (I don't know if writeValue() is good, or a dedicated copy method would be better)

    return pNewState;
  }

  /** Logs attempts to write outside of the objects field size. */
  private void logOutOfRangeInformation(
      CFAEdge cfaEdge, SMGObject memoryOfField, BigInteger valueOffset, BigInteger valueSize) {
    // TODO: Does this work with DLS?
    logger.logf(
        Level.INFO,
        "%s, Out of range: Attempting to write %d bytes at offset %d into a field with size %d"
            + " bytes: %s",
        cfaEdge.getFileLocation(),
        valueSize,
        valueOffset,
        memoryOfField.getSize(),
        cfaEdge.getRawStatement());
  }

  /*
   * Preliminary options. Copied and modified from value CPA!
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
