// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.math.BigInteger;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;
import org.sosy_lab.cpachecker.util.smg.test.SMGTest0;
import org.sosy_lab.cpachecker.util.smg.util.SMGandValue;

public class SMGWriteReadTest extends SMGTest0 {

  private SMG smg;
  private final SMGValue value1 = createValue("value1");
  private final SMGValue value2 = createValue("value2");
  private final SMGValue value3 = createValue("value3");
  private final SMGValue value4 = createValue("value4");

  private SMGPointsToEdge nullPointer;

  @Before
  public void setUp() {
    smg = new SMG(mockType8bSize);

    nullPointer =
        new SMGPointsToEdge(smg.getNullObject(), BigInteger.ZERO, SMGTargetSpecifier.IS_REGION);
  }

  /*
   * Idea: make parameterized tests that write a value according to the parameters (from, to).
   * We can additionaly do this for the size of the tested region! We just have to be careful that we do not exceed the boundries.
   * Using this we can check many cases easily.
   * Note: we work byte precise inside of writeValue(), not bit precise!
   * Example:
   * We have a region w 256 bits, we write everything to 0, then a value into the middle of it.
   * Then the parameters can check what happens if we don't overlap at all, overlap from one side or the other etc.
   */

  /*
   * Test creating an empty SMG (has only NullPointer and ZeroValue) and adding an SMGObject to it.
   */
  @Test
  public void emptyObjectTest() {
    // There are 3 values for 0 to discern int 0, double 0.0 and float 0.0
    // Assert empty SMG
    assertThat(smg.getPTEdges().toList())
        .isEqualTo(ImmutableList.of(nullPointer, nullPointer, nullPointer));
    assertThat(smg.getHVEdges().toList()).isEmpty();
    assertThat(smg.getValues())
        .isEqualTo(
            PersistentSet.of(SMGValue.zeroValue())
                .addAndCopy(SMGValue.zeroDoubleValue())
                .addAndCopy(SMGValue.zeroFloatValue())
                .addAndCopy(SMGValue.zeroDoubleValue())
                .addAndCopy(SMGValue.zeroFloatValue()));
    assertThat(smg.getObjects()).isEqualTo(PersistentSet.of(SMGObject.nullInstance()));

    // Add a SMGObject and assert again
    SMGObject testObject = createRegion(BigInteger.valueOf(256));
    smg = smg.copyAndAddObject(testObject);

    assertThat(smg.getPTEdges().toList())
        .isEqualTo(ImmutableList.of(nullPointer, nullPointer, nullPointer));
    assertThat(smg.getHVEdges().toList()).isEmpty();
    assertThat(smg.getValues())
        .isEqualTo(
            PersistentSet.of(SMGValue.zeroValue())
                .addAndCopy(SMGValue.zeroDoubleValue())
                .addAndCopy(SMGValue.zeroFloatValue())
                .addAndCopy(SMGValue.zeroDoubleValue())
                .addAndCopy(SMGValue.zeroFloatValue()));
    assertThat(smg.getObjects())
        .isEqualTo(PersistentSet.of(SMGObject.nullInstance()).addAndCopy(testObject));
  }

  /*
   * Test adding an SMGObject, writing everything to 0 for it, then add
   * value1 from (Bytes) 3 to 11 (size 8 Bytes) and then adding value2 from
   *  (Bytes) 7 to 15 (size again 8 Bytes). This causes the invalidation of value1.
   */
  @Test
  public void writeOverlappingTest() {
    // Create an SMG with a region sized 32 bytes
    SMGObject testObject = createRegion(BigInteger.valueOf(256));
    smg = smg.copyAndAddObject(testObject);

    // First we need to write everything to 0
    smg = smg.writeValue(testObject, BigInteger.ZERO, testObject.getSize(), SMGValue.zeroValue());
    // writeValue() to that region with a ptr to a value1 at position 3 (byte) (a1 has size 8
    // bytes)
    smg = smg.writeValue(testObject, BigInteger.valueOf(3 * 8), BigInteger.valueOf(8 * 8), value1);

    SMGHasValueEdge expectedZeroEdgeBeginning =
        new SMGHasValueEdge(SMGValue.zeroValue(), BigInteger.ZERO, BigInteger.valueOf(3 * 8));

    // Assert that a1 is in range [3; 8)
    SMGHasValueEdge expectedValue1Edge =
        new SMGHasValueEdge(value1, BigInteger.valueOf(3 * 8), BigInteger.valueOf(8 * 8));

    // Assert that zero values are at ranges [0; 3) and [11, 21) (left is starting byte, right is
    // size!)
    SMGHasValueEdge expectedZeroEdgeEnd =
        new SMGHasValueEdge(
            SMGValue.zeroValue(), BigInteger.valueOf(11 * 8), BigInteger.valueOf(21 * 8));

    PersistentSet<SMGHasValueEdge> expectedEdges =
        PersistentSet.of(expectedZeroEdgeBeginning)
            .addAndCopy(expectedValue1Edge)
            .addAndCopy(expectedZeroEdgeEnd);

    // Check that nothing changed for the points to edges
    assertThat(smg.getPTEdges().toList())
        .isEqualTo(ImmutableList.of(nullPointer, nullPointer, nullPointer));
    // Assert that now the region of the testObject is coverd in a nullyfied block completely
    assertThat(smg.getHVEdges().toList()).containsExactlyElementsIn(expectedEdges);
    assertThat(smg.getValues())
        .isEqualTo(
            PersistentSet.of(SMGValue.zeroValue())
                .addAndCopy(SMGValue.zeroDoubleValue())
                .addAndCopy(SMGValue.zeroFloatValue())
                .addAndCopy(value1));
    assertThat(smg.getValues()).hasSize(4);
    assertThat(smg.getObjects())
        .isEqualTo(PersistentSet.of(SMGObject.nullInstance()).addAndCopy(testObject));
    // Check that only the expected HVEdges exists for the object
    assertThat(smg.getEdges(testObject)).containsExactlyElementsIn(expectedEdges);
    assertThat(smg.getEdges(testObject)).hasSize(expectedEdges.size());

    // writeValue() a2 to the region at byte 7, a2 is [7; 8), this deletes the value1 edge
    smg = smg.writeValue(testObject, BigInteger.valueOf(7 * 8), BigInteger.valueOf(8 * 8), value2);

    // Assert that a2 is [7, 8]
    SMGHasValueEdge expectedValue2Edge =
        new SMGHasValueEdge(value2, BigInteger.valueOf(7 * 8), BigInteger.valueOf(8 * 8));

    // Assert that zero values are at ranges [0; 3) and [15; 17)
    expectedZeroEdgeEnd =
        new SMGHasValueEdge(
            SMGValue.zeroValue(), BigInteger.valueOf(15 * 8), BigInteger.valueOf(17 * 8));

    // The value1 edge is gone, there is an area of [3; 4) with an undefined value now
    expectedEdges =
        PersistentSet.of(expectedZeroEdgeBeginning)
            .addAndCopy(expectedValue2Edge)
            .addAndCopy(expectedZeroEdgeEnd);

    // Check that nothing changed for the points to edges
    assertThat(smg.getPTEdges().toList())
        .isEqualTo(ImmutableList.of(nullPointer, nullPointer, nullPointer));
    // Assert that there are only the 4 HVEdges in total
    assertThat(smg.getHVEdges().toList()).containsExactlyElementsIn(expectedEdges);
    // Assert that there are only the zero value and value1 and2
    assertThat(smg.getValues())
        .isEqualTo(
            PersistentSet.of(SMGValue.zeroValue())
                .addAndCopy(SMGValue.zeroDoubleValue())
                .addAndCopy(SMGValue.zeroFloatValue())
                .addAndCopy(value1)
                .addAndCopy(value2));
    // Assert that there is still only our testObject
    assertThat(smg.getObjects())
        .isEqualTo(PersistentSet.of(SMGObject.nullInstance()).addAndCopy(testObject));
    // Check that only the expected HVEdges exists for the SMGObject
    assertThat(smg.getEdges(testObject)).isEqualTo(expectedEdges);
    assertThat(smg.getEdges(testObject)).hasSize(expectedEdges.size());
  }

