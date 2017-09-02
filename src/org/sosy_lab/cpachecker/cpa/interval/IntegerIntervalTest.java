/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.interval;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class IntegerIntervalTest {

    @Test
    public void testEquals(){
        IntegerInterval i11 = new IntegerInterval(1L, 1L);
        IntegerInterval i12 = new IntegerInterval(1L, 2L);
        IntegerInterval i11_ref = i11;
        IntegerInterval inn = new IntegerInterval(null, null);

        assertTrue(i11.equals(i11_ref));
        assertFalse(i11.equals(i12));
        assertFalse(i11.equals(inn));
    }

    @Test
    public void testUnion(){
        IntegerInterval i11 = new IntegerInterval(1L, 1L);
        IntegerInterval i12 = new IntegerInterval(1L, 2L);
        IntegerInterval i23 = new IntegerInterval(2L, 3L);
        IntegerInterval inn = new IntegerInterval(null, null);

        assertEquals(IntegerInterval.EMPTY, inn.union(i12));
        assertEquals(i12, i12.union(i11));
        assertEquals(i12, i11.union(i12));
        assertEquals(new IntegerInterval(1L, 3L), i11.union(i23));
    }

    @Test
    public void testIntersect(){
        IntegerInterval i11 = new IntegerInterval(1L, 1L);
        IntegerInterval i12 = new IntegerInterval(1L, 2L);
        IntegerInterval i23 = new IntegerInterval(2L, 3L);
        IntegerInterval inn = new IntegerInterval(null, null);

        assertEquals(IntegerInterval.EMPTY, inn.intersect(i12));
        assertEquals(i11, i12.intersect(i11));
        assertEquals(i11, i11.intersect(i12));
        assertEquals(IntegerInterval.EMPTY, i11.intersect(i23));
    }

    @Test
    public void testIntersects(){
        IntegerInterval i11 = new IntegerInterval(1L, 1L);
        IntegerInterval i12 = new IntegerInterval(1L, 2L);
        IntegerInterval i23 = new IntegerInterval(2L, 3L);
        IntegerInterval inn = new IntegerInterval(null, null);

      assertTrue(i11.intersects(i12));
      assertFalse(i11.intersects(i23));
      assertFalse(inn.intersects(i11));
      assertFalse(i11.intersects(inn));
    }

    @Test
    public void testIsGreaterThan(){
        IntegerInterval i11 = new IntegerInterval(1L, 1L);
        IntegerInterval i23 = new IntegerInterval(2L, 3L);
        IntegerInterval inn = new IntegerInterval(null, null);

        assertTrue(i23.isGreaterThan(i11));
        assertFalse(i11.isGreaterThan(i23));
        assertFalse(inn.isGreaterThan(i11));
        assertFalse(i11.isGreaterThan(inn));
    }

    @Test
    public void testIsGreaterOrEqualThan() {
        IntegerInterval i11 = new IntegerInterval(1L, 1L);
        IntegerInterval i12 = new IntegerInterval(1L, 2L);
        IntegerInterval i23 = new IntegerInterval(2L, 3L);
        IntegerInterval inn = new IntegerInterval(null, null);

        assertTrue(i23.isGreaterOrEqualThan(i11));
        assertTrue(i12.isGreaterOrEqualThan(i11));
        assertFalse(i11.isGreaterOrEqualThan(i23));
        assertFalse(inn.isGreaterOrEqualThan(i11));
        assertFalse(i11.isGreaterOrEqualThan(inn));
    }

    @Test
    public void testMayBeGreaterThan(){
        IntegerInterval i11 = new IntegerInterval(1L, 1L);
        IntegerInterval i23 = new IntegerInterval(2L, 3L);
        IntegerInterval inn = new IntegerInterval(null, null);

        assertTrue(i23.mayBeGreaterThan(i11));
        assertFalse(i11.mayBeGreaterThan(i23));
        assertFalse(inn.mayBeGreaterThan(i11));
        assertTrue(i11.mayBeGreaterThan(inn));
    }

    @Test
    public void testMayBeGreaterOrEqualThan(){
        IntegerInterval i11 = new IntegerInterval(1L, 1L);
        IntegerInterval i12 = new IntegerInterval(1L, 2L);
        IntegerInterval i23 = new IntegerInterval(2L, 3L);
        IntegerInterval inn = new IntegerInterval(null, null);

        assertTrue(i23.mayBeGreaterOrEqualThan(i11));
        assertTrue(i12.mayBeGreaterOrEqualThan(i23));
        assertTrue(i11.mayBeGreaterOrEqualThan(inn));
        assertFalse(i11.mayBeGreaterOrEqualThan(i23));
        assertFalse(inn.mayBeGreaterOrEqualThan(i11));

    }

    @Test
    public void testModulo() {
        IntegerInterval i11 = new IntegerInterval(1L, 1L);
        IntegerInterval i00 = IntegerInterval.ZERO;
        IntegerInterval i22 = new IntegerInterval(2L, 2L);
        IntegerInterval iExpected = new IntegerInterval(1L, 1L);

        assertEquals(IntegerInterval.UNBOUND, i11.modulo(i00));
        assertEquals(iExpected, i11.modulo(i22));
    }

    @Test
    public void testLimitLowerBoundBy() {
        IntegerInterval i11 = new IntegerInterval(1L, 1L);
        IntegerInterval i14 = new IntegerInterval(1L, 4L);
        IntegerInterval inn = new IntegerInterval(null, null);
        IntegerInterval i23 = new IntegerInterval(2L, 3L);
        IntegerInterval iExpected = new IntegerInterval(2L, 4L);

        assertEquals(IntegerInterval.EMPTY, inn.limitLowerBoundBy(i14));
        assertEquals(IntegerInterval.EMPTY, i14.limitLowerBoundBy(inn));
        assertEquals(IntegerInterval.EMPTY, i11.limitLowerBoundBy(i23));
        assertEquals(iExpected, i14.limitLowerBoundBy(i23));
    }

    @Test
    public void testLimitUpperBoundBy()  {
        IntegerInterval i11 = new IntegerInterval(1L, 1L);
        IntegerInterval i14 = new IntegerInterval(1L, 4L);
        IntegerInterval inn = new IntegerInterval(null, null);
        IntegerInterval i23 = new IntegerInterval(2L, 3L);
        IntegerInterval iExpected = new IntegerInterval(1L, 3L);

        assertEquals(IntegerInterval.EMPTY, inn.limitUpperBoundBy(i14));
        assertEquals(IntegerInterval.EMPTY, i14.limitUpperBoundBy(inn));
        assertEquals(IntegerInterval.EMPTY,i23.limitUpperBoundBy(i11));
        assertEquals(iExpected, i14.limitUpperBoundBy(i23));
    }

    @Test
    public void testContains(){
        IntegerInterval i11 = new IntegerInterval(1L, 1L);
        IntegerInterval i12 = new IntegerInterval(1L, 2L);
        IntegerInterval i13 = new IntegerInterval(1L, 3L);
        IntegerInterval inn = new IntegerInterval(null, null);

        assertTrue(i11.contains(i11));
        assertTrue(i13.contains(i12));
        assertFalse(i11.contains(inn));
        assertFalse(i11.contains(i13));
        assertFalse(inn.contains(i11));
    }

    @Test
    public void testPlus() {
        IntegerInterval i11 = new IntegerInterval(1L, 1L);
        IntegerInterval i14 = new IntegerInterval(1L, 4L);
        IntegerInterval inn = new IntegerInterval(null, null);
        IntegerInterval iExpected = new IntegerInterval(2L, 5L);

        assertEquals(IntegerInterval.EMPTY, inn.plus(i14));
        assertEquals(IntegerInterval.EMPTY, i14.plus(inn));
        assertEquals(iExpected, i11.plus(i14));
    }

    @Test
    public void testMinus() throws Exception {
        IntegerInterval i11 = new IntegerInterval(1L, 1L);
        IntegerInterval i14 = new IntegerInterval(1L, 4L);
        IntegerInterval inn = new IntegerInterval(null, null);
        IntegerInterval iExpected = new IntegerInterval(-3L, 0L);

        assertEquals(IntegerInterval.EMPTY, inn.minus(i14));
        assertEquals(iExpected, i11.minus(i14));
    }

    @Test
    public void testTimes() throws Exception {
        IntegerInterval i23 = new IntegerInterval(2L, 3L);
        IntegerInterval i14 = new IntegerInterval(1L, 4L);
        IntegerInterval i_1_4 = new IntegerInterval(-4L, -1L);
        IntegerInterval iExpected212 = new IntegerInterval(2L, 12L);
        IntegerInterval iExpected_4_4 = new IntegerInterval(Long.MIN_VALUE, -4L); //Long.MIN_VALUE

//        assertEquals(IntegerInterval.EMPTY, inn.times(i14));
        assertEquals(iExpected212, i23.times(i14));
        assertEquals(iExpected_4_4, i_1_4.times(i14));
    }

    @Test
    public void testDivide() throws Exception {
        IntegerInterval i22 = new IntegerInterval(2L, 2L);
        IntegerInterval i14 = new IntegerInterval(1L, 4L);
        IntegerInterval izz = IntegerInterval.ZERO;
        IntegerInterval iExpected = new IntegerInterval(0L, 2L);
        IntegerInterval iExpectedUnbound = IntegerInterval.UNBOUND;

        assertEquals(iExpected, i14.divide(i22));
        assertEquals(iExpectedUnbound, i22.divide(izz));
    }

    @Test
    public void testShiftLeft() throws Exception {
        IntegerInterval i_2_1 = new IntegerInterval(-2L, -1L);
        IntegerInterval i14 = new IntegerInterval(1L, 4L);
        IntegerInterval i_20= new IntegerInterval(-2L, -0L);
        IntegerInterval iExpected = new IntegerInterval(-32L, 0L);
        IntegerInterval iExpectedUnbound = IntegerInterval.UNBOUND;

        assertEquals(iExpectedUnbound, i14.shiftLeft(i_2_1));
        assertEquals(iExpectedUnbound, i_2_1.shiftLeft(i_20));
        assertEquals(iExpected, i_20.shiftLeft(i14));
    }

    @Test
    public void testShiftRight() throws Exception {
        IntegerInterval i_2_1 = new IntegerInterval(-2L, -1L);
        IntegerInterval i14 = new IntegerInterval(1L, 4L);
        IntegerInterval i_20= new IntegerInterval(-2L, -0L);
        IntegerInterval iExpected = new IntegerInterval(-1L, 0L);
        IntegerInterval iExpectedUnbound = IntegerInterval.UNBOUND;

        assertEquals(iExpectedUnbound, i14.shiftRight(i_2_1));
        assertEquals(iExpectedUnbound, i_2_1.shiftRight(i_20));
        assertEquals(iExpected, i_20.shiftRight(i14));
    }
}
