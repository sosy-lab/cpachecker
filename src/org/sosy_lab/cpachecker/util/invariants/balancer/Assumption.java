/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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

import java.util.Vector;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.invariants.Rational;


public class Assumption {

  private final RationalFunction func;
  private final AssumptionType atype;

  public Assumption(Polynomial f, AssumptionType a) {
    // First simplify, if possible.
    Pair<RationalFunction,AssumptionType> p = simplify(f,a);
    func = p.getFirst();
    atype = p.getSecond();
  }

  public Assumption(RationalFunction f, AssumptionType a) {
    // First simplify, if possible.
    if (f.isPolynomial()) {
      Pair<RationalFunction,AssumptionType> p = simplify(f.getNumerator(),a);
      f = p.getFirst();
      a = p.getSecond();
    }
    func = f;
    atype = a;
  }

  /*
   * Checks whether this can strengthen b to a stronger assumption c.
   * If so, returns c; else returns null.
   */
  public Assumption strengthen(Assumption b) {
    // This can strengthen b iff b has either the same rational function, or the additive inverse thereof.
    if (RationalFunction.subtract(func, b.func).isZero()) {
      // In this case the rational functions are the same, so we just conjoin the assumption types.
      AssumptionType t = AssumptionType.conjoin(this.atype,b.atype);
      return new Assumption(b.func,t);
    }
    else if (RationalFunction.add(func,b.func).isZero()) {
      // In this case the rational functions are additive inverses, so we flip one assumption type
      // before conjoining.
      AssumptionType t = AssumptionType.conjoin(this.atype.flip(),b.atype);
      return new Assumption(b.func,t);
    }
    else {
      // Else we can do nothing.
      return null;
    }
  }

  private Pair<RationalFunction,AssumptionType> simplify(Polynomial f, AssumptionType a) {
    int u = f.getUnitContent();
    // If u = 0, then f is 0. Otherwise, we try to simplify.
    if (u != 0) {
      int c = f.getIntegerContent().intValue(); // c is nonnegative
      int d;
      if (c == 0) {
        // In this case not all the coeffs were integral.
        d = 1;
      } else {
        d = c;
      }
      d = u*d;
      // Now we want to divide through by d, and flip a if needed.
      Rational r = new Rational(1,d);
      f = Polynomial.multiply(f, new Polynomial(r));
      if (u < 0) {
        a = a.flip();
      }
    }
    RationalFunction g = new RationalFunction(f);
    return Pair.<RationalFunction, AssumptionType>of(g, a);
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
    if (atype == AssumptionType.TRUE || atype == AssumptionType.FALSE) {
      // If true or false, don't need the rational function at all.
      return atype.toString();
    } else if (func.isPolynomial()) {
      // Else, if rational function a polynomial, then simple.
      return func.toString()+atype.toString();
    } else {
      // Else we must think about the sign of the denominator.
      // (Redlog does not accept inequalities with a quotient on one side.)
      Polynomial num = func.getNumerator();
      Polynomial denom = func.getDenominator();
      if (atype == AssumptionType.ZERO || atype == AssumptionType.NONZERO) {
        return num.toString()+atype.toString();
      } else {
        String s =  "(("+denom.toString()+" > 0 and "+num.toString()+atype.toString()+")";
        s += " or "+"("+denom.toString()+" < 0 and "+num.toString()+atype.flip().toString()+"))";
        return s;
      }
    }
  }

  public enum AssumptionType {
    TRUE          ("0 = 0"),
    NONZERO       (" <> 0"),
    NONPOSITIVE   (" <= 0"),
    NEGATIVE      (" < 0"),
    NONNEGATIVE   (" >= 0"),
    POSITIVE      (" > 0"),
    ZERO          (" = 0"),
    FALSE         ("0 <> 0");

    private final String text;
    private static final Vector<AssumptionType> codes = new Vector<AssumptionType>(8);

    static {
      codes.add(AssumptionType.FALSE);       // 000
      codes.add(AssumptionType.ZERO);        // 001
      codes.add(AssumptionType.POSITIVE);    // 010
      codes.add(AssumptionType.NONNEGATIVE); // 011
      codes.add(AssumptionType.NEGATIVE);    // 100
      codes.add(AssumptionType.NONPOSITIVE); // 101
      codes.add(AssumptionType.NONZERO);     // 110
      codes.add(AssumptionType.TRUE);        // 111
    }

    private AssumptionType(String t) {
      text = t;
    }

    public static AssumptionType conjoin(AssumptionType a, AssumptionType b) {
      int na = codes.indexOf(a);
      int nb = codes.indexOf(b);
      int nc = na & nb;
      return codes.get(nc);
    }

    public AssumptionType flip() {
      AssumptionType a;
      switch(this) {
      case POSITIVE:
        a = AssumptionType.NEGATIVE;
        break;
      case NEGATIVE:
        a = AssumptionType.POSITIVE;
        break;
      case NONNEGATIVE:
        a = AssumptionType.NONPOSITIVE;
        break;
      case NONPOSITIVE:
        a = AssumptionType.NONNEGATIVE;
        break;
      default:
        a = this;
      }
      return a;
    }

    @Override
    public String toString() {
      return text;
    }

  }

}
