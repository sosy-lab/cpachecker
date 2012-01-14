/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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


public class Assumption {

  private final RationalFunction func;
  private final AssumptionType atype;

  public Assumption(Polynomial f, AssumptionType a) {
    func = new RationalFunction(f);
    atype = a;
  }

  public Assumption(RationalFunction f, AssumptionType a) {
    func = f;
    atype = a;
  }

  public RationalFunction getRationalFunction() {
    return func;
  }

  public AssumptionType getAssumptionType() {
    return atype;
  }

  public Polynomial getNumerator() {
    return func.getNumerator();
  }

  @Override
  public boolean equals(Object o) {
    boolean ans = false;
    if (o instanceof Assumption) {
      Assumption a = (Assumption) o;
      String s1 = toString();
      String s2 = a.toString();
      ans = s1.equals(s2);
    }
    return ans;
  }

  /**
   * HashSet only looks to the equals method if the hashCodes of the
   * two objects are the same.
   */
  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public String toString() {
    return func.toString()+atype.toString();
  }

  public enum AssumptionType {
    ZERO          (" = 0"),
    NONZERO       (" <> 0"),
    POSITIVE      (" > 0"),
    NONNEGATIVE   (" >= 0"),
    NONPOSITIVE   (" <= 0"),
    NEGATIVE      (" < 0");

    private final String text;

    private AssumptionType(String t) {
      text = t;
    }

    @Override
    public String toString() {
      return text;
    }

  }

}