  /*
   * Test independence of objects when writing to one.
   * Essentially the same as writeOverlappingTest() but with 2 objects (created both in the beginning) and value 3 and 4 for object2.
   * First we write value1 to the first object, then check that nothing changed in object 2 etc.
   */
  @Test
  public void writeIndependentObjectsTest() {
    // Create an SMG with a region sized 32 bytes
    SMGObject testObject1 = createRegion(BigInteger.valueOf(256));
    SMGObject testObject2 = createRegion(BigInteger.valueOf(256));
    smg = smg.copyAndAddObject(testObject1);
    smg = smg.copyAndAddObject(testObject2);

    // First we need to write everything to 0 for both objects
    smg = smg.writeValue(testObject1, BigInteger.ZERO, testObject1.getSize(), SMGValue.zeroValue());
    assertThat(smg.getEdges(testObject1)).isNotEmpty();
    assertThat(smg.getEdges(testObject2)).isEmpty();
    smg = smg.writeValue(testObject2, BigInteger.ZERO, testObject2.getSize(), SMGValue.zeroValue());
    assertThat(smg.getEdges(testObject1)).isNotEmpty();
    assertThat(smg.getEdges(testObject2)).isNotEmpty();
    // writeValue() to testObject1 with value1 [3; 8)
    smg = smg.writeValue(testObject1, BigInteger.valueOf(3 * 8), BigInteger.valueOf(8 * 8), value1);

    SMGHasValueEdge expectedZeroEdgeObject2 =
        new SMGHasValueEdge(SMGValue.zeroValue(), BigInteger.ZERO, testObject2.getSize());

    // We added the zero edge to object2, and did not change anything, so thats the only edge in it!
    assertThat(smg.getEdges(testObject2))
        .containsExactlyElementsIn(ImmutableList.of(expectedZeroEdgeObject2));

    smg = smg.writeValue(testObject1, BigInteger.valueOf(7 * 8), BigInteger.valueOf(8 * 8), value2);
    assertThat(smg.getEdges(testObject2))
        .containsExactlyElementsIn(ImmutableList.of(expectedZeroEdgeObject2));

    // Same as with value1 on object2 with value3
    smg = smg.writeValue(testObject2, BigInteger.valueOf(3 * 8), BigInteger.valueOf(8 * 8), value3);

    // Assert that nothing changed for object1
    SMGHasValueEdge expectedZeroEdgeBeginningObject1 =
        new SMGHasValueEdge(SMGValue.zeroValue(), BigInteger.ZERO, BigInteger.valueOf(3 * 8));

    SMGHasValueEdge expectedValue2EdgeObject1 =
        new SMGHasValueEdge(value2, BigInteger.valueOf(7 * 8), BigInteger.valueOf(8 * 8));

    SMGHasValueEdge expectedZeroEdgeEndObject1 =
        new SMGHasValueEdge(
            SMGValue.zeroValue(), BigInteger.valueOf(15 * 8), BigInteger.valueOf(17 * 8));

    SMGHasValueEdge expectedValue3EdgeObject2 =
        new SMGHasValueEdge(value3, BigInteger.valueOf(3 * 8), BigInteger.valueOf(8 * 8));

    SMGHasValueEdge expectedZeroEdgeBeginningObject2 =
        new SMGHasValueEdge(SMGValue.zeroValue(), BigInteger.ZERO, BigInteger.valueOf(3 * 8));

    SMGHasValueEdge expectedZeroEdgeEndObject2 =
        new SMGHasValueEdge(
            SMGValue.zeroValue(), BigInteger.valueOf(11 * 8), BigInteger.valueOf(21 * 8));

    PersistentSet<SMGHasValueEdge> expectedEdgesObject1 =
        PersistentSet.of(expectedZeroEdgeBeginningObject1)
            .addAndCopy(expectedValue2EdgeObject1)
            .addAndCopy(expectedZeroEdgeEndObject1);

    PersistentSet<SMGHasValueEdge> expectedEdgesObject2 =
        PersistentSet.of(expectedValue3EdgeObject2)
            .addAndCopy(expectedZeroEdgeBeginningObject2)
            .addAndCopy(expectedZeroEdgeEndObject2);

    ImmutableList<SMGHasValueEdge> expectedEdges =
        ImmutableList.of(
            expectedZeroEdgeEndObject2,
            expectedZeroEdgeBeginningObject2,
            expectedValue3EdgeObject2,
            expectedZeroEdgeEndObject1,
            expectedValue2EdgeObject1,
            expectedZeroEdgeBeginningObject1);

    // Check that nothing changed for the points to edges
    assertThat(smg.getPTEdges().toList())
        .isEqualTo(ImmutableList.of(nullPointer, nullPointer, nullPointer));
    assertThat(smg.getValues())
        .isEqualTo(
            PersistentSet.of(SMGValue.zeroValue())
                .addAndCopy(SMGValue.zeroDoubleValue())
                .addAndCopy(SMGValue.zeroFloatValue())
                .addAndCopy(value1)
                .addAndCopy(value2)
                .addAndCopy(value3));
    assertThat(smg.getObjects())
        .isEqualTo(
            PersistentSet.of(SMGObject.nullInstance())
                .addAndCopy(testObject1)
                .addAndCopy(testObject2));
    // Check that only the expected HVEdges exists for the objects
    assertThat(smg.getEdges(testObject1)).containsExactlyElementsIn(expectedEdgesObject1);
    assertThat(smg.getEdges(testObject2)).containsExactlyElementsIn(expectedEdgesObject2);
    // Assert all existing HVEdges (List because the first edge should be there 2 times)
    assertThat(smg.getHVEdges().toList()).containsExactlyElementsIn(expectedEdges);

    // Same as with value2 on object2 with value4
    smg = smg.writeValue(testObject2, BigInteger.valueOf(7 * 8), BigInteger.valueOf(8 * 8), value4);

    SMGHasValueEdge expectedValue4EdgeObject2 =
        new SMGHasValueEdge(value4, BigInteger.valueOf(7 * 8), BigInteger.valueOf(8 * 8));

    expectedZeroEdgeEndObject2 =
        new SMGHasValueEdge(
            SMGValue.zeroValue(), BigInteger.valueOf(15 * 8), BigInteger.valueOf(17 * 8));

    expectedEdgesObject2 =
        PersistentSet.of(expectedValue4EdgeObject2)
            .addAndCopy(expectedZeroEdgeBeginningObject2)
            .addAndCopy(expectedZeroEdgeEndObject2);

    expectedEdges =
        ImmutableList.of(
            expectedZeroEdgeEndObject2,
            expectedZeroEdgeBeginningObject2,
            expectedValue4EdgeObject2,
            expectedZeroEdgeEndObject1,
            expectedValue2EdgeObject1,
            expectedZeroEdgeBeginningObject1);

    assertThat(smg.getPTEdges().toList())
        .isEqualTo(ImmutableList.of(nullPointer, nullPointer, nullPointer));
    assertThat(smg.getValues())
        .isEqualTo(
            PersistentSet.of(SMGValue.zeroValue())
                .addAndCopy(SMGValue.zeroDoubleValue())
                .addAndCopy(SMGValue.zeroFloatValue())
                .addAndCopy(value1)
                .addAndCopy(value2)
                .addAndCopy(value3)
                .addAndCopy(value4));

    assertThat(smg.getObjects())
        .isEqualTo(
            PersistentSet.of(SMGObject.nullInstance())
                .addAndCopy(testObject1)
                .addAndCopy(testObject2));
    // Check that only the expected HVEdges exists for the objects
    assertThat(smg.getEdges(testObject1)).containsExactlyElementsIn(expectedEdgesObject1);
    assertThat(smg.getEdges(testObject2)).containsExactlyElementsIn(expectedEdgesObject2);
    // Assert all existing HVEdges
    assertThat(smg.getHVEdges().toList()).containsExactlyElementsIn(expectedEdges);
  }

