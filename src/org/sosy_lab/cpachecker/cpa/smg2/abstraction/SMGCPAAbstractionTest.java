// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.abstraction;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.List;
import java.util.Map.Entry;
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
import org.sosy_lab.cpachecker.cpa.smg2.util.value.ValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.smg.graph.SMGDoublyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGSinglyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

/*
 * We always assume lists to be head, nfo, pfo each pointer sized.
 */
public class SMGCPAAbstractionTest {

  private MachineModel machineModel;
  // Pointer size for the machine model in bits
  private BigInteger pointerSizeInBits;

  private LogManagerWithoutDuplicates logger;
  private SMGState currentState;
  private SMGCPAMaterializer materializer;

  private BigInteger sllSize;
  private BigInteger dllSize;

  private BigInteger hfo = BigInteger.ZERO;
  private BigInteger nfo;
  private BigInteger pfo;

  // Keep this above ~10 for the tests. Reduce if this class is slow.
  private static final int TEST_LIST_LENGTH = 50;

  // The visitor should always use the currentState!
  @Before
  public void init() throws InvalidConfigurationException {
    // We always assume lists to be head, nfo, pfo each pointer sized.
    machineModel = MachineModel.LINUX32;
    pointerSizeInBits =
        BigInteger.valueOf(
            machineModel.getSizeof(machineModel.getPointerEquivalentSimpleType()) * 8L);
    sllSize = pointerSizeInBits.multiply(BigInteger.TWO);
    dllSize = pointerSizeInBits.multiply(BigInteger.valueOf(3));
    nfo = hfo.add(pointerSizeInBits);
    pfo = nfo.add(pointerSizeInBits);
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

  /**
   * Tests that pointers are correctly nested in SLL segments and dereferencing them correctly
   * materializes the list up to that memory, and all pointers are still valid, correctly nested and
   * point to the correct segments.
   */
  @Test
  public void correctPointerNestingSLLTest() throws InvalidConfigurationException {
    // int sizeOfList = 10;
    resetSMGStateAndVisitor();
    // Value[] pointers = buildConcreteList(false, sllSize, sizeOfList);
    // SMGCPAAbstractionManager absFinder = new SMGCPAAbstractionManager(currentState, sizeOfList);
    // currentState = absFinder.findAndAbstractLists();
    // Now we have a 10+SLS
    // Deref a pointer not in the beginning or end, check that the list is consistent with the
    // pointers and the nesting level and materialization is correct afterwards
    // TODO:
  }

  /**
   * Tests behaviour for the read of a pointer pointing to a 0+ list segment for SLLs. Should split
   * into 2 states, one where the 0+ is deleted and the end of the list nfo value is read, one for
   * the extension of the list.
   *
   * @throws SMG2Exception not thrown
   * @throws InvalidConfigurationException not thrown
   */
  @Test
  public void zeroPlusRemovalSLLTest() throws SMG2Exception, InvalidConfigurationException {
    // We use a small length, does not matter at all
    int sizeOfList = 3;
    resetSMGStateAndVisitor();
    Value[] pointers = buildConcreteList(false, sllSize, sizeOfList);
    SMGCPAAbstractionManager absFinder = new SMGCPAAbstractionManager(currentState, sizeOfList);
    // Now we have a 3+SLS
    currentState = absFinder.findAndAbstractLists();
    // Materialize the complete list; We should have 3 concrete objects again and a fourth 0+
    List<SMGStateAndOptionalSMGObjectAndOffset> statesAndResultingObjects =
        currentState.dereferencePointer(pointers[sizeOfList - 1]);
    assertThat(statesAndResultingObjects.size() == 1).isTrue();
    SMGStateAndOptionalSMGObjectAndOffset stateAndResultingObject =
        statesAndResultingObjects.get(0);
    currentState = stateAndResultingObject.getSMGState();
    SMGObject lastConcreteListObject = stateAndResultingObject.getSMGObject();
    assertThat(stateAndResultingObject.getOffsetForObject())
        .isEquivalentAccordingToCompareTo(BigInteger.ZERO);
    // Note: this read is a non-materializing read! Using any of the values (pointers) for a 0+ is
    // therefore invalid.
    checkNextPointsToZeroPlus(lastConcreteListObject, false);

    // Now read the next pointer to the 0+ object; this results in 2 states, one where the 0+
    // vanishes and one where we generate a new concrete list element and keep our 0+
    checkZeroPlusBehaviour(false, lastConcreteListObject, BigInteger.ZERO);
  }

  /**
   * Tests behaviour for the read of a pointer pointing to a 0+ list segment for DLLs. Should split
   * into 2 states, one where the 0+ is deleted and the end of the list nfo value is read, one for
   * the extension of the list.
   *
   * @throws SMG2Exception not thrown
   * @throws InvalidConfigurationException not thrown
   */
  @Test
  public void zeroPlusRemovalDLLTest() throws SMG2Exception, InvalidConfigurationException {
    // We use a small length, does not matter at all
    int sizeOfList = 3;
    resetSMGStateAndVisitor();
    Value[] pointers = buildConcreteList(true, dllSize, sizeOfList);
    SMGCPAAbstractionManager absFinder = new SMGCPAAbstractionManager(currentState, sizeOfList);
    // Now we have a 3+DLS
    currentState = absFinder.findAndAbstractLists();
    // Materialize the complete list; We should have 3 concrete objects again and a fourth 0+
    List<SMGStateAndOptionalSMGObjectAndOffset> statesAndResultingObjects =
        currentState.dereferencePointer(pointers[sizeOfList - 1]);
    assertThat(statesAndResultingObjects.size() == 1).isTrue();
    SMGStateAndOptionalSMGObjectAndOffset stateAndResultingObject =
        statesAndResultingObjects.get(0);
    currentState = stateAndResultingObject.getSMGState();
    SMGObject lastConcreteListObject = stateAndResultingObject.getSMGObject();
    assertThat(stateAndResultingObject.getOffsetForObject())
        .isEquivalentAccordingToCompareTo(BigInteger.ZERO);
    // Note: this read is a non-materializing read! Using any of the values (pointers) for a 0+ is
    // therefore invalid.
    checkNextPointsToZeroPlus(lastConcreteListObject, true);

    // Now read the next pointer to the 0+ object; this results in 2 states, one where the 0+
    // vanishes and one where we generate a new concrete list element and keep our 0+
    checkZeroPlusBehaviour(true, lastConcreteListObject, BigInteger.ZERO);
  }

  /**
   * Checks with a non dereferencing and not materializing read that nfo points to a 0+ segment. For
   * DLLs it confirms that the 0+ has a pfo that points to the concrete segment.
   *
   * @param lastConcreteListObject the last concrete segment who's nfo points to 0+.
   */
  private void checkNextPointsToZeroPlus(SMGObject lastConcreteListObject, boolean dll) {
    SMGValueAndSMGState readValueAndState =
        currentState.readSMGValue(lastConcreteListObject, nfo, pointerSizeInBits);
    currentState = readValueAndState.getSMGState();
    SMGValue nextPointerValue = readValueAndState.getSMGValue();
    // Confirm that this is a pointer to a 0+ SLS
    assertThat(currentState.getMemoryModel().getSmg().isPointer(nextPointerValue)).isTrue();
    SMGPointsToEdge pointsToEdge =
        currentState.getMemoryModel().getSmg().getPTEdge(nextPointerValue).orElseThrow();
    assertThat(pointsToEdge.pointsTo() instanceof SMGSinglyLinkedListSegment).isTrue();
    assertThat(((SMGSinglyLinkedListSegment) pointsToEdge.pointsTo()).getMinLength()).isEqualTo(0);
    if (dll) {
      SMGValueAndSMGState readPfoValueAndState =
          currentState.readSMGValue(pointsToEdge.pointsTo(), pfo, pointerSizeInBits);
      currentState = readPfoValueAndState.getSMGState();
      SMGValue prevPointerValue = readPfoValueAndState.getSMGValue();
      // Confirm that this is a pointer to a 0+ SLS
      assertThat(currentState.getMemoryModel().getSmg().isPointer(prevPointerValue)).isTrue();
      SMGPointsToEdge pointsToPrevConcreteEdge =
          currentState.getMemoryModel().getSmg().getPTEdge(prevPointerValue).orElseThrow();
      assertThat(pointsToPrevConcreteEdge.pointsTo()).isEqualTo(lastConcreteListObject);
    }
  }

  /**
   * Checks that a proper read of the NFO pointing to 0+ generates 2 states. One with a value
   * expectedNfoValue, one with the extented list by 1.
   *
   * @param dll true for DLLs. False else.
   * @param lastConcreteListObject the last concrete segment who's nfo points to 0+.
   * @param expectedNfoValue the value for the non extented read state.
   * @throws SMG2Exception not thrown
   */
  private void checkZeroPlusBehaviour(
      boolean dll, SMGObject lastConcreteListObject, BigInteger expectedNfoValue)
      throws SMG2Exception {
    List<ValueAndSMGState> statesAndReadValueZeroPlus =
        currentState.readValue(lastConcreteListObject, nfo, pointerSizeInBits, null);
    assertThat(statesAndReadValueZeroPlus).hasSize(2);
    // The states are ordered, with the ending list being the first
    ValueAndSMGState firstReadValueAndState = statesAndReadValueZeroPlus.get(0);
    currentState = firstReadValueAndState.getState();
    assertThat(firstReadValueAndState.getValue().isNumericValue()).isTrue();
    assertThat(firstReadValueAndState.getValue().asNumericValue().bigInteger())
        .isEquivalentAccordingToCompareTo(expectedNfoValue);

    // The second is the one with a new concrete segment and another 0+
    // Check that its a concrete region with a valid pointer to it
    ValueAndSMGState secondReadValueAndState = statesAndReadValueZeroPlus.get(1);
    currentState = secondReadValueAndState.getState();
    assertThat(secondReadValueAndState.getValue().isNumericValue()).isFalse();
    SMGValue pointerValueFromZeroPlus =
        currentState
            .getMemoryModel()
            .getSMGValueFromValue(secondReadValueAndState.getValue())
            .orElseThrow();
    assertThat(currentState.getMemoryModel().getSmg().isPointer(pointerValueFromZeroPlus)).isTrue();
    SMGPointsToEdge pointsToAdditionalSegmentEdge =
        currentState.getMemoryModel().getSmg().getPTEdge(pointerValueFromZeroPlus).orElseThrow();
    assertThat(pointsToAdditionalSegmentEdge.pointsTo() instanceof SMGSinglyLinkedListSegment)
        .isFalse();
    assertThat(pointsToAdditionalSegmentEdge.targetSpecifier())
        .isEqualTo(SMGTargetSpecifier.IS_REGION);
    // Read the next pointer, check that it is a pointer to a 0+
    // Note: this read is a non-materializing read! Using any of the values (pointers) for a 0+ is
    // therefore invalid.
    SMGValueAndSMGState readNfoValueAndState =
        currentState.readSMGValue(pointsToAdditionalSegmentEdge.pointsTo(), nfo, pointerSizeInBits);
    currentState = readNfoValueAndState.getSMGState();
    SMGValue nextPointerValue = readNfoValueAndState.getSMGValue();
    // Confirm that this is a pointer to a 0+ SLS
    assertThat(currentState.getMemoryModel().getSmg().isPointer(nextPointerValue)).isTrue();
    SMGPointsToEdge pointsToNextConcreteEdge =
        currentState.getMemoryModel().getSmg().getPTEdge(nextPointerValue).orElseThrow();
    assertThat(pointsToNextConcreteEdge.pointsTo() instanceof SMGSinglyLinkedListSegment).isTrue();
    // assertThat(pointsToNextConcreteEdge.targetSpecifier()).isEqualTo(SMGTargetSpecifier.IS_FIRST_POINTER);
    assertThat(((SMGSinglyLinkedListSegment) pointsToNextConcreteEdge.pointsTo()).getMinLength())
        .isEqualTo(0);
    if (dll) {
      SMGValueAndSMGState readPfoValueAndState =
          currentState.readSMGValue(
              pointsToAdditionalSegmentEdge.pointsTo(), pfo, pointerSizeInBits);
      currentState = readPfoValueAndState.getSMGState();
      SMGValue prevPointerValue = readPfoValueAndState.getSMGValue();
      // Confirm that this is a pointer to a 0+ SLS
      assertThat(currentState.getMemoryModel().getSmg().isPointer(prevPointerValue)).isTrue();
      SMGPointsToEdge pointsToPrevConcreteEdge =
          currentState.getMemoryModel().getSmg().getPTEdge(prevPointerValue).orElseThrow();
      assertThat(pointsToPrevConcreteEdge.pointsTo()).isEqualTo(lastConcreteListObject);
    }
  }

  /*
   * Build a concrete list by hand and then use the abstraction algorithm on it and check the result.
   */
  @Test
  public void basicSLLFullAbstractionTest() throws SMG2Exception {
    Value[] pointers = buildConcreteList(false, sllSize, TEST_LIST_LENGTH);

    {
      SMGStateAndOptionalSMGObjectAndOffset stateAndObject =
          currentState.dereferencePointerWithoutMaterilization(pointers[0]).orElseThrow();
      currentState = stateAndObject.getSMGState();
      assertThat(stateAndObject.getSMGObject().isSLL()).isFalse();
      assertThat(stateAndObject.getSMGObject() instanceof SMGSinglyLinkedListSegment).isFalse();
    }

    SMGCPAAbstractionManager absFinder = new SMGCPAAbstractionManager(currentState, 3);
    currentState = absFinder.findAndAbstractLists();

    SMGStateAndOptionalSMGObjectAndOffset stateAndObjectAfterAbstraction =
        currentState.dereferencePointerWithoutMaterilization(pointers[0]).orElseThrow();
    currentState = stateAndObjectAfterAbstraction.getSMGState();
    assertThat(stateAndObjectAfterAbstraction.getSMGObject().isSLL()).isTrue();
    assertThat(
            ((SMGSinglyLinkedListSegment) stateAndObjectAfterAbstraction.getSMGObject())
                    .getMinLength()
                == TEST_LIST_LENGTH)
        .isTrue();
    assertThat(
            currentState
                .readSMGValue(
                    stateAndObjectAfterAbstraction.getSMGObject(),
                    pointerSizeInBits,
                    pointerSizeInBits)
                .getSMGValue()
                .isZero())
        .isTrue();
    assertThat(stateAndObjectAfterAbstraction.getSMGObject().isSLL()).isTrue();
    assertThat(stateAndObjectAfterAbstraction.getOffsetForObject().compareTo(BigInteger.ZERO) == 0)
        .isTrue();
    // There should be exactly listSize normal SMGObjects that are invalid (not zero objects)
    // + listSize - 1 SLL objects that are invalid and the 0 object invalid
    assertThat(currentState.getMemoryModel().getSmg().getObjects())
        .hasSize(1 + TEST_LIST_LENGTH + TEST_LIST_LENGTH - 1);
    int normalObjectCounter = 0;
    Boolean[] found = new Boolean[TEST_LIST_LENGTH - 1];
    for (SMGObject object : currentState.getMemoryModel().getSmg().getObjects()) {
      if (object.isZero()) {
        continue;
      } else if (!(object instanceof SMGSinglyLinkedListSegment)) {
        normalObjectCounter++;
        assertThat(currentState.getMemoryModel().getSmg().isValid(object)).isFalse();
      } else {
        assertThat(found[((SMGSinglyLinkedListSegment) object).getMinLength() - 2]).isNull();
        // We always start with at least element 2+
        found[((SMGSinglyLinkedListSegment) object).getMinLength() - 2] = true;
        if (((SMGSinglyLinkedListSegment) object).getMinLength() == TEST_LIST_LENGTH) {
          assertThat(currentState.getMemoryModel().getSmg().isValid(object)).isTrue();
        } else {
          assertThat(currentState.getMemoryModel().getSmg().isValid(object)).isFalse();
        }
      }
    }
    assertThat(normalObjectCounter).isEqualTo(TEST_LIST_LENGTH);
    for (boolean f : found) {
      assertThat(f).isTrue();
    }

    // Also only 2 heap objects known, the SLL and the 0 object
    assertThat(currentState.getMemoryModel().getHeapObjects()).hasSize(2);
    for (SMGObject heapObj : currentState.getMemoryModel().getHeapObjects()) {
      assertThat(
              heapObj.isZero()
                  || ((heapObj instanceof SMGSinglyLinkedListSegment)
                      && ((SMGSinglyLinkedListSegment) heapObj).getMinLength() == TEST_LIST_LENGTH))
          .isTrue();
    }
  }

  /*
   * Build a concrete list by hand and then use the abstraction algorithm on it and check the result.
   */
  @Test
  public void basicDLLFullAbstractionTest() throws SMG2Exception {
    int listSize = 100;
    Value[] pointers =
        buildConcreteList(true, pointerSizeInBits.multiply(BigInteger.valueOf(3)), listSize);

    {
      SMGStateAndOptionalSMGObjectAndOffset stateAndObject =
          currentState.dereferencePointerWithoutMaterilization(pointers[0]).orElseThrow();
      currentState = stateAndObject.getSMGState();
      assertThat(stateAndObject.getSMGObject().isSLL()).isFalse();
      assertThat(stateAndObject.getSMGObject() instanceof SMGSinglyLinkedListSegment).isFalse();
    }

    SMGCPAAbstractionManager absFinder = new SMGCPAAbstractionManager(currentState, 3);
    currentState = absFinder.findAndAbstractLists();

    SMGStateAndOptionalSMGObjectAndOffset stateAndObjectAfterAbstraction =
        currentState.dereferencePointerWithoutMaterilization(pointers[0]).orElseThrow();
    currentState = stateAndObjectAfterAbstraction.getSMGState();
    assertThat(stateAndObjectAfterAbstraction.getSMGObject().isSLL()).isFalse();
    assertThat(stateAndObjectAfterAbstraction.getSMGObject() instanceof SMGDoublyLinkedListSegment)
        .isTrue();
    assertThat(
            ((SMGDoublyLinkedListSegment) stateAndObjectAfterAbstraction.getSMGObject())
                    .getMinLength()
                == listSize)
        .isTrue();

    assertThat(
            currentState
                .readSMGValue(
                    stateAndObjectAfterAbstraction.getSMGObject(),
                    pointerSizeInBits,
                    pointerSizeInBits)
                .getSMGValue()
                .isZero())
        .isTrue();
    assertThat(stateAndObjectAfterAbstraction.getSMGObject().isSLL()).isFalse();
    assertThat(stateAndObjectAfterAbstraction.getOffsetForObject().compareTo(BigInteger.ZERO) == 0)
        .isTrue();

    assertThat(
            currentState
                .readSMGValue(
                    stateAndObjectAfterAbstraction.getSMGObject(),
                    pointerSizeInBits.add(pointerSizeInBits),
                    pointerSizeInBits)
                .getSMGValue()
                .isZero())
        .isTrue();
  }

  /*
   * Build a concrete list by hand that has pointers from the outside on it and then use the abstraction algorithm on it and check the result.
   */
  @Test
  public void basicSLLFullAbstractionWithExternalPointerTest() throws SMG2Exception {
    Value[] pointers =
        buildConcreteList(
            false, pointerSizeInBits.multiply(BigInteger.valueOf(2)), TEST_LIST_LENGTH);

    {
      SMGStateAndOptionalSMGObjectAndOffset stateAndObject =
          currentState.dereferencePointerWithoutMaterilization(pointers[0]).orElseThrow();
      currentState = stateAndObject.getSMGState();
      assertThat(stateAndObject.getSMGObject().isSLL()).isFalse();
      assertThat(stateAndObject.getSMGObject() instanceof SMGSinglyLinkedListSegment).isFalse();
    }

    SMGCPAAbstractionManager absFinder = new SMGCPAAbstractionManager(currentState, 3);
    currentState = absFinder.findAndAbstractLists();

    SMGStateAndOptionalSMGObjectAndOffset stateAndObjectAfterAbstraction =
        currentState.dereferencePointerWithoutMaterilization(pointers[0]).orElseThrow();
    currentState = stateAndObjectAfterAbstraction.getSMGState();
    assertThat(stateAndObjectAfterAbstraction.getSMGObject().isSLL()).isTrue();
    assertThat(
            ((SMGSinglyLinkedListSegment) stateAndObjectAfterAbstraction.getSMGObject())
                    .getMinLength()
                == TEST_LIST_LENGTH)
        .isTrue();
    assertThat(
            currentState
                .readSMGValue(
                    stateAndObjectAfterAbstraction.getSMGObject(),
                    pointerSizeInBits,
                    pointerSizeInBits)
                .getSMGValue()
                .isZero())
        .isTrue();
    assertThat(stateAndObjectAfterAbstraction.getSMGObject().isSLL()).isTrue();
    assertThat(stateAndObjectAfterAbstraction.getOffsetForObject().compareTo(BigInteger.ZERO) == 0)
        .isTrue();

    int level = TEST_LIST_LENGTH - 1;
    for (Value pointer : pointers) {
      Optional<SMGStateAndOptionalSMGObjectAndOffset> maybeTarget =
          currentState.dereferencePointerWithoutMaterilization(pointer);
      assertThat(maybeTarget.isPresent()).isTrue();
      SMGStateAndOptionalSMGObjectAndOffset targetAndState = maybeTarget.orElseThrow();
      assertThat(
              targetAndState.getSMGObject().equals(stateAndObjectAfterAbstraction.getSMGObject()))
          .isTrue();
      SMGValue smgValue = currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow();
      assertThat(currentState.getMemoryModel().getSmg().getPTEdge(smgValue).isPresent()).isTrue();

      for (SMGValue pteMapping :
          currentState.getMemoryModel().getSmg().getPTEdgeMapping().keySet()) {
        if (pteMapping.equals(smgValue)) {
          assertThat(pteMapping.getNestingLevel()).isEqualTo(level);
        }
      }
      level--;
    }
  }

  /*
   * Build a concrete list by hand that has pointers from the outside on it and then use the abstraction algorithm on it and check the result.
   */
  @Test
  public void basicDLLFullAbstractionWithExternalPointerTest() throws SMG2Exception {
    // Min abstraction length before the list is abstracted
    int minAbstractionLength = 3;
    Value[] pointers = buildConcreteList(true, dllSize, TEST_LIST_LENGTH);

    {
      SMGStateAndOptionalSMGObjectAndOffset stateAndObject =
          currentState.dereferencePointerWithoutMaterilization(pointers[0]).orElseThrow();
      currentState = stateAndObject.getSMGState();
      assertThat(stateAndObject.getSMGObject().isSLL()).isFalse();
      assertThat(stateAndObject.getSMGObject() instanceof SMGSinglyLinkedListSegment).isFalse();
    }

    SMGCPAAbstractionManager absFinder =
        new SMGCPAAbstractionManager(currentState, minAbstractionLength);
    currentState = absFinder.findAndAbstractLists();

    SMGStateAndOptionalSMGObjectAndOffset stateAndObjectAfterAbstraction =
        currentState.dereferencePointerWithoutMaterilization(pointers[0]).orElseThrow();
    currentState = stateAndObjectAfterAbstraction.getSMGState();
    assertThat(stateAndObjectAfterAbstraction.getSMGObject().isSLL()).isFalse();
    assertThat(stateAndObjectAfterAbstraction.getSMGObject() instanceof SMGDoublyLinkedListSegment)
        .isTrue();
    assertThat(
            ((SMGDoublyLinkedListSegment) stateAndObjectAfterAbstraction.getSMGObject())
                    .getMinLength()
                == TEST_LIST_LENGTH)
        .isTrue();

    assertThat(
            currentState
                .readSMGValue(
                    stateAndObjectAfterAbstraction.getSMGObject(),
                    pointerSizeInBits,
                    pointerSizeInBits)
                .getSMGValue()
                .isZero())
        .isTrue();
    assertThat(stateAndObjectAfterAbstraction.getSMGObject().isSLL()).isFalse();
    assertThat(stateAndObjectAfterAbstraction.getOffsetForObject().compareTo(BigInteger.ZERO) == 0)
        .isTrue();

    assertThat(
            currentState
                .readSMGValue(
                    stateAndObjectAfterAbstraction.getSMGObject(),
                    pointerSizeInBits.add(pointerSizeInBits),
                    pointerSizeInBits)
                .getSMGValue()
                .isZero())
        .isTrue();

    int level = TEST_LIST_LENGTH - 1;
    for (Value pointer : pointers) {
      Optional<SMGStateAndOptionalSMGObjectAndOffset> maybeTarget =
          currentState.dereferencePointerWithoutMaterilization(pointer);
      assertThat(maybeTarget.isPresent()).isTrue();
      SMGStateAndOptionalSMGObjectAndOffset targetAndState = maybeTarget.orElseThrow();
      assertThat(
              targetAndState.getSMGObject().equals(stateAndObjectAfterAbstraction.getSMGObject()))
          .isTrue();
      SMGValue smgValue = currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow();
      assertThat(currentState.getMemoryModel().getSmg().getPTEdge(smgValue).isPresent()).isTrue();

      for (SMGValue pteMapping :
          currentState.getMemoryModel().getSmg().getPTEdgeMapping().keySet()) {
        if (pteMapping.equals(smgValue)) {
          assertThat(pteMapping.getNestingLevel()).isEqualTo(level);
        }
      }
      level--;
    }
  }

  /*
   * Build a concrete list by hand that has pointers from the outside on it and then use the abstraction algorithm on it and check the result.
   * Then materialize the list back and check every pointer.
   */
  @Test
  public void basicDLLFullAbstractionWithExternalPointerMaterializationTest() throws SMG2Exception {
    int listSize = 100;
    Value[] pointers =
        buildConcreteList(true, pointerSizeInBits.multiply(BigInteger.valueOf(3)), listSize);

    {
      SMGStateAndOptionalSMGObjectAndOffset stateAndObject =
          currentState.dereferencePointerWithoutMaterilization(pointers[0]).orElseThrow();
      currentState = stateAndObject.getSMGState();
      assertThat(stateAndObject.getSMGObject().isSLL()).isFalse();
      assertThat(stateAndObject.getSMGObject() instanceof SMGSinglyLinkedListSegment).isFalse();
    }

    SMGCPAAbstractionManager absFinder = new SMGCPAAbstractionManager(currentState, 3);
    currentState = absFinder.findAndAbstractLists();

    for (int i = 0; i < listSize; i++) {
      SMGStateAndOptionalSMGObjectAndOffset returnedObjAndState =
          currentState.dereferencePointer(pointers[i]).get(0);
      currentState = returnedObjAndState.getSMGState();
      SMGObject newObj = returnedObjAndState.getSMGObject();
      assertThat(!(newObj instanceof SMGSinglyLinkedListSegment)).isTrue();
      assertThat(currentState.getMemoryModel().isObjectValid(newObj)).isTrue();
      // get(0) takes the list that is not extending for 0+
      ValueAndSMGState nextPointerAndState =
          currentState.readValue(newObj, pointerSizeInBits, pointerSizeInBits, null).get(0);
      currentState = nextPointerAndState.getState();
      for (Entry<SMGValue, SMGPointsToEdge> entry :
          currentState.getMemoryModel().getSmg().getPTEdgeMapping().entrySet()) {
        if (entry.getValue().pointsTo().equals(newObj)) {
          assertThat(entry.getKey().getNestingLevel()).isEqualTo(0);
        }
      }

      // Next pointer check equal pointers[i + 1]
      Value nextPointer = nextPointerAndState.getValue();
      assertThat(currentState.getMemoryModel().isPointer(nextPointer)).isTrue();
      if (i == listSize - 1) {
        assertThat(nextPointer.asNumericValue().bigInteger().compareTo(BigInteger.ZERO) == 0)
            .isTrue();
        // Check the nesting level
        // We only change the nesting level for the values mappings to pointers and in the objects
        // but not the mapping to Values
        for (Entry<SMGValue, SMGPointsToEdge> entry :
            currentState.getMemoryModel().getSmg().getPTEdgeMapping().entrySet()) {
          if (entry.getValue().pointsTo().equals(SMGObject.nullInstance())) {
            assertThat(entry.getKey().getNestingLevel()).isEqualTo(0);
          }
        }
      } else {
        Optional<SMGStateAndOptionalSMGObjectAndOffset> maybeNextObj =
            currentState.dereferencePointerWithoutMaterilization(nextPointer);
        assertThat(maybeNextObj.isPresent()).isTrue();
        SMGStateAndOptionalSMGObjectAndOffset nextObjAndState = maybeNextObj.orElseThrow();
        currentState = nextObjAndState.getSMGState();
        SMGObject nextObj = nextObjAndState.getSMGObject();
        assertThat((nextObj instanceof SMGDoublyLinkedListSegment)).isTrue();
        assertThat(currentState.getMemoryModel().isObjectValid(nextObj)).isTrue();
        assertThat(((SMGDoublyLinkedListSegment) nextObj).getMinLength())
            .isEqualTo(listSize - i - 1);
        // Check the nesting level
        // We only change the nesting level for the values mappings to pointers and in the objects
        // but not the mapping to Values
        for (Entry<SMGValue, SMGPointsToEdge> entry :
            currentState.getMemoryModel().getSmg().getPTEdgeMapping().entrySet()) {
          if (entry.getValue().pointsTo().equals(nextObj)) {
            assertThat(entry.getKey().getNestingLevel()).isLessThan(listSize - i - 1);
          }
        }

        // Now get the next obj from the next pointer in the array. It should be the same obj
        SMGStateAndOptionalSMGObjectAndOffset nextObjAndStateFromExternalPointer =
            currentState.dereferencePointerWithoutMaterilization(pointers[i + 1]).orElseThrow();
        currentState = nextObjAndStateFromExternalPointer.getSMGState();
        SMGObject newObjFromExternalPointer = nextObjAndStateFromExternalPointer.getSMGObject();
        assertThat((newObjFromExternalPointer instanceof SMGDoublyLinkedListSegment)).isTrue();
        assertThat(currentState.getMemoryModel().isObjectValid(newObjFromExternalPointer)).isTrue();
        assertThat(((SMGDoublyLinkedListSegment) newObjFromExternalPointer).getMinLength())
            .isEqualTo(listSize - (i + 1));
        // Check the nesting level
        // We only change the nesting level for the values mappings to pointers and in the objects
        // but not the mapping to Values
        for (Entry<SMGValue, SMGPointsToEdge> entry :
            currentState.getMemoryModel().getSmg().getPTEdgeMapping().entrySet()) {
          if (entry.getValue().pointsTo().equals(newObjFromExternalPointer)) {
            assertThat(entry.getKey().getNestingLevel()).isLessThan(listSize - i - 1);
          }
        }

        assertThat(nextObj).isEqualTo(newObjFromExternalPointer);
      }

      // Back pointer equals pointers [i - 1]
      // TODO: back pointer
    }
  }

  /*
   * Build a concrete list by hand that has pointers from the outside on it and then use the abstraction algorithm on it and check the result.
   * Then materialize the list back and check every pointer.
   */
  @Test
  public void basicSLLFullAbstractionWithExternalPointerMaterializationTest() throws SMG2Exception {
    Value[] pointers = buildConcreteList(false, sllSize, TEST_LIST_LENGTH);

    {
      SMGStateAndOptionalSMGObjectAndOffset stateAndObject =
          currentState.dereferencePointerWithoutMaterilization(pointers[0]).orElseThrow();
      currentState = stateAndObject.getSMGState();
      assertThat(stateAndObject.getSMGObject().isSLL()).isFalse();
      assertThat(stateAndObject.getSMGObject() instanceof SMGSinglyLinkedListSegment).isFalse();
    }

    SMGCPAAbstractionManager absFinder = new SMGCPAAbstractionManager(currentState, 3);
    currentState = absFinder.findAndAbstractLists();

    for (int i = 0; i < TEST_LIST_LENGTH; i++) {
      SMGStateAndOptionalSMGObjectAndOffset returnedObjAndState =
          currentState.dereferencePointer(pointers[i]).get(0);
      currentState = returnedObjAndState.getSMGState();
      SMGObject newObj = returnedObjAndState.getSMGObject();
      assertThat(!(newObj instanceof SMGSinglyLinkedListSegment)).isTrue();
      assertThat(currentState.getMemoryModel().isObjectValid(newObj)).isTrue();
      // get(0) takes the list that is not extending for 0+
      ValueAndSMGState nextPointerAndState =
          currentState.readValue(newObj, pointerSizeInBits, pointerSizeInBits, null).get(0);
      currentState = nextPointerAndState.getState();
      for (Entry<SMGValue, SMGPointsToEdge> entry :
          currentState.getMemoryModel().getSmg().getPTEdgeMapping().entrySet()) {
        if (entry.getValue().pointsTo().equals(newObj)) {
          assertThat(entry.getKey().getNestingLevel()).isEqualTo(0);
        }
      }

      // Next pointer check equal pointers[i + 1]
      Value nextPointer = nextPointerAndState.getValue();
      assertThat(currentState.getMemoryModel().isPointer(nextPointer)).isTrue();
      if (i == TEST_LIST_LENGTH - 1) {
        assertThat(nextPointer.asNumericValue().bigInteger().compareTo(BigInteger.ZERO) == 0)
            .isTrue();
        // Check the nesting level
        // We only change the nesting level for the values mappings to pointers and in the objects
        // but not the mapping to Values
        for (Entry<SMGValue, SMGPointsToEdge> entry :
            currentState.getMemoryModel().getSmg().getPTEdgeMapping().entrySet()) {
          if (entry.getValue().pointsTo().equals(SMGObject.nullInstance())) {
            assertThat(entry.getKey().getNestingLevel()).isEqualTo(0);
          }
        }
      } else {
        Optional<SMGStateAndOptionalSMGObjectAndOffset> maybeNextObj =
            currentState.dereferencePointerWithoutMaterilization(nextPointer);
        assertThat(maybeNextObj.isPresent()).isTrue();
        SMGStateAndOptionalSMGObjectAndOffset nextObjAndState = maybeNextObj.orElseThrow();
        currentState = nextObjAndState.getSMGState();
        SMGObject nextObj = nextObjAndState.getSMGObject();
        assertThat((nextObj instanceof SMGSinglyLinkedListSegment)).isTrue();
        assertThat(currentState.getMemoryModel().isObjectValid(nextObj)).isTrue();
        assertThat(((SMGSinglyLinkedListSegment) nextObj).getMinLength())
            .isEqualTo(TEST_LIST_LENGTH - i - 1);
        // Check the nesting level
        // We only change the nesting level for the values mappings to pointers and in the objects
        // but not the mapping to Values
        for (Entry<SMGValue, SMGPointsToEdge> entry :
            currentState.getMemoryModel().getSmg().getPTEdgeMapping().entrySet()) {
          if (entry.getValue().pointsTo().equals(nextObj)) {
            assertThat(entry.getKey().getNestingLevel()).isLessThan(TEST_LIST_LENGTH - i);
          }
        }

        // Now get the next obj from the next pointer in the array. It should be the same obj
        SMGStateAndOptionalSMGObjectAndOffset nextObjAndStateFromExternalPointer =
            currentState.dereferencePointerWithoutMaterilization(pointers[i + 1]).orElseThrow();
        currentState = nextObjAndStateFromExternalPointer.getSMGState();
        SMGObject newObjFromExternalPointer = nextObjAndStateFromExternalPointer.getSMGObject();
        assertThat((newObjFromExternalPointer instanceof SMGSinglyLinkedListSegment)).isTrue();
        assertThat(currentState.getMemoryModel().isObjectValid(newObjFromExternalPointer)).isTrue();
        assertThat(((SMGSinglyLinkedListSegment) newObjFromExternalPointer).getMinLength())
            .isEqualTo(TEST_LIST_LENGTH - (i + 1));
        // Check the nesting level
        // We only change the nesting level for the values mappings to pointers and in the objects
        // but not the mapping to Values
        for (Entry<SMGValue, SMGPointsToEdge> entry :
            currentState.getMemoryModel().getSmg().getPTEdgeMapping().entrySet()) {
          if (entry.getValue().pointsTo().equals(newObjFromExternalPointer)) {
            assertThat(entry.getKey().getNestingLevel()).isLessThan(TEST_LIST_LENGTH - i - 1);
          }
        }

        assertThat(nextObj).isEqualTo(newObjFromExternalPointer);
      }

      // Back pointer equals pointers [i - 1]
      // TODO: back pointer
    }
  }

  @Test
  public void basicDLLMaterializationTest() throws SMG2Exception {
    BigInteger offset = BigInteger.ZERO;

    SMGDoublyLinkedListSegment currentAbstraction =
        new SMGDoublyLinkedListSegment(0, dllSize, offset, hfo, nfo, pfo, TEST_LIST_LENGTH);

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
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState
                .getMemoryModel()
                .replaceSMGValueNestingLevel(
                    currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow(),
                    TEST_LIST_LENGTH - 1));
    SMGValue pointerToFirst =
        currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow();

    SMGObject[] previous = new SMGObject[TEST_LIST_LENGTH];
    // The result value is simply the value pointer to the first concrete element
    for (int i = 0; i < TEST_LIST_LENGTH; i++) {
      List<SMGValueAndSMGState> resultValuesAndStates =
          materializer.handleMaterilisation(pointerToFirst, currentAbstraction, currentState);
      SMGValueAndSMGState resultValueAndState;
      assertThat(resultValuesAndStates).hasSize(1);
      resultValueAndState = resultValuesAndStates.get(0);

      currentState = resultValueAndState.getSMGState();
      // i + 3 because we always have i + 1 objects as a concrete list + zero object + abstract list
      // (0+ in the end)
      assertThat(currentState.getMemoryModel().getHeapObjects().size() == i + 3).isTrue();
      Value currentPointer =
          currentState
              .getMemoryModel()
              .getValueFromSMGValue(resultValueAndState.getSMGValue())
              .orElseThrow();
      if (i == TEST_LIST_LENGTH - 1) {
        Optional<SMGStateAndOptionalSMGObjectAndOffset> object =
            currentState.dereferencePointerWithoutMaterilization(currentPointer);
        assertThat(object.isPresent()).isTrue();
        assertThat(object.orElseThrow().getSMGObject().getSize().compareTo(dllSize) == 0).isTrue();

        // get(0) takes the list that is not extending for 0+
        ValueAndSMGState nextPointer =
            currentState
                .readValue(object.orElseThrow().getSMGObject(), nfo, pointerSizeInBits, null)
                .get(0);
        assertThat(nextPointer.getValue().isNumericValue()).isTrue();
        assertThat(
                nextPointer.getValue().asNumericValue().bigInteger().compareTo(BigInteger.ZERO)
                    == 0)
            .isTrue();
        break;
      } else if (i == 0) {
        assertThat(currentPointer.equals(pointer)).isTrue();
      }
      SMGStateAndOptionalSMGObjectAndOffset targetAndOffset =
          resultValueAndState
              .getSMGState()
              .dereferencePointerWithoutMaterilization(currentPointer)
              .orElseThrow();
      assertThat(targetAndOffset.getOffsetForObject().compareTo(BigInteger.ZERO) == 0).isTrue();

      SMGObject newObj = targetAndOffset.getSMGObject();
      SMGValueAndSMGState headAndState =
          currentState.readSMGValue(newObj, BigInteger.ZERO, pointerSizeInBits);
      currentState = headAndState.getSMGState();
      assertThat(
              currentState
                  .getMemoryModel()
                  .getValueFromSMGValue(headAndState.getSMGValue())
                  .isPresent())
          .isTrue();
      assertThat(
              currentState
                  .getMemoryModel()
                  .getValueFromSMGValue(headAndState.getSMGValue())
                  .orElseThrow()
                  .isNumericValue())
          .isTrue();
      assertThat(
              currentState
                      .getMemoryModel()
                      .getValueFromSMGValue(headAndState.getSMGValue())
                      .orElseThrow()
                      .asNumericValue()
                      .bigInteger()
                      .compareTo(BigInteger.ONE)
                  == 0)
          .isTrue();
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
        assertThat(prevObjFromPrevPointerAndOffset.getSMGObject().isZero()).isTrue();
        previous[i] = prevObjAndOffset.getSMGObject();
      } else {
        previous[i] = prevObjAndOffset.getSMGObject();
        assertThat(previous[i - 1].equals(prevObjFromPrevPointerAndOffset.getSMGObject())).isTrue();
      }

      SMGStateAndOptionalSMGObjectAndOffset targetToNextAndOffset =
          resultValueAndState
              .getSMGState()
              .dereferencePointerWithoutMaterilization(
                  currentState.getMemoryModel().getValueFromSMGValue(pointerToFirst).orElseThrow())
              .orElseThrow();
      assertThat(targetToNextAndOffset.getSMGObject() instanceof SMGSinglyLinkedListSegment)
          .isTrue();
      currentAbstraction = (SMGDoublyLinkedListSegment) targetToNextAndOffset.getSMGObject();
    }
  }

  @Test
  public void basicSLLMaterializationTest() throws SMG2Exception {
    BigInteger offset = BigInteger.ZERO;

    SMGSinglyLinkedListSegment currentAbstraction =
        new SMGSinglyLinkedListSegment(0, sllSize, offset, hfo, nfo, TEST_LIST_LENGTH);

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
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState
                .getMemoryModel()
                .replaceSMGValueNestingLevel(
                    currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow(),
                    TEST_LIST_LENGTH - 1));
    SMGValue pointerToFirst =
        currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow();

    // The result value is simply the value pointer to the first concrete element
    for (int i = 0; i < TEST_LIST_LENGTH; i++) {
      List<SMGValueAndSMGState> resultValuesAndStates =
          materializer.handleMaterilisation(pointerToFirst, currentAbstraction, currentState);
      SMGValueAndSMGState resultValueAndState;

      assertThat(resultValuesAndStates).hasSize(1);
      resultValueAndState = resultValuesAndStates.get(0);

      currentState = resultValueAndState.getSMGState();
      // i + 3 because we always have i + 1 objects as a concrete list + zero object + abstract list
      // (0+ in the end)
      assertThat(currentState.getMemoryModel().getHeapObjects().size() == i + 3).isTrue();
      // currentPointer == pointer to just materilized list segment
      Value currentPointer =
          currentState
              .getMemoryModel()
              .getValueFromSMGValue(resultValueAndState.getSMGValue())
              .orElseThrow();
      if (i == TEST_LIST_LENGTH - 1) {
        Optional<SMGStateAndOptionalSMGObjectAndOffset> object =
            currentState.dereferencePointerWithoutMaterilization(currentPointer);
        assertThat(object.isPresent()).isTrue();
        assertThat(object.orElseThrow().getSMGObject().getSize().compareTo(sllSize) == 0).isTrue();

        // get(0) takes the list that is not extending for 0+
        ValueAndSMGState nextPointer =
            currentState
                .readValue(object.orElseThrow().getSMGObject(), nfo, pointerSizeInBits, null)
                .get(0);
        assertThat(nextPointer.getValue().isNumericValue()).isTrue();
        assertThat(
                nextPointer.getValue().asNumericValue().bigInteger().compareTo(BigInteger.ZERO)
                    == 0)
            .isTrue();
        break;
      } else if (i == 0) {
        assertThat(currentPointer.equals(pointer)).isTrue();
      }
      SMGStateAndOptionalSMGObjectAndOffset targetAndOffset =
          resultValueAndState
              .getSMGState()
              .dereferencePointerWithoutMaterilization(currentPointer)
              .orElseThrow();
      assertThat(targetAndOffset.getOffsetForObject().compareTo(BigInteger.ZERO) == 0).isTrue();

      SMGObject newObj = targetAndOffset.getSMGObject();
      SMGValueAndSMGState headAndState =
          currentState.readSMGValue(newObj, BigInteger.ZERO, pointerSizeInBits);
      currentState = headAndState.getSMGState();
      assertThat(
              currentState
                  .getMemoryModel()
                  .getValueFromSMGValue(headAndState.getSMGValue())
                  .isPresent())
          .isTrue();
      assertThat(
              currentState
                  .getMemoryModel()
                  .getValueFromSMGValue(headAndState.getSMGValue())
                  .orElseThrow()
                  .isNumericValue())
          .isTrue();
      assertThat(
              currentState
                      .getMemoryModel()
                      .getValueFromSMGValue(headAndState.getSMGValue())
                      .orElseThrow()
                      .asNumericValue()
                      .bigInteger()
                      .compareTo(BigInteger.ONE)
                  == 0)
          .isTrue();
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
      assertThat(targetToNextAndOffset.getSMGObject() instanceof SMGSinglyLinkedListSegment)
          .isTrue();
      currentAbstraction = (SMGSinglyLinkedListSegment) targetToNextAndOffset.getSMGObject();
    }
  }

  @Test
  public void basicSLLDetectionTest() throws SMG2Exception, InvalidConfigurationException {
    // Min abstraction length before the list is abstracted
    int minAbstractionLength = 3;
    for (int i = 1; i < TEST_LIST_LENGTH; i++) {
      resetSMGStateAndVisitor();
      SMGState state = createXLongExplicitSLLOnHeap(i);
      SMGCPAAbstractionManager absFinder =
          new SMGCPAAbstractionManager(state, minAbstractionLength);
      ImmutableList<SMGCandidate> candidates = absFinder.getRefinedLinkedCandidates();

      if (i > minAbstractionLength - 1) {
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
    // Min abstraction length before the list is abstracted
    int minAbstractionLength = 3;
    for (int i = 1; i < TEST_LIST_LENGTH; i++) {
      resetSMGStateAndVisitor();
      SMGState state = createXLongExplicitDLLOnHeap(i);
      SMGCPAAbstractionManager absFinder =
          new SMGCPAAbstractionManager(state, minAbstractionLength);
      ImmutableList<SMGCandidate> candidates = absFinder.getRefinedLinkedCandidates();
      if (i > minAbstractionLength - 1) {
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
    // Minimum abstraction length before a list is abstracted
    int minAbstractionLength = 3;

    for (int i = 1; i < TEST_LIST_LENGTH; i++) {
      resetSMGStateAndVisitor();
      SMGState state = createXLongExplicitSLLOnHeap(i);
      SMGCPAAbstractionManager absFinder =
          new SMGCPAAbstractionManager(state, minAbstractionLength);
      ImmutableList<SMGCandidate> candidates = absFinder.getRefinedLinkedCandidates();
      if (i < minAbstractionLength) {
        continue;
      }
      SMGCandidate firstObj = candidates.iterator().next();
      assertThat(firstObj.getSuspectedNfo()).isEquivalentAccordingToCompareTo(nfo);
      state = state.abstractIntoSLL(firstObj.getObject(), nfo, ImmutableSet.of());

      Set<SMGObject> objects = state.getMemoryModel().getSmg().getObjects();
      // All should be invalid except our SLL here
      SMGSinglyLinkedListSegment sll = null;
      for (SMGObject object : objects) {
        if (object instanceof SMGSinglyLinkedListSegment
            && state.getMemoryModel().isObjectValid(object)) {
          assertThat(sll == null).isTrue();
          sll = (SMGSinglyLinkedListSegment) object;
        } else {
          assertThat(!state.getMemoryModel().isObjectValid(object)).isTrue();
        }
      }
      assertThat(sll.getMinLength() == i).isTrue();
      assertThat(sll.getNextOffset().compareTo(nfo) == 0).isTrue();
      assertThat(sll.getSize().compareTo(sllSize) == 0).isTrue();
    }
  }

  @SuppressWarnings("null")
  @Test
  public void abstractDLLTest() throws InvalidConfigurationException, SMG2Exception {
    // Min abstraction length before the list is abstracted
    int minAbstractionLength = 3;
    for (int i = 1; i < TEST_LIST_LENGTH; i++) {
      resetSMGStateAndVisitor();
      SMGState state = createXLongExplicitDLLOnHeap(i);
      SMGCPAAbstractionManager absFinder =
          new SMGCPAAbstractionManager(state, minAbstractionLength);
      ImmutableList<SMGCandidate> candidates = absFinder.getRefinedLinkedCandidates();
      if (i < minAbstractionLength) {
        continue;
      }
      SMGCandidate firstObj = candidates.iterator().next();
      assertThat(firstObj.getSuspectedNfo()).isEquivalentAccordingToCompareTo(nfo);
      state = state.abstractIntoDLL(firstObj.getObject(), nfo, pfo, ImmutableSet.of());

      Set<SMGObject> objects = state.getMemoryModel().getSmg().getObjects();
      // All should be invalid except our SLL here
      SMGDoublyLinkedListSegment dll = null;
      for (SMGObject object : objects) {
        if (object instanceof SMGDoublyLinkedListSegment
            && state.getMemoryModel().isObjectValid(object)) {
          assertThat(dll == null).isTrue();
          dll = (SMGDoublyLinkedListSegment) object;
        } else {
          assertThat(!state.getMemoryModel().isObjectValid(object)).isTrue();
        }
      }
      assertThat(dll.getMinLength() == i).isTrue();
      assertThat(dll.getNextOffset().compareTo(nfo) == 0).isTrue();
      assertThat(dll.getPrevOffset().compareTo(pfo) == 0).isTrue();
      assertThat(dll.getSize().compareTo(dllSize) == 0).isTrue();
      assertThat(state.readSMGValue(dll, pfo, pointerSizeInBits).getSMGValue().isZero()).isTrue();
      assertThat(state.readSMGValue(dll, nfo, pointerSizeInBits).getSMGValue().isZero()).isTrue();
    }
  }

  // Test the minimum length needed for abstraction
  @SuppressWarnings("null")
  @Test
  public void abstractDLLLimitTest() throws InvalidConfigurationException, SMG2Exception {
    // Min abstraction length before the list is abstracted
    int minAbstractionLength = 3;

    for (int minLength = minAbstractionLength;
        minLength < TEST_LIST_LENGTH;
        minLength = minLength + 10) {
      for (int i = 1; i < TEST_LIST_LENGTH; i++) {
        resetSMGStateAndVisitor();
        SMGState state = createXLongExplicitDLLOnHeap(i);
        SMGCPAAbstractionManager absFinder = new SMGCPAAbstractionManager(state, minLength);
        ImmutableList<SMGCandidate> candidates = absFinder.getRefinedLinkedCandidates();
        if (i < minLength) {
          assertThat(candidates.isEmpty()).isTrue();
          continue;
        }
        SMGCandidate firstObj = candidates.iterator().next();
        assertThat(firstObj.getSuspectedNfo()).isEquivalentAccordingToCompareTo(nfo);
        state = state.abstractIntoDLL(firstObj.getObject(), nfo, pfo, ImmutableSet.of());

        Set<SMGObject> objects = state.getMemoryModel().getSmg().getObjects();
        // All should be invalid except our SLL here
        SMGDoublyLinkedListSegment dll = null;
        for (SMGObject object : objects) {
          if (object instanceof SMGDoublyLinkedListSegment
              && state.getMemoryModel().isObjectValid(object)) {
            assertThat(dll == null).isTrue();
            dll = (SMGDoublyLinkedListSegment) object;
          } else {
            assertThat(!state.getMemoryModel().isObjectValid(object)).isTrue();
          }
        }
        assertThat(dll.getMinLength() == i).isTrue();
        assertThat(dll.getNextOffset().compareTo(nfo) == 0).isTrue();
        assertThat(dll.getPrevOffset().compareTo(pfo) == 0).isTrue();
        assertThat(dll.getSize().compareTo(dllSize) == 0).isTrue();
        assertThat(state.readSMGValue(dll, pfo, pointerSizeInBits).getSMGValue().isZero()).isTrue();
        assertThat(state.readSMGValue(dll, nfo, pointerSizeInBits).getSMGValue().isZero()).isTrue();
      }
    }
  }

  // The next and prev pointers at the end point to 0. The nfo is offset 32, pfo 64.
  // value at offset 0 is 1
  private SMGState createXLongExplicitDLLOnHeap(int length) throws SMG2Exception {
    BigInteger offset = BigInteger.ZERO;

    SMGDoublyLinkedListSegment currentAbstraction =
        new SMGDoublyLinkedListSegment(0, dllSize, offset, hfo, nfo, pfo, length);

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
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState
                .getMemoryModel()
                .replaceSMGValueNestingLevel(
                    currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow(),
                    length - 1));
    SMGValue pointerToFirst =
        currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow();

    // The result value is simply the value pointer to the first concrete element
    for (int i = 0; i < length; i++) {
      List<SMGValueAndSMGState> resultValuesAndStates =
          materializer.handleMaterilisation(pointerToFirst, currentAbstraction, currentState);
      SMGValueAndSMGState resultValueAndState;
      if (i + 1 == TEST_LIST_LENGTH) {
        assertThat(resultValuesAndStates).hasSize(2);
        // 2 states, one for a extended list with 0+ and one without 0+ that is not extended
        // We don't check that in this test, but assume that the second item is the non extended
        // version
        // TODO: make this assumption concrete somehow
        resultValueAndState = resultValuesAndStates.get(1);
      } else {
        assertThat(resultValuesAndStates).hasSize(1);
        resultValueAndState = resultValuesAndStates.get(0);
      }
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
      assertThat(targetAndOffset.getOffsetForObject().compareTo(BigInteger.ZERO) == 0).isTrue();

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
      assertThat(targetToNextAndOffset.getSMGObject() instanceof SMGDoublyLinkedListSegment)
          .isTrue();
      currentAbstraction = (SMGDoublyLinkedListSegment) targetToNextAndOffset.getSMGObject();
    }
    return currentState;
  }

  // The next pointer at the end points to 0. nfo offset 32. value at 0 is 1.
  private SMGState createXLongExplicitSLLOnHeap(int length) throws SMG2Exception {
    BigInteger offset = BigInteger.ZERO;

    SMGSinglyLinkedListSegment currentAbstraction =
        new SMGSinglyLinkedListSegment(0, sllSize, offset, hfo, nfo, length);

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
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState
                .getMemoryModel()
                .replaceSMGValueNestingLevel(
                    currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow(),
                    length - 1));
    SMGValue pointerToFirst =
        currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow();

    // The result value is simply the value pointer to the first concrete element
    for (int i = 0; i < length; i++) {
      List<SMGValueAndSMGState> resultValuesAndStates =
          materializer.handleMaterilisation(pointerToFirst, currentAbstraction, currentState);
      SMGValueAndSMGState resultValueAndState;
      if (i + 1 == TEST_LIST_LENGTH) {
        assertThat(resultValuesAndStates).hasSize(2);
        // 2 states, one for a extended list with 0+ and one without 0+ that is not extended
        // We don't check that in this test, but assume that the second item is the non extended
        // version
        // TODO: make this assumption concrete somehow
        resultValueAndState = resultValuesAndStates.get(1);
      } else {
        assertThat(resultValuesAndStates).hasSize(1);
        resultValueAndState = resultValuesAndStates.get(0);
      }
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
      assertThat(targetAndOffset.getOffsetForObject().compareTo(BigInteger.ZERO) == 0).isTrue();

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
      assertThat(targetToNextAndOffset.getSMGObject() instanceof SMGSinglyLinkedListSegment)
          .isTrue();
      currentAbstraction = (SMGSinglyLinkedListSegment) targetToNextAndOffset.getSMGObject();
    }
    return currentState;
  }

  /*
   * Will fill the list with data such that the nfo (and pfo) are last. The data is int and the same every list segment.
   * The data is numeric starting from 0, +1 each new value such that the space until nfo is filled.
   * Valid sizes are divisible by 32. The nfo for the last and pfo for the first segment are 0.
   */
  private Value[] buildConcreteList(boolean dll, BigInteger sizeOfSegment, int listLength)
      throws SMG2Exception {
    Value[] pointerArray = new Value[listLength];
    SMGObject prevObject = null;

    for (int i = 0; i < listLength; i++) {
      SMGObject listSegment = SMGObject.of(0, sizeOfSegment, BigInteger.ZERO);
      currentState = currentState.copyAndAddObjectToHeap(listSegment);
      for (int j = 0; j < sizeOfSegment.intValueExact() % 32; j++) {
        currentState =
            currentState.writeValueTo(
                listSegment,
                BigInteger.valueOf(j).multiply(BigInteger.valueOf(32)),
                pointerSizeInBits,
                new NumericValue(j),
                null);
      }

      // Pointer to the next list segment (from the prev to this, except for the last)
      if (i == listLength - 1) {
        Value nextPointer = new NumericValue(0);
        currentState =
            currentState.writeValueTo(listSegment, nfo, pointerSizeInBits, nextPointer, null);
      }
      if (prevObject != null) {
        ValueAndSMGState pointerAndState =
            currentState.searchOrCreateAddress(listSegment, BigInteger.ZERO);
        currentState = pointerAndState.getState();
        currentState =
            currentState.writeValueTo(
                prevObject, nfo, pointerSizeInBits, pointerAndState.getValue(), null);
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
            currentState.writeValueTo(listSegment, pfo, pointerSizeInBits, prevPointer, null);
      }
      // Pointer to the list segment
      ValueAndSMGState pointerAndState =
          currentState.searchOrCreateAddress(listSegment, BigInteger.ZERO);
      pointerArray[i] = pointerAndState.getValue();
      currentState = pointerAndState.getState();

      prevObject = listSegment;
    }
    return pointerArray;
  }
}
