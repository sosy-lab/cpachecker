// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2;

import static com.google.common.truth.Truth.assertThat;

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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.cpa.constraints.ConstraintsStatistics;
import org.sosy_lab.cpachecker.cpa.smg2.abstraction.SMGCPAMaterializer;
import org.sosy_lab.cpachecker.cpa.smg2.constraint.SMGConstraintsSolver;
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

public class SMGCPATest0 {

  protected MachineModel machineModel;
  // Pointer size for the machine model in bits
  protected BigInteger pointerSizeInBits;

  protected LogManagerWithoutDuplicates logger;
  protected SMGState currentState;
  protected SMGCPAMaterializer materializer;
  protected SMGOptions smgOptions;

  protected SMGCPAExpressionEvaluator evaluator;

  protected BigInteger sllSize;
  protected BigInteger dllSize;

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
    // Per default we expect the nfo after the hfo and the pfo after that
    nfo = hfo.add(pointerSizeInBits);
    pfo = nfo.add(pointerSizeInBits);
    logger = new LogManagerWithoutDuplicates(LogManager.createTestLogManager());

    materializer = new SMGCPAMaterializer(logger);

    smgOptions = new SMGOptions(Configuration.defaultConfiguration());
    evaluator =
        new SMGCPAExpressionEvaluator(
            machineModel,
            logger,
            SMGCPAExportOptions.getNoExportInstance(),
            smgOptions,
            makeTestSolver());
    currentState = SMGState.of(machineModel, logger, smgOptions, evaluator);
  }

  // Resets state and visitor to an empty state
  @After
  public void resetSMGStateAndVisitor() {
    currentState = SMGState.of(machineModel, logger, smgOptions, evaluator);
  }

  private SMGConstraintsSolver makeTestSolver() throws InvalidConfigurationException {
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

    return new SMGConstraintsSolver(
        smtSolver, formulaManager, converter, new ConstraintsStatistics(), smgOptions);
  }

  /*
   * Will fill the list with data such that the nfo (and pfo) are last. The data is int and the same every list segment.
   * The data is numeric starting from 0, +1 each new value such that the space until nfo is filled.
   * Valid sizes are divisible by 32. The nfo for the last and pfo for the first segment are 0.
   */
  protected Value[] buildConcreteList(boolean dll, BigInteger sizeOfSegment, int listLength)
      throws SMGSolverException, SMGException {
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
                pointerSizeInBits,
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
                pointerSizeInBits,
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
                pointerSizeInBits,
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
                pointerSizeInBits,
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
    checkListDataIntegrity(pointerArray, dll);
    return pointerArray;
  }

  // Adds an EQUAL sublists depending on nfo, pfo and dll to each object that the pointer array
  // points to
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
      throws SMGSolverException {
    int objectSize = arraySize * sizeOfElements * valuesInOrder.length;
    SMGObjectAndSMGState arrayAndState =
        currentState.copyAndAddStackObject(BigInteger.valueOf(objectSize));
    currentState = arrayAndState.getState();
    SMGObject array = arrayAndState.getSMGObject();

    for (int i = 0; i < valuesInOrder.length; i++) {
      currentState =
          currentState.writeValueWithChecks(
              array,
              new NumericValue(BigInteger.valueOf(i).multiply(BigInteger.valueOf(sizeOfElements))),
              BigInteger.valueOf(sizeOfElements),
              valuesInOrder[i],
              null,
              dummyCDAEdge);
    }

    return array;
  }
}