  /*
   * Test adding a SMGObject to an empty SMG and then write the entire region of the object to 0.
   */
  @Test
  public void writeZeroValueForEntireObjectTest() {
    final BigInteger sizeInBitsOfObject = BigInteger.valueOf(256);
    SMGObject testObject = createRegion(sizeInBitsOfObject);

    // Assert that the smg with the object does not have any values/edges etc.
    assertThat(smg.getPTEdges().toList())
        .isEqualTo(ImmutableList.of(nullPointer, nullPointer, nullPointer));
    assertThat(smg.getHVEdges().toList()).isEmpty();
    assertThat(smg.getValues())
        .isEqualTo(
            PersistentSet.of(SMGValue.zeroValue())
                .addAndCopy(SMGValue.zeroDoubleValue())
                .addAndCopy(SMGValue.zeroFloatValue()));
    assertThat(smg.getObjects()).isEqualTo(PersistentSet.of(SMGObject.nullInstance()));

    smg = smg.copyAndAddObject(testObject);
    assertThat(smg.getObjects())
        .isEqualTo(PersistentSet.of(SMGObject.nullInstance()).addAndCopy(testObject));

    // Write everything to 0
    smg = smg.writeValue(testObject, BigInteger.ZERO, testObject.getSize(), SMGValue.zeroValue());

    SMGHasValueEdge expectedEdge =
        new SMGHasValueEdge(SMGValue.zeroValue(), BigInteger.ZERO, sizeInBitsOfObject);

    PersistentSet<SMGHasValueEdge> expectedEdges =
        PersistentSet.<SMGHasValueEdge>of().addAndCopy(expectedEdge);

    // Check that nothing changed for the points to edges
    assertThat(smg.getPTEdges().toList())
        .isEqualTo(ImmutableList.of(nullPointer, nullPointer, nullPointer));
    // Assert that now the region of the testObject is coverd in a nullyfied block completely
    assertThat(smg.getHVEdges().toList()).isEqualTo(ImmutableList.of(expectedEdge));
    assertThat(smg.getValues())
        .isEqualTo(
            PersistentSet.of(SMGValue.zeroValue())
                .addAndCopy(SMGValue.zeroDoubleValue())
                .addAndCopy(SMGValue.zeroFloatValue()));
    assertThat(smg.getObjects())
        .isEqualTo(PersistentSet.of(SMGObject.nullInstance()).addAndCopy(testObject));
    // Check that only the expected HVEdges exists for the object
    assertThat(smg.getEdges(testObject)).isEqualTo(expectedEdges);
    assertThat(smg.getEdges(testObject)).hasSize(expectedEdges.size());
  }

  /** Write everything to 0, then the first half of the region to value1. */
  @Test
  public void writeSingleValueTest() {
    final BigInteger sizeInBitsOfObject = BigInteger.valueOf(256);
    SMGObject testObject = createRegion(sizeInBitsOfObject);
    smg = smg.copyAndAddObject(testObject);
    smg = smg.writeValue(testObject, BigInteger.ZERO, testObject.getSize(), SMGValue.zeroValue());

    // Write value1 in the region of the object from 0 to 127 (1/2 of its length)
    smg =
        smg.writeValue(
            testObject, BigInteger.ZERO, sizeInBitsOfObject.divide(BigInteger.valueOf(2)), value1);

    // From 128 till 256 the zero value remains
    SMGHasValueEdge expectedZeroEdge =
        new SMGHasValueEdge(
            SMGValue.zeroValue(),
            sizeInBitsOfObject.divide(BigInteger.valueOf(2)),
            sizeInBitsOfObject.divide(BigInteger.valueOf(2)));

    // From 0 till 127 there is now value1
    SMGHasValueEdge expectedValueEdge =
        new SMGHasValueEdge(
            value1, BigInteger.ZERO, sizeInBitsOfObject.divide(BigInteger.valueOf(2)));

    PersistentSet<SMGHasValueEdge> expectedEdges =
        PersistentSet.<SMGHasValueEdge>of()
            .addAndCopy(expectedValueEdge)
            .addAndCopy(expectedZeroEdge);

    // Assert that nothing changed for the points to edges
    assertThat(smg.getPTEdges().toList())
        .isEqualTo(ImmutableList.of(nullPointer, nullPointer, nullPointer));
    // Check that the HVEdge exists
    assertThat(smg.getHVEdges().toList()).hasSize(2);
    assertThat(smg.getHVEdges().toList()).contains(expectedValueEdge);
    assertThat(smg.getHVEdges().toList()).contains(expectedZeroEdge);
    assertThat(smg.getValues())
        .isEqualTo(
            PersistentSet.of(SMGValue.zeroValue())
                .addAndCopy(SMGValue.zeroDoubleValue())
                .addAndCopy(SMGValue.zeroFloatValue())
                .addAndCopy(value1));
    assertThat(smg.getObjects())
        .isEqualTo(PersistentSet.of(SMGObject.nullInstance()).addAndCopy(testObject));
    // Check that the HVEdges exist for the object
    assertThat(smg.getEdges(testObject)).isEqualTo(expectedEdges);
  }

