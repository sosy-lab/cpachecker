// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.abstraction;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.Optional;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg2.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.smg2.abstraction.SMGCPAAbstractionManager.SMGCandidate;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMG2Exception;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGStateAndOptionalSMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.smg.graph.SMGDoublyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGSinglyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

public class SMGCPAAbstractionTest {

  private MachineModel machineModel;
  // Pointer size for the machine model in bits
  private BigInteger pointerSizeInBits;

  private LogManagerWithoutDuplicates logger;
  private SMGState currentState;
  private SMGCPAMaterializer materializer;

  // The visitor should always use the currentState!
  // private SMGCPAValueVisitor visitor;
  // private SMGCPAValueExpressionEvaluator evaluator;

  @Before
  public void init() throws InvalidConfigurationException {
    machineModel = MachineModel.LINUX32;
    pointerSizeInBits =
        BigInteger.valueOf(
            machineModel.getSizeof(machineModel.getPointerEquivalentSimpleType()) * 8L);
    logger = new LogManagerWithoutDuplicates(LogManager.createTestLogManager());

    // null null is fine as long as builtin functions are not used!
    // evaluator = new SMGCPAValueExpressionEvaluator(machineModel, logger, null, null,
    // ImmutableList.of());

    materializer = new SMGCPAMaterializer(logger);

    currentState =
        SMGState.of(machineModel, logger, new SMGOptions(Configuration.defaultConfiguration()));

    // visitor = new SMGCPAValueVisitor(evaluator, currentState, new DummyCFAEdge(null, null),
    // logger);
  }

  // Resets state and visitor to an empty state
  @After
  public void resetSMGStateAndVisitor() throws InvalidConfigurationException {
    currentState =
        SMGState.of(machineModel, logger, new SMGOptions(Configuration.defaultConfiguration()));

    // visitor = new SMGCPAValueVisitor(evaluator, currentState, new DummyCFAEdge(null, null),
    // logger);
  }

