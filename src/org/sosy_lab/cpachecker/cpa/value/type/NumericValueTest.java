/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import org.junit.Test;
import org.sosy_lab.common.rationals.Rational;

public class NumericValueTest {

    @Test
    public void longValue() throws Exception {
        NumericValue val = new NumericValue(5L);
        if(val.longValue() != 5) {
            throw new Exception("Wrong conversion from long to long");
        }

        val = new NumericValue(5.3d);
        if (val.longValue() != 5) {
            throw new Exception("Wrong conversion from double to long");
        }

        val = new NumericValue(5.3f);
        if (val.longValue() != 5) {
            throw new Exception("Wrong conversion from float to long");
        }

        val = new NumericValue(Rational.of(BigInteger.ONE, BigInteger.TWO));
        if (val.longValue() != 0) {
            throw new Exception("Wrong conversion from rational to long");
        }

        val = new NumericValue(Rational.of(BigInteger.TEN, BigInteger.TWO));
        if (val.longValue() != 5) {
            throw new Exception("Wrong conversion from rational to long");
        }

        val = new NumericValue(10);
        if (val.longValue() != 10d) {
            throw new Exception("Wrong conversion from int to long");
        }
    }

    @Test
    public void bigDecimalValue() throws Exception {
        NumericValue val = new NumericValue(5L);
        if(!val.bigDecimalValue().equals(BigDecimal.valueOf(5))) {
            throw new Exception("Wrong conversion from long to BigDecimal");
        }

        val = new NumericValue(5.3d);
        if(!val.bigDecimalValue().equals(BigDecimal.valueOf(5.3))) {
            throw new Exception("Wrong conversion from double to BigDecimal");
        }

        //Note that this test will fail if one sets val = new NumericValue(5.3f) and check for 5.3d
        val = new NumericValue(5.5f);
        if(!val.bigDecimalValue().equals(BigDecimal.valueOf(5.5))) {
            throw new Exception("Wrong conversion from float to BigDecimal");
        }

        val = new NumericValue(Rational.of(BigInteger.ONE, BigInteger.valueOf(3)));
        if (!val.bigDecimalValue()
                .equals(BigDecimal.ONE.divide(BigDecimal.valueOf(3), 100, RoundingMode.HALF_UP))) {
            throw new Exception("Wrong conversion from rational to BigDecimal");
        }

        //Note that Rational(10/2) = 5.0000... (with 100 zeros)
        // Checking if BigDecimal(Rational(10/2)) == BigDecimal(5) will return false
        val = new NumericValue(Rational.of(BigInteger.TEN, BigInteger.TWO));
        if(!val.bigDecimalValue().equals(BigDecimal.TEN.divide(BigDecimal.valueOf(2), 100, RoundingMode.HALF_UP))) {
            throw new Exception("Wrong conversion from rational to BigDecimal");
        }

        val = new NumericValue(10);
        if(!val.bigDecimalValue().equals(BigDecimal.valueOf(10))) {
            throw new Exception("Wrong conversion from int to BigDecimal");
        }
    }

    @Test
    public void bigInteger() throws Exception {
        NumericValue val = new NumericValue(5L);
        if(!val.bigInteger().equals(BigInteger.valueOf(5))) {
            throw new Exception("Wrong conversion from long to BigInteger");
        }

        val = new NumericValue(5.3d);
        if(!val.bigInteger().equals(BigInteger.valueOf(5))) {
            throw new Exception("Wrong conversion from double to BigInteger");
        }

        //Note that this test will fail if one sets val = new NumericValue(5.3f) and check for 5.3d
        val = new NumericValue(5.5f);
        if(!val.bigInteger().equals(BigInteger.valueOf(5))) {
            throw new Exception("Wrong conversion from float to BigInteger");
        }

        val = new NumericValue(Rational.of(BigInteger.ONE, BigInteger.valueOf(3)));
        if(!val.bigInteger().equals(BigInteger.valueOf(0))) {
            throw new Exception("Wrong conversion from rational to BigInteger");
        }

        //Note that Rational(10/2) = 5.0000... (with 100 zeros)
        // Checking if BigDecimal(Rational(10/2)) == BigDecimal(5) will return false
        val = new NumericValue(Rational.of(BigInteger.TEN, BigInteger.TWO));
        if(!val.bigInteger().equals(BigInteger.valueOf(5))) {
            throw new Exception("Wrong conversion from rational to BigInteger");
        }

        val = new NumericValue(10);
        if(!val.bigInteger().equals(BigInteger.valueOf(10))) {
            throw new Exception("Wrong conversion from int to BigInteger");
        }
    }

    @Test
    public void doubleValue() throws Exception {
        NumericValue val = new NumericValue(5L);
        if(val.doubleValue() != 5) {
            throw new Exception("Wrong conversion from long to double");
        }

        val = new NumericValue(5.3d);
        if (val.doubleValue() != 5.3d) {
            throw new Exception("Wrong conversion from double to double");
        }

        //Note that this test will fail if one sets val = new NumericValue(5.3f) and check for 5.3d
        val = new NumericValue(5.5f);
        if (val.doubleValue() != 5.5d) {
            throw new Exception("Wrong conversion from float to double");
        }

        val = new NumericValue(Rational.of(BigInteger.ONE, BigInteger.valueOf(3)));
        if (val.doubleValue() != 1d/3) {
            throw new Exception("Wrong conversion from rational to double");
        }

        val = new NumericValue(Rational.of(BigInteger.TEN, BigInteger.TWO));
        if (val.doubleValue() != 5d) {
            throw new Exception("Wrong conversion from rational to double");
        }

        val = new NumericValue(10);
        if (val.doubleValue() != 10d) {
            throw new Exception("Wrong conversion from int to double");
        }
    }

    @Test
    public void checkConverions() throws Exception {
        //Simple tests for convertions
        Number[] numbers = {Integer.MAX_VALUE,
                Integer.MIN_VALUE,
                Double.MAX_VALUE,
                Double.MIN_VALUE,
                Float.MAX_VALUE,
                Float.MIN_VALUE,
                BigDecimal.valueOf(Double.MAX_VALUE)};

        boolean passed = true;
        for (Number pNumber : numbers) {
            NumericValue test = new NumericValue(pNumber);
            passed &= test.getNumber().equals(pNumber);
            if (pNumber instanceof Double || pNumber instanceof Float) {
                passed &= test.bigDecimalValue().doubleValue() == pNumber.doubleValue();
                passed &= test.bigInteger().equals(BigInteger.valueOf((long) pNumber.doubleValue()));
            } else {
                passed &= test.bigDecimalValue().equals(new BigDecimal(pNumber + ""));
                if (pNumber instanceof BigDecimal) {
                    passed &= test.bigInteger().equals(((BigDecimal) pNumber).toBigInteger());
                } else {
                    passed &= test.bigInteger().equals(new BigInteger(pNumber + ""));
                }
            }
            passed &= test.doubleValue() == pNumber.doubleValue();
        }

        if (!passed) {
            throw new Exception("Conversion failed");
        }
    }


}