  /** Write everything to 0, then the second half of the region to value1. */
  @Test
  public void writeSingleValueTest2() {
    final BigInteger sizeInBitsOfObject = BigInteger.valueOf(256);
    SMGObject testObject = createRegion(sizeInBitsOfObject);
    smg = smg.copyAndAddObject(testObject);
    smg = smg.writeValue(testObject, BigInteger.ZERO, testObject.getSize(), SMGValue.zeroValue());

    // Write value1 in the region of the object from sizeOfObject/2 to end (1/2 of objectlength is
    // the length of the edge)
    smg =
        smg.writeValue(
            testObject,
            sizeInBitsOfObject.divide(BigInteger.valueOf(2)),
            sizeInBitsOfObject.divide(BigInteger.valueOf(2)),
            value1);

    // From 128 till 256 the zero value remains
    SMGHasValueEdge expectedZeroEdge =
        new SMGHasValueEdge(
            SMGValue.zeroValue(),
            BigInteger.ZERO,
            sizeInBitsOfObject.divide(BigInteger.valueOf(2)));

    // From 0 till 127 there is now value1
    SMGHasValueEdge expectedValueEdge =
        new SMGHasValueEdge(
            value1,
            sizeInBitsOfObject.divide(BigInteger.valueOf(2)),
            sizeInBitsOfObject.divide(BigInteger.valueOf(2)));

    PersistentSet<SMGHasValueEdge> expectedEdges =
        PersistentSet.<SMGHasValueEdge>of()
            .addAndCopy(expectedValueEdge)
            .addAndCopy(expectedZeroEdge);

    // Assert that nothing changed for the points to edges
    assertThat(smg.getPTEdges().toList())
        .isEqualTo(ImmutableList.of(nullPointer, nullPointer, nullPointer));
    // Check that the HVEdge exists
    assertThat(smg.getHVEdges().toList()).hasSize(2);
    assertThat(smg.getHVEdges().toList()).contains(expectedValueEdge);
    assertThat(smg.getHVEdges().toList()).contains(expectedZeroEdge);
    assertThat(smg.getValues())
        .isEqualTo(
            PersistentSet.of(SMGValue.zeroValue())
                .addAndCopy(SMGValue.zeroDoubleValue())
                .addAndCopy(SMGValue.zeroFloatValue())
                .addAndCopy(value1));
    assertThat(smg.getObjects())
        .isEqualTo(PersistentSet.of(SMGObject.nullInstance()).addAndCopy(testObject));
    // Check that the HVEdges exist for the object
    assertThat(smg.getEdges(testObject)).isEqualTo(expectedEdges);
  }

  /*
   * Write the region to 0, then value1 in the first half and value2 in the second half such that they do not overlapp.
   */
  @Test
  public void writeMulitpleValuesNonOverlappingTest() {
    SMGObject testObject = createRegion(BigInteger.valueOf(256));
    smg = smg.copyAndAddObject(testObject);

    // First write everything to 0, then value1 and value2
    smg = smg.writeValue(testObject, BigInteger.ZERO, testObject.getSize(), SMGValue.zeroValue());
    smg =
        smg.writeValue(
            testObject, BigInteger.ZERO, testObject.getSize().divide(BigInteger.TWO), value1);
    smg =
        smg.writeValue(
            testObject,
            testObject.getSize().divide(BigInteger.TWO),
            testObject.getSize().divide(BigInteger.TWO),
            value2);

    SMGHasValueEdge expectedValue1Edge =
        new SMGHasValueEdge(value1, BigInteger.ZERO, testObject.getSize().divide(BigInteger.TWO));

    SMGHasValueEdge expectedValue2Edge =
        new SMGHasValueEdge(
            value2,
            testObject.getSize().divide(BigInteger.TWO),
            testObject.getSize().divide(BigInteger.TWO));

    PersistentSet<SMGHasValueEdge> expectedEdges =
        PersistentSet.of(expectedValue1Edge).addAndCopy(expectedValue2Edge);

    // Check that nothing changed for the points to edges
    assertThat(smg.getPTEdges().toList())
        .isEqualTo(ImmutableList.of(nullPointer, nullPointer, nullPointer));
    // Assert that now the region of the testObject is coverd in the 2 value1 & 2 edges
    assertThat(smg.getHVEdges().toList()).containsExactlyElementsIn(expectedEdges);
    assertThat(smg.getValues())
        .isEqualTo(
            PersistentSet.of(SMGValue.zeroValue())
                .addAndCopy(SMGValue.zeroDoubleValue())
                .addAndCopy(SMGValue.zeroFloatValue())
                .addAndCopy(value1)
                .addAndCopy(value2));
    assertThat(smg.getObjects())
        .isEqualTo(PersistentSet.of(SMGObject.nullInstance()).addAndCopy(testObject));
    // Check that only the expected HVEdges exists for the object
    assertThat(smg.getEdges(testObject)).containsExactlyElementsIn(expectedEdges);
    assertThat(smg.getEdges(testObject)).hasSize(expectedEdges.size());
  }

  /*
   * Write the region to 0, then value1 in the first half and value2 in the second half such that they do not overlapp.
   * After that write value3 right in the middle of then such that it overlapps just 1 Byte in each direction,
   * invalidating both fields of value1 and 2.
   */
  @Test
  public void writeMulitpleValuesOverlappingTest() {
    SMGObject testObject = createRegion(BigInteger.valueOf(256));
    smg = smg.copyAndAddObject(testObject);

    // First write everything to 0, then value1 and value2
    smg = smg.writeValue(testObject, BigInteger.ZERO, testObject.getSize(), SMGValue.zeroValue());
    smg =
        smg.writeValue(
            testObject, BigInteger.ZERO, testObject.getSize().divide(BigInteger.TWO), value1);
    smg =
        smg.writeValue(
            testObject,
            testObject.getSize().divide(BigInteger.TWO),
            testObject.getSize().divide(BigInteger.TWO),
            value2);
    smg =
        smg.writeValue(
            testObject,
            testObject.getSize().divide(BigInteger.TWO).subtract(BigInteger.valueOf(8)),
            BigInteger.valueOf(2 * 8),
            value3);

    SMGHasValueEdge expectedValue3Edge =
        new SMGHasValueEdge(
            value3,
            testObject.getSize().divide(BigInteger.TWO).subtract(BigInteger.valueOf(8)),
            BigInteger.valueOf(2 * 8));

    PersistentSet<SMGHasValueEdge> expectedEdges = PersistentSet.of(expectedValue3Edge);

    // Check that nothing changed for the points to edges
    assertThat(smg.getPTEdges().toList())
        .isEqualTo(ImmutableList.of(nullPointer, nullPointer, nullPointer));
    // Assert that now the region of the testObject is coverd in the 2 value1 & 2 edges
    assertThat(smg.getHVEdges().toList()).containsExactlyElementsIn(expectedEdges);
    // Values are never deleted!
    assertThat(smg.getValues())
        .isEqualTo(
            PersistentSet.of(SMGValue.zeroValue())
                .addAndCopy(SMGValue.zeroDoubleValue())
                .addAndCopy(SMGValue.zeroFloatValue())
                .addAndCopy(value1)
                .addAndCopy(value2)
                .addAndCopy(value3));
    assertThat(smg.getObjects())
        .isEqualTo(PersistentSet.of(SMGObject.nullInstance()).addAndCopy(testObject));
    // Check that only the expected HVEdges exists for the object
    assertThat(smg.getEdges(testObject)).containsExactlyElementsIn(expectedEdges);
    assertThat(smg.getEdges(testObject)).hasSize(expectedEdges.size());
  }

