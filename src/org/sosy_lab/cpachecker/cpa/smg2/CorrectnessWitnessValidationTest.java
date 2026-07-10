// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.sosy_lab.cpachecker.cpa.automaton.AutomatonState.buildTestAutomatonStateWithFalseInvariantOnly;
import static org.sosy_lab.cpachecker.cpa.automaton.AutomatonState.buildTestAutomatonStateWithTrueInvariantOnly;
import static org.sosy_lab.cpachecker.cpa.smg2.SMGCPATest0.makeTestSolver;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Optional;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.DummyCFAEdge;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonInternalState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.automaton.InvalidAutomatonException;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsSolver;
import org.sosy_lab.cpachecker.cpa.smg2.abstraction.SMGCPAAbstractionManager;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;

/** Unit-Tests for validating MemSafety correctness witnesses v2. */
@RunWith(Parameterized.class)
public abstract class CorrectnessWitnessValidationTest {

  private static final CFAEdge DUMMY_CFA_EDGE =
      new DummyCFAEdge(CFANode.newDummyCFANode(), CFANode.newDummyCFANode());

  private final MachineModel machineModel;

  private final LogManagerWithoutDuplicates logger;
  private final SMGOptions smgOptions;
  private final ConstraintsSolver constraintsSolver;
  private final SMGCPAExpressionEvaluator evaluator;
  private final SMGTransferRelation transferRelation;
  private final Precision precision;

  private final Automaton dummyAutomaton;

  @Parameters
  public static Object[] machineModels() {
    return new Object[] {MachineModel.LINUX32, MachineModel.LINUX64};
  }

  public CorrectnessWitnessValidationTest(MachineModel pMachineModel)
      throws InvalidConfigurationException, InvalidAutomatonException {
    machineModel = pMachineModel;
    logger = new LogManagerWithoutDuplicates(LogManager.createTestLogManager());
    Configuration config = Configuration.defaultConfiguration();
    smgOptions = new SMGOptions(config, null);
    constraintsSolver = makeTestSolver(machineModel, logger);
    evaluator =
        new SMGCPAExpressionEvaluator(
            machineModel,
            logger,
            SMGCPAExportOptions.getNoExportInstance(),
            smgOptions,
            constraintsSolver);
    transferRelation =
        new SMGTransferRelation(
            logger,
            smgOptions,
            SMGCPAExportOptions.getNoExportInstance(),
            machineModel,
            ImmutableList.of(), // We don't need boolean-variables for now
            null,
            evaluator);
    // We need a precision (default is OK) so that we can call prec after strengthening to abstract
    // the list (further) potentially.
    // The CFAEdge is used to extract the function name only. SMG2 does not use it, so we use some
    // dummy.
    precision =
        VariableTrackingPrecision.createStaticPrecision(config, Optional.empty(), SMGCPA.class);
    dummyAutomaton =
        new Automaton(
            "DummyAutomaton",
            ImmutableMap.of(),
            ImmutableList.of(
                new AutomatonInternalState("initAutomatonInternalState", ImmutableList.of())),
            "initAutomatonInternalState");
  }

  @Test
  public void falseInvariantWitnessValidationTest() throws CPATransferException {
    // Trivial FALSE invariants are unsupported currently
    String functionName = "create";
    for (int i = 1; i < 10; i++) {
      // TODO: this is not very clean. Improve!
      final SMGState initialState =
          setUpNewStateWithSLL(i, i == 6, i == 7, i == 8, i == 9, functionName);
      setUpTransferRelationWith(initialState);

      final AutomatonState automatonStateWithFalseWitness =
          buildTestAutomatonStateWithFalseInvariantOnly(dummyAutomaton, false);

      assertThrows(
          IllegalArgumentException.class,
          () ->
              transferRelation.strengthen(
                  initialState,
                  ImmutableList.of(automatonStateWithFalseWitness),
                  DUMMY_CFA_EDGE,
                  precision));
    }
  }