  @Test
  public void basicDLLMaterializationTest() throws SMG2Exception {
    BigInteger sizeInBits = pointerSizeInBits.add(pointerSizeInBits).add(pointerSizeInBits);
    BigInteger hfo = BigInteger.ZERO;
    BigInteger nfo = hfo.add(pointerSizeInBits);
    BigInteger pfo = nfo.add(pointerSizeInBits);
    BigInteger offset = BigInteger.ZERO;
    int size = 10;

    SMGDoublyLinkedListSegment currentAbstraction =
        new SMGDoublyLinkedListSegment(0, sizeInBits, offset, hfo, nfo, pfo, 10);

    currentState = currentState.copyAndAddObjectToHeap(currentAbstraction);
    currentState =
        currentState.writeValueTo(
            currentAbstraction, hfo, pointerSizeInBits, new NumericValue(1), null);
    currentState =
        currentState.writeValueTo(
            currentAbstraction, nfo, pointerSizeInBits, new NumericValue(0), null);
    currentState =
        currentState.writeValueTo(
            currentAbstraction, pfo, pointerSizeInBits, new NumericValue(0), null);
    // Pointer to the abstracted list
    Value pointer = SymbolicValueFactory.getInstance().newIdentifier(null);
    currentState = currentState.createAndAddPointer(pointer, currentAbstraction, BigInteger.ZERO);
    SMGValue pointerToFirst =
        currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow();

    SMGObject[] previous = new SMGObject[10];
    // The result value is simply the value pointer to the first concrete element
    for (int i = 0; i < size; i++) {
      SMGValueAndSMGState resultValueAndState =
          materializer.handleMaterilisation(pointerToFirst, currentAbstraction, currentState);
      currentState = resultValueAndState.getSMGState();
      Preconditions.checkArgument(
          currentState.getMemoryModel().getHeapObjects().size() == (i != 9 ? i + 3 : i + 2));
      Value currentPointer =
          currentState
              .getMemoryModel()
              .getValueFromSMGValue(resultValueAndState.getSMGValue())
              .orElseThrow();
      if (i == size - 1) {
        Preconditions.checkArgument(currentPointer.isNumericValue());
        Preconditions.checkArgument(
            currentPointer.asNumericValue().bigInteger().compareTo(BigInteger.ZERO) == 0);
        break;
      } else if (i == 0) {
        Preconditions.checkArgument(currentPointer.equals(pointer));
      }
      SMGStateAndOptionalSMGObjectAndOffset targetAndOffset =
          resultValueAndState
              .getSMGState()
              .dereferencePointerWithoutMaterilization(currentPointer)
              .orElseThrow();
      Preconditions.checkArgument(
          targetAndOffset.getOffsetForObject().compareTo(BigInteger.ZERO) == 0);

      SMGObject newObj = targetAndOffset.getSMGObject();
      SMGValueAndSMGState headAndState =
          currentState.readSMGValue(newObj, BigInteger.ZERO, pointerSizeInBits);
      currentState = headAndState.getSMGState();
      Preconditions.checkArgument(
          currentState
              .getMemoryModel()
              .getValueFromSMGValue(headAndState.getSMGValue())
              .isPresent());
      Preconditions.checkArgument(
          currentState
              .getMemoryModel()
              .getValueFromSMGValue(headAndState.getSMGValue())
              .orElseThrow()
              .isNumericValue());
      Preconditions.checkArgument(
          currentState
                  .getMemoryModel()
                  .getValueFromSMGValue(headAndState.getSMGValue())
                  .orElseThrow()
                  .asNumericValue()
                  .bigInteger()
                  .compareTo(BigInteger.ONE)
              == 0);
      SMGStateAndOptionalSMGObjectAndOffset prevObjAndOffset =
          resultValueAndState
              .getSMGState()
              .dereferencePointerWithoutMaterilization(
                  currentState.getMemoryModel().getValueFromSMGValue(pointerToFirst).orElseThrow())
              .orElseThrow();
      SMGValueAndSMGState nextPointerAndState =
          currentState.readSMGValue(newObj, pointerSizeInBits, pointerSizeInBits);
      currentState = nextPointerAndState.getSMGState();
      SMGValueAndSMGState prevPointerAndState =
          currentState.readSMGValue(newObj, pfo, pointerSizeInBits);
      currentState = prevPointerAndState.getSMGState();
      SMGValue prevPointer = prevPointerAndState.getSMGValue();
      pointerToFirst = nextPointerAndState.getSMGValue();

      SMGStateAndOptionalSMGObjectAndOffset prevObjFromPrevPointerAndOffset =
          resultValueAndState
              .getSMGState()
              .dereferencePointerWithoutMaterilization(
                  currentState.getMemoryModel().getValueFromSMGValue(prevPointer).orElseThrow())
              .orElseThrow();
      if (i == 0) {
        Preconditions.checkArgument(prevObjFromPrevPointerAndOffset.getSMGObject().isZero());
        previous[i] = prevObjAndOffset.getSMGObject();
      } else {
        previous[i] = prevObjAndOffset.getSMGObject();
        Preconditions.checkArgument(
            previous[i - 1].equals(prevObjFromPrevPointerAndOffset.getSMGObject()));
      }

      SMGStateAndOptionalSMGObjectAndOffset targetToNextAndOffset =
          resultValueAndState
              .getSMGState()
              .dereferencePointerWithoutMaterilization(
                  currentState.getMemoryModel().getValueFromSMGValue(pointerToFirst).orElseThrow())
              .orElseThrow();
      Preconditions.checkArgument(
          targetToNextAndOffset.getSMGObject() instanceof SMGSinglyLinkedListSegment);
      currentAbstraction = (SMGDoublyLinkedListSegment) targetToNextAndOffset.getSMGObject();
    }
  }