  /*
   * Write the region to 0, then value1 in the first half and value2 in the second half such that they do not overlapp.
   * After that write value3 at the very beginning of value1, invalidating only the field of value1.
   */
  @Test
  public void writeMulitpleValuesOverlappingTest2() {
    SMGObject testObject = createRegion(BigInteger.valueOf(256));
    smg = smg.copyAndAddObject(testObject);

    // First write everything to 0, then value1 and value2
    smg = smg.writeValue(testObject, BigInteger.ZERO, testObject.getSize(), SMGValue.zeroValue());
    smg =
        smg.writeValue(
            testObject, BigInteger.ZERO, testObject.getSize().divide(BigInteger.TWO), value1);
    smg =
        smg.writeValue(
            testObject,
            testObject.getSize().divide(BigInteger.TWO),
            testObject.getSize().divide(BigInteger.TWO),
            value2);
    smg = smg.writeValue(testObject, BigInteger.ZERO, BigInteger.valueOf(8), value3);

    SMGHasValueEdge expectedValue2Edge =
        new SMGHasValueEdge(
            value2,
            testObject.getSize().divide(BigInteger.TWO),
            testObject.getSize().divide(BigInteger.TWO));

    SMGHasValueEdge expectedValue3Edge =
        new SMGHasValueEdge(value3, BigInteger.ZERO, BigInteger.valueOf(8));

    PersistentSet<SMGHasValueEdge> expectedEdges =
        PersistentSet.of(expectedValue2Edge).addAndCopy(expectedValue3Edge);

    // Check that nothing changed for the points to edges
    assertThat(smg.getPTEdges().toList())
        .isEqualTo(ImmutableList.of(nullPointer, nullPointer, nullPointer));
    // Assert that now the region of the testObject is coverd in the 2 value1 & 2 edges
    assertThat(smg.getHVEdges().toList()).containsExactlyElementsIn(expectedEdges);
    // Values are never deleted!
    assertThat(smg.getValues())
        .isEqualTo(
            PersistentSet.of(SMGValue.zeroValue())
                .addAndCopy(SMGValue.zeroDoubleValue())
                .addAndCopy(SMGValue.zeroFloatValue())
                .addAndCopy(value1)
                .addAndCopy(value2)
                .addAndCopy(value3));
    assertThat(smg.getObjects())
        .isEqualTo(PersistentSet.of(SMGObject.nullInstance()).addAndCopy(testObject));
    // Check that only the expected HVEdges exists for the object
    assertThat(smg.getEdges(testObject)).containsExactlyElementsIn(expectedEdges);
    assertThat(smg.getEdges(testObject)).hasSize(expectedEdges.size());
  }

  /*
   * Write the region to 0, then value1 in the first half and value2 in the second half such that they do not overlapp.
   * After that write value3 at the very end of value1, invalidating only the field of value1.
   */
  @Test
  public void writeMulitpleValuesOverlappingTest3() {
    SMGObject testObject = createRegion(BigInteger.valueOf(256));
    smg = smg.copyAndAddObject(testObject);

    // First write everything to 0, then value1 and value2
    smg = smg.writeValue(testObject, BigInteger.ZERO, testObject.getSize(), SMGValue.zeroValue());
    smg =
        smg.writeValue(
            testObject, BigInteger.ZERO, testObject.getSize().divide(BigInteger.TWO), value1);
    smg =
        smg.writeValue(
            testObject,
            testObject.getSize().divide(BigInteger.TWO),
            testObject.getSize().divide(BigInteger.TWO),
            value2);
    smg =
        smg.writeValue(
            testObject,
            testObject.getSize().divide(BigInteger.TWO).subtract(BigInteger.valueOf(8)),
            BigInteger.valueOf(8),
            value3);

    SMGHasValueEdge expectedValue2Edge =
        new SMGHasValueEdge(
            value2,
            testObject.getSize().divide(BigInteger.TWO),
            testObject.getSize().divide(BigInteger.TWO));

    SMGHasValueEdge expectedValue3Edge =
        new SMGHasValueEdge(
            value3,
            testObject.getSize().divide(BigInteger.TWO).subtract(BigInteger.valueOf(8)),
            BigInteger.valueOf(8));

    PersistentSet<SMGHasValueEdge> expectedEdges =
        PersistentSet.of(expectedValue2Edge).addAndCopy(expectedValue3Edge);

    // Check that nothing changed for the points to edges
    assertThat(smg.getPTEdges().toList())
        .isEqualTo(ImmutableList.of(nullPointer, nullPointer, nullPointer));
    // Assert that now the region of the testObject is coverd in the 2 value1 & 2 edges
    assertThat(smg.getHVEdges().toList()).containsExactlyElementsIn(expectedEdges);
    // Values are never deleted!
    assertThat(smg.getValues())
        .isEqualTo(
            PersistentSet.of(SMGValue.zeroValue())
                .addAndCopy(SMGValue.zeroDoubleValue())
                .addAndCopy(SMGValue.zeroFloatValue())
                .addAndCopy(value1)
                .addAndCopy(value2)
                .addAndCopy(value3));
    assertThat(smg.getObjects())
        .isEqualTo(PersistentSet.of(SMGObject.nullInstance()).addAndCopy(testObject));
    // Check that only the expected HVEdges exists for the object
    assertThat(smg.getEdges(testObject)).containsExactlyElementsIn(expectedEdges);
    assertThat(smg.getEdges(testObject)).hasSize(expectedEdges.size());
  }

  /*
   * Write the region to 0, then value1 in the first half and value2 in the second half such that they do not overlapp.
   * After that write value3 at the very end of value2, invalidating only the field of value2.
   */
  @Test
  public void writeMulitpleValuesOverlappingTest4() {
    SMGObject testObject = createRegion(BigInteger.valueOf(256));
    smg = smg.copyAndAddObject(testObject);

    // First write everything to 0, then value1 and value2
    smg = smg.writeValue(testObject, BigInteger.ZERO, testObject.getSize(), SMGValue.zeroValue());
    smg =
        smg.writeValue(
            testObject, BigInteger.ZERO, testObject.getSize().divide(BigInteger.TWO), value1);
    smg =
        smg.writeValue(
            testObject,
            testObject.getSize().divide(BigInteger.TWO),
            testObject.getSize().divide(BigInteger.TWO),
            value2);
    smg =
        smg.writeValue(
            testObject,
            testObject.getSize().subtract(BigInteger.valueOf(8)),
            BigInteger.valueOf(8),
            value3);

    SMGHasValueEdge expectedValue1Edge =
        new SMGHasValueEdge(value1, BigInteger.ZERO, testObject.getSize().divide(BigInteger.TWO));

    SMGHasValueEdge expectedValue3Edge =
        new SMGHasValueEdge(
            value3, testObject.getSize().subtract(BigInteger.valueOf(8)), BigInteger.valueOf(8));

    PersistentSet<SMGHasValueEdge> expectedEdges =
        PersistentSet.of(expectedValue1Edge).addAndCopy(expectedValue3Edge);

    // Check that nothing changed for the points to edges
    assertThat(smg.getPTEdges().toList())
        .isEqualTo(ImmutableList.of(nullPointer, nullPointer, nullPointer));
    // Assert that now the region of the testObject is coverd in the 2 value1 & 2 edges
    assertThat(smg.getHVEdges().toList()).containsExactlyElementsIn(expectedEdges);
    // Values are never deleted!
    assertThat(smg.getValues())
        .isEqualTo(
            PersistentSet.of(SMGValue.zeroValue())
                .addAndCopy(SMGValue.zeroDoubleValue())
                .addAndCopy(SMGValue.zeroFloatValue())
                .addAndCopy(value1)
                .addAndCopy(value2)
                .addAndCopy(value3));
    assertThat(smg.getObjects())
        .isEqualTo(PersistentSet.of(SMGObject.nullInstance()).addAndCopy(testObject));
    // Check that only the expected HVEdges exists for the object
    assertThat(smg.getEdges(testObject)).containsExactlyElementsIn(expectedEdges);
    assertThat(smg.getEdges(testObject)).hasSize(expectedEdges.size());
  }

