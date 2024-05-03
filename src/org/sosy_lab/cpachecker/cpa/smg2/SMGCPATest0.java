// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Preconditions;
import java.math.BigInteger;
import java.util.Optional;
import org.junit.After;
import org.junit.Before;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.DummyCFAEdge;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsStatistics;
import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsSolver;
import org.sosy_lab.cpachecker.cpa.smg2.abstraction.SMGCPAMaterializer;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndSMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGSolverException;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGStateAndOptionalSMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.SMGCPAExpressionEvaluator;
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.CToFormulaConverterWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.FormulaEncodingWithPointerAliasingOptions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.TypeHandlerWithPointerAliasing;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGSinglyLinkedListSegment;

public class SMGCPATest0 {

  protected MachineModel machineModel;
  // Pointer size for the machine model in bits
  protected BigInteger pointerSizeInBits;

  protected Value numericPointerSizeInBits;

  protected LogManagerWithoutDuplicates logger;
  protected SMGState currentState;
  protected SMGCPAMaterializer materializer;
  protected SMGOptions smgOptions;

  protected SMGCPAExpressionEvaluator evaluator;

  protected BigInteger sllSize;
  protected BigInteger dllSize;

  protected Value sllSizeValue;
  protected Value dllSizeValue;

  protected BigInteger hfo = BigInteger.ZERO;
  protected BigInteger nfo;
  protected BigInteger pfo;

  // Keep this above ~10 for the tests. Reduce if this class is slow.
  // Some tasks define their own list length, as e.g. nested lists get quite expensive fast
  protected static final int TEST_LIST_LENGTH = 50;

  protected CFAEdge dummyCDAEdge =
      new DummyCFAEdge(CFANode.newDummyCFANode(), CFANode.newDummyCFANode());

  // The visitor should always use the currentState!
  @Before
  public void init() throws InvalidConfigurationException {
    // We always assume lists to be head, nfo and pfo, each pointer sized.
    machineModel = MachineModel.LINUX32;
    pointerSizeInBits = BigInteger.valueOf(machineModel.getSizeofPtrInBits());
    // We expect the sizes of SLL/DLL to be hfo + nfo ( + pfo)
    sllSize = pointerSizeInBits.multiply(BigInteger.TWO);
    dllSize = pointerSizeInBits.multiply(BigInteger.valueOf(3));
    dllSizeValue = new NumericValue(dllSize);
    sllSizeValue = new NumericValue(sllSize);
    // Per default we expect the nfo after the hfo and the pfo after that
    nfo = hfo.add(pointerSizeInBits);
    pfo = nfo.add(pointerSizeInBits);
    logger = new LogManagerWithoutDuplicates(LogManager.createTestLogManager());

    materializer = new SMGCPAMaterializer(logger, new SMGCPAStatistics());

    smgOptions = new SMGOptions(Configuration.defaultConfiguration());
    evaluator =
        new SMGCPAExpressionEvaluator(
            machineModel,
            logger,
            SMGCPAExportOptions.getNoExportInstance(),
            smgOptions,
            makeTestSolver());
    currentState = SMGState.of(machineModel, logger, smgOptions, evaluator, new SMGCPAStatistics());
    numericPointerSizeInBits = new NumericValue(pointerSizeInBits);
  }

  // Resets state and visitor to an empty state
  @After
  public void resetSMGStateAndVisitor() {
    currentState = SMGState.of(machineModel, logger, smgOptions, evaluator, new SMGCPAStatistics());
  }

  private ConstraintsSolver makeTestSolver() throws InvalidConfigurationException {
    Solver smtSolver =
        Solver.create(Configuration.defaultConfiguration(), logger, ShutdownNotifier.createDummy());
    FormulaManagerView formulaManager = smtSolver.getFormulaManager();
    FormulaEncodingWithPointerAliasingOptions formulaOptions =
        new FormulaEncodingWithPointerAliasingOptions(Configuration.defaultConfiguration());
    TypeHandlerWithPointerAliasing typeHandler =
        new TypeHandlerWithPointerAliasing(logger, machineModel, formulaOptions);

    CtoFormulaConverter converter =
        new CToFormulaConverterWithPointerAliasing(
            formulaOptions,
            formulaManager,
            machineModel,
            Optional.empty(),
            logger,
            ShutdownNotifier.createDummy(),
            typeHandler,
            AnalysisDirection.FORWARD);

    return new ConstraintsSolver(
        Configuration.defaultConfiguration(),
        smtSolver,
        formulaManager,
        converter,
        new ConstraintsStatistics());
  }

