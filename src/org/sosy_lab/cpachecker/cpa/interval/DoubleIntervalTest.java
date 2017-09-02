/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  icensed under the Apache icense, Version 2.0 (the "icense");
 *  you may not use this file except in compliance with the icense.
 *  You may obtain a copy of the icense at
 *
 *      http://www.apache.org/licenses/ICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the icense is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the icense for the specific language governing permissions and
 *  limitations under the icense.
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

public class DoubleIntervalTest {
    @Test
    public void testEquals(){
        DoubleInterval i11 = new DoubleInterval(1.0, 1.0);
        DoubleInterval i12 = new DoubleInterval(1.0, 2.0);
        DoubleInterval i11_ref = i11;
        DoubleInterval inn = new DoubleInterval(null, null);

        assertTrue(i11.equals(i11_ref));
        assertFalse(i11.equals(i12));
        assertFalse(i11.equals(inn));
    }

    @Test
    public void testUnion(){
        DoubleInterval i11 = new DoubleInterval(1.0, 1.0);
        DoubleInterval i12 = new DoubleInterval(1.0, 2.0);
        DoubleInterval i23 = new DoubleInterval(2.0, 3.0);
        DoubleInterval inn = new DoubleInterval(null, null);

        assertEquals(DoubleInterval.EMPTY, inn.union(i12));
        assertEquals(i12, i12.union(i11));
        assertEquals(i12, i11.union(i12));
        assertEquals(new DoubleInterval(1.0, 3.0), i11.union(i23));
    }

    @Test
    public void testIntersect(){
        DoubleInterval i11 = new DoubleInterval(1.0, 1.0);
        DoubleInterval i12 = new DoubleInterval(1.0, 2.0);
        DoubleInterval i23 = new DoubleInterval(2.0, 3.0);
        DoubleInterval inn = new DoubleInterval(null, null);

        assertEquals(DoubleInterval.EMPTY, inn.intersect(i12));
        assertEquals(i11, i12.intersect(i11));
        assertEquals(i11, i11.intersect(i12));
        assertEquals(DoubleInterval.EMPTY, i11.intersect(i23));
    }

    @Test
    public void testIntersects(){
        DoubleInterval i11 = new DoubleInterval(1.0, 1.0);
        DoubleInterval i12 = new DoubleInterval(1.0, 2.0);
        DoubleInterval i23 = new DoubleInterval(2.0, 3.0);
        DoubleInterval inn = new DoubleInterval(null, null);

      assertTrue(i11.intersects(i12));
      assertFalse(i11.intersects(i23));
      assertFalse(inn.intersects(i11));
      assertFalse(i11.intersects(inn));
    }

    @Test
    public void testIsGreaterThan(){
        DoubleInterval i11 = new DoubleInterval(1.0, 1.0);
        DoubleInterval i23 = new DoubleInterval(2.0, 3.0);
        DoubleInterval inn = new DoubleInterval(null, null);

        assertTrue(i23.isGreaterThan(i11));
        assertFalse(i11.isGreaterThan(i23));
        assertFalse(inn.isGreaterThan(i11));
        assertFalse(i11.isGreaterThan(inn));
    }

    @Test
    public void testIsGreaterOrEqualThan() {
        DoubleInterval i11 = new DoubleInterval(1.0, 1.0);
        DoubleInterval i12 = new DoubleInterval(1.0, 2.0);
        DoubleInterval i23 = new DoubleInterval(2.0, 3.0);
        DoubleInterval inn = new DoubleInterval(null, null);

        assertTrue(i23.isGreaterOrEqualThan(i11));
        assertTrue(i12.isGreaterOrEqualThan(i11));
        assertFalse(i11.isGreaterOrEqualThan(i23));
        assertFalse(inn.isGreaterOrEqualThan(i11));
        assertFalse(i11.isGreaterOrEqualThan(inn));
    }

    @Test
    public void testMayBeGreaterThan(){
        DoubleInterval i11 = new DoubleInterval(1.0, 1.0);
        DoubleInterval i23 = new DoubleInterval(2.0, 3.0);
        DoubleInterval inn = new DoubleInterval(null, null);

        assertTrue(i23.mayBeGreaterThan(i11));
        assertFalse(i11.mayBeGreaterThan(i23));
        assertFalse(inn.mayBeGreaterThan(i11));
        assertTrue(i11.mayBeGreaterThan(inn));
    }

    @Test
    public void testMayBeGreaterOrEqualThan(){
        DoubleInterval i11 = new DoubleInterval(1.0, 1.0);
        DoubleInterval i12 = new DoubleInterval(1.0, 2.0);
        DoubleInterval i23 = new DoubleInterval(2.0, 3.0);
        DoubleInterval inn = new DoubleInterval(null, null);

        assertTrue(i23.mayBeGreaterOrEqualThan(i11));
        assertTrue(i12.mayBeGreaterOrEqualThan(i23));
        assertTrue(i11.mayBeGreaterOrEqualThan(inn));
        assertFalse(i11.mayBeGreaterOrEqualThan(i23));
        assertFalse(inn.mayBeGreaterOrEqualThan(i11));

    }

    @Test
    public void testModulo() {
        DoubleInterval i11 = new DoubleInterval(1.0, 1.0);
        DoubleInterval i00 = DoubleInterval.ZERO;
        DoubleInterval i22 = new DoubleInterval(2.0, 2.0);
        DoubleInterval iExpected = new DoubleInterval(1.0, 1.0);

        assertEquals(DoubleInterval.UNBOUND, i11.modulo(i00));
        assertEquals(iExpected, i11.modulo(i22));
    }

