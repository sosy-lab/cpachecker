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
package org.sosy_lab.cpachecker.cpa.sign;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SIGNTest {

    @Test
    public void testCombineWith() throws Exception {
        SIGN zero = SIGN.ZERO;
        SIGN plusMinus = SIGN.PLUSMINUS;
        SIGN all = SIGN.ALL;

        assertEquals(all, zero.combineWith(plusMinus));
    }

    @Test
    public void testCoversSIGN() throws Exception {
        SIGN zero = SIGN.ZERO;
        SIGN plusMinus = SIGN.PLUSMINUS;
        SIGN all = SIGN.ALL;

        assertFalse(zero.covers(plusMinus));
        assertTrue(all.covers(zero));
    }

    @Test
    public void testIntersects() throws Exception {
        SIGN zero = SIGN.ZERO;
        SIGN plusMinus = SIGN.PLUSMINUS;
        SIGN all = SIGN.ALL;

        assertFalse(zero.intersects(plusMinus));
        assertTrue(all.intersects(zero));
    }

    @Test
    public void testEvaluateNonCommutativePlusOperator() throws Exception {
        SIGN empty = SIGN.EMPTY;
        SIGN plus = SIGN.PLUS;
        SIGN minus = SIGN.MINUS;
        SIGN plusMinus = SIGN.PLUSMINUS;
        SIGN zero = SIGN.ZERO;
        SIGN all = SIGN.ALL;

        assertEquals(empty, zero.evaluateNonCommutativePlusOperator(plusMinus));
        assertEquals(plus, plus.evaluateNonCommutativePlusOperator(zero));
        assertEquals(all, plus.evaluateNonCommutativePlusOperator(minus));
        assertEquals(plus, plus.evaluateNonCommutativePlusOperator(plus));
        assertEquals(minus, minus.evaluateNonCommutativePlusOperator(minus));
    }

    @Test
    public void testEvaluateMulOperator() throws Exception {
        SIGN plus = SIGN.PLUS;
        SIGN minus = SIGN.MINUS;
        SIGN plusMinus = SIGN.PLUSMINUS;
        SIGN zero = SIGN.ZERO;
        SIGN plus0 = SIGN.PLUS0;


        assertEquals(zero, zero.evaluateMulOperator(plusMinus));
        assertEquals(plus0, plus.evaluateMulOperator(zero));
        assertEquals(plusMinus, plus.evaluateMulOperator(minus));
        assertEquals(plus, minus.evaluateMulOperator(minus));
    }

    @Test
    public void testEvaluateNonCommutativeMulOperator() throws Exception {
        SIGN empty = SIGN.EMPTY;
        SIGN plus = SIGN.PLUS;
        SIGN minus = SIGN.MINUS;
        SIGN plusMinus = SIGN.PLUSMINUS;
        SIGN zero = SIGN.ZERO;

        assertEquals(empty, zero.evaluateNonCommutativeMulOperator(plusMinus));
        assertEquals(zero, plus.evaluateNonCommutativeMulOperator(zero));
        assertEquals(minus, plus.evaluateNonCommutativeMulOperator(minus));
        assertEquals(plus, minus.evaluateNonCommutativeMulOperator(minus));
    }

    @Test
    public void testEvaluateModuloOperator() throws Exception {
        SIGN plus = SIGN.PLUS;
        SIGN minus = SIGN.MINUS;
        SIGN plusMinus = SIGN.PLUSMINUS;
        SIGN zero = SIGN.ZERO;
        SIGN all = SIGN.ALL;
        SIGN plus0 = SIGN.PLUS0;
        SIGN minus0 = SIGN.MINUS0;

        assertEquals(zero, zero.evaluateModuloOperator(plusMinus));
        assertEquals(all, plus.evaluateModuloOperator(zero));
        assertEquals(plus0, plus.evaluateModuloOperator(minus));
        assertEquals(minus0, minus.evaluateModuloOperator(minus));
    }

    @Test
    public void testEvaluateAndOperator() throws Exception {
        SIGN plus = SIGN.PLUS;
        SIGN minus = SIGN.MINUS;
        SIGN plusMinus = SIGN.PLUSMINUS;
        SIGN zero = SIGN.ZERO;
        SIGN plus0 = SIGN.PLUS0;
        SIGN minus0 = SIGN.MINUS0;

        assertEquals(zero, zero.evaluateAndOperator(plusMinus));
        assertEquals(minus0, minus.evaluateAndOperator(minus));
        assertEquals(plus0, plus.evaluateAndOperator(plus));
    }

    @Test
    public void testEvaluateLessOperator() throws Exception {
        SIGN empty = SIGN.EMPTY;
        SIGN plus = SIGN.PLUS;
        SIGN minus = SIGN.MINUS;
        SIGN plusMinus = SIGN.PLUSMINUS;
        SIGN zero = SIGN.ZERO;
        SIGN all = SIGN.ALL;
        SIGN plus0 = SIGN.PLUS0;
        SIGN minus0 = SIGN.MINUS0;

        assertEquals(all, zero.evaluateLessOperator(plusMinus));
        assertEquals(zero, plus.evaluateLessOperator(minus));
        assertEquals(zero, minus.evaluateLessOperator(plus));
        assertEquals(zero, zero.evaluateLessOperator(minus));
        assertEquals(zero, plus0.evaluateLessOperator(minus));
        assertEquals(plusMinus, minus0.evaluateLessOperator(plus));
    }

    @Test
    public void testEvaluateLessEqualOperator() throws Exception {
        SIGN empty = SIGN.EMPTY;
        SIGN plus = SIGN.PLUS;
        SIGN minus = SIGN.MINUS;
        SIGN plusMinus = SIGN.PLUSMINUS;
        SIGN zero = SIGN.ZERO;
        SIGN all = SIGN.ALL;
        SIGN plus0 = SIGN.PLUS0;
        SIGN minus0 = SIGN.MINUS0;

        assertEquals(all, zero.evaluateLessEqualOperator(plusMinus));
        assertEquals(zero, plus.evaluateLessEqualOperator(minus));
        assertEquals(zero, minus.evaluateLessEqualOperator(plus));
        assertEquals(zero, zero.evaluateLessEqualOperator(minus));
        assertEquals(zero, plus0.evaluateLessEqualOperator(minus));
        assertEquals(plusMinus, minus0.evaluateLessEqualOperator(plus));
    }

    @Test
    public void testEvaluateEqualOperator() throws Exception {
        SIGN empty = SIGN.EMPTY;
        SIGN plus = SIGN.PLUS;
        SIGN minus = SIGN.MINUS;
        SIGN plusMinus = SIGN.PLUSMINUS;
        SIGN zero = SIGN.ZERO;
        SIGN all = SIGN.ALL;
        SIGN plus0 = SIGN.PLUS0;
        SIGN minus0 = SIGN.MINUS0;

        assertEquals(empty, empty.evaluateEqualOperator(plusMinus));
        assertEquals(empty, plus.evaluateEqualOperator(empty));
        assertEquals(plusMinus, zero.evaluateEqualOperator(zero));
    }


}