  public void assertThatPointersPointToEqualAbstractedList(
      SMGState pState, int listMinLength, Value[] pointers) {
    Optional<SMGStateAndOptionalSMGObjectAndOffset> maybeTarget =
        pState.dereferencePointerWithoutMaterilization(pointers[0]);
    assertThat(maybeTarget).isPresent();

    SMGState targetState = maybeTarget.orElseThrow().getSMGState();
    assertThat(maybeTarget.orElseThrow().hasSMGObjectAndOffset()).isTrue();
    SMGObject target = maybeTarget.orElseThrow().getSMGObject();
    assertThat(target).isInstanceOf(SMGSinglyLinkedListSegment.class);
    assertThat(((SMGSinglyLinkedListSegment) target).getMinLength()).isEqualTo(listMinLength);
    for (Value ptr : pointers) {
      Optional<SMGStateAndOptionalSMGObjectAndOffset> maybeSameTarget =
          targetState.dereferencePointerWithoutMaterilization(ptr);
      assertThat(maybeSameTarget).isPresent();

      targetState = maybeTarget.orElseThrow().getSMGState();
      assertThat(maybeSameTarget.orElseThrow().hasSMGObjectAndOffset()).isTrue();
      SMGObject sameTarget = maybeSameTarget.orElseThrow().getSMGObject();
      assertThat(sameTarget).isInstanceOf(SMGSinglyLinkedListSegment.class);
      assertThat(sameTarget).isEqualTo(target);
    }
  }

  /*
   * Will fill the list with data such that the nfo (and pfo) are last. The data is int and the same every list segment.
   * The data is numeric starting from 0, +1 each new value such that the space until nfo is filled.
   * Valid sizes are divisible by 32. The nfo for the last and pfo for the first segment are 0.
   * Returns the pointers to the first and last element in the array. Might be equal.
   */
  protected Value[] buildConcreteListReturnFstAndLstPointer(
      boolean dll, BigInteger sizeOfSegment, int listLength)
      throws SMGException, SMGSolverException {
    Value[] pointerArray = new Value[2];
    SMGObject prevObject = null;

    for (int i = 0; i < listLength; i++) {
      SMGObject listSegment = SMGObject.of(0, sizeOfSegment, BigInteger.ZERO);
      currentState = currentState.copyAndAddObjectToHeap(listSegment);
      for (int j = 0; j < sizeOfSegment.divide(pointerSizeInBits).intValue(); j++) {
        currentState =
            currentState.writeValueWithChecks(
                listSegment,
                new NumericValue(BigInteger.valueOf(j).multiply(BigInteger.valueOf(32))),
                new NumericValue(pointerSizeInBits),
                new NumericValue(j),
                null,
                dummyCDAEdge);
      }

      // Pointer to the next list segment (from the prev to this, except for the last)
      if (i == listLength - 1) {
        Value nextPointer = new NumericValue(0);
        currentState =
            currentState.writeValueWithChecks(
                listSegment,
                new NumericValue(nfo),
                numericPointerSizeInBits,
                nextPointer,
                null,
                dummyCDAEdge);
      }
      if (prevObject != null) {
        ValueAndSMGState pointerAndState =
            currentState.searchOrCreateAddress(listSegment, BigInteger.ZERO);
        currentState = pointerAndState.getState();
        currentState =
            currentState.writeValueWithChecks(
                prevObject,
                new NumericValue(nfo),
                numericPointerSizeInBits,
                pointerAndState.getValue(),
                null,
                dummyCDAEdge);
      }

      if (dll) {
        // Pointer to the prev list segment
        Value prevPointer;
        if (i == 0) {
          prevPointer = new NumericValue(0);
        } else {
          ValueAndSMGState pointerAndState =
              currentState.searchOrCreateAddress(prevObject, BigInteger.ZERO);
          prevPointer = pointerAndState.getValue();
          currentState = pointerAndState.getState();
        }
        currentState =
            currentState.writeValueWithChecks(
                listSegment,
                new NumericValue(pfo),
                numericPointerSizeInBits,
                prevPointer,
                null,
                dummyCDAEdge);
      }
      if (i == 0 || i == listLength - 1) {
        // Pointer to the list segment
        ValueAndSMGState pointerAndState =
            currentState.searchOrCreateAddress(listSegment, BigInteger.ZERO);
        pointerArray[i == 0 ? i : 1] = pointerAndState.getValue();
        currentState = pointerAndState.getState();
        // Save all pointers in objects to not confuse the internal SMG assertions
        if (!currentState.hasStackFrameForFunctionDef(CFunctionDeclaration.DUMMY)) {
          currentState = currentState.copyAndAddStackFrame(CFunctionDeclaration.DUMMY);
        }
        currentState =
            currentState.copyAndAddLocalVariable(
                numericPointerSizeInBits, i == 0 ? "first" : "last", null);
        currentState =
            currentState.writeToStackOrGlobalVariable(
                i == 0 ? "first" : "last",
                new NumericValue(BigInteger.ZERO),
                new NumericValue(pointerSizeInBits),
                pointerAndState.getValue(),
                null,
                dummyCDAEdge);
      }
      if (listLength == 1) {
        ValueAndSMGState pointerAndState =
            currentState.searchOrCreateAddress(listSegment, BigInteger.ZERO);
        pointerArray[1] = pointerAndState.getValue();
        currentState = pointerAndState.getState();
        // Save all pointers in objects to not confuse the internal SMG assertions
        currentState = currentState.copyAndAddLocalVariable(numericPointerSizeInBits, "last", null);
        currentState =
            currentState.writeToStackOrGlobalVariable(
                "last",
                new NumericValue(BigInteger.ZERO),
                numericPointerSizeInBits,
                pointerAndState.getValue(),
                null,
                dummyCDAEdge);
      }

      prevObject = listSegment;
    }

    checkListDataIntegrity(pointerArray, dll);

    return pointerArray;
  }

