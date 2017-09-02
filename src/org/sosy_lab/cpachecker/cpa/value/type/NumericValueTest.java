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
package org.sosy_lab.cpachecker.cpa.value.type;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NumericValueTest {

    @Test
    public void testNegate() throws Exception {
        NumericValue n5 = new NumericValue(5);
        NumericValue n_5 = new NumericValue(-5);
        NumericValue ninf = new NumericValue(Double.POSITIVE_INFINITY);
        NumericValue n_inf = new NumericValue(Double.NEGATIVE_INFINITY);

        assertEquals(n_5.getNumber(), n5.negate().getNumber().intValue());
        assertEquals(n5.getNumber(), n_5.negate().getNumber().intValue());
        assertEquals(ninf.getNumber(), n_inf.negate().getNumber());
        assertEquals(n_inf.getNumber(), ninf.negate().getNumber());
    }

    @Test
    public void testPlus() throws Exception {
        NumericValue n5 = new NumericValue(5L);
        NumericValue n_5 = new NumericValue(-5L);
        NumericValue n0 = new NumericValue(0);

        assertEquals(n0.getNumber().longValue(), (n5.plus(n_5)).getNumber().longValue());
    }

    @Test
    public void testMinus() throws Exception {
        NumericValue n5 = new NumericValue(5L);
        NumericValue n_5 = new NumericValue(-5L);
        NumericValue n0 = new NumericValue(10);

        assertEquals(n0.getNumber().longValue(), (n5.minus(n_5)).getNumber().longValue());
    }

    @Test
    public void testUnsignedDivide() throws Exception {
        NumericValue n5 = new NumericValue(922337203685755807L);
        NumericValue n2 = new NumericValue(2L);
        NumericValue expected = new NumericValue(461168601842877903L);

        assertEquals(expected.getNumber().longValue(), (n5.unsignedDivide(n2)).getNumber().longValue());
    }

    @Test
    public void testDivide() throws Exception {
        NumericValue n5 = new NumericValue(922337203685755807L);
        NumericValue n2 = new NumericValue(2L);
        NumericValue expected = new NumericValue(461168601842877903L);

        assertEquals(expected.getNumber().longValue(), (n5.divide(n2)).getNumber().longValue());
    }

    @Test
    public void testTimes() throws Exception {
        NumericValue n5 = new NumericValue(10L);
        NumericValue n2 = new NumericValue(2L);
        NumericValue expected = new NumericValue(20L);

        assertEquals(expected.getNumber().longValue(), (n5.times(n2)).getNumber().longValue());
    }

    @Test(expected = AssertionError.class)
    public void testShiftLeft() throws Exception {
        NumericValue n2_5 = new NumericValue(2.5);
        NumericValue n_5 = new NumericValue(2L);

        n2_5.shiftRight(n_5);
    }

    @Test
    public void testUnsignedModulo() throws Exception {
        NumericValue n_max = new NumericValue(922337203685755807L);
        NumericValue n2 = new NumericValue(2L);
        NumericValue expected = new NumericValue(1L);

        assertEquals(expected.getNumber().longValue(), (n_max.modulo(n2)).getNumber().longValue());
    }

    @Test
    public void testModulo() throws Exception {
        NumericValue n_max = new NumericValue(922337203685755807L);
        NumericValue n2 = new NumericValue(2L);
        NumericValue expected = new NumericValue(1L);

        assertEquals(expected.getNumber().longValue(), (n_max.modulo(n2)).getNumber().longValue());
    }

    @Test(expected = AssertionError.class)
    public void testBinaryOr() throws Exception {
        NumericValue n2_5 = new NumericValue(2.5);
        NumericValue n_5 = new NumericValue(2L);

        n2_5.binaryOr(n_5);
    }

    @Test(expected = AssertionError.class)
    public void testBinaryXor() throws Exception {
        NumericValue n2_5 = new NumericValue(2.5);
        NumericValue n_5 = new NumericValue(2L);
        n2_5.binaryAnd(n_5);
    }

    @Test(expected = AssertionError.class)
    public void testBinaryAnd() throws Exception {
        NumericValue n2_5 = new NumericValue(2.5);
        NumericValue n_5 = new NumericValue(2L);

        n2_5.binaryAnd(n_5);
    }





}