  /*
   * Write the region to 0, then value1 in the first half and value2 in the second half such that they do not overlapp.
   * After that write value3 at the very beginning of value2, invalidating only the field of value2.
   */
  @Test
  public void writeMulitpleValuesOverlappingTest5() {
    SMGObject testObject = createRegion(BigInteger.valueOf(256));
    smg = smg.copyAndAddObject(testObject);

    // First write everything to 0, then value1 and value2
    smg = smg.writeValue(testObject, BigInteger.ZERO, testObject.getSize(), SMGValue.zeroValue());
    smg =
        smg.writeValue(
            testObject, BigInteger.ZERO, testObject.getSize().divide(BigInteger.TWO), value1);
    smg =
        smg.writeValue(
            testObject,
            testObject.getSize().divide(BigInteger.TWO),
            testObject.getSize().divide(BigInteger.TWO),
            value2);
    smg =
        smg.writeValue(
            testObject, testObject.getSize().divide(BigInteger.TWO), BigInteger.valueOf(8), value3);

    SMGHasValueEdge expectedValue1Edge =
        new SMGHasValueEdge(value1, BigInteger.ZERO, testObject.getSize().divide(BigInteger.TWO));

    SMGHasValueEdge expectedValue3Edge =
        new SMGHasValueEdge(
            value3, testObject.getSize().divide(BigInteger.TWO), BigInteger.valueOf(8));

    PersistentSet<SMGHasValueEdge> expectedEdges =
        PersistentSet.of(expectedValue1Edge).addAndCopy(expectedValue3Edge);

    // Check that nothing changed for the points to edges
    assertThat(smg.getPTEdges().toList())
        .isEqualTo(ImmutableList.of(nullPointer, nullPointer, nullPointer));
    // Assert that now the region of the testObject is coverd in the 2 value1 & 2 edges
    assertThat(smg.getHVEdges().toList()).containsExactlyElementsIn(expectedEdges);
    // Values are never deleted!
    assertThat(smg.getValues())
        .isEqualTo(
            PersistentSet.of(SMGValue.zeroValue())
                .addAndCopy(SMGValue.zeroDoubleValue())
                .addAndCopy(SMGValue.zeroFloatValue())
                .addAndCopy(value1)
                .addAndCopy(value2)
                .addAndCopy(value3));
    assertThat(smg.getObjects())
        .isEqualTo(PersistentSet.of(SMGObject.nullInstance()).addAndCopy(testObject));
    // Check that only the expected HVEdges exists for the object
    assertThat(smg.getEdges(testObject)).containsExactlyElementsIn(expectedEdges);
    assertThat(smg.getEdges(testObject)).hasSize(expectedEdges.size());
  }

  // Try writing a value that goes beyond the testObjects field
  @Test(expected = IllegalArgumentException.class)
  public void writeBeyondRangeTest() {
    SMGObject testObject = createRegion(BigInteger.valueOf(256));
    smg = smg.copyAndAddObject(testObject);

    smg =
        smg.writeValue(
            testObject,
            BigInteger.ZERO,
            testObject.getSize().add(BigInteger.ONE),
            SMGValue.zeroValue());
  }

  // Try writing a value that goes beyond the testObjects field
  @Test(expected = IllegalArgumentException.class)
  public void writeBeyondRangeTest2() {
    SMGObject testObject = createRegion(BigInteger.valueOf(256));
    smg = smg.copyAndAddObject(testObject);

    smg = smg.writeValue(testObject, testObject.getSize(), BigInteger.ONE, SMGValue.zeroValue());
  }

  // Try writing a value that goes beyond the testObjects field
  @Test(expected = IllegalArgumentException.class)
  public void writeBeyondRangeTest3() {
    SMGObject testObject = createRegion(BigInteger.valueOf(256));
    smg = smg.copyAndAddObject(testObject);

    smg =
        smg.writeValue(
            testObject,
            testObject.getSize().subtract(BigInteger.ONE),
            BigInteger.TWO,
            SMGValue.zeroValue());
  }

  // Try reading a value that goes beyond the testObjects field
  @Test(expected = IllegalArgumentException.class)
  public void readBeyondRangeTest() {
    SMGObject testObject = createRegion(BigInteger.valueOf(256));
    smg = smg.copyAndAddObject(testObject);

    smg = smg.writeValue(testObject, BigInteger.ZERO, testObject.getSize(), SMGValue.zeroValue());
    smg.readValue(testObject, BigInteger.ZERO, testObject.getSize().add(BigInteger.ONE));
  }

  // Try reading a value that goes beyond the testObjects field
  @Test(expected = IllegalArgumentException.class)
  public void readBeyondRangeTest2() {
    SMGObject testObject = createRegion(BigInteger.valueOf(256));
    smg = smg.copyAndAddObject(testObject);

    smg = smg.writeValue(testObject, BigInteger.ZERO, testObject.getSize(), SMGValue.zeroValue());
    smg.readValue(testObject, testObject.getSize(), BigInteger.ONE);
  }

  // Try reading a value that goes beyond the testObjects field
  @Test(expected = IllegalArgumentException.class)
  public void readBeyondRangeTest3() {
    SMGObject testObject = createRegion(BigInteger.valueOf(256));
    smg = smg.copyAndAddObject(testObject);

    smg = smg.writeValue(testObject, BigInteger.ZERO, testObject.getSize(), SMGValue.zeroValue());
    smg.readValue(testObject, testObject.getSize().subtract(BigInteger.ONE), BigInteger.TWO);
  }

  /**
   * Write an object to the smg, write the field of the object to 0, write a value, read the value
   * in different situations. The 0 value can only be read if its covered in completely nullified
   * blocks. We read the entie value field (results in the value), a combination of value and 0 (=
   * new value) and a subpart ob the value (=new value).
   */
  @Test
  public void readValueZeroValueCombinationTest() {
    SMGObject testObject = createRegion(BigInteger.valueOf(256));
    smg = smg.copyAndAddObject(testObject);
    smg = smg.writeValue(testObject, BigInteger.ZERO, testObject.getSize(), SMGValue.zeroValue());
    smg =
        smg.writeValue(
            testObject, BigInteger.ZERO, testObject.getSize().divide(BigInteger.TWO), value1);

    // Read exactly the value1 area -> value1 read
    checkReadExpectedValue(
        testObject, BigInteger.ZERO, testObject.getSize().divide(BigInteger.TWO), value1);
    // Read some smaller part of only the value -> new value in this area
    smg = checkReadUnknownValue(testObject, BigInteger.ZERO, BigInteger.valueOf(8)).getSMG();
    // Read the complete value + a part of the 0 value -> new value in this area
    checkReadUnknownValue(
        testObject,
        testObject.getSize().divide(BigInteger.TWO).subtract(BigInteger.valueOf(8)),
        BigInteger.valueOf(2 * 8));
  }