  @Test
  public void basicSLLMaterializationTest() throws SMG2Exception {
    BigInteger sizeInBits = pointerSizeInBits.add(pointerSizeInBits);
    BigInteger hfo = BigInteger.ZERO;
    BigInteger nfo = hfo.add(pointerSizeInBits);
    BigInteger offset = BigInteger.ZERO;
    int size = 10;

    SMGSinglyLinkedListSegment currentAbstraction =
        new SMGSinglyLinkedListSegment(0, sizeInBits, offset, hfo, nfo, 10);

    currentState = currentState.copyAndAddObjectToHeap(currentAbstraction);
    currentState =
        currentState.writeValueTo(
            currentAbstraction, hfo, pointerSizeInBits, new NumericValue(1), null);
    currentState =
        currentState.writeValueTo(
            currentAbstraction, nfo, pointerSizeInBits, new NumericValue(0), null);
    // Pointer to the abstracted list
    Value pointer = SymbolicValueFactory.getInstance().newIdentifier(null);
    currentState = currentState.createAndAddPointer(pointer, currentAbstraction, BigInteger.ZERO);
    SMGValue pointerToFirst =
        currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow();

    // The result value is simply the value pointer to the first concrete element
    for (int i = 0; i < size; i++) {
      SMGValueAndSMGState resultValueAndState =
          materializer.handleMaterilisation(pointerToFirst, currentAbstraction, currentState);
      currentState = resultValueAndState.getSMGState();
      Preconditions.checkArgument(
          currentState.getMemoryModel().getHeapObjects().size() == (i != 9 ? i + 3 : i + 2));
      Value currentPointer =
          currentState
              .getMemoryModel()
              .getValueFromSMGValue(resultValueAndState.getSMGValue())
              .orElseThrow();
      if (i == size - 1) {
        Preconditions.checkArgument(currentPointer.isNumericValue());
        Preconditions.checkArgument(
            currentPointer.asNumericValue().bigInteger().compareTo(BigInteger.ZERO) == 0);
        break;
      } else if (i == 0) {
        Preconditions.checkArgument(currentPointer.equals(pointer));
      }
      SMGStateAndOptionalSMGObjectAndOffset targetAndOffset =
          resultValueAndState
              .getSMGState()
              .dereferencePointerWithoutMaterilization(currentPointer)
              .orElseThrow();
      Preconditions.checkArgument(
          targetAndOffset.getOffsetForObject().compareTo(BigInteger.ZERO) == 0);

      SMGObject newObj = targetAndOffset.getSMGObject();
      SMGValueAndSMGState headAndState =
          currentState.readSMGValue(newObj, BigInteger.ZERO, pointerSizeInBits);
      currentState = headAndState.getSMGState();
      Preconditions.checkArgument(
          currentState
              .getMemoryModel()
              .getValueFromSMGValue(headAndState.getSMGValue())
              .isPresent());
      Preconditions.checkArgument(
          currentState
              .getMemoryModel()
              .getValueFromSMGValue(headAndState.getSMGValue())
              .orElseThrow()
              .isNumericValue());
      Preconditions.checkArgument(
          currentState
                  .getMemoryModel()
                  .getValueFromSMGValue(headAndState.getSMGValue())
                  .orElseThrow()
                  .asNumericValue()
                  .bigInteger()
                  .compareTo(BigInteger.ONE)
              == 0);
      SMGValueAndSMGState nextPointerAndState =
          currentState.readSMGValue(newObj, pointerSizeInBits, pointerSizeInBits);
      currentState = nextPointerAndState.getSMGState();
      pointerToFirst = nextPointerAndState.getSMGValue();

      SMGStateAndOptionalSMGObjectAndOffset targetToNextAndOffset =
          resultValueAndState
              .getSMGState()
              .dereferencePointerWithoutMaterilization(
                  currentState.getMemoryModel().getValueFromSMGValue(pointerToFirst).orElseThrow())
              .orElseThrow();
      Preconditions.checkArgument(
          targetToNextAndOffset.getSMGObject() instanceof SMGSinglyLinkedListSegment);
      currentAbstraction = (SMGSinglyLinkedListSegment) targetToNextAndOffset.getSMGObject();
    }
  }

  @Test
  public void basicSLLDetectionTest() throws SMG2Exception, InvalidConfigurationException {
    for (int i = 1; i < 100; i++) {
      resetSMGStateAndVisitor();
      SMGState state = createXLongExplicitSLLOnHeap(i);
      SMGCPAAbstractionManager absFinder = new SMGCPAAbstractionManager(state, 3);
      ImmutableList<SMGCandidate> candidates = absFinder.getRefinedLinkedCandidates();

      if (i > 2) {
        assertThat(candidates).hasSize(1);
        assertThat(
                absFinder
                    .isDLL(candidates.iterator().next(), state.getMemoryModel().getSmg())
                    .isEmpty())
            .isTrue();
      } else {
        assertThat(candidates).hasSize(0);
      }
    }
  }

