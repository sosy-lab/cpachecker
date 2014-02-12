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
package org.sosy_lab.cpachecker.util.invariants.balancer.test;

import org.sosy_lab.cpachecker.util.invariants.balancer.AssumptionSet;
import org.sosy_lab.cpachecker.util.invariants.balancer.IRMatrix;
import org.sosy_lab.cpachecker.util.invariants.balancer.RationalFunction;
import org.sosy_lab.cpachecker.util.invariants.balancer.Variable;


public class MatrixTester {

  public static void main(String[] args) {
    test5();
  }

  static void test1() {

    IRMatrix A = new IRMatrix(3, 3);
    for (int n = 0; n < 9; n++) {
      int i = n/3;
      int j = n%3;
      RationalFunction f = new RationalFunction(n+1);
      A.set(i, j, f);
    }

    System.out.println(A);

    RationalFunction p = RationalFunction.buildVar("p");
    A.set(0, 0, p);
    System.out.println(A);
    A.putInRREF();

    System.out.println(A);


  }

  static void test2() {
    RationalFunction p = RationalFunction.buildVar("p");
    System.out.println(p);
  }

  static void test3() {
    Variable v = new Variable("p");
    RationalFunction p = new RationalFunction(v);
    System.out.println(p);
  }

  static void test4() {

    IRMatrix A = new IRMatrix(3, 3);
    A.zeroFill();
    System.out.println("zeros:");
    System.out.println(A);

    RationalFunction p = RationalFunction.buildVar("p");
    A.set(0, 0, p);
    A.set(0, 1, new RationalFunction(2));
    A.set(0, 2, new RationalFunction(3));
    A.set(1, 0, new RationalFunction(1));
    A.set(1, 1, new RationalFunction(1));

    System.out.println("filled:");
    System.out.println(A);

    AssumptionSet alist = A.putInRREF();
    System.out.println("RREF:");
    System.out.println(A);
    System.out.println("assumptions:");
    System.out.println(alist);

  }

  static void test5() {
    IRMatrix A = new IRMatrix(5, 8);
    A.zeroFill();
    Variable pv = new Variable("p");
    Variable qv = new Variable("q");
    Variable rv = new Variable("r");

    RationalFunction p = new RationalFunction(pv);
    RationalFunction q = new RationalFunction(qv);
    RationalFunction r = new RationalFunction(rv);

    RationalFunction np = RationalFunction.makeNegative(p);
    RationalFunction nq = RationalFunction.makeNegative(q);
    RationalFunction nr = RationalFunction.makeNegative(r);

    A.set(0, 0, p);
    A.set(1, 0, q);
    A.set(4, 0, r);

    A.set(0, 1, np);
    A.set(1, 1, nq);
    A.set(4, 1, nr);

    A.set(0, 2, new RationalFunction(-1));
    A.set(2, 2, new RationalFunction(1));
    A.set(4, 2, new RationalFunction(1));

    A.set(0, 3, new RationalFunction(1));
    A.set(2, 3, new RationalFunction(-1));
    A.set(4, 3, new RationalFunction(-1));

    A.set(1, 4, new RationalFunction(-1));
    A.set(3, 4, new RationalFunction(1));
    A.set(4, 4, new RationalFunction(1));

    A.set(1, 5, new RationalFunction(1));
    A.set(3, 5, new RationalFunction(-1));
    A.set(4, 5, new RationalFunction(-1));

    A.set(0, 6, new RationalFunction(1));
    A.set(4, 6, new RationalFunction(20));

    A.set(2, 7, p);
    A.set(3, 7, q);
    A.set(4, 7, r);

    System.out.println(A);

    AssumptionSet aset1 = A.putInRREF();
    AssumptionSet aset2 = A.getAlmostZeroRowAssumptions();
    System.out.println("RREF:");
    System.out.println(A);
    System.out.println("assumptions:");
    System.out.println(aset1);
    System.out.println(aset2);

  }

}
