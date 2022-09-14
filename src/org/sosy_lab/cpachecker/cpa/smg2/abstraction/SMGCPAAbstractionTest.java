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
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import junit.framework.AssertionFailedError;
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

  /*
   * Build a concrete list by hand and then use the abstraction algorithm on it and check the result.
   */
  @Test
  public void basicSLLFullAbstractionTest() throws SMG2Exception {
    int listSize = 100;
    Value[] pointers =
        buildConcreteList(false, pointerSizeInBits.multiply(BigInteger.valueOf(2)), listSize);

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
    assertThat(stateAndObjectAfterAbstraction.getSMGObject().isSLL()).isTrue();
    assertThat(stateAndObjectAfterAbstraction.getOffsetForObject().compareTo(BigInteger.ZERO) == 0)
        .isTrue();
    // There should be exactly listSize normal SMGObjects that are invalid (not zero objects)
    // + listSize - 1 SLL objects that are invalid and the 0 object invalid
    assertThat(currentState.getMemoryModel().getSmg().getObjects())
        .hasSize(1 + listSize + listSize - 1);
    int normalObjectCounter = 0;
    Boolean[] found = new Boolean[listSize - 1];
    for (SMGObject object : currentState.getMemoryModel().getSmg().getObjects()) {
      if (object.isZero()) {
        continue;
      } else if (!(object instanceof SMGSinglyLinkedListSegment)) {
        normalObjectCounter++;
        assertThat(currentState.getMemoryModel().getSmg().isValid(object)).isFalse();
      } else if (object instanceof SMGSinglyLinkedListSegment) {
        assertThat(found[((SMGSinglyLinkedListSegment) object).getMinLength() - 2]).isNull();
        // We always start with at least element 2+
        found[((SMGSinglyLinkedListSegment) object).getMinLength() - 2] = true;
        if (((SMGSinglyLinkedListSegment) object).getMinLength() == listSize) {
          assertThat(currentState.getMemoryModel().getSmg().isValid(object)).isTrue();
        } else {
          assertThat(currentState.getMemoryModel().getSmg().isValid(object)).isFalse();
        }
      } else {
        // Not expected obj
        throw new AssertionFailedError();
      }
    }
    assertThat(normalObjectCounter).isEqualTo(listSize);
    for (boolean f : found) {
      assertThat(f).isTrue();
    }

    // Also only 2 heap objects known, the SLL and the 0 object
    assertThat(currentState.getMemoryModel().getHeapObjects()).hasSize(2);
    for (SMGObject heapObj : currentState.getMemoryModel().getHeapObjects()) {
      assertThat(
              heapObj.isZero()
                  || ((heapObj instanceof SMGSinglyLinkedListSegment)
                      && ((SMGSinglyLinkedListSegment) heapObj).getMinLength() == listSize))
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
    int listSize = 100;
    Value[] pointers =
        buildConcreteList(false, pointerSizeInBits.multiply(BigInteger.valueOf(2)), listSize);

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
    assertThat(stateAndObjectAfterAbstraction.getSMGObject().isSLL()).isTrue();
    assertThat(stateAndObjectAfterAbstraction.getOffsetForObject().compareTo(BigInteger.ZERO) == 0)
        .isTrue();

    int level = listSize - 1;
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

      for (Entry<SMGValue, SMGPointsToEdge> pteMapping :
          currentState.getMemoryModel().getSmg().getPTEdgeMapping().entrySet()) {
        if (pteMapping.getKey().equals(smgValue)) {
          assertThat(pteMapping.getKey().getNestingLevel()).isEqualTo(level);
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

    int level = listSize - 1;
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

      for (Entry<SMGValue, SMGPointsToEdge> pteMapping :
          currentState.getMemoryModel().getSmg().getPTEdgeMapping().entrySet()) {
        if (pteMapping.getKey().equals(smgValue)) {
          assertThat(pteMapping.getKey().getNestingLevel()).isEqualTo(level);
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
          currentState.dereferencePointer(pointers[i]);
      currentState = returnedObjAndState.getSMGState();
      SMGObject newObj = returnedObjAndState.getSMGObject();
      assertThat(!(newObj instanceof SMGSinglyLinkedListSegment)).isTrue();
      assertThat(currentState.getMemoryModel().isObjectValid(newObj)).isTrue();
      ValueAndSMGState nextPointerAndState =
          currentState.readValue(newObj, pointerSizeInBits, pointerSizeInBits, null);
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
    int listSize = 100;
    Value[] pointers =
        buildConcreteList(false, pointerSizeInBits.multiply(BigInteger.valueOf(2)), listSize);

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
          currentState.dereferencePointer(pointers[i]);
      currentState = returnedObjAndState.getSMGState();
      SMGObject newObj = returnedObjAndState.getSMGObject();
      assertThat(!(newObj instanceof SMGSinglyLinkedListSegment)).isTrue();
      assertThat(currentState.getMemoryModel().isObjectValid(newObj)).isTrue();
      ValueAndSMGState nextPointerAndState =
          currentState.readValue(newObj, pointerSizeInBits, pointerSizeInBits, null);
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
        assertThat((nextObj instanceof SMGSinglyLinkedListSegment)).isTrue();
        assertThat(currentState.getMemoryModel().isObjectValid(nextObj)).isTrue();
        assertThat(((SMGSinglyLinkedListSegment) nextObj).getMinLength())
            .isEqualTo(listSize - i - 1);
        // Check the nesting level
        // We only change the nesting level for the values mappings to pointers and in the objects
        // but not the mapping to Values
        for (Entry<SMGValue, SMGPointsToEdge> entry :
            currentState.getMemoryModel().getSmg().getPTEdgeMapping().entrySet()) {
          if (entry.getValue().pointsTo().equals(nextObj)) {
            assertThat(entry.getKey().getNestingLevel()).isLessThan(listSize - i);
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
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState
                .getMemoryModel()
                .replaceSMGValueNestingLevel(
                    currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow(),
                    size - 1));
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
        Optional<SMGStateAndOptionalSMGObjectAndOffset> object =
            currentState.dereferencePointerWithoutMaterilization(currentPointer);
        assertThat(object.isPresent()).isTrue();
        assertThat(object.orElseThrow().getSMGObject().getSize().compareTo(sizeInBits) == 0)
            .isTrue();

        ValueAndSMGState nextPointer =
            currentState.readValue(
                object.orElseThrow().getSMGObject(), nfo, pointerSizeInBits, null);
        Preconditions.checkArgument(nextPointer.getValue().isNumericValue());
        Preconditions.checkArgument(
            nextPointer.getValue().asNumericValue().bigInteger().compareTo(BigInteger.ZERO) == 0);
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
    currentState =
        currentState.copyAndReplaceMemoryModel(
            currentState
                .getMemoryModel()
                .replaceSMGValueNestingLevel(
                    currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow(),
                    size - 1));
    SMGValue pointerToFirst =
        currentState.getMemoryModel().getSMGValueFromValue(pointer).orElseThrow();

    // The result value is simply the value pointer to the first concrete element
    for (int i = 0; i < size; i++) {
      SMGValueAndSMGState resultValueAndState =
          materializer.handleMaterilisation(pointerToFirst, currentAbstraction, currentState);
      currentState = resultValueAndState.getSMGState();
      Preconditions.checkArgument(
          currentState.getMemoryModel().getHeapObjects().size() == (i != 9 ? i + 3 : i + 2));
      // currentPointer == pointer to just materilized list segment
      Value currentPointer =
          currentState
              .getMemoryModel()
              .getValueFromSMGValue(resultValueAndState.getSMGValue())
              .orElseThrow();
      if (i == size - 1) {
        Optional<SMGStateAndOptionalSMGObjectAndOffset> object =
            currentState.dereferencePointerWithoutMaterilization(currentPointer);
        assertThat(object.isPresent()).isTrue();
        assertThat(object.orElseThrow().getSMGObject().getSize().compareTo(sizeInBits) == 0)
            .isTrue();

        ValueAndSMGState nextPointer =
            currentState.readValue(
                object.orElseThrow().getSMGObject(), nfo, pointerSizeInBits, null);
        Preconditions.checkArgument(nextPointer.getValue().isNumericValue());
        Preconditions.checkArgument(
            nextPointer.getValue().asNumericValue().bigInteger().compareTo(BigInteger.ZERO) == 0);
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
  @SuppressWarnings("null")
  @Test
  public void abstractDLLLimitTest() throws InvalidConfigurationException, SMG2Exception {
    BigInteger nfo = pointerSizeInBits;
    BigInteger pfo = pointerSizeInBits.add(pointerSizeInBits);
    BigInteger sizeInBits = pointerSizeInBits.add(pointerSizeInBits).add(pointerSizeInBits);

    for (int minLength = 3; minLength < 50; minLength = minLength + 10) {
      for (int i = 1; i < 50; i++) {
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

  /*
   * Will fill the list with data such that the nfo (and pfo) are last. The data is int and the same every list segment.
   * The data is numeric starting from 0, +1 each new value such that the space until nfo is filled.
   * Valid sizes are divisible by 32.
   */
  private Value[] buildConcreteList(boolean dll, BigInteger sizeOfSegment, int listLength)
      throws SMG2Exception {
    BigInteger nfo =
        dll
            ? sizeOfSegment.subtract(pointerSizeInBits).subtract(pointerSizeInBits)
            : sizeOfSegment.subtract(pointerSizeInBits);
    BigInteger pfo = sizeOfSegment.subtract(pointerSizeInBits);
    Value pointerArray[] = new Value[listLength];
    Value prevNextPointer = null;
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
      if (prevNextPointer != null) {
        currentState =
            currentState.createAndAddPointer(prevNextPointer, listSegment, BigInteger.ZERO);
      }

      // Pointer to the next list segment
      Value nextPointer = SymbolicValueFactory.getInstance().newIdentifier(null);
      if (i == listLength - 1) {
        nextPointer = new NumericValue(0);
      } else {
        nextPointer = SymbolicValueFactory.getInstance().newIdentifier(null);
        SMGValueAndSMGState valueAndState = currentState.copyAndAddValue(nextPointer);
        currentState = valueAndState.getSMGState();
      }

      currentState =
          currentState.writeValueTo(listSegment, nfo, pointerSizeInBits, nextPointer, null);

      if (dll) {
        // Pointer to the prev list segment
        Value prevPointer;
        if (i == 0) {
          prevPointer = new NumericValue(0);
        } else {
          prevPointer = SymbolicValueFactory.getInstance().newIdentifier(null);
          currentState = currentState.createAndAddPointer(prevPointer, prevObject, BigInteger.ZERO);
        }
        currentState =
            currentState.writeValueTo(listSegment, pfo, pointerSizeInBits, prevPointer, null);
      }
      // Pointer to the list segment
      Value pointer = SymbolicValueFactory.getInstance().newIdentifier(null);
      pointerArray[i] = pointer;
      currentState = currentState.createAndAddPointer(pointer, listSegment, BigInteger.ZERO);

      prevObject = listSegment;
      prevNextPointer = nextPointer;
    }
    return pointerArray;
  }
}