  /*
   * Will fill the list with data such that the nfo (and pfo) are last. The data is int and the same every list segment.
   * The data is numeric starting from 0, +1 each new value such that the space until nfo is filled.
   * Valid sizes are divisible by 32. The nfo for the last and pfo for the first segment are 0.
   */
  protected Value[] buildConcreteList(boolean dll, BigInteger sizeOfSegment, int listLength)
      throws SMGException, SMGSolverException {
    return buildConcreteListWithEqualValues(
        dll,
        sizeOfSegment,
        listLength,
        0,
        BigInteger.ZERO,
        dll ? Optional.of(BigInteger.ZERO) : Optional.empty());
  }

  /**
   * Will fill the list with data such that the nfo (and pfo) are last. The data is int and the same
   * every list segment. The data is numeric starting from valueStart, +1 each new value such that
   * the space until nfo is filled. Valid sizes are divisible by 32. The nfo for the last and pfo
   * for the first segment are 0.
   */
  protected Value[] buildConcreteListWithEqualValues(
      boolean dll,
      BigInteger sizeOfSegment,
      int listLength,
      int valueStart,
      BigInteger nextPointerTargetOffset,
      Optional<BigInteger> prevPointerTargetOffset)
      throws SMGSolverException, SMGException {
    Preconditions.checkArgument(!dll || prevPointerTargetOffset.isPresent());
    Value[] pointerArray = new Value[listLength];
    SMGObject prevObject = null;

    for (int i = 0; i < listLength; i++) {
      SMGObject listSegment = SMGObject.of(0, sizeOfSegment, BigInteger.ZERO);
      currentState = currentState.copyAndAddObjectToHeap(listSegment);
      for (int j = 0; j < sizeOfSegment.divide(pointerSizeInBits).intValue(); j++) {
        currentState =
            currentState.writeValueWithChecks(
                listSegment,
                new NumericValue(BigInteger.valueOf(j).multiply(BigInteger.valueOf(32))),
                numericPointerSizeInBits,
                new NumericValue(valueStart + j),
                null,
                dummyCDAEdge);
      }

      // Pointer to the next list segment (from the prev to this, except for the last)
      if (i == listLength - 1) {
        Value nextPointer = new NumericValue(0);
        currentState =
            currentState.writeValueWithChecks(
                listSegment,
                new NumericValue(nfo),
                numericPointerSizeInBits,
                nextPointer,
                null,
                dummyCDAEdge);
      }
      if (prevObject != null) {
        ValueAndSMGState pointerAndState =
            currentState.searchOrCreateAddress(listSegment, nextPointerTargetOffset);
        currentState = pointerAndState.getState();
        currentState =
            currentState.writeValueWithChecks(
                prevObject,
                new NumericValue(nfo),
                numericPointerSizeInBits,
                pointerAndState.getValue(),
                null,
                dummyCDAEdge);
      }

      if (dll) {
        // Pointer to the prev list segment
        Value prevPointer;
        if (i == 0) {
          prevPointer = new NumericValue(0);
        } else {
          ValueAndSMGState pointerAndState =
              currentState.searchOrCreateAddress(prevObject, prevPointerTargetOffset.orElseThrow());
          prevPointer = pointerAndState.getValue();
          currentState = pointerAndState.getState();
        }
        currentState =
            currentState.writeValueWithChecks(
                listSegment,
                new NumericValue(pfo),
                numericPointerSizeInBits,
                prevPointer,
                null,
                dummyCDAEdge);
      }
      // Pointer to the list segment
      ValueAndSMGState pointerAndState =
          currentState.searchOrCreateAddress(listSegment, BigInteger.ZERO);
      pointerArray[i] = pointerAndState.getValue();
      currentState = pointerAndState.getState();

      prevObject = listSegment;
    }
    // Save all pointers in objects to not confuse the internal SMG assertions
    for (Value pointer : pointerArray) {
      SMGObjectAndSMGState stackObjAndState =
          currentState.copyAndAddStackObject(numericPointerSizeInBits);
      currentState = stackObjAndState.getState();
      SMGObject dummyStackObject = stackObjAndState.getSMGObject();
      currentState =
          currentState.writeValueWithChecks(
              dummyStackObject,
              new NumericValue(BigInteger.ZERO),
              numericPointerSizeInBits,
              pointer,
              null,
              dummyCDAEdge);
    }
    if (valueStart == 0) {
      checkListDataIntegrity(pointerArray, dll);
    }

    return pointerArray;
  }