  @Test
  public void basicDLLDetectionTest() throws SMG2Exception, InvalidConfigurationException {
    for (int i = 1; i < 100; i++) {
      resetSMGStateAndVisitor();
      SMGState state = createXLongExplicitDLLOnHeap(i);
      SMGCPAAbstractionManager absFinder = new SMGCPAAbstractionManager(state, 3);
      ImmutableList<SMGCandidate> candidates = absFinder.getRefinedLinkedCandidates();
      if (i > 2) {
        assertThat(candidates).hasSize(1);
        SMGCandidate candidate = candidates.iterator().next();
        Optional<BigInteger> maybePfo = absFinder.isDLL(candidate, state.getMemoryModel().getSmg());
        assertThat(maybePfo.isPresent()).isTrue();
        // PFO is 64
        assertThat(maybePfo.orElseThrow().compareTo(pointerSizeInBits.add(pointerSizeInBits)) == 0)
            .isTrue();
      } else {
        assertThat(candidates).hasSize(0);
      }
    }
  }

  @SuppressWarnings("null")
  @Test
  public void abstractSLLTest() throws InvalidConfigurationException, SMG2Exception {
    BigInteger nfo = pointerSizeInBits;
    BigInteger sizeInBits = pointerSizeInBits.add(pointerSizeInBits);

    for (int i = 1; i < 100; i++) {
      resetSMGStateAndVisitor();
      SMGState state = createXLongExplicitSLLOnHeap(i);
      SMGCPAAbstractionManager absFinder = new SMGCPAAbstractionManager(state, 3);
      ImmutableList<SMGCandidate> candidates = absFinder.getRefinedLinkedCandidates();
      if (i < 3) {
        continue;
      }
      SMGCandidate firstObj = candidates.iterator().next();
      assertThat(firstObj.getSuspectedNfo()).isEquivalentAccordingToCompareTo(nfo);
      state = state.abstractIntoSLL(firstObj.getObject(), nfo);

      Set<SMGObject> objects = state.getMemoryModel().getSmg().getObjects();
      // All should be invalid except our SLL here
      SMGSinglyLinkedListSegment sll = null;
      for (SMGObject object : objects) {
        if (object instanceof SMGSinglyLinkedListSegment
            && state.getMemoryModel().isObjectValid(object)) {
          Preconditions.checkArgument(sll == null);
          sll = (SMGSinglyLinkedListSegment) object;
        } else {
          assertThat(!state.getMemoryModel().isObjectValid(object)).isTrue();
        }
      }
      assertThat(sll.getMinLength() == i).isTrue();
      assertThat(sll.getNextOffset().compareTo(nfo) == 0).isTrue();
      assertThat(sll.getSize().compareTo(sizeInBits) == 0).isTrue();
    }
  }

  @SuppressWarnings("null")
  @Test
  public void abstractDLLTest() throws InvalidConfigurationException, SMG2Exception {
    BigInteger nfo = pointerSizeInBits;
    BigInteger pfo = pointerSizeInBits.add(pointerSizeInBits);
    BigInteger sizeInBits = pointerSizeInBits.add(pointerSizeInBits).add(pointerSizeInBits);

    for (int i = 1; i < 100; i++) {
      resetSMGStateAndVisitor();
      SMGState state = createXLongExplicitDLLOnHeap(i);
      SMGCPAAbstractionManager absFinder = new SMGCPAAbstractionManager(state, 3);
      ImmutableList<SMGCandidate> candidates = absFinder.getRefinedLinkedCandidates();
      if (i < 3) {
        continue;
      }
      SMGCandidate firstObj = candidates.iterator().next();
      assertThat(firstObj.getSuspectedNfo()).isEquivalentAccordingToCompareTo(nfo);
      state = state.abstractIntoDLL(firstObj.getObject(), nfo, pfo);

      Set<SMGObject> objects = state.getMemoryModel().getSmg().getObjects();
      // All should be invalid except our SLL here
      SMGDoublyLinkedListSegment dll = null;
      for (SMGObject object : objects) {
        if (object instanceof SMGDoublyLinkedListSegment
            && state.getMemoryModel().isObjectValid(object)) {
          Preconditions.checkArgument(dll == null);
          dll = (SMGDoublyLinkedListSegment) object;
        } else {
          assertThat(!state.getMemoryModel().isObjectValid(object)).isTrue();
        }
      }
      assertThat(dll.getMinLength() == i).isTrue();
      assertThat(dll.getNextOffset().compareTo(nfo) == 0).isTrue();
      assertThat(dll.getPrevOffset().compareTo(pfo) == 0).isTrue();
      assertThat(dll.getSize().compareTo(sizeInBits) == 0).isTrue();
      assertThat(state.readSMGValue(dll, pfo, pointerSizeInBits).getSMGValue().isZero()).isTrue();
      assertThat(state.readSMGValue(dll, nfo, pointerSizeInBits).getSMGValue().isZero()).isTrue();
    }
  }