  @Test
  public void trueInvariantWitnessValidationTest()
      throws CPATransferException, InterruptedException {
    // TRUE invariants mean that the initial state is simply returned.
    String functionName = "create";
    for (int i = 1; i < 10; i++) {
      // TODO: this is not very clean. Improve!
      final SMGState initialState =
          setUpNewStateWithSLL(i, i == 6, i == 7, i == 8, i == 9, functionName);
      setUpTransferRelationWith(initialState);

      final AutomatonState automatonStateWithFalseWitness =
          buildTestAutomatonStateWithTrueInvariantOnly(dummyAutomaton, true);

      Collection<? extends AbstractState> strengthenedStates =
          transferRelation.strengthen(
              initialState,
              ImmutableList.of(automatonStateWithFalseWitness),
              DUMMY_CFA_EDGE,
              precision);

      assertThat(strengthenedStates).hasSize(1);
      AbstractState strengthenedState = strengthenedStates.iterator().next();
      assertThat(strengthenedState).isInstanceOf(SMGState.class);
      SMGState postStrengthenSMGState = (SMGState) strengthenedState;
      assertThat(postStrengthenSMGState).isSameInstanceAs(initialState);
    }
  }

  void setUpTransferRelationWith(SMGState stateForTransfer) {
    transferRelation.setInfo(
        stateForTransfer,
        precision,
        new CDeclarationEdge(
            "", FileLocation.DUMMY, CFANode.newDummyCFANode(), CFANode.newDummyCFANode(), null));
  }

