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
package org.sosy_lab.cpachecker.util.invariants.balancer;



public class LCTester {

  public static void main(String[] args) {
    test1();
  }

  static void test1() {
    RationalFunction a = new RationalFunction(2);
    RationalFunction b = new RationalFunction(6);
    RationalFunction c = new RationalFunction(10);
    RationalFunction d = new RationalFunction(14);

    RationalFunction e = new RationalFunction(3);
    RationalFunction f = new RationalFunction(100);
    RationalFunction g = new RationalFunction(200);
    RationalFunction h = new RationalFunction(300);

    RationalFunction[] rfa1 = {a, b, c, d};
    RationalFunction[] rfa2 = {e, f, g, h};

    LinCombOverParamField lc1 = new LinCombOverParamField(rfa1);
    LinCombOverParamField lc2 = new LinCombOverParamField(rfa2);

    LinCombOverParamField lc = lc1.setZeroAndSolveFor(0);
    System.out.println(lc.toString());
    lc = lc2.substitute(0, lc);
    System.out.println(lc.toString());

  }

}