    @Test
    public void testimitowerBoundBy() {
        DoubleInterval i11 = new DoubleInterval(1.0, 1.0);
        DoubleInterval i14 = new DoubleInterval(1.0, 4.0);
        DoubleInterval inn = new DoubleInterval(null, null);
        DoubleInterval i23 = new DoubleInterval(2.0, 3.0);
        DoubleInterval iExpected = new DoubleInterval(2.0, 4.0);

        assertEquals(DoubleInterval.EMPTY, inn.limitLowerBoundBy(i14));
        assertEquals(DoubleInterval.EMPTY, i14.limitLowerBoundBy(inn));
        assertEquals(DoubleInterval.EMPTY, i11.limitLowerBoundBy(i23));
        assertEquals(iExpected, i14.limitLowerBoundBy(i23));
    }

    @Test
    public void testimitUpperBoundBy()  {
        DoubleInterval i11 = new DoubleInterval(1.0, 1.0);
        DoubleInterval i14 = new DoubleInterval(1.0, 4.0);
        DoubleInterval inn = new DoubleInterval(null, null);
        DoubleInterval i23 = new DoubleInterval(2.0, 3.0);
        DoubleInterval iExpected = new DoubleInterval(1.0, 3.0);

        assertEquals(DoubleInterval.EMPTY, inn.limitUpperBoundBy(i14));
        assertEquals(DoubleInterval.EMPTY, i14.limitUpperBoundBy(inn));
        assertEquals(DoubleInterval.EMPTY,i23.limitUpperBoundBy(i11));
        assertEquals(iExpected, i14.limitUpperBoundBy(i23));
    }

    @Test
    public void testContains(){
        DoubleInterval i11 = new DoubleInterval(1.0, 1.0);
        DoubleInterval i12 = new DoubleInterval(1.0, 2.0);
        DoubleInterval i13 = new DoubleInterval(1.0, 3.0);
        DoubleInterval inn = new DoubleInterval(null, null);

        assertTrue(i11.contains(i11));
        assertTrue(i13.contains(i12));
        assertFalse(i11.contains(inn));
        assertFalse(i11.contains(i13));
        assertFalse(inn.contains(i11));
    }

    @Test
    public void testPlus() {
        DoubleInterval i11 = new DoubleInterval(1.0, 1.0);
        DoubleInterval i14 = new DoubleInterval(1.0, 4.0);
        DoubleInterval inn = new DoubleInterval(null, null);
        DoubleInterval iExpected = new DoubleInterval(2.0, 5.0);

        assertEquals(DoubleInterval.EMPTY, inn.plus(i14));
        assertEquals(DoubleInterval.EMPTY, i14.plus(inn));
        assertEquals(iExpected, i11.plus(i14));
    }

    @Test
    public void testMinus() throws Exception {
        DoubleInterval i11 = new DoubleInterval(1.0, 1.0);
        DoubleInterval i14 = new DoubleInterval(1.0, 4.0);
        DoubleInterval inn = new DoubleInterval(null, null);
        DoubleInterval iExpected = new DoubleInterval(-3.0, 0.0);

        assertEquals(DoubleInterval.EMPTY, inn.minus(i14));
        assertEquals(iExpected, i11.minus(i14));
    }

    @Test
    public void testTimes() throws Exception {
        DoubleInterval i23 = new DoubleInterval(2.0, 3.0);
        DoubleInterval i14 = new DoubleInterval(1.0, 4.0);
        DoubleInterval i_1_4 = new DoubleInterval(-4.0, -1.0);
        DoubleInterval iExpected212 = new DoubleInterval(2.0, 12.0);
        DoubleInterval iExpected_4_4 = new DoubleInterval(-16.0, -1.0);

        assertEquals(iExpected212, i23.times(i14));
        assertEquals(iExpected_4_4, i_1_4.times(i14));
    }

    @Test
    public void testDivide() throws Exception {
        DoubleInterval i22 = new DoubleInterval(2.0, 2.0);
        DoubleInterval i14 = new DoubleInterval(1.0, 4.0);
        DoubleInterval izz = DoubleInterval.ZERO;
        DoubleInterval iExpected = new DoubleInterval(0.5, 2.0);
        DoubleInterval iExpectedUnbound = DoubleInterval.UNBOUND;

        assertEquals(iExpected, i14.divide(i22));
        assertEquals(iExpectedUnbound, i22.divide(izz));
    }

    @Test
    public void testShifteft() throws Exception {
        DoubleInterval i_2_1 = new DoubleInterval(-2.0, -1.0);
        DoubleInterval i14 = new DoubleInterval(1.0, 4.0);
        DoubleInterval i_20= new DoubleInterval(-2.0, -0.0);
        DoubleInterval iExpected = new DoubleInterval(-32.0, 0.0);
        DoubleInterval iExpectedUnbound = DoubleInterval.UNBOUND;

        assertEquals(iExpectedUnbound, i14.shiftLeft(i_2_1));
        assertEquals(iExpectedUnbound, i_2_1.shiftLeft(i_20));
        assertEquals(iExpected, i_20.shiftLeft(i14));
    }

    @Test
    public void testShiftRight() throws Exception {
        DoubleInterval i_2_1 = new DoubleInterval(-2.0, -1.0);
        DoubleInterval i14 = new DoubleInterval(1.0, 4.0);
        DoubleInterval i_20= new DoubleInterval(-2.0, -0.0);
        DoubleInterval iExpected = new DoubleInterval(-1.0, 0.0);
        DoubleInterval iExpectedUnbound = DoubleInterval.UNBOUND;

        assertEquals(iExpectedUnbound, i14.shiftRight(i_2_1));
        assertEquals(iExpectedUnbound, i_2_1.shiftRight(i_20));
        assertEquals(iExpected, i_20.shiftRight(i14));
    }

}