  /**
   * Write an object to the smg, write the entire field full of values, read the values in different
   * situations. Reads should only be new values (when multiple edges are read) or the values
   * themselfs. Never 0!
   */
  @Test
  public void readValueValueCombinationTest() {
    SMGObject testObject = createRegion(BigInteger.valueOf(256));
    BigInteger objectSizeQuartered = testObject.getSize().divide(BigInteger.valueOf(4));
    smg = smg.copyAndAddObject(testObject);
    smg = smg.writeValue(testObject, BigInteger.ZERO, objectSizeQuartered, value1);
    smg = smg.writeValue(testObject, objectSizeQuartered, objectSizeQuartered, value2);
    smg =
        smg.writeValue(
            testObject, objectSizeQuartered.multiply(BigInteger.TWO), objectSizeQuartered, value3);
    smg =
        smg.writeValue(
            testObject,
            objectSizeQuartered.multiply(BigInteger.valueOf(3)),
            objectSizeQuartered,
            value4);
    // Read exactly the entire object size -> new value read
    smg = checkReadUnknownValue(testObject, BigInteger.ZERO, testObject.getSize()).getSMG();
    // Read only value1 -> value1
    smg = checkReadExpectedValue(testObject, BigInteger.ZERO, objectSizeQuartered, value1);
    // Read only value2 -> value2
    smg =
        checkReadExpectedValue(
            testObject, objectSizeQuartered.multiply(BigInteger.ONE), objectSizeQuartered, value2);
    // Read only value3 -> value3
    smg =
        checkReadExpectedValue(
            testObject, objectSizeQuartered.multiply(BigInteger.TWO), objectSizeQuartered, value3);
    // Read only value4 -> value4
    smg =
        checkReadExpectedValue(
            testObject,
            objectSizeQuartered.multiply(BigInteger.valueOf(3)),
            objectSizeQuartered,
            value4);
  }

  /**
   * Write an object to the smg, write the field of the object to 0, write 3 values in several
   * positions. Read the values/zero/in between repeatedly. Expected: No changes in the reads of the
   * written values or zero. Further every new value read must be consistent if the (exact same)
   * field is read multiple times!
   */
  @Test
  public void repeatedReadValueTest() {
    SMGObject testObject = createRegion(BigInteger.valueOf(256));
    smg = smg.copyAndAddObject(testObject);
    smg = smg.writeValue(testObject, BigInteger.ZERO, testObject.getSize(), SMGValue.zeroValue());
    smg = smg.writeValue(testObject, BigInteger.valueOf(8), BigInteger.valueOf(8), value1);
    smg = smg.writeValue(testObject, BigInteger.valueOf(4 * 8), BigInteger.valueOf(8), value2);
    smg = smg.writeValue(testObject, BigInteger.valueOf(16 * 8), BigInteger.valueOf(8), value3);
    // Read exactly the values
    smg = checkReadExpectedValue(testObject, BigInteger.valueOf(8), BigInteger.valueOf(8), value1);
    smg =
        checkReadExpectedValue(
            testObject, BigInteger.valueOf(4 * 8), BigInteger.valueOf(8), value2);
    smg =
        checkReadExpectedValue(
            testObject, BigInteger.valueOf(16 * 8), BigInteger.valueOf(8), value3);
    // Read exactly the zero values in between the values
    smg = checkReadZeroValue(testObject, BigInteger.ZERO, BigInteger.valueOf(8));
    smg = checkReadZeroValue(testObject, BigInteger.valueOf(2 * 8), BigInteger.valueOf(2 * 8));
    smg = checkReadZeroValue(testObject, BigInteger.valueOf(5 * 8), BigInteger.valueOf(11 * 8));
    smg = checkReadZeroValue(testObject, BigInteger.valueOf(17 * 8), BigInteger.valueOf(14 * 8));

    // Read a new value with a field from [0;16); covering value1 (we want to save the new value to
    // compare it later)
    SMGandValue readReinterpreation0to16 =
        checkReadUnknownValue(testObject, BigInteger.ZERO, BigInteger.valueOf(2 * 8));
    smg = readReinterpreation0to16.getSMG();
    SMGValue newValueInSMG0to16 = readReinterpreation0to16.getValue();

    // Read in the field [0;32) resulting in another new value
    SMGandValue readReinterpreation0to32 =
        checkReadUnknownValue(testObject, BigInteger.ZERO, BigInteger.valueOf(4 * 8));
    SMGValue newValueInSMG0to32 = readReinterpreation0to32.getValue();
    smg = readReinterpreation0to32.getSMG();

    // We now read 0 to 16 again, which should return the value from before and not change anything
    // in the smg
    smg =
        checkReadExpectedValue(
            testObject, BigInteger.ZERO, BigInteger.valueOf(2 * 8), newValueInSMG0to16);

    // Read from 0 to 17 -> new value
    SMGandValue readReinterpreation0to17 =
        checkReadUnknownValue(testObject, BigInteger.ZERO, BigInteger.valueOf(2 * 8 + 1));
    smg = readReinterpreation0to17.getSMG();
    SMGValue newValueInSMG0to17 = readReinterpreation0to17.getValue();

    // We now read 0 to 16 again, no change in the smg
    smg =
        checkReadExpectedValue(
            testObject, BigInteger.ZERO, BigInteger.valueOf(2 * 8), newValueInSMG0to16);
    // Read 0 to 32 again, the smg does not change
    smg =
        checkReadExpectedValue(
            testObject, BigInteger.ZERO, BigInteger.valueOf(4 * 8), newValueInSMG0to32);
    // Read 0 to 17 again, nothing changes
    smg =
        checkReadExpectedValue(
            testObject, BigInteger.ZERO, BigInteger.valueOf(2 * 8 + 1), newValueInSMG0to17);
  }

  /**
   * Write an object to the smg, write the field of the object to 0, read different sized parts of
   * it (should always be 0).
   */
  @Test
  public void readSingleZeroValueTest() {
    SMGObject testObject = createRegion(BigInteger.valueOf(256));
    smg = smg.copyAndAddObject(testObject);
    smg = smg.writeValue(testObject, BigInteger.ZERO, testObject.getSize(), SMGValue.zeroValue());
    // Read 1 Byte in the very beginning
    smg = checkReadZeroValue(testObject, BigInteger.ZERO, BigInteger.valueOf(8));
    // Read 1 Byte at the very end
    smg = checkReadZeroValue(testObject, BigInteger.valueOf(31 * 8), BigInteger.valueOf(8));
    // Read the entire field (32 Byte)
    smg = checkReadZeroValue(testObject, BigInteger.ZERO, BigInteger.valueOf(32 * 8));
  }

  /**
   * When reading, as long as the field one is reading is covered in zero values (even with multiple
   * edges) it is read as zero.
   */
  @Test
  public void readMulitpleZeroValuesTest() {
    SMGObject testObject = createRegion(BigInteger.valueOf(256));
    smg = smg.copyAndAddObject(testObject);
    // Write the entire SMG to the zero value in 1 Byte blocks
    for (long i = 0; i < 32; i++) {
      smg =
          smg.writeValue(
              testObject, BigInteger.valueOf(i * 8), BigInteger.valueOf(8), SMGValue.zeroValue());
    }
    // Read 1 Byte in the very beginning
    smg = checkReadZeroValue(testObject, BigInteger.ZERO, BigInteger.valueOf(8));
    // Read 2 Byte at the very beginning
    smg = checkReadZeroValue(testObject, BigInteger.ZERO, BigInteger.valueOf(2 * 8));
    // Read 10 Byte at the very beginning
    smg = checkReadZeroValue(testObject, BigInteger.ZERO, BigInteger.valueOf(10 * 8));
    // Read 10 Byte at the very end
    smg = checkReadZeroValue(testObject, BigInteger.valueOf(21 * 8), BigInteger.valueOf(10 * 8));
    // Read the entire field (32 Byte)
    smg = checkReadZeroValue(testObject, BigInteger.ZERO, BigInteger.valueOf(32 * 8));
  }

