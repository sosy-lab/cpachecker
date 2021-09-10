// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
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
    // Assert empty SMG
    assertThat(smg.getPTEdges().toList()).isEqualTo(ImmutableList.of(nullPointer));
    assertTrue(smg.getHVEdges().toList().isEmpty());
    assertThat(smg.getValues()).isEqualTo(PersistentSet.of(SMGValue.zeroValue()));
    assertThat(smg.getObjects()).isEqualTo(PersistentSet.of(SMGObject.nullInstance()));

    // Add a SMGObject and assert again
    SMGObject testObject = createRegion(BigInteger.valueOf(256));
    smg = smg.copyAndAddObject(testObject);

    // TODO: Determine if new Objects should automatically recieve HVEdges to the zero value!
    assertThat(smg.getPTEdges().toList()).isEqualTo(ImmutableList.of(nullPointer));
    assertTrue(smg.getHVEdges().toList().isEmpty());
    assertThat(smg.getValues()).isEqualTo(PersistentSet.of(SMGValue.zeroValue()));
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
    assertThat(smg.getPTEdges().toList()).isEqualTo(ImmutableList.of(nullPointer));
    // Assert that now the region of the testObject is coverd in a nullyfied block completely
    assertThat(smg.getHVEdges().toList()).containsExactlyElementsIn(expectedEdges);
    assertThat(smg.getValues())
        .isEqualTo(PersistentSet.of(SMGValue.zeroValue()).addAndCopy(value1));
    assertThat(smg.getValues().size()).isEqualTo(2);
    assertThat(smg.getObjects())
        .isEqualTo(PersistentSet.of(SMGObject.nullInstance()).addAndCopy(testObject));
    // Check that only the expected HVEdges exists for the object
    assertThat(smg.getEdges(testObject)).containsExactlyElementsIn(expectedEdges);
    assertThat(smg.getEdges(testObject).size()).isEqualTo(expectedEdges.size());

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
    assertThat(smg.getPTEdges().toList()).isEqualTo(ImmutableList.of(nullPointer));
    // Assert that there are only the 4 HVEdges in total
    assertThat(smg.getHVEdges().toList()).containsExactlyElementsIn(expectedEdges);
    // Assert that there are only the zero value and value1 and2
    assertThat(smg.getValues())
        .isEqualTo(PersistentSet.of(SMGValue.zeroValue()).addAndCopy(value1).addAndCopy(value2));
    // Assert that there is still only our testObject
    assertThat(smg.getObjects())
        .isEqualTo(PersistentSet.of(SMGObject.nullInstance()).addAndCopy(testObject));
    // Check that only the expected HVEdges exists for the SMGObject
    assertThat(smg.getEdges(testObject)).isEqualTo(expectedEdges);
    assertThat(smg.getEdges(testObject).size()).isEqualTo(expectedEdges.size());
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
    assertTrue(!smg.getEdges(testObject1).isEmpty());
    assertTrue(smg.getEdges(testObject2).isEmpty());
    smg = smg.writeValue(testObject2, BigInteger.ZERO, testObject2.getSize(), SMGValue.zeroValue());
    assertTrue(!smg.getEdges(testObject1).isEmpty());
    assertTrue(!smg.getEdges(testObject2).isEmpty());
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
        new SMGHasValueEdge(
            SMGValue.zeroValue(), BigInteger.ZERO, BigInteger.valueOf(3 * 8));

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
    assertThat(smg.getPTEdges().toList()).isEqualTo(ImmutableList.of(nullPointer));
    assertThat(smg.getValues())
        .isEqualTo(PersistentSet.of(SMGValue.zeroValue()).addAndCopy(value1).addAndCopy(value2).addAndCopy(value3));
    assertThat(smg.getObjects())
        .isEqualTo(PersistentSet.of(SMGObject.nullInstance()).addAndCopy(testObject1).addAndCopy(testObject2));
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

    assertThat(smg.getPTEdges().toList()).isEqualTo(ImmutableList.of(nullPointer));
    assertThat(smg.getValues())
        .isEqualTo(
            PersistentSet.of(SMGValue.zeroValue())
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

  // TODO: this test with multiple values in the size of the object
  /*
   * Test adding a SMGObject to an empty SMG and then write the entire region of the object to 0.
   */
  @Test
  public void writeZeroValueForEntireObjectTest() {
    final BigInteger sizeInBitsOfObject = BigInteger.valueOf(256);
    SMGObject testObject = createRegion(sizeInBitsOfObject);

    // Assert that the smg with the object does not have any values/edges etc.
    assertThat(smg.getPTEdges().toList()).isEqualTo(ImmutableList.of(nullPointer));
    assertTrue(smg.getHVEdges().toList().isEmpty());
    assertThat(smg.getValues()).isEqualTo(PersistentSet.of(SMGValue.zeroValue()));
    assertThat(smg.getObjects()).isEqualTo(PersistentSet.of(SMGObject.nullInstance()));

    smg = smg.copyAndAddObject(testObject);
    assertThat(smg.getObjects())
        .isEqualTo(PersistentSet.of(SMGObject.nullInstance()).addAndCopy(testObject));

    // Write everything to 0
    // TODO: should this happen automatically?
    smg = smg.writeValue(testObject, BigInteger.ZERO, testObject.getSize(), SMGValue.zeroValue());

    SMGHasValueEdge expectedEdge =
        new SMGHasValueEdge(SMGValue.zeroValue(), BigInteger.ZERO, sizeInBitsOfObject);

    PersistentSet<SMGHasValueEdge> expectedEdges =
        PersistentSet.<SMGHasValueEdge>of().addAndCopy(expectedEdge);

    // Check that nothing changed for the points to edges
    assertThat(smg.getPTEdges().toList()).isEqualTo(ImmutableList.of(nullPointer));
    // Assert that now the region of the testObject is coverd in a nullyfied block completely
    assertThat(smg.getHVEdges().toList()).isEqualTo(ImmutableList.of(expectedEdge));
    assertThat(smg.getValues()).isEqualTo(PersistentSet.of(SMGValue.zeroValue()));
    assertThat(smg.getObjects())
        .isEqualTo(PersistentSet.of(SMGObject.nullInstance()).addAndCopy(testObject));
    // Check that only the expected HVEdges exists for the object
    assertThat(smg.getEdges(testObject)).isEqualTo(expectedEdges);
    assertThat(smg.getEdges(testObject).size()).isEqualTo(expectedEdges.size());
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
    assertThat(smg.getPTEdges().toList()).isEqualTo(ImmutableList.of(nullPointer));
    // Check that the HVEdge exists
    assertThat(smg.getHVEdges().toList().size()).isEqualTo(2);
    assertThat(smg.getHVEdges().toList()).contains(expectedValueEdge);
    assertThat(smg.getHVEdges().toList()).contains(expectedZeroEdge);
    assertThat(smg.getValues())
        .isEqualTo(PersistentSet.of(SMGValue.zeroValue()).addAndCopy(value1));
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
    assertThat(smg.getPTEdges().toList()).isEqualTo(ImmutableList.of(nullPointer));
    // Check that the HVEdge exists
    assertThat(smg.getHVEdges().toList().size()).isEqualTo(2);
    assertThat(smg.getHVEdges().toList()).contains(expectedValueEdge);
    assertThat(smg.getHVEdges().toList()).contains(expectedZeroEdge);
    assertThat(smg.getValues())
        .isEqualTo(PersistentSet.of(SMGValue.zeroValue()).addAndCopy(value1));
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
    assertThat(smg.getPTEdges().toList()).isEqualTo(ImmutableList.of(nullPointer));
    // Assert that now the region of the testObject is coverd in the 2 value1 & 2 edges
    assertThat(smg.getHVEdges().toList()).containsExactlyElementsIn(expectedEdges);
    assertThat(smg.getValues())
        .isEqualTo(PersistentSet.of(SMGValue.zeroValue()).addAndCopy(value1).addAndCopy(value2));
    assertThat(smg.getObjects())
        .isEqualTo(PersistentSet.of(SMGObject.nullInstance()).addAndCopy(testObject));
    // Check that only the expected HVEdges exists for the object
    assertThat(smg.getEdges(testObject)).containsExactlyElementsIn(expectedEdges);
    assertThat(smg.getEdges(testObject).size()).isEqualTo(expectedEdges.size());
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
    assertThat(smg.getPTEdges().toList()).isEqualTo(ImmutableList.of(nullPointer));
    // Assert that now the region of the testObject is coverd in the 2 value1 & 2 edges
    assertThat(smg.getHVEdges().toList()).containsExactlyElementsIn(expectedEdges);
    // Values are never deleted!
    assertThat(smg.getValues())
        .isEqualTo(
            PersistentSet.of(SMGValue.zeroValue())
                .addAndCopy(value1)
                .addAndCopy(value2)
                .addAndCopy(value3));
    assertThat(smg.getObjects())
        .isEqualTo(PersistentSet.of(SMGObject.nullInstance()).addAndCopy(testObject));
    // Check that only the expected HVEdges exists for the object
    assertThat(smg.getEdges(testObject)).containsExactlyElementsIn(expectedEdges);
    assertThat(smg.getEdges(testObject).size()).isEqualTo(expectedEdges.size());
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
    assertThat(smg.getPTEdges().toList()).isEqualTo(ImmutableList.of(nullPointer));
    // Assert that now the region of the testObject is coverd in the 2 value1 & 2 edges
    assertThat(smg.getHVEdges().toList()).containsExactlyElementsIn(expectedEdges);
    // Values are never deleted!
    assertThat(smg.getValues())
        .isEqualTo(
            PersistentSet.of(SMGValue.zeroValue())
                .addAndCopy(value1)
                .addAndCopy(value2)
                .addAndCopy(value3));
    assertThat(smg.getObjects())
        .isEqualTo(PersistentSet.of(SMGObject.nullInstance()).addAndCopy(testObject));
    // Check that only the expected HVEdges exists for the object
    assertThat(smg.getEdges(testObject)).containsExactlyElementsIn(expectedEdges);
    assertThat(smg.getEdges(testObject).size()).isEqualTo(expectedEdges.size());
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
    assertThat(smg.getPTEdges().toList()).isEqualTo(ImmutableList.of(nullPointer));
    // Assert that now the region of the testObject is coverd in the 2 value1 & 2 edges
    assertThat(smg.getHVEdges().toList()).containsExactlyElementsIn(expectedEdges);
    // Values are never deleted!
    assertThat(smg.getValues())
        .isEqualTo(
            PersistentSet.of(SMGValue.zeroValue())
                .addAndCopy(value1)
                .addAndCopy(value2)
                .addAndCopy(value3));
    assertThat(smg.getObjects())
        .isEqualTo(PersistentSet.of(SMGObject.nullInstance()).addAndCopy(testObject));
    // Check that only the expected HVEdges exists for the object
    assertThat(smg.getEdges(testObject)).containsExactlyElementsIn(expectedEdges);
    assertThat(smg.getEdges(testObject).size()).isEqualTo(expectedEdges.size());
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
    assertThat(smg.getPTEdges().toList()).isEqualTo(ImmutableList.of(nullPointer));
    // Assert that now the region of the testObject is coverd in the 2 value1 & 2 edges
    assertThat(smg.getHVEdges().toList()).containsExactlyElementsIn(expectedEdges);
    // Values are never deleted!
    assertThat(smg.getValues())
        .isEqualTo(
            PersistentSet.of(SMGValue.zeroValue())
                .addAndCopy(value1)
                .addAndCopy(value2)
                .addAndCopy(value3));
    assertThat(smg.getObjects())
        .isEqualTo(PersistentSet.of(SMGObject.nullInstance()).addAndCopy(testObject));
    // Check that only the expected HVEdges exists for the object
    assertThat(smg.getEdges(testObject)).containsExactlyElementsIn(expectedEdges);
    assertThat(smg.getEdges(testObject).size()).isEqualTo(expectedEdges.size());
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
    assertThat(smg.getPTEdges().toList()).isEqualTo(ImmutableList.of(nullPointer));
    // Assert that now the region of the testObject is coverd in the 2 value1 & 2 edges
    assertThat(smg.getHVEdges().toList()).containsExactlyElementsIn(expectedEdges);
    // Values are never deleted!
    assertThat(smg.getValues())
        .isEqualTo(
            PersistentSet.of(SMGValue.zeroValue())
                .addAndCopy(value1)
                .addAndCopy(value2)
                .addAndCopy(value3));
    assertThat(smg.getObjects())
        .isEqualTo(PersistentSet.of(SMGObject.nullInstance()).addAndCopy(testObject));
    // Check that only the expected HVEdges exists for the object
    assertThat(smg.getEdges(testObject)).containsExactlyElementsIn(expectedEdges);
    assertThat(smg.getEdges(testObject).size()).isEqualTo(expectedEdges.size());
  }

  // Try writing a value that goes beyond the testObjects field
  @Test(expected = AssertionError.class)
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
  @Test(expected = AssertionError.class)
  public void writeBeyondRangeTest2() {
    SMGObject testObject = createRegion(BigInteger.valueOf(256));
    smg = smg.copyAndAddObject(testObject);

    smg = smg.writeValue(testObject, testObject.getSize(), BigInteger.ONE, SMGValue.zeroValue());
  }

  // Try writing a value that goes beyond the testObjects field
  @Test(expected = AssertionError.class)
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
  @Test(expected = AssertionError.class)
  public void readBeyondRangeTest() {
    SMGObject testObject = createRegion(BigInteger.valueOf(256));
    smg = smg.copyAndAddObject(testObject);

    smg = smg.writeValue(testObject, BigInteger.ZERO, testObject.getSize(), SMGValue.zeroValue());
    smg.readValue(testObject, BigInteger.ZERO, testObject.getSize().add(BigInteger.ONE));
  }

  // Try reading a value that goes beyond the testObjects field
  @Test(expected = AssertionError.class)
  public void readBeyondRangeTest2() {
    SMGObject testObject = createRegion(BigInteger.valueOf(256));
    smg = smg.copyAndAddObject(testObject);

    smg = smg.writeValue(testObject, BigInteger.ZERO, testObject.getSize(), SMGValue.zeroValue());
    smg.readValue(testObject, testObject.getSize(), BigInteger.ONE);
  }

  // Try reading a value that goes beyond the testObjects field
  @Test(expected = AssertionError.class)
  public void readBeyondRangeTest3() {
    SMGObject testObject = createRegion(BigInteger.valueOf(256));
    smg = smg.copyAndAddObject(testObject);

    smg = smg.writeValue(testObject, BigInteger.ZERO, testObject.getSize(), SMGValue.zeroValue());
    smg.readValue(testObject, testObject.getSize().subtract(BigInteger.ONE), BigInteger.TWO);
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
    SMGandValue readReinterpretation1 =
        smg.readValue(testObject, BigInteger.ZERO, BigInteger.valueOf(8));
    // Read 1 Byte at the very end
    SMGandValue readReinterpretation2 =
        smg.readValue(testObject, BigInteger.valueOf(31 * 8), BigInteger.valueOf(8));
    // Read the entire field (32 Byte)
    SMGandValue readReinterpretation3 =
        smg.readValue(testObject, BigInteger.ZERO, BigInteger.valueOf(32 * 8));

    assertTrue(readReinterpretation1.getValue().isZero());
    assertTrue(readReinterpretation2.getValue().isZero());
    assertTrue(readReinterpretation3.getValue().isZero());
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
    for (int i = 0; i < 32; i++) {
      smg =
          smg.writeValue(
              testObject, BigInteger.valueOf(i * 8), BigInteger.valueOf(8), SMGValue.zeroValue());
    }
    // Read 1 Byte in the very beginning
    SMGandValue readReinterpretation1 =
        smg.readValue(testObject, BigInteger.ZERO, BigInteger.valueOf(8));
    // Read 2 Byte at the very beginning
    SMGandValue readReinterpretation2 =
        smg.readValue(testObject, BigInteger.ZERO, BigInteger.valueOf(2 * 8));
    // Read 10 Byte at the very beginning
    SMGandValue readReinterpretation3 =
        smg.readValue(testObject, BigInteger.ZERO, BigInteger.valueOf(10 * 8));
    // Read 10 Byte at the very end
    SMGandValue readReinterpretation4 =
        smg.readValue(testObject, BigInteger.valueOf(21 * 8), BigInteger.valueOf(10 * 8));
    // Read the entire field (32 Byte)
    SMGandValue readReinterpretation5 =
        smg.readValue(testObject, BigInteger.ZERO, BigInteger.valueOf(32 * 8));

    assertTrue(readReinterpretation1.getValue().isZero());
    assertTrue(readReinterpretation2.getValue().isZero());
    assertTrue(readReinterpretation3.getValue().isZero());
    assertTrue(readReinterpretation4.getValue().isZero());
    assertTrue(readReinterpretation5.getValue().isZero());
  }

  /**
   * When reading, as long as the field one is reading is covered in zero values (even with multiple
   * edges) it is read as zero.
   */
  @Test
  public void readMulitpleZeroValuesWithUndefinedTest() {
    SMGObject testObject = createRegion(BigInteger.valueOf(256));
    smg = smg.copyAndAddObject(testObject);
    // Write the SMG to the zero value in 1 Byte blocks except every 4th Byte
    for (int i = 0; i < 32; i++) {
      if (i % 4 != 0 || i == 0) {
        smg =
            smg.writeValue(
                testObject, BigInteger.valueOf(i * 8), BigInteger.valueOf(8), SMGValue.zeroValue());
      }
    }
    // Read 3 Byte at the very beginning; The 4th Byte is undefined
    SMGandValue readReinterpretation1 =
        smg.readValue(testObject, BigInteger.ZERO, BigInteger.valueOf(3 * 8));
    // Read 4 Bytes; This reads the undefined block
    SMGandValue readReinterpretation2 =
        smg.readValue(testObject, BigInteger.ZERO, BigInteger.valueOf(10 * 8));
    // Read 10 Byte at the very end
    SMGandValue readReinterpretation4 =
        smg.readValue(testObject, BigInteger.valueOf(21 * 8), BigInteger.valueOf(10 * 8));
    // Read the entire field (32 Byte)
    SMGandValue readReinterpretation5 =
        smg.readValue(testObject, BigInteger.ZERO, BigInteger.valueOf(32 * 8));

    assertTrue(readReinterpretation1.getValue().isZero());
    assertTrue(!readReinterpretation2.getValue().isZero());
    assertTrue(!readReinterpretation4.getValue().isZero());
    assertTrue(!readReinterpretation5.getValue().isZero());
  }
}
