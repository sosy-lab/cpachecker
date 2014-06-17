/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.rationals;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Testing rationals library
 */
public class TestExtendedRational {
  @Test
  public void testTypes() {
    ExtendedRational x;

    x = ExtendedRational.ofLongs(23, 7);
    Assert.assertEquals(ExtendedRational.NumberType.RATIONAL, x.getType());

    x = ExtendedRational.ofLongs(23, 0);
    Assert.assertEquals(ExtendedRational.NumberType.INFTY, x.getType());

    x = ExtendedRational.ofLongs(-100, 0);
    Assert.assertEquals(ExtendedRational.NumberType.NEG_INFTY, x.getType());

    x = ExtendedRational.ofLongs(0, 0);
    Assert.assertEquals(ExtendedRational.NumberType.NaN, x.getType());
  }

  @Test public void testInstantiation() {
    ExtendedRational x;
    x = ExtendedRational.ofLongs(108, 96);
    Assert.assertEquals("9/8", x.toString());
  }

  @Test public void testAddition() {
    ExtendedRational a, b;
    a = ExtendedRational.ofLongs(12, 8);
    b = ExtendedRational.ofLongs(-54, 12);
    Assert.assertEquals("-3/1", a.plus(b).toString());

    b = ExtendedRational.ofLongs(1, 0);
    Assert.assertEquals("Infinity", a.plus(b).toString());

    b = ExtendedRational.ofString("NaN");
    Assert.assertEquals("NaN", a.plus(b).toString());

    a = ExtendedRational.ofString("Infinity");
    b = ExtendedRational.ofString("-Infinity");
    Assert.assertEquals("NaN", a.plus(b).toString());


    a = ExtendedRational.ofString("2309820938409238490");
    b = ExtendedRational.ofLongs(-1, 0);
    Assert.assertEquals("-Infinity", a.plus(b).toString());
  }

  @Test public void testSubtraction() {
    ExtendedRational a, b;

    a = ExtendedRational.ofString("5/2");
    b = ExtendedRational.ofString("3/2");

    Assert.assertEquals("1/1", a.minus(b).toString());
  }

  @Test public void testMultiplication() {
    ExtendedRational a, b;
    a = ExtendedRational.ofString("2/4");
    b = ExtendedRational.ofString("-1/3");
    Assert.assertEquals(ExtendedRational.ofString("-2/12"), a.times(b));

    a = ExtendedRational.ofString("100/4");
    b = ExtendedRational.ofString("1/100");
    Assert.assertEquals(ExtendedRational.ofString("1/4"), a.times(b));

    a = ExtendedRational.ofString("100/4");
    b = ExtendedRational.ofString("Infinity");
    Assert.assertEquals(ExtendedRational.ofString("Infinity"), a.times(b));
  }

  @Test public void testDivision() {
    ExtendedRational a, b;
    a = ExtendedRational.ofString("2/4");
    b = ExtendedRational.ofString("1/4");
    Assert.assertEquals("2/1", a.divides(b).toString());

    a = ExtendedRational.ofString("234234");
    b = ExtendedRational.INFTY;
    Assert.assertEquals("0/1", a.divides(b).toString());

    a = ExtendedRational.ofString("234234");
    b = ExtendedRational.NEG_INFTY;
    Assert.assertEquals("0/1", a.divides(b).toString());

    a = ExtendedRational.ofString("234234");
    b = ExtendedRational.NaN;
    Assert.assertEquals("NaN", a.divides(b).toString());
  }

  @Test public void testComparison() {
    List<ExtendedRational> unsorted = Arrays.asList(
       ExtendedRational.ofLongs(0, 0),
       ExtendedRational.ofLongs(-1, 0),
       ExtendedRational.ofLongs(-2, 4),
       ExtendedRational.ofLongs(1, 3),
       ExtendedRational.ofLongs(2, 3),
       ExtendedRational.ofLongs(1, 0)
    );
    Collections.shuffle(unsorted);

    List<ExtendedRational> sorted = Arrays.asList(
        ExtendedRational.ofLongs(-1, 0),
        ExtendedRational.ofLongs(-2, 4),
        ExtendedRational.ofLongs(1, 3),
        ExtendedRational.ofLongs(2, 3),
        ExtendedRational.ofLongs(1, 0),
        ExtendedRational.ofLongs(0, 0)
    );

    Collections.sort(unsorted);

    Assert.assertEquals(sorted, unsorted);
  }

  @Test public void testOfString() {
    ExtendedRational a;
    a = ExtendedRational.ofString("6/8");
    Assert.assertEquals(ExtendedRational.ofLongs(3, 4), a);

    a = ExtendedRational.ofString("Infinity");
    Assert.assertEquals(ExtendedRational.ofLongs(1, 0), a);

    a = ExtendedRational.ofString("-Infinity");
    Assert.assertEquals(ExtendedRational.ofLongs(-1, 0), a);

    a = ExtendedRational.ofString("NaN");
    Assert.assertEquals(ExtendedRational.ofLongs(0, 0), a);

    a = ExtendedRational.ofString("-2");
    Assert.assertEquals(ExtendedRational.ofLongs(-2, 1), a);
  }

}