  // TODO: add argument for used list type + argument for list type + argument(s) for next/prev
  // TODO: add argument for list pointer offset (Linux style lists)
  /**
   * Sets up an {@link SMGState} with two variables, 'sll' and 'now', as well as values/memory for
   * an SLL as if it were at the loop head below after 'length - 1' iterations. You can expect that
   * 'alloc_and_zero()' allocates a new memory section successfully and sets 'next' to 0.
   * Auto-adjusts to the machine model chosen.
   *
   * <pre>
   * <code>struct sll {
   *    struct sll *next;
   *  };</code>
   * </pre>
   *
   * <pre>
   * <code>struct sll* create(void) {
   *   struct sll *sll = alloc_and_zero();
   *   struct sll *now = sll;
   *   while(random()) {
   *     now->next = alloc_and_zero();
   *     now = now->next;
   *   }
   *   return sll;
   * }</code>
   * </pre>
   *
   * @param length needs to be larger than 0 and indicates the length of the list starting from the
   *     memory behind the pointer in 'sll'. 'now' always points towards the last element.
   * @param abstractList the list is abstracted if this flag is set (and the minimum length is long
   *     enough for the default settings to abstract, i.e. 4+2 if 'now' is not 0, and 4+1 if it is
   *     zero here), else it is concrete.
   * @param setNowToZero sets the variable 'now' to 0 if true.
   * @param loopingList if true, connects the last element of the list to the first via its 'next'
   *     pointer.
   * @param variablesAreGlobal if true, the 2 variables 'now' and 'sll' are global variables instead
   *     of local ones.
   * @param functionName the name of the function the variables are in. E.g. "create" in the example
   *     above.
   */
  private SMGState setUpNewStateWithSLL(
      final int length,
      final boolean abstractList,
      final boolean setNowToZero,
      final boolean loopingList,
      final boolean variablesAreGlobal,
      final String functionName)
      throws CPATransferException {
    checkArgument(length > 0);
    checkNotNull(machineModel);

    final BigInteger pointerSizeInBits = BigInteger.valueOf(machineModel.getSizeofPtrInBits());
    final NumericValue pointerSizeInBitsValue = new NumericValue(pointerSizeInBits);
    final NumericValue zeroValue = new NumericValue(BigInteger.ZERO);

    final CCompositeType sllCType =
        new CCompositeType(CTypeQualifiers.NONE, ComplexTypeKind.STRUCT, "sll", "sll");
    final CPointerType ptrToStructType = new CPointerType(CTypeQualifiers.NONE, sllCType);
    sllCType.setMembers(
        ImmutableList.of(new CCompositeTypeMemberDeclaration(ptrToStructType, "next")));

    SMGState state =
        SMGState.of(machineModel, logger, smgOptions, evaluator, new SMGCPAStatistics());
    state = state.copyAndAddDummyStackFrame(functionName);

    // The 'sll' variable is the size of a single pointer
    final BigInteger sllVariableSize = pointerSizeInBits;
    final NumericValue sllVariableSizeValue = new NumericValue(sllVariableSize);

    // The 'now' variable is the size of a single pointer
    final BigInteger nowVariableSize = pointerSizeInBits;
    final NumericValue nowVariableSizeValue = new NumericValue(nowVariableSize);

    // The list elements are just the size of a single pointer (i.e. the 'next' field)
    final BigInteger listElementSize = pointerSizeInBits;
    final NumericValue listElementSizeValue = new NumericValue(listElementSize);

    // The 'next' field is at the origin of the 'sll' struct, i.e. offset = 0
    final BigInteger nextOffset = BigInteger.ZERO;
    final NumericValue nextOffsetValue = new NumericValue(nextOffset);
    final NumericValue nextPointerTargetOffset = new NumericValue(BigInteger.ZERO);

    final String nowName = "now";
    final String sllName = "sll";
    final String nowQualifiedName = functionName + "::" + nowName;
    final String sllQualifiedName = functionName + "::" + sllName;

    final SMGObject sllObj;
    final SMGObject nowObj;

    // Add variables
    if (variablesAreGlobal) {
      state = state.copyAndAddGlobalVariable(nowVariableSizeValue, nowName, ptrToStructType);
      state = state.copyAndAddGlobalVariable(sllVariableSizeValue, sllName, ptrToStructType);
      sllObj = checkNotNull(state.getMemoryModel().getGlobalVariableToSmgObjectMap().get(sllName));
      nowObj = checkNotNull(state.getMemoryModel().getGlobalVariableToSmgObjectMap().get(nowName));
    } else {
      // qualified names would be current function name + :: + name, e.g.: "create::" + names
      sllObj = SMGObject.of(0, sllVariableSizeValue, BigInteger.ZERO, sllQualifiedName);
      nowObj = SMGObject.of(0, nowVariableSizeValue, BigInteger.ZERO, nowQualifiedName);
      state = state.copyAndAddLocalVariable(sllObj, sllQualifiedName, ptrToStructType);
      state = state.copyAndAddLocalVariable(nowObj, nowQualifiedName, ptrToStructType);
    }

    // Optional because so that we can easily handle "no list elements" and
    //  "list not connected via first/last" etc.
    Optional<SMGObject> firstListElement = Optional.empty();
    Optional<SMGObject> lastListElement = Optional.empty();
    for (int i = 0; i < length; i++) {
      // Create list element
      final SMGObject currentListElement = SMGObject.of(0, listElementSizeValue, BigInteger.ZERO);
      state = state.copyAndAddObjectToHeap(currentListElement);

      // Remember first for easier access to them while setting up the state
      if (firstListElement.isEmpty()) {
        firstListElement = Optional.of(currentListElement);
      }

      // Write 'next' to 0
      state =
          state.writeValueWithChecks(
              currentListElement,
              nextOffsetValue,
              pointerSizeInBitsValue,
              zeroValue,
              ptrToStructType,
              DUMMY_CFA_EDGE);

      // Connect the list (except for if it's the first element)
      if (lastListElement.isPresent()) {
        final ValueAndSMGState pointerToCurrentAndState =
            state.searchOrCreateAddress(
                currentListElement, ptrToStructType, nextPointerTargetOffset);
        state = pointerToCurrentAndState.getState();
        final Value pointerToCurrentListElement = pointerToCurrentAndState.getValue();

        state =
            state.writeValueWithChecks(
                lastListElement.orElseThrow(),
                nextOffsetValue,
                pointerSizeInBitsValue,
                pointerToCurrentListElement,
                ptrToStructType,
                DUMMY_CFA_EDGE);
      }
      lastListElement = Optional.of(currentListElement);
    }

    // Create a pointer towards the first element and save it in 'sll'
    // TODO: Linux style linked lists have offsets in their pointers, but i don't know whether they
    // have it in the pointers from the variables towards the first/last element right now. Check
    // and add behavior if needed if we ever want this kind of list!
    checkState(nextPointerTargetOffset.bigIntegerValue().equals(BigInteger.ZERO), "Look at TODO");
    final ValueAndSMGState pointerToFirstAndState =
        state.searchOrCreateAddress(
            firstListElement.orElseThrow(), ptrToStructType, nextPointerTargetOffset);
    state = pointerToFirstAndState.getState();
    final Value pointerToFirstListElementFromVariable = pointerToFirstAndState.getValue();

    state =
        state.writeValueWithChecks(
            sllObj,
            zeroValue, // Offset in pointer variables is always 0
            pointerSizeInBitsValue,
            pointerToFirstListElementFromVariable,
            ptrToStructType,
            DUMMY_CFA_EDGE);

    // Create a pointer towards the last element and save it in 'now'
    // TODO: Linux style linked lists have offsets in their pointers, but i don't know whether they
    // have it in the pointers from the variables towards the first/last element right now. Check
    // and add behavior if needed if we ever want this kind of list!
    checkState(nextPointerTargetOffset.bigIntegerValue().equals(BigInteger.ZERO), "Look at TODO");
    final ValueAndSMGState pointerToLastAndState =
        state.searchOrCreateAddress(
            lastListElement.orElseThrow(), ptrToStructType, nextPointerTargetOffset);
    state = pointerToLastAndState.getState();
    final Value pointerToLastListElementFromVariable = pointerToLastAndState.getValue();

    state =
        state.writeValueWithChecks(
            nowObj,
            zeroValue, // Offset in pointer variables is always 0
            pointerSizeInBitsValue,
            pointerToLastListElementFromVariable,
            ptrToStructType,
            DUMMY_CFA_EDGE);

    if (loopingList) {
      checkState(
          length > 0, "You can only loop lists that exist! Length needs to be greater than 1!");
      checkState(lastListElement.isPresent()); // We write the pointer to it below
      checkState(firstListElement.isPresent()); // We need it for the pointer below
      final ValueAndSMGState pointerToFirstListElementFromLastAndState =
          state.searchOrCreateAddress(
              firstListElement.orElseThrow(), ptrToStructType, nextPointerTargetOffset);
      state = pointerToFirstListElementFromLastAndState.getState();
      final Value pointerToFirstListElementFromLast =
          pointerToFirstListElementFromLastAndState.getValue();

      state =
          state.writeValueWithChecks(
              lastListElement.orElseThrow(),
              nextOffsetValue,
              pointerSizeInBitsValue,
              pointerToFirstListElementFromLast,
              ptrToStructType,
              DUMMY_CFA_EDGE);
    }

    if (setNowToZero) {
      // now = 0;
      state =
          state.writeToStackOrGlobalVariable(
              nowName,
              zeroValue,
              pointerSizeInBitsValue,
              zeroValue,
              ptrToStructType,
              DUMMY_CFA_EDGE);
    }

    checkState(state.getMemoryModel().getSmg().getAllValidAbstractedObjects().isEmpty());
    if (abstractList) {
      // This is a conservative check, that does not take elements into account that can't be
      // abstracted for whatever reason (e.g. outside pointers)
      checkArgument(
          length >= smgOptions.getAbstractionOptions().getListAbstractionMinimumLengthThreshold(),
          "You can't abstract a list with length %s if abstraction requires a minimal length of %s",
          length,
          smgOptions.getAbstractionOptions().getListAbstractionMinimumLengthThreshold());
      SMGCPAAbstractionManager absFinder =
          new SMGCPAAbstractionManager(
              state,
              smgOptions.getAbstractionOptions().getListAbstractionMinimumLengthThreshold(),
              new SMGCPAStatistics());
      state = absFinder.findAndAbstractLists();
      checkState(
          state.getMemoryModel().getSmg().getAllValidAbstractedObjects().size() == 1,
          "List abstraction failed. Is your minimum list length long enough?");
    }
    return state;
  }
}