  // Test the minimum length needed for abstraction
  public void abstractDLLLimitTest() throws InvalidConfigurationException, SMG2Exception {
    BigInteger nfo = pointerSizeInBits;
    BigInteger pfo = pointerSizeInBits.add(pointerSizeInBits);
    BigInteger sizeInBits = pointerSizeInBits.add(pointerSizeInBits).add(pointerSizeInBits);

    for (int minLength = 3; minLength < 100; minLength++) {
      for (int i = 1; i < 100; i++) {
        resetSMGStateAndVisitor();
        SMGState state = createXLongExplicitDLLOnHeap(i);
        SMGCPAAbstractionManager absFinder = new SMGCPAAbstractionManager(state, minLength);
        ImmutableList<SMGCandidate> candidates = absFinder.getRefinedLinkedCandidates();
        if (i < minLength) {
          assertThat(candidates.size() == 0).isTrue();
          continue;
        }
        SMGCandidate firstObj = candidates.iterator().next();
        assertThat(firstObj.getSuspectedNfo()).isEquivalentAccordingToCompareTo(nfo);
        state = state.abstractIntoDLL(firstObj.getObject(), nfo, pfo);

        Set<SMGObject> objects = state.getMemoryModel().getSmg().getObjects();
        // All should be invalid except our SLL here
        SMGDoublyLinkedListSegment dll = null;
        for (SMGObject object : objects) {
          if (object instanceof SMGDoublyLinkedListSegment
              && state.getMemoryModel().isObjectValid(object)) {
            Preconditions.checkArgument(dll == null);
            dll = (SMGDoublyLinkedListSegment) object;
          } else {
            assertThat(!state.getMemoryModel().isObjectValid(object)).isTrue();
          }
        }
        assertThat(dll.getMinLength() == i).isTrue();
        assertThat(dll.getNextOffset().compareTo(nfo) == 0).isTrue();
        assertThat(dll.getPrevOffset().compareTo(pfo) == 0).isTrue();
        assertThat(dll.getSize().compareTo(sizeInBits) == 0).isTrue();
        assertThat(state.readSMGValue(dll, pfo, pointerSizeInBits).getSMGValue().isZero()).isTrue();
        assertThat(state.readSMGValue(dll, nfo, pointerSizeInBits).getSMGValue().isZero()).isTrue();
      }
    }
  }

  // The next and prev pointers at the end point to 0. The nfo is offset 32, pfo 64.
  // value at offset 0 is 1
  private SMGState createXLongExplicitDLLOnHeap(int length) throws SMG2Exception {
    BigInteger sizeInBits = pointerSizeInBits.add(pointerSizeInBits).add(pointerSizeInBits);
    BigInteger hfo = BigInteger.ZERO;
    BigInteger nfo = hfo.add(pointerSizeInBits);
    BigInteger pfo = nfo.add(pointerSizeInBits);
    BigInteger offset = BigInteger.ZERO;

    SMGDoublyLinkedListSegment currentAbstraction =
        new SMGDoublyLinkedListSegment(0, sizeInBits, offset, hfo, nfo, pfo, length);

    currentState = currentState.copyAndAddObjectToHeap(currentAbstraction);
    currentState =
        currentState.writeValueTo(
            currentAbstraction, hfo, pointerSizeInBits, new NumericValue(1), null);
    currentState =
        currentState.writeValueTo(
            currentAbstraction, nfo, pointerSizeInBits, new NumericValue(0), null);
    currentState =
        currentState.writeValueTo(
            currentAbstraction, pfo, pointerSizeInBits, new NumericValue(0), null);
    // Pointer to the abstracted list
    Value pointer = SymbolicValueFactory.getInstance().newIdentifier(null);
    currentState = currentState.createAndAddPointer(pointer, currentAbstraction, BigInteger.ZERO);
    SMGValue pointerToFirst =
        currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow();

    // The result value is simply the value pointer to the first concrete element
    for (int i = 0; i < length; i++) {
      SMGValueAndSMGState resultValueAndState =
          materializer.handleMaterilisation(pointerToFirst, currentAbstraction, currentState);
      currentState = resultValueAndState.getSMGState();
      if (i + 1 == length) {
        break;
      }
      Value currentPointer =
          currentState
              .getMemoryModel()
              .getValueFromSMGValue(resultValueAndState.getSMGValue())
              .orElseThrow();
      SMGStateAndOptionalSMGObjectAndOffset targetAndOffset =
          resultValueAndState
              .getSMGState()
              .dereferencePointerWithoutMaterilization(currentPointer)
              .orElseThrow();
      Preconditions.checkArgument(
          targetAndOffset.getOffsetForObject().compareTo(BigInteger.ZERO) == 0);

      SMGObject newObj = targetAndOffset.getSMGObject();
      SMGValueAndSMGState nextPointerAndState =
          currentState.readSMGValue(newObj, pointerSizeInBits, pointerSizeInBits);
      currentState = nextPointerAndState.getSMGState();
      pointerToFirst = nextPointerAndState.getSMGValue();

      SMGStateAndOptionalSMGObjectAndOffset targetToNextAndOffset =
          resultValueAndState
              .getSMGState()
              .dereferencePointerWithoutMaterilization(
                  currentState.getMemoryModel().getValueFromSMGValue(pointerToFirst).orElseThrow())
              .orElseThrow();
      Preconditions.checkArgument(
          targetToNextAndOffset.getSMGObject() instanceof SMGDoublyLinkedListSegment);
      currentAbstraction = (SMGDoublyLinkedListSegment) targetToNextAndOffset.getSMGObject();
    }
    return currentState;
  }