  /**
   * Write everything to 0 in Byte blocks but leave out every 4th block (starting with block #4).
   * Read the blocks in different combinations. TODO: we should talk about undefined fields! The C99
   * standard states that as long as a array is uninizialized, it reads whatever is in its memory.
   * We essentially do that currently by returning some value.
   */
  @Test
  public void readUndefinedTest() {
    SMGObject testObject = createRegion(BigInteger.valueOf(256));
    smg = smg.copyAndAddObject(testObject);
    // Write the SMG to the zero value in 1 Byte blocks except every 4th Byte
    for (long i = 0; i < 32; i++) {
      if (i % 4 != 3) {
        smg =
            smg.writeValue(
                testObject, BigInteger.valueOf(i * 8), BigInteger.valueOf(8), SMGValue.zeroValue());
      }
    }
    // Read 3 Byte at the very beginning = 0 value; The 4th Byte is undefined
    smg = checkReadZeroValue(testObject, BigInteger.ZERO, BigInteger.valueOf(3 * 8));
    // Read 4 Bytes; This reads the undefined block -> new value
    smg = checkReadUnknownValue(testObject, BigInteger.ZERO, BigInteger.valueOf(4 * 8)).getSMG();
    // Read the first 3 Bytes + 1 bit of the undefined block
    smg =
        checkReadUnknownValue(testObject, BigInteger.ZERO, BigInteger.valueOf(3 * 8 + 1)).getSMG();
    // Read only exactly the first undefined block
    smg =
        checkReadUnknownValue(testObject, BigInteger.valueOf(3 * 8), BigInteger.valueOf(8))
            .getSMG();
    // Read the first 7 Bytes; with the undefined block in between 0 values
    smg = checkReadUnknownValue(testObject, BigInteger.ZERO, BigInteger.valueOf(7 * 8)).getSMG();
    // Read 3 Bytes at the very end, excluding the last byte = 0 value
    smg = checkReadZeroValue(testObject, BigInteger.valueOf(28 * 8), BigInteger.valueOf(3 * 8));
    // Read the last 4 Bytes
    smg =
        checkReadUnknownValue(testObject, BigInteger.valueOf(28 * 8), BigInteger.valueOf(4 * 8))
            .getSMG();
    // Read the entire field (32 Byte), reading multiple undefined blocks
    smg = checkReadUnknownValue(testObject, BigInteger.ZERO, BigInteger.valueOf(32 * 8)).getSMG();
  }

  /**
   * Write everything to 0 in bits. Insert 1 bit value at the offset 31 bits and 254. Read 0 blocks
   * in between the value blocks (= 0 value), over the value blocks (= new value) and read exactly
   * the value bits.
   */
  @Test
  public void readValuesWithBitPrecisionTest() {
    SMGObject testObject = createRegion(BigInteger.valueOf(256));
    smg = smg.copyAndAddObject(testObject);
    // Write the SMG to the zero value in bit blocks
    for (int i = 0; i < 256; i++) {
      smg = smg.writeValue(testObject, BigInteger.valueOf(i), BigInteger.ONE, SMGValue.zeroValue());
    }
    // Write values
    smg = smg.writeValue(testObject, BigInteger.valueOf(31), BigInteger.ONE, value1);
    smg = smg.writeValue(testObject, BigInteger.valueOf(254), BigInteger.ONE, value2);
    // Read up until value1 but not the value1 bit
    smg = checkReadZeroValue(testObject, BigInteger.ZERO, BigInteger.valueOf(31));
    // Read including the value1 bit
    smg = checkReadUnknownValue(testObject, BigInteger.ZERO, BigInteger.valueOf(32)).getSMG();
    // Read beyond the value1 bit
    smg = checkReadUnknownValue(testObject, BigInteger.ZERO, BigInteger.valueOf(64)).getSMG();
    // Read only the value1 bit
    smg = checkReadExpectedValue(testObject, BigInteger.valueOf(31), BigInteger.ONE, value1);
    // Read beginning from value1 until some point with nothing but zeros
    smg =
        checkReadUnknownValue(testObject, BigInteger.valueOf(31), BigInteger.valueOf(32)).getSMG();
    // Read beginning 1 bit after value1 up until value2 (non uncluding value2)
    smg = checkReadZeroValue(testObject, BigInteger.valueOf(32), BigInteger.valueOf(222));
    // Read only the value2 bit
    smg = checkReadExpectedValue(testObject, BigInteger.valueOf(254), BigInteger.ONE, value2);
    // Read the last 2 bits
    smg = checkReadUnknownValue(testObject, BigInteger.valueOf(254), BigInteger.TWO).getSMG();
    // Read the last 3 bits
    smg =
        checkReadUnknownValue(testObject, BigInteger.valueOf(253), BigInteger.valueOf(3)).getSMG();
    // Read the entire object size
    smg = checkReadUnknownValue(testObject, BigInteger.ZERO, BigInteger.valueOf(256)).getSMG();
  }

  /**
   * Check that the read reinterpretation for the entered SMGObject, offset and size equals the
   * expected SMGValue.
   */
  private SMG checkReadExpectedValue(
      SMGObject testObject, BigInteger offset, BigInteger size, SMGValue expectedValue) {
    if (!expectedValue.isZero()) {
      // Check that there is a HasValueEdge for the object, offset, size combination with the value.
      // This does not work for zero values as those are reinterpreted if they are split up. (Normal
      // values are not!)
      assertThat(smg.getEdges(testObject))
          .contains(new SMGHasValueEdge(expectedValue, offset, size));
    }
    Set<SMGValue> allValuesBeforeRead = smg.getValues();
    SMGandValue readReinterpretation = smg.readValue(testObject, offset, size);
    SMG newSMG = readReinterpretation.getSMG();

    assertThat(readReinterpretation.getValue()).isEqualTo(expectedValue);
    // Check that no new values are introduced by the read
    assertThat(newSMG.getValues()).isEqualTo(allValuesBeforeRead);
    return newSMG;
  }

  /**
   * Check that the read reinterpretation for the entered SMGObject, offset and size equals the zero
   * value.
   */
  private SMG checkReadZeroValue(SMGObject testObject, BigInteger offset, BigInteger size) {
    return checkReadExpectedValue(testObject, offset, size, SMGValue.zeroValue());
  }

  /**
   * Check that the read reinterpretation for the entered SMGObject, offset and size generates a new
   * SMGValue that was not present before. The smg is not changed here as it might be that one wants
   * to keep the old etc.
   */
  private SMGandValue checkReadUnknownValue(
      SMGObject testObject, BigInteger offset, BigInteger size) {
    Set<SMGValue> allValuesBeforeRead = smg.getValues();
    SMGandValue readReinterpretation = smg.readValue(testObject, offset, size);
    PersistentSet<SMGHasValueEdge> oldEdgesForObject =
        (PersistentSet<SMGHasValueEdge>) smg.getEdges(testObject);
    SMG newSMG = readReinterpretation.getSMG();

    // Check that there is 1 additional value
    assertThat(newSMG.getValues()).hasSize(allValuesBeforeRead.size() + 1);
    // Check that the new value is indeed the value that is read
    SMGValue newValue =
        Sets.difference(newSMG.getValues(), allValuesBeforeRead).stream().findFirst().orElseThrow();
    assertThat(readReinterpretation.getValue()).isEqualTo(newValue);
    // Assert that there is now a new SMGHasValueEdge for the field with the new value.
    // Since its sets, it doesn't matter if this is read repeatedly.
    assertThat(newSMG.getEdges(testObject))
        .isEqualTo(oldEdgesForObject.addAndCopy(new SMGHasValueEdge(newValue, offset, size)));
    return readReinterpretation;
  }
}