  // Adds an EQUAL sublists depending on nfo, pfo and dll to each object that the pointer array
  // points to
  // Returns a matrix of the nested pointers
  protected Value[][] addSubListsToList(int listLength, Value[] pointersOfTopList, boolean dll)
      throws SMGSolverException, SMGException {
    Value[][] nestedPointers = new Value[listLength][];
    int i = 0;
    for (Value pointer : pointersOfTopList) {
      // Generate the same list for each top list segment and save the first pointer as data
      Value[] pointersNested = buildConcreteList(dll, sllSize, listLength);
      nestedPointers[i] = pointersNested;
      // We care only about the first pointer here
      SMGStateAndOptionalSMGObjectAndOffset topListSegmentAndState =
          currentState.dereferencePointerWithoutMaterilization(pointer).orElseThrow();
      currentState = topListSegmentAndState.getSMGState();
      SMGObject topListSegment = topListSegmentAndState.getSMGObject();
      currentState =
          currentState.writeValueWithoutChecks(
              topListSegment,
              hfo,
              pointerSizeInBits,
              currentState.getMemoryModel().getSMGValueFromValue(pointersNested[0]).orElseThrow());
      i++;
    }
    return nestedPointers;
  }

  /**
   * Checks that all pointers given have data that is located in the beginning of the list as 32bit
   * integers with the first being 0, then +1 for each after that in the same list.
   *
   * @param pointers a array of pointers pointing to a list with the default data scheme.
   */
  protected void checkListDataIntegrity(Value[] pointers, boolean dll) throws SMGException {
    int toCheckData = sllSize.divide(pointerSizeInBits).subtract(BigInteger.ONE).intValue();
    if (dll) {
      toCheckData =
          sllSize
              .divide(pointerSizeInBits)
              .subtract(BigInteger.ONE)
              .subtract(BigInteger.ONE)
              .intValue();
    }
    for (Value pointer : pointers) {
      for (int j = 0; j < toCheckData; j++) {
        ValueAndSMGState readDataWithoutMaterialization =
            currentState.readValueWithoutMaterialization(
                currentState
                    .dereferencePointerWithoutMaterilization(pointer)
                    .orElseThrow()
                    .getSMGObject(),
                BigInteger.valueOf(j).multiply(pointerSizeInBits),
                pointerSizeInBits,
                null);
        currentState = readDataWithoutMaterialization.getState();
        assertThat(readDataWithoutMaterialization.getValue().isNumericValue()).isTrue();
        assertThat(readDataWithoutMaterialization.getValue().asNumericValue().bigIntegerValue())
            .isEquivalentAccordingToCompareTo(BigInteger.valueOf(j));
      }
    }
  }

  /**
   * Builds an array (stack) in an object with the values given in the size given and returns the
   * array obj.
   */
  @SuppressWarnings("NarrowCalculation")
  protected SMGObject buildFilledArray(int arraySize, Value[] valuesInOrder, int sizeOfElements)
      throws SMGSolverException, SMGException {
    int objectSize = arraySize * sizeOfElements * valuesInOrder.length;
    SMGObjectAndSMGState arrayAndState =
        currentState.copyAndAddStackObject(new NumericValue(BigInteger.valueOf(objectSize)));
    currentState = arrayAndState.getState();
    SMGObject array = arrayAndState.getSMGObject();

    for (int i = 0; i < valuesInOrder.length; i++) {
      currentState =
          currentState.writeValueWithChecks(
              array,
              new NumericValue(BigInteger.valueOf(i).multiply(BigInteger.valueOf(sizeOfElements))),
              new NumericValue(BigInteger.valueOf(sizeOfElements)),
              valuesInOrder[i],
              null,
              dummyCDAEdge);
    }

    return array;
  }
}