  // The next pointer at the end points to 0. nfo offset 32. value at 0 is 1.
  private SMGState createXLongExplicitSLLOnHeap(int length) throws SMG2Exception {
    BigInteger sizeInBits = pointerSizeInBits.add(pointerSizeInBits);
    BigInteger hfo = BigInteger.ZERO;
    BigInteger nfo = hfo.add(pointerSizeInBits);
    BigInteger offset = BigInteger.ZERO;

    SMGSinglyLinkedListSegment currentAbstraction =
        new SMGSinglyLinkedListSegment(0, sizeInBits, offset, hfo, nfo, length);

    currentState = currentState.copyAndAddObjectToHeap(currentAbstraction);
    currentState =
        currentState.writeValueTo(
            currentAbstraction, hfo, pointerSizeInBits, new NumericValue(1), null);
    currentState =
        currentState.writeValueTo(
            currentAbstraction, nfo, pointerSizeInBits, new NumericValue(0), null);
    // Pointer to the abstracted list
    Value pointer = SymbolicValueFactory.getInstance().newIdentifier(null);
    currentState = currentState.createAndAddPointer(pointer, currentAbstraction, BigInteger.ZERO);
    SMGValue pointerToFirst =
        currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow();

    // The result value is simply the value pointer to the first concrete element
    for (int i = 0; i < length; i++) {
      SMGValueAndSMGState resultValueAndState =
          materializer.handleMaterilisation(pointerToFirst, currentAbstraction, currentState);
      currentState = resultValueAndState.getSMGState();
      if (i + 1 == length) {
        break;
      }
      Value currentPointer =
          currentState
              .getMemoryModel()
              .getValueFromSMGValue(resultValueAndState.getSMGValue())
              .orElseThrow();
      SMGStateAndOptionalSMGObjectAndOffset targetAndOffset =
          resultValueAndState
              .getSMGState()
              .dereferencePointerWithoutMaterilization(currentPointer)
              .orElseThrow();
      Preconditions.checkArgument(
          targetAndOffset.getOffsetForObject().compareTo(BigInteger.ZERO) == 0);

      SMGObject newObj = targetAndOffset.getSMGObject();
      SMGValueAndSMGState nextPointerAndState =
          currentState.readSMGValue(newObj, pointerSizeInBits, pointerSizeInBits);
      currentState = nextPointerAndState.getSMGState();
      pointerToFirst = nextPointerAndState.getSMGValue();

      SMGStateAndOptionalSMGObjectAndOffset targetToNextAndOffset =
          resultValueAndState
              .getSMGState()
              .dereferencePointerWithoutMaterilization(
                  currentState.getMemoryModel().getValueFromSMGValue(pointerToFirst).orElseThrow())
              .orElseThrow();
      Preconditions.checkArgument(
          targetToNextAndOffset.getSMGObject() instanceof SMGSinglyLinkedListSegment);
      currentAbstraction = (SMGSinglyLinkedListSegment) targetToNextAndOffset.getSMGObject();
    }
    return currentState;
  }
}
