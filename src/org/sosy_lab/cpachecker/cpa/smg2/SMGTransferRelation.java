// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static com.google.common.base.Preconditions.checkState;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.LESS_EQUAL;
import static org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator.LESS_THAN;
import static org.sosy_lab.cpachecker.util.StandardFunctions.isMemoryAllocatingFunction;

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
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
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
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsStatistics;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsSolver;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.cpa.rtt.RTTState;
import org.sosy_lab.cpachecker.cpa.smg2.SMGOptions.SMGAbstractionOptions;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndOffsetMaybeNestingLvl;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGSolverException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGStateAndOptionalSMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.ConstraintsStrengthenOperator;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.AddressExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicIdentifier;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CFormulaEncodingWithPointerAliasingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.TypeHandlerWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.smg.datastructures.PersistentStack;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.SolverException;

public class SMGTransferRelation
    extends ForwardingTransferRelation<Collection<SMGState>, SMGState, SMGPrecision> {

  private final SMGOptions options;
  private final SMGAbstractionOptions abstractionOptions;

  @SuppressWarnings("unused")
  private final SMGCPAExportOptions exportSMGOptions;

  private final LogManagerWithoutDuplicates logger;

  private final SMGCPAExpressionEvaluator evaluator;

  @Nullable
  @SuppressWarnings("unused")
  private final ConstraintsStrengthenOperator constraintsStrengthenOperator;

  // Collection of tracked symbolic boolean variables that get used when learning assumptions
  // (see SMGCPAAssigningValueVisitor). Used to assign boolean concrete values.
  private final Collection<String> booleanVariables;

  private final @Nullable SMGCPAStatistics stats;

  private final ConstraintsSolver solver;

  private final CFA cfa;

  public SMGTransferRelation(
      LogManager pLogger,
      SMGOptions pOptions,
      SMGCPAExportOptions pExportSMGOptions,
      CFA pCfa,
      ConstraintsStrengthenOperator pConstraintsStrengthenOperator,
      SMGCPAStatistics pStats,
      ConstraintsSolver pSolver,
      SMGCPAExpressionEvaluator pEvaluator) {
    cfa = pCfa;
    logger = new LogManagerWithoutDuplicates(pLogger);
    options = pOptions;
    abstractionOptions = options.getAbstractionOptions();
    exportSMGOptions = pExportSMGOptions;
    solver = pSolver;

    if (pCfa.getVarClassification().isPresent()) {
      booleanVariables = pCfa.getVarClassification().orElseThrow().getIntBoolVars();
    } else {
      booleanVariables = ImmutableSet.of();
    }

    evaluator = pEvaluator;
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
      @Nullable ConstraintsStrengthenOperator pConstraintsStrengthenOperator,
      SMGCPAExpressionEvaluator pEvaluator)
      throws InvalidConfigurationException {
    logger = new LogManagerWithoutDuplicates(pLogger);
    options = pOptions;
    abstractionOptions = options.getAbstractionOptions();
    exportSMGOptions = pExportSMGOptions;
    booleanVariables = pBooleanVariables;
    constraintsStrengthenOperator = pConstraintsStrengthenOperator;
    stats = null;

    Solver smtSolver =
        Solver.create(
            Configuration.defaultConfiguration(), pLogger, ShutdownNotifier.createDummy());
    FormulaManagerView formulaManager = smtSolver.getFormulaManager();
    CFormulaEncodingWithPointerAliasingOptions formulaOptions =
        new CFormulaEncodingWithPointerAliasingOptions(Configuration.defaultConfiguration());
    TypeHandlerWithPointerAliasing typeHandler =
        new TypeHandlerWithPointerAliasing(logger, pMachineModel, formulaOptions);

    CtoFormulaConverter converter =
        new CToFormulaConverterWithPointerAliasing(
            formulaOptions,
            formulaManager,
            pMachineModel,
            Optional.empty(),
            pLogger,
            ShutdownNotifier.createDummy(),
            typeHandler,
            AnalysisDirection.FORWARD);

    solver =
        new ConstraintsSolver(
            Configuration.defaultConfiguration(),
            pMachineModel,
            smtSolver,
            formulaManager,
            converter,
            new ConstraintsStatistics());

    evaluator = pEvaluator;
    cfa = null;
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
        s = s.invalidateVariable(MemoryLocation.forDeclaration(variable), true);
      }
      successors.add(s);
    }
    return successors.build();
  }

  @Override
  protected Set<SMGState> handleBlankEdge(BlankEdge cfaEdge) throws CPATransferException {
    if ((cfaEdge.getSuccessor() instanceof FunctionExitNode) && isEntryFunction(cfaEdge)) {
      // Entry functions need special handling as they don't have a return edge
      // (i.e. check for memory leaks)
      return handleReturnEntryFunction(Collections.singleton(state), cfaEdge);
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
  private Set<SMGState> handleReturnEntryFunction(Collection<SMGState> pSuccessors, CFAEdge edge)
      throws SMGException {
    ImmutableSet.Builder<SMGState> newSuccessors = ImmutableSet.builder();
    for (SMGState s : pSuccessors) {
      SMGState successor = s;
      if (options.isHandleNonFreedMemoryInMainAsMemLeak()) {
        successor = successor.dropStackFrame();
      }
      // Pruning checks for memory leaks and updates the error state if one is found!
      newSuccessors.add(successor.copyAndPruneUnreachable(edge));
    }
    return newSuccessors.build();
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
      // retType == left hand side type of the return statement, i.e. the type in the function
      // header
      CType retType = SMGCPAExpressionEvaluator.getCanonicalType(returnExp);
      Optional<CAssignment> returnAssignment = returnEdge.asAssignment();
      CType rightHandSideType = retType;
      if (returnAssignment.isPresent()) {
        retType = returnAssignment.orElseThrow().getLeftHandSide().getExpressionType();
        rightHandSideType = returnAssignment.orElseThrow().getRightHandSide().getExpressionType();
      }

      SMGCPAValueVisitor vv = new SMGCPAValueVisitor(evaluator, state, returnEdge, logger, options);
      for (ValueAndSMGState returnValueAndState : vv.evaluate(returnExp, retType)) {
        // We get the size per state as it could theoretically change per state (abstraction)
        Value sizeInBits = evaluator.getBitSizeof(state, retType, returnEdge);

        // Iff the target and source are a struct, we need to copy the struct.
        // The value visitor gives us a pointer (singular value) that is then used to find the
        // memory of the source.
        if (SMGCPAExpressionEvaluator.isStructOrUnionType(rightHandSideType)
            && SMGCPAExpressionEvaluator.isStructOrUnionType(retType)) {

          SMGState currentState = returnValueAndState.getState();
          Value targetPointer = returnValueAndState.getValue();

          if (targetPointer.isUnknown()) {
            successorsBuilder.add(currentState);
          }

          Preconditions.checkArgument(targetPointer instanceof SymbolicIdentifier);
          Preconditions.checkArgument(
              ((SymbolicIdentifier) targetPointer).getRepresentedLocation().isPresent());
          MemoryLocation locationInPrevStackFrame =
              ((SymbolicIdentifier) targetPointer).getRepresentedLocation().orElseThrow();
          Optional<SMGObject> maybeKnownMemory =
              currentState
                  .getMemoryModel()
                  .getObjectForVisibleVariable(locationInPrevStackFrame.getQualifiedName());
          if (maybeKnownMemory.isEmpty()) {
            throw new SMGException("Usage of unknown variable in function " + returnEdge);
          }
          // Structs get copies
          BigInteger offsetSource = BigInteger.valueOf(locationInPrevStackFrame.getOffset());
          SMGObject returnMemory =
              currentState.getMemoryModel().getReturnObjectForCurrentStackFrame().orElseThrow();

          successorsBuilder.add(
              currentState.copySMGObjectContentToSMGObject(
                  maybeKnownMemory.orElseThrow(),
                  offsetSource,
                  returnMemory,
                  BigInteger.ZERO,
                  evaluator.subtractBitOffsetValues(returnMemory.getSize(), offsetSource)));

        } else {
          ValueAndSMGState valueAndStateToWrite =
              evaluator.unpackAddressExpression(
                  returnValueAndState.getValue(), returnValueAndState.getState());

          successorsBuilder.add(
              valueAndStateToWrite
                  .getState()
                  .writeToReturn(
                      sizeInBits,
                      valueAndStateToWrite.getValue(),
                      returnExp.getExpressionType(),
                      returnEdge));
        }
      }
    } else {
      successorsBuilder.add(state);
    }

    // Handle entry function return (check for mem leaks)
    if (isEntryFunction(returnEdge)) {
      return handleReturnEntryFunction(successorsBuilder.build(), returnEdge);
    }
    return successorsBuilder.build();
  }

  @Override
  protected Collection<SMGState> handleFunctionReturnEdge(
      CFunctionReturnEdge functionReturnEdge, CFunctionCall summaryExpr, String callerFunctionName)
      throws CPATransferException {

    Collection<SMGState> successors = handleFunctionReturn(functionReturnEdge);
    if (options.isCheckForMemLeaksAtEveryFrameDrop()) {
      ImmutableList.Builder<SMGState> prunedSuccessors = ImmutableList.builder();
      for (SMGState successor : successors) {
        // Pruning checks for memory leaks and updates the error state if one is found!
        prunedSuccessors.add(successor.copyAndPruneUnreachable(functionReturnEdge));
      }
      successors = prunedSuccessors.build();
    }
    return successors;
  }

  private List<SMGState> handleFunctionReturn(CFunctionReturnEdge functionReturnEdge)
      throws CPATransferException {
    CFunctionSummaryEdge summaryEdge = functionReturnEdge.getSummaryEdge();
    CFunctionCall summaryExpr = functionReturnEdge.getFunctionCall();

    Preconditions.checkArgument(
        state.getMemoryModel().getStackFrames().peek().getFunctionDefinition()
            == functionReturnEdge.getFunctionEntry().getFunctionDefinition());

    if (summaryExpr instanceof CFunctionCallAssignmentStatement funcCallExpr) {
      CExpression leftValue = funcCallExpr.getLeftHandSide();
      CType leftValueType =
          SMGCPAExpressionEvaluator.getCanonicalType(funcCallExpr.getRightHandSide());
      CType rightValueType =
          SMGCPAExpressionEvaluator.getCanonicalType(funcCallExpr.getRightHandSide());
      Value sizeInBitsValue = evaluator.getBitSizeof(state, rightValueType, functionReturnEdge);
      checkState(sizeInBitsValue.isNumericValue());
      BigInteger sizeInBits = sizeInBitsValue.asNumericValue().bigIntegerValue();
      Optional<SMGObject> returnObject =
          state.getMemoryModel().getReturnObjectForCurrentStackFrame();
      // There should always be a return memory object in the case of a
      // CFunctionCallAssignmentStatement!
      Preconditions.checkArgument(returnObject.isPresent());
      // Read the return object with its type
      ImmutableList.Builder<SMGState> stateBuilder = ImmutableList.builder();
      if (SMGCPAExpressionEvaluator.isStructOrUnionType(leftValueType)
          && SMGCPAExpressionEvaluator.isStructOrUnionType(rightValueType)) {

        SMGState currentState = state;

        for (SMGState stateWithNewVar :
            createVariableOnTheSpotForPreviousStackframe(
                leftValue, currentState, functionReturnEdge)) {
          Preconditions.checkArgument(leftValue instanceof CIdExpression);
          String leftHandSideVarName =
              ((CIdExpression) leftValue).getDeclaration().getQualifiedName();
          // We are still on the stackframe of the function call!
          Optional<SMGObject> maybeTargetVariableMemory =
              stateWithNewVar
                  .getMemoryModel()
                  .getObjectForVisibleVariableFromPreviousStackframe(leftHandSideVarName);
          Preconditions.checkArgument(maybeTargetVariableMemory.isPresent());

          stateBuilder.add(
              stateWithNewVar
                  .copySMGObjectContentToSMGObject(
                      returnObject.orElseThrow(),
                      BigInteger.ZERO,
                      maybeTargetVariableMemory.orElseThrow(),
                      BigInteger.ZERO,
                      maybeTargetVariableMemory.orElseThrow().getSize())
                  .dropStackFrame());
        }
      } else {
        for (ValueAndSMGState readValueAndState :
            state.readValue(
                returnObject.orElseThrow(), BigInteger.ZERO, sizeInBits, rightValueType)) {

          // Now we can drop the stack frame as we left the function and have the return value
          SMGState currentState = readValueAndState.getState().dropStackFrame();
          // The memory on the left hand side might not exist because of CEGAR
          for (SMGState stateWithNewVar :
              createVariableOnTheSpot(leftValue, currentState, functionReturnEdge)) {
            stateBuilder.addAll(
                evaluator.writeValueToExpression(
                    summaryEdge, leftValue, readValueAndState.getValue(), stateWithNewVar));
          }
        }
      }
      return stateBuilder.build();
    } else {
      return ImmutableList.of(state.dropStackFrame());
    }
  }

  private List<SMGState> createVariableOnTheSpotForPreviousStackframe(
      CExpression leftHandSideExpr, SMGState pState, CFAEdge pEdge) throws CPATransferException {
    // TODO: move this method to the state
    // Remove top stackframe
    PersistentStack<StackFrame> completeStack = pState.getMemoryModel().getStackFrames();
    StackFrame topStackframe = completeStack.peek();
    PersistentStack<StackFrame> stackWOTop = completeStack.popAndCopy();
    SMGState tempState =
        pState.copyAndReplaceMemoryModel(pState.getMemoryModel().withNewStackFrame(stackWOTop));

    // Create variable on the stack below
    ImmutableList.Builder<SMGState> returnListBuilder = ImmutableList.builder();
    for (SMGState stateWVar : createVariableOnTheSpot(leftHandSideExpr, tempState, pEdge)) {
      // Restore the stack
      PersistentStack<StackFrame> incompleteStack = stateWVar.getMemoryModel().getStackFrames();
      PersistentStack<StackFrame> newCompleteStack = incompleteStack.pushAndCopy(topStackframe);
      returnListBuilder.add(
          stateWVar.copyAndReplaceMemoryModel(
              stateWVar.getMemoryModel().withNewStackFrame(newCompleteStack)));
    }
    return returnListBuilder.build();
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
  private List<SMGState> createVariableOnTheSpot(
      CExpression leftHandSideExpr, SMGState pState, CFAEdge pEdge) throws CPATransferException {
    return switch (leftHandSideExpr) {
      case CIdExpression leftCIdExpr -> {
        CSimpleDeclaration decl = leftCIdExpr.getDeclaration();
        String varName = decl.getQualifiedName();
        SMGState currentState = pState;
        // The orElse(true) skips the call on purpose!
        if (pState.isLocalOrGlobalVariablePresent(varName)
            && !pState.isLocalOrGlobalVariableValid(varName).orElse(true)) {
          currentState = pState.copyAndRemoveStackVariable(varName);
        }
        if (!currentState.isLocalOrGlobalVariablePresent(varName)) {
          if (decl instanceof CVariableDeclaration cVariableDeclaration) {
            yield evaluator.handleVariableDeclarationWithoutInitializer(
                currentState, cVariableDeclaration, pEdge);
          } else if (decl instanceof CParameterDeclaration cParameterDeclaration) {
            yield evaluator.handleVariableDeclarationWithoutInitializer(
                currentState, cParameterDeclaration.asVariableDeclaration(), pEdge);
          }
        }
        yield ImmutableList.of(pState);
      }
      case CArraySubscriptExpression cArraySubscriptExpression ->
          createVariableOnTheSpot(cArraySubscriptExpression.getArrayExpression(), pState, pEdge);

      case CFieldReference cFieldReference ->
          createVariableOnTheSpot(cFieldReference.getFieldOwner(), pState, pEdge);

      case CPointerExpression cPointerExpression ->
          createVariableOnTheSpot(cPointerExpression.getOperand(), pState, pEdge);

      case CUnaryExpression cUnaryExpression ->
          createVariableOnTheSpot(cUnaryExpression.getOperand(), pState, pEdge);

      case CCastExpression cCastExpression ->
          createVariableOnTheSpot(cCastExpression.getOperand(), pState, pEdge);

      case null /*TODO check if null is necessary*/, default -> ImmutableList.of(pState);
    };
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
        throw new SMGException(
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
        Value typeSizeInBits = evaluator.getBitSizeof(currentState, cParamExp, callEdge);
        checkState(typeSizeInBits.isNumericValue());
        overallVarArgsSizeInBits =
            overallVarArgsSizeInBits.add(typeSizeInBits.asNumericValue().bigIntegerValue());
      }

      ValueAndSMGState valueAndState;
      if (parameterType instanceof CPointerType && argumentType instanceof CArrayType) {
        if (cParamExp instanceof CStringLiteralExpression stringExpr) {
          // For example: print("string"); does not create a String constant beforehand
          String stringName = currentState.getCStringLiteralExpressionVariableName(stringExpr);
          if (!currentState.isLocalOrGlobalVariablePresent(stringName)) {
            // If s String literal is part of varArgs, we don't have a variable decl
            List<SMGState> statesWithString =
                evaluator.handleStringInitializer(
                    currentState,
                    paramDecl.size() > i ? paramDecl.get(i).asVariableDeclaration() : null,
                    callEdge,
                    stringName,
                    new NumericValue(BigInteger.ZERO),
                    parameterType,
                    callEdge.getFileLocation(),
                    stringExpr);
            Preconditions.checkArgument(statesWithString.size() == 1);
            currentState = statesWithString.getFirst();
          }
        }
        // Implicit & on the array expr
        List<ValueAndSMGState> addressesAndStates =
            evaluator.createAddress(cParamExp, currentState, callEdge);

        Preconditions.checkArgument(addressesAndStates.size() == 1);
        valueAndState = addressesAndStates.getFirst();
      } else {
        // Evaluate the CExpr into a Value
        // Note: this evaluates local arrays into pointers!!!!!
        List<ValueAndSMGState> valuesAndStates =
            cParamExp.accept(
                new SMGCPAValueVisitor(evaluator, currentState, callEdge, logger, options));

        // If this ever fails; we need to take all states/values into account, meaning we would need
        // to proceed from this point onwards with all of them with all following operations
        Preconditions.checkArgument(valuesAndStates.size() == 1);
        valueAndState = valuesAndStates.getFirst();
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
      throws CPATransferException {

    SMGState currentState = pCurrentState;
    if ((valueType instanceof CArrayType || valueType instanceof CPointerType)
        && cParamType instanceof CArrayType) {
      if (paramValue instanceof AddressExpression addrParam) {
        // For pointer -> array we get an addressExpr that wraps the pointer

        // We don't support symbolic pointer arithmetics yet
        if (!(addrParam.getOffset() instanceof NumericValue numAddrParamOffset)
            || !numAddrParamOffset.bigIntegerValue().equals(BigInteger.ZERO)) {
          // UNKNOWN as we can't handle symbolic or non-zero offsets right now
          // TODO: implement either a workaround for pointers with offset to a memory region or
          //  switch to pointers for arrays per default
          throw new SMGException(
              "Usage of symbolic or non-zero offsets for pointer targets in function arguments for"
                  + " pointer to array assignment not supported at the moment: "
                  + callEdge);
        }
        paramValue = addrParam.getMemoryAddress();
      }
      // Take the pointer to the local array and get the memory area, then associate this memory
      // area with the variable name
      List<SMGStateAndOptionalSMGObjectAndOffset> knownMemoriesAndStates =
          currentState.dereferencePointer(paramValue);
      Preconditions.checkArgument(knownMemoriesAndStates.size() == 1);
      SMGStateAndOptionalSMGObjectAndOffset knownMemoryAndState = knownMemoriesAndStates.getFirst();
      currentState = knownMemoryAndState.getSMGState();
      if (!knownMemoryAndState.hasSMGObjectAndOffset()) {
        throw new SMGException("Could not associate a local array in a new function.");
      } else if (knownMemoryAndState.getOffsetForObject() instanceof NumericValue knownMemoryOffset
          && knownMemoryOffset.bigIntegerValue().compareTo(BigInteger.ZERO) != 0) {
        throw new SMGException("Could not associate a local array in a new function.");
      } else if (!(knownMemoryAndState.getOffsetForObject() instanceof NumericValue)) {
        throw new SMGException("Could not associate a local array in a new function.");
      }
      // arrays don't get copied! They are handled via pointers.
      SMGObject knownMemory = knownMemoryAndState.getSMGObject();
      return currentState.copyAndAddLocalVariable(knownMemory, varName, cParamType);

    } else if (SMGCPAExpressionEvaluator.isStructOrUnionType(valueType)
        && SMGCPAExpressionEvaluator.isStructOrUnionType(cParamType)) {

      if (paramValue.isUnknown()) {
        return currentState;
      }

      BigInteger offsetSource;
      Value sizeOfNewVariable;
      SMGObject memorySource;
      if (paramValue instanceof AddressExpression addrParamValue) {
        // The SymbolicIdentifier might be wrapped in an AddressExpression,
        // iff it results from a pointer deref on an address of a local struct
        List<SMGStateAndOptionalSMGObjectAndOffset> derefedPointerOffsetAndState =
            currentState.dereferencePointer(addrParamValue.getMemoryAddress());

        // Nothing can materialize here
        Preconditions.checkArgument(derefedPointerOffsetAndState.size() == 1);
        currentState = derefedPointerOffsetAndState.getFirst().getSMGState();

        if (!derefedPointerOffsetAndState.getFirst().hasSMGObjectAndOffset()) {
          throw new SMGException("Usage of unknown variable in function " + callEdge);
        }

        Value offsetForObject = derefedPointerOffsetAndState.getFirst().getOffsetForObject();
        if (!(offsetForObject instanceof NumericValue numOffsetForObject)) {
          throw new SMGException(
              "Usage of symbolic offsets in function arguments for structs not supported at the"
                  + " moment. "
                  + callEdge);
        }
        offsetSource = numOffsetForObject.bigIntegerValue();
        memorySource = derefedPointerOffsetAndState.getFirst().getSMGObject();
        sizeOfNewVariable = evaluator.subtractBitOffsetValues(memorySource.getSize(), offsetSource);

      } else if (paramValue instanceof SymbolicIdentifier symbParamValue) {
        // Local struct to local struct copy
        Preconditions.checkArgument(symbParamValue.getRepresentedLocation().isPresent());

        MemoryLocation locationInPrevStackFrame =
            symbParamValue.getRepresentedLocation().orElseThrow();

        Optional<SMGObject> maybeKnownMemory =
            currentState
                .getMemoryModel()
                .getObjectForVisibleVariableFromPreviousStackframe(
                    locationInPrevStackFrame.getQualifiedName());

        if (maybeKnownMemory.isEmpty()) {
          throw new SMGException("Usage of unknown variable in function " + callEdge);
        }

        offsetSource = BigInteger.valueOf(locationInPrevStackFrame.getOffset());
        memorySource = maybeKnownMemory.orElseThrow();
        sizeOfNewVariable = evaluator.subtractBitOffsetValues(memorySource.getSize(), offsetSource);
      } else {
        throw new SMGException(
            "Unexpected argument evaluation for struct argument in function call: " + callEdge);
      }

      currentState = currentState.copyAndAddLocalVariable(sizeOfNewVariable, varName, cParamType);
      SMGObject newMemory =
          currentState.getMemoryModel().getObjectForVisibleVariable(varName).orElseThrow();

      return currentState.copySMGObjectContentToSMGObject(
          memorySource,
          offsetSource,
          newMemory,
          BigInteger.ZERO,
          evaluator.subtractBitOffsetValues(newMemory.getSize(), offsetSource));
    } else {

      return evaluator.writeValueToNewVariableBasedOnTypes(
          paramValue, cParamType, valueType, varName, currentState, callEdge);
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
    // Assumptions may be value analysis or symbolic execution. We might get the values from the
    // SMGs. Assumptions are for example all comparisons like ==, !=, <.... and should
    // always be a CBinaryExpression.
    // We also might learn something by assuming symbolic or unknown values based on known values
    try {
      Collection<SMGState> handledAssumptions =
          handleAssumption(expression, cfaEdge, truthAssumption);
      if (handledAssumptions == null || handledAssumptions.isEmpty()) {
        return null;
      }
      return handledAssumptions;
    } catch (SMGSolverException e) {
      if (e.isSolverException()) {
        throw new CPATransferException(
            "Error while computing the constraint for "
                + expression
                + ". With: \n"
                + e.getSolverException());
      } else if (e.isInterruptedException()) {
        throw e.getInterruptedException();
      } else {
        throw e.getUnrecognizedCodeException();
      }
    } catch (SolverException e) {
      throw new CPATransferException(
          "Error while computing the constraint for " + expression + ".");
    }
  }

  private @Nullable Collection<SMGState> handleAssumption(
      AExpression expression, CFAEdge cfaEdge, boolean truthValue)
      throws CPATransferException, SolverException, InterruptedException {

    if (stats != null) {
      stats.incrementAssumptions();
    }

    // We know it has to be a CExpression as this analysis only supports C
    Pair<AExpression, Boolean> simplifiedExpression = simplifyAssumption(expression, truthValue);
    CExpression cExpression = (CExpression) simplifiedExpression.getFirst();
    truthValue = simplifiedExpression.getSecond();

    if (expression instanceof CBinaryExpression binEx
        && binEx.getOperand2() instanceof CIntegerLiteralExpression loopBound
        && loopBound.getValue().bitCount() <= 32) {
      if (binEx.getOperator().equals(LESS_THAN) || binEx.getOperator().equals(LESS_EQUAL)) {
        // TODO: add option and extract to method
        // Concrete loop of the form x < 5, increment abstraction bound to 1 larger than loop
        if (abstractionOptions.getListAbstractionMinimumLengthThreshold()
                <= loopBound.getValue().intValueExact()
            && abstractionOptions.getListAbstractionMinimumLengthThreshold()
                < abstractionOptions.getListAbstractionMaximumIncreaseLengthThreshold()) {
          abstractionOptions.incListAbstractionMinimumLengthThreshold();
        }
      }
    }

    ImmutableList.Builder<SMGState> resultStateBuilder = ImmutableList.builder();
    // Get the value of the expression (either true[1L], false[0L], or unknown[null])
    SMGCPAValueVisitor vv = new SMGCPAValueVisitor(evaluator, state, cfaEdge, logger, options);
    for (ValueAndSMGState valueAndState :
        vv.evaluate(
            cExpression, SMGCPAExpressionEvaluator.getCanonicalType((CExpression) expression))) {
      Value value = valueAndState.getValue();
      SMGState currentState = valueAndState.getState();

      if (value.isExplicitlyKnown() && stats != null) {
        stats.incrementDeterministicAssumptions();
      }

      if (!value.isExplicitlyKnown()) {
        // Use the assigning value visitor, we might be able to deterministically assume values (for
        // example 0 == x -> x = 0).
        // This utilizes symbolic execution if enabled and adds constraints accordingly. Might
        // return null if a path is infeasible.
        SMGCPAAssigningValueVisitor avv =
            new SMGCPAAssigningValueVisitor(
                evaluator,
                solver,
                currentState,
                cfaEdge,
                logger,
                truthValue,
                options,
                booleanVariables,
                functionName);
        try {
          List<ValueAndSMGState> maybeFeasiblePaths = cExpression.accept(avv);
          if (maybeFeasiblePaths == null) {
            // Infeasible
            return null;
          }
          return transformedImmutableListCopy(maybeFeasiblePaths, ValueAndSMGState::getState);
        } catch (SMGSolverException e) {
          if (e.isSolverException()) {
            throw e.getSolverException();
          } else {
            Preconditions.checkArgument(e.isInterruptedException());
            throw e.getInterruptedException();
          }
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

  /**
   * Returns 'true' if the given value represents the specified boolean value in the C programming
   * language.
   */
  private boolean representsBoolean(Value value, boolean bool) {
    if (value instanceof NumericValue numericValue) {
      boolean numericIsZero = numericValue.bigIntegerValue().compareTo(BigInteger.ZERO) == 0;
      if (bool) {
        return !numericIsZero;
      } else {
        return numericIsZero;
      }
    }
    return false;
  }

  @Override
  protected Collection<SMGState> handleStatementEdge(CStatementEdge pCfaEdge, CStatement cStmt)
      throws CPATransferException {
    // Either assignments a = b; or function calls foo(..);
    if (cStmt instanceof CAssignment cAssignment) {
      // Assignments, evaluate the right hand side value using the value visitor and write it into
      // the address returned by the address evaluator for the left hand side.
      CExpression lValue = cAssignment.getLeftHandSide();
      CRightHandSide rValue = cAssignment.getRightHandSide();
      ImmutableList.Builder<SMGState> stateBuilder = ImmutableList.builder();
      for (SMGState currentState : createVariableOnTheSpot(lValue, state, pCfaEdge)) {
        stateBuilder.addAll(handleAssignment(currentState, pCfaEdge, lValue, rValue));
      }
      return stateBuilder.build();

    } else if (cStmt instanceof CFunctionCallStatement cFCall) {
      // Check the arguments for the function, then simply execute the function
      CFunctionCallExpression cFCExpression = cFCall.getFunctionCallExpression();
      CExpression fileNameExpression = cFCExpression.getFunctionNameExpression();
      String calledFunctionName = fileNameExpression.toASTString();

      List<ValueAndSMGState> handledFunReturn =
          evaluator
              .getBuiltinFunctionHandler()
              .handleFunctionCallWithoutBody(cFCExpression, calledFunctionName, state, pCfaEdge);

      // Memory allocating function calls that need to be freed without remembering the pointer
      // leads to memory-leaks. Speed up this process.
      handledFunReturn =
          postprocessBuiltinCFunctionWithoutReturn(handledFunReturn, pCfaEdge, calledFunctionName);

      return transformedImmutableListCopy(handledFunReturn, ValueAndSMGState::getState);

    } else {
      // Fall through for unneeded cases
      return ImmutableList.of(state);
    }
  }

  /**
   * Post-processes C builtin function calls without subsequent assignments. Shortcuts errors for
   * memory allocating functions and returns them with memory-leaks, as their pointers can never be
   * freed. Other function calls are handled normally. Expects the input states to already have the
   * functions handled, and the values being the return values of the functions.
   */
  private List<ValueAndSMGState> postprocessBuiltinCFunctionWithoutReturn(
      List<ValueAndSMGState> statesWithHandledFunctions,
      CStatementEdge pCfaEdge,
      String calledFunctionName)
      throws CPATransferException {

    if (isMemoryAllocatingFunction(calledFunctionName)) {
      // Shortcut to faster errors
      ImmutableList.Builder<ValueAndSMGState> newReturnBuilder = ImmutableList.builder();
      String errorMSG =
          "Calling "
              + calledFunctionName
              + " and not using the return value, results in a memory-leak at "
              + pCfaEdge;

      for (ValueAndSMGState valueAndState : statesWithHandledFunctions) {
        Value funReturn = valueAndState.getValue();
        SMGState funReturnState = valueAndState.getState();

        if (funReturn instanceof NumericValue numRet
            && numRet.bigIntegerValue().equals(BigInteger.ZERO)) {
          // 0 returns do not need freeing
          newReturnBuilder.add(valueAndState);
        } else if (funReturnState.isPointer(funReturn)) {
          // Valid pointer returned, this needs to be freed, but can't be freed as the pointer will
          // be lost
          logger.logf(
              Level.INFO,
              "Called memory allocating C function %s() that needs freeing, but did not assign the"
                  + " returned value %s; memory-leak assumed following %s",
              funReturn,
              calledFunctionName,
              pCfaEdge);

          newReturnBuilder.add(
              ValueAndSMGState.of(funReturn, funReturnState.withMemoryLeak(errorMSG, funReturn)));

        } else {
          throw new SMGException(
              "Error: unexpected return value "
                  + funReturn
                  + " encountered in post-processing of function call to "
                  + functionName
                  + "() without assigning the functions return value at "
                  + pCfaEdge);
        }
      }
      return newReturnBuilder.build();
    }

    return statesWithHandledFunctions;
  }

  @Override
  protected List<SMGState> handleDeclarationEdge(CDeclarationEdge edge, CDeclaration cDecl)
      throws CPATransferException {
    SMGState currentState = state;
    // CEGAR checks inside the ifs! Else we check every typedef!

    if (cDecl instanceof CFunctionDeclaration cFuncDecl) {
      if (cfa != null
          && cFuncDecl.getQualifiedName().equals(cfa.getMainFunction().getFunctionName())) {
        if (cFuncDecl.getParameters() != null) {
          // Init main parameters if there are any
          for (CParameterDeclaration parameters : cFuncDecl.getParameters()) {
            CType paramType = SMGCPAExpressionEvaluator.getCanonicalType(parameters.getType());
            Value paramSizeInBits = evaluator.getBitSizeof(currentState, paramType, edge);
            if (paramType instanceof CPointerType || paramType instanceof CArrayType) {
              currentState =
                  currentState.copyAndAddLocalVariable(
                      paramSizeInBits, parameters.getQualifiedName(), paramType, true);
            } else {
              // argc and argv are also allocated here if they are in the program
              // argc is some nondet > 1 while argv is non-null array of unknown values size argc
              currentState =
                  currentState.copyAndAddLocalVariable(
                      paramSizeInBits, parameters.getQualifiedName(), paramType);
            }
          }
        }
      }
    } else if (cDecl instanceof CComplexTypeDeclaration) {
      // don't handle, just let pass through
    } else if (cDecl instanceof CTypeDefDeclaration) {
      // don't handle, just let pass through
    } else if (cDecl instanceof CVariableDeclaration cVarDecl) {
      try {
        return evaluator.handleVariableDeclaration(currentState, cVarDecl, edge);
      } catch (UnsupportedOperationException e) {
        // Since we lose the CFA edge (and the CExpression for other cases) we can not throw this in
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
      } else if (ae instanceof AbstractStateWithAssumptions stateWithAssumptions) {
        try {
          result.clear();
          for (SMGState stateToStrengthen : toStrengthen) {
            super.setInfo(element, pPrecision, cfaEdge);

            result.addAll(
                strengthenWithAssumptions(stateWithAssumptions, stateToStrengthen, cfaEdge));
          }
          toStrengthen.clear();
          toStrengthen.addAll(result);
        } catch (SolverException e) {
          throw new CPATransferException("Solver error while strengthening. " + e);
        }
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
      throws CPATransferException, SolverException, InterruptedException {

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
        lValue.accept(
            new SMGCPAAddressVisitor(evaluator, currentState, cfaEdge, logger, options))) {
      currentState = targetAndOffsetAndState.getSMGState();

      if (!targetAndOffsetAndState.hasSMGObjectAndOffset()) {
        // No memory for the left hand side found -> UNKNOWN
        // We still evaluate the right hand side to find errors though
        List<ValueAndSMGState> listOfStates =
            rValue.accept(
                new SMGCPAValueVisitor(evaluator, currentState, cfaEdge, logger, options));
        returnStateBuilder.addAll(Lists.transform(listOfStates, ValueAndSMGState::getState));
        continue;
      }

      SMGObject addressToWriteTo = targetAndOffsetAndState.getSMGObject();
      Value offsetToWriteTo = targetAndOffsetAndState.getOffsetForObject();

      if (rValue instanceof CStringLiteralExpression cStringLiteralExpression
          && leftHandSideType instanceof CPointerType
          && rightHandSideType instanceof CArrayType
          && lValue instanceof CIdExpression cIdExpression) {
        // Assignment of a String pointer to an existing variable with a not yet existing String
        // Create the String, get the address, save address to left hand side var
        returnStateBuilder.addAll(
            evaluator.handleStringInitializer(
                currentState,
                (CVariableDeclaration) cIdExpression.getDeclaration(),
                cfaEdge,
                cIdExpression.getDeclaration().getQualifiedName(),
                offsetToWriteTo,
                lValue.getExpressionType(),
                cfaEdge.getFileLocation(),
                cStringLiteralExpression));
        continue;
      } else if (leftHandSideType instanceof CPointerType
          && rightHandSideType instanceof CArrayType) {
        // Implicit & on the array expr
        for (ValueAndSMGState addressAndState :
            evaluator.createAddress(rValue, currentState, cfaEdge)) {

          Value sizeOfTypeLeft = evaluator.getBitSizeof(currentState, leftHandSideType, cfaEdge);
          Value addressToAssign = addressAndState.getValue();
          currentState = addressAndState.getState();

          returnStateBuilder.add(
              currentState.writeValueWithChecks(
                  addressToWriteTo,
                  offsetToWriteTo,
                  sizeOfTypeLeft,
                  addressToAssign,
                  leftHandSideType,
                  cfaEdge));
        }
        continue;
      }

      // The right hand side either returns Values representing values or an AddressExpression. In
      // the later case this means the entire structure behind it needs to be copied as C is
      // pass-by-value.
      SMGCPAValueVisitor vv =
          new SMGCPAValueVisitor(evaluator, currentState, cfaEdge, logger, options);
      for (ValueAndSMGState valueAndState : vv.evaluate(rValue, leftHandSideType)) {
        returnStateBuilder.addAll(
            handleAssignmentOfValueTo(
                valueAndState.getValue(),
                leftHandSideType,
                lValue,
                addressToWriteTo,
                offsetToWriteTo,
                rightHandSideType,
                rValue,
                valueAndState.getState(),
                cfaEdge));
      }
    }

    return returnStateBuilder.build();
  }

  /*
   * Handles the concrete assignment of the value to its destination based on the types given.
   */
  private List<SMGState> handleAssignmentOfValueTo(
      Value valueToWrite,
      CType leftHandSideType,
      CExpression lValueExpr,
      SMGObject addressToWriteTo,
      Value offsetToWriteToInBits,
      CType rightHandSideType,
      CRightHandSide rValueExpr,
      SMGState pCurrentState,
      CFAEdge edge)
      throws CPATransferException {

    SMGState currentState = pCurrentState;

    // Size of the left hand side as vv.evaluate() casts automatically to this type
    Value sizeInBits = evaluator.getBitSizeof(currentState, leftHandSideType, edge);

    if (valueToWrite instanceof SymbolicIdentifier
        && ((SymbolicIdentifier) valueToWrite).getRepresentedLocation().isPresent()) {
      Preconditions.checkArgument(SMGCPAExpressionEvaluator.isStructOrUnionType(rightHandSideType));
      // A SymbolicIdentifier with location is used to copy entire variable structures (i.e.
      // arrays/structs etc.)
      return ImmutableList.of(
          evaluator.copyStructOrArrayFromValueTo(
              valueToWrite,
              leftHandSideType,
              addressToWriteTo,
              offsetToWriteToInBits,
              currentState));

    } else if (valueToWrite instanceof AddressExpression addressInValue) {
      if (SMGCPAExpressionEvaluator.isStructOrUnionType(rightHandSideType)) {
        Preconditions.checkArgument(
            rightHandSideType.equals(leftHandSideType)
                || rightHandSideType.canBeAssignedFrom(leftHandSideType));
        // This is a copy based on a pointer
        Value pointerOffset = addressInValue.getOffset();
        if (!(pointerOffset instanceof NumericValue numPointerOffset)) {
          // Write unknown to left
          return ImmutableList.of(
              currentState.writeValueWithChecks(
                  addressToWriteTo,
                  offsetToWriteToInBits,
                  sizeInBits,
                  UnknownValue.getInstance(),
                  leftHandSideType,
                  edge));
        }
        BigInteger baseOffsetFromPointer = numPointerOffset.bigIntegerValue();

        Value properPointer;
        // We need a true pointer without AddressExpr
        if (baseOffsetFromPointer.compareTo(BigInteger.ZERO) == 0) {
          // offset == 0 -> known pointer
          properPointer = addressInValue.getMemoryAddress();
        } else {
          // Offset known but not 0, search for/create the correct address
          List<ValueAndSMGState> newAddressesAndStates =
              evaluator.findOrcreateNewPointer(
                  addressInValue.getMemoryAddress(), addressInValue.getOffset(), currentState);

          // Very unlikely that a 0+ list abstraction gets materialized here
          Preconditions.checkArgument(newAddressesAndStates.size() == 1);
          ValueAndSMGState newAddressAndState = newAddressesAndStates.getFirst();
          currentState = newAddressAndState.getState();
          properPointer = newAddressAndState.getValue();
        }

        Optional<SMGObjectAndOffsetMaybeNestingLvl> maybeRightHandSideMemoryAndOffset =
            currentState.getPointsToTarget(properPointer);

        if (maybeRightHandSideMemoryAndOffset.isEmpty()) {
          return ImmutableList.of(currentState);
        }
        SMGObjectAndOffsetMaybeNestingLvl rightHandSideMemoryAndOffset =
            maybeRightHandSideMemoryAndOffset.orElseThrow();
        // copySMGObjectContentToSMGObject checks for sizes etc.

        return ImmutableList.of(
            currentState.copySMGObjectContentToSMGObject(
                rightHandSideMemoryAndOffset.getSMGObject(),
                rightHandSideMemoryAndOffset.getOffsetForObject(),
                addressToWriteTo,
                offsetToWriteToInBits,
                evaluator.subtractBitOffsetValues(
                    addressToWriteTo.getSize(), offsetToWriteToInBits)));

      } else {
        // Genuine pointer that needs to be written
        // Retranslate into a pointer and write the pointer
        if (addressInValue.getOffset() instanceof NumericValue addressInValueOffset
            && addressInValueOffset.longValue() == 0) {
          // offset == 0 -> write the value directly (known pointer)
          valueToWrite = addressInValue.getMemoryAddress();
        } else if (addressInValue.getOffset() instanceof NumericValue
            || options.trackPredicates()) {
          // Offset known but not 0, search for/create the correct address
          List<ValueAndSMGState> newAddressesAndStates =
              evaluator.findOrcreateNewPointer(
                  addressInValue.getMemoryAddress(), addressInValue.getOffset(), currentState);

          // Very unlikely that a 0+ list abstraction gets materialized here
          Preconditions.checkArgument(newAddressesAndStates.size() == 1);
          ValueAndSMGState newAddressAndState = newAddressesAndStates.getFirst();
          currentState = newAddressAndState.getState();
          valueToWrite = newAddressAndState.getValue();
        } else {
          // Offset unknown/symbolic. This is not usable!
          valueToWrite = UnknownValue.getInstance();
        }
        Preconditions.checkArgument(
            sizeInBits.equals(evaluator.getBitSizeof(currentState, leftHandSideType, edge)));

        return ImmutableList.of(
            currentState.writeValueWithChecks(
                addressToWriteTo,
                offsetToWriteToInBits,
                sizeInBits,
                valueToWrite,
                leftHandSideType,
                edge));
      }

    } else {
      // All other cases should return such that the value can be written directly to the left
      // hand side!
      return currentState.writeValueWithChecks(
          addressToWriteTo,
          offsetToWriteToInBits,
          sizeInBits,
          lValueExpr,
          valueToWrite,
          leftHandSideType,
          rValueExpr,
          edge);
    }
  }
}
