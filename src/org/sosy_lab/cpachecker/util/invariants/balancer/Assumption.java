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

import java.util.Vector;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.invariants.Rational;


public class Assumption {

  private final RationalFunction func;
  private final AssumptionType atype;

  public Assumption(Polynomial f, AssumptionType a) {
    // First simplify, if possible.
    Pair<RationalFunction, AssumptionType> p = simplify(f, a);
    func = p.getFirst();
    atype = p.getSecond();
  }

  public Assumption(RationalFunction f, AssumptionType a) {
    // First simplify, if possible.
    if (f.isPolynomial()) {
      Pair<RationalFunction, AssumptionType> p = simplify(f.getNumerator(), a);
      f = p.getFirst();
      a = p.getSecond();
    }
    f.simplify();
    func = f;
    atype = a;
  }

  /*
   * Return the logical negation of this assumption.
   */
  public Assumption not() {
    AssumptionType b = atype.not();
    return new Assumption(func, b);
  }

  /*
   * Checks whether this can strengthen b to a stronger assumption c.
   * If so, returns c; else returns null.
   */
  public Assumption strengthen(Assumption b) {
    // This can strengthen b iff b has either the same rational function, or the additive inverse thereof.
    if (RationalFunction.subtract(func, b.func).isZero()) {
      // In this case the rational functions are the same, so we just conjoin the assumption types.
      AssumptionType t = AssumptionType.conjoin(this.atype, b.atype);
      return new Assumption(b.func, t);
    } else if (RationalFunction.add(func, b.func).isZero()) {
      // In this case the rational functions are additive inverses, so we flip one assumption type
      // before conjoining.
      AssumptionType t = AssumptionType.conjoin(this.atype.flip(), b.atype);
      return new Assumption(b.func, t);
    } else {
      // Else we can do nothing.
      return null;
    }
  }

  /*
   * What the returned AssumptionRelation says will be true with this Assumption as subject,
   * and the other Assumption as direct object. E.g. if you get WEAKENS, then that means that
   * this Assumption weakens the other, i.e. is implied by the other.
   *
   * We call this method 'matchAgainst' instead of 'compareTo', because the latter is used
   * by the 'Comparable' interface (which we might want to implement later???).
   */
  public AssumptionRelation matchAgainst(Assumption other) {
    // this and other are comparable iff their rational functions are the same or additive inverses.
    // TODO: Really they should be comparable iff one is a constant multiple of the other.
    AssumptionType ot = null;
    if (RationalFunction.subtract(func, other.func).isZero()) {
      ot = other.atype;
    } else if (RationalFunction.add(func, other.func).isZero()) {
      ot = other.atype.flip();
    }
    if (ot == null) {
      // Then they are not comparable.
      return AssumptionRelation.DOESNOTCOMPARETO;
    }
    AssumptionType tt = this.atype;
    // Are they the same?
    if (tt == ot) {
      return AssumptionRelation.ISSAMEAS;
    }
    // Compute the conjunction of tt and ot.
    AssumptionType ct = AssumptionType.conjoin(tt, ot);
    // If the conjunction is of type 'false', then this and other contradict each other.
    if (ct == AssumptionType.FALSE) {
      return AssumptionRelation.CONTRADICTS;
    }
    // If it is equal to one of the conjuncts, then that one implies the other.
    if (ct == tt) {
      // Then tt ^ ot <--> tt, so in particular tt --> ot, i.e. this stengthens other.
      return AssumptionRelation.STRENGTHENS;
    }
    if (ct == ot) {
      // Then tt ^ ot <--> ot, so in particular ot --> tt, i.e. this weakens other.
      return AssumptionRelation.WEAKENS;
    }
    // Otherwise, their conjunction is a common refinement, different from them both.
    return AssumptionRelation.REFINES;
  }


  /*
   * This simplification function will, for example, turn the assumption
   *   -2*p1 + -4*p2 <= 0
   * into
   *   p1 + 2*p2 >= 0.
   * In general, it looks for integer content in the polynomial, as well as
   * "sign content" which is -1 if every coefficient in the polynomial is negative.
   */
  private Pair<RationalFunction, AssumptionType> simplify(Polynomial f, AssumptionType a) {
    // Cancel rational content. Since this is always positive, we need not flip the assumption type.
    f = f.cancelRationalContent();
    // TODO: Clean up.
    // cancelRationalContent is a new method, added long after this simplify method was
    // written. Is the rest of the method still needed? Or does cancelRationalContent already
    // do everything we want? Perhaps the unit content part is still needed, but not the part
    // pertaining to integer content?
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
      Rational r = new Rational(1, d);
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

  public enum AssumptionRelation {
    CONTRADICTS       (5),
    ISSAMEAS          (4),
    WEAKENS           (3),
    REFINES           (2),
    STRENGTHENS       (1),
    DOESNOTCOMPARETO  (0);

    private final int num;
    private AssumptionRelation(int n) {
      num = n;
    }

    public int getNum() {
      return num;
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
    private static final Vector<AssumptionType> codes = new Vector<>(8);

    // static block to initialize the static field 'codes'
    // Here, we use the position in the vector to represent the code assigned to
    // each AssumptionType. The one lying at position n in the vector has code equal
    // to the binary representation abc of n.
    // Respectively, a, b, and c are set to 1 when the assumption says of the quantity
    // in question that it could be negative (a), positive (b), or zero (c).
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

    public int getCode() {
      return codes.indexOf(this);
    }

    private AssumptionType(String t) {
      text = t;
    }

    /*
     * Return the logical negation of this assumption type.
     */
    public AssumptionType not() {
      int n = codes.indexOf(this);
      return codes.get(7-n);
    }

    public static AssumptionType conjoin(AssumptionType a, AssumptionType b) {
      int na = codes.indexOf(a);
      int nb = codes.indexOf(b);
      int nc = na & nb;
      return codes.get(nc);
    }

    public AssumptionType flip() {
      AssumptionType a;
      switch (this) {
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
