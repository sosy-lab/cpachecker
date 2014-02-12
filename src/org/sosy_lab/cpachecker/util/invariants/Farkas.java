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
package org.sosy_lab.cpachecker.util.invariants;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Farkas {

  private static AtomicInteger nextUsableIndex = new AtomicInteger(0);

  // Control whether the x's are existentially quantified.
  private static boolean exqx = false;

  public Farkas() {}

  public static void resetConstantIndices() {
    nextUsableIndex.set(0);
  }

  public static void setExqx(boolean exqx) {
    Farkas.exqx = exqx;
  }

  public static boolean willDoExqx() {
    return exqx;
  }

  /**
   * This version of makeRedlogFormula takes no "thirdIndex" argument,
   * but instead uses the nextUsableIndex to simply make all indices
   * ascend in one dimension.
   *
   * We handle strict inequalities simplistically, by simply demanding
   * for the final entry the same relation as is needed for each k (see
   * code below).
   *
   * @param prem The premises for Farkas's lemma.
   * @param concl The conclusions for Farkas's lemma.
   * @return the formula to be passed to Redlog
   */
  public static String makeRedlogFormula(LinearInequality prem,
      LinearInequality concl) {

    int J = prem.getNumIneqs();
    int K = concl.getNumIneqs();
    int I = prem.getNumVars();

    // So if the inequalities are written as columns, then the
    // prem matrix has J columns, the concl matrix has K columns,
    // and they both have I+1 rows, where the last one is the
    // "LEQ" row (whereas all the others are "EQ" rows).

    String s = "";
    String cj;
    String a, b, rel;
    boolean begun_statements = false;
    boolean begun_terms = false;
    HashSet<String> jUsed = new HashSet<>();
    HashSet<String> allc = new HashSet<>();
    Iterator<String> thecj;
    int[] cColumn = new int[J];

    for (int k = 0; k < K; k++) {

      // Populate cColumn.
      for (int j = 0; j < J; j++) {
        cColumn[j] = nextUsableIndex.incrementAndGet();
      }

      // No cj have been used yet, for this k.
      jUsed.clear();

      // I equations:
      for (int i = 0; i < I; i++) {
        // conjoin, if needed:
        if (begun_statements) {
          s += " and ";
        }
        // add J terms:
        for (int j = 0; j < J; j++) {
          a = prem.getCoeff(j, i).toString();
          // Add the term iff it is NOT a 0 prefixed with any number
          // of minus signs.
          if (!a.matches("-*0")) {
            // add, as needed:
            if (begun_terms) {
              s += " + ";
            }
            cj = "c"+Integer.toString(cColumn[j]);
            s += a+"*"+cj;
            // Have added a term, so:
            begun_terms = true;
            jUsed.add(cj);
            allc.add(cj);
          }
        }
        // write 0 if there were no terms:
        if (!begun_terms) {
          s += "0";
        }
        // state the equation:
        b = concl.getCoeff(k, i).toString();
        s += " = "+b;
        // At least one statement has now been made. So:
        begun_statements = true;
        // Finished this statement, so:
        begun_terms = false;
      }

      // 1 inequality:
      // conjoin, if needed:
      if (begun_statements) {
        s += " and ";
      }
      // add J terms:
      for (int j = 0; j < J; j++) {
        a = prem.getRHSCoeff(j).toString();
        if (!a.matches("-*0")) {
          // add, as needed:
          if (begun_terms) {
            s += " + ";
          }
          cj = "c"+Integer.toString(cColumn[j]);
          s += a+"*"+cj;
          // Have added a term, so:
          begun_terms = true;
          jUsed.add(cj);
          allc.add(cj);
        }
      }
      // write 0 if there were no terms:
      if (!begun_terms) {
        s += "0";
      }
      // state the inequality:
      b = concl.getRHSCoeff(k).toString();
      rel = concl.getReln(k).toString();
      s += " "+rel+" "+b;
      // At least one statement has now been made. So:
      begun_statements = true;
      // Finished this statement, so:
      begun_terms = false;

      // Any cj used need to be nonnegative.
      thecj = jUsed.iterator();
      while (thecj.hasNext()) {
        // If any cj have been used, then by now we have
        // added at least one statement, so we should say
        // " and ".
        cj = thecj.next();
        s += " and " + cj + " >= 0";
      }
    }

    if (exqx) {
      Iterator<String> thec = allc.iterator();
      String q = "ex({";
      boolean begun = false;
      while (thec.hasNext()) {
        cj = thec.next();
        if (begun) {
          q += ",";
        }
        q += cj;
        begun = true;
      }
      q += "},";
      s = q+s+")";
    }

    return s;
  }

  /**
   * This version handles strict inequalities with more sophistication,
   * but results in a more complex formula to be passed to Redlog.
   * It attempts to utilize the strength of the inequalities
   * in the premises.
   *
   * Namely, we determine which of the premise inequalities are strict,
   * and then weaken the inequality we demand in the case that any of these
   * are used.
   *
   * For example, suppose the 2nd and 7th premise inequalities are the strict
   * ones. Let the ci be the coefficients of the linear combination, let the
   * ai be the RHS coefficients, and let b be the target upper bound. If the
   * current (kth) target conclusion inequality is to be strict, then the
   * subformula we conjoin into the Redlog formula is as follows:
   *   (sum ci ai) + sl = b ^ sl >= 0 ^ (c2 > 0 | c7 > 0 | sl > 0).
   * Thus, if at least one of c2 or c7 is nonzero, so that we already have
   * a strict inequality, then we allow the slack variable sl to be 0.
   * Otherwise, we demand that it be positive.
   *
   * If instead the kth target conclusion inequality is lax, then, while the
   * neatest way to alter the above formula would be to simply lop off the final
   * conjunct, it results in a simpler formula to use:
   *   (sum ci ai) <= b
   *
   * @param prem The premises for Farkas's lemma.
   * @param concl The conclusions for Farkas's lemma.
   * @return the formula to be passed to Redlog
   */
  public static String makeRedlogFormulaUsingPremiseStrength(LinearInequality prem,
      LinearInequality concl) {

    int J = prem.getNumIneqs();
    int K = concl.getNumIneqs();
    int I = prem.getNumVars();

    // So if the inequalities are written as columns, then the
    // prem matrix has J columns, the concl matrix has K columns,
    // and they both have I+1 rows, where the last one is the
    // "LEQ" row (whereas all the others are "EQ" rows).

    // Find strict inequalities in the premises.
    List<Integer> strict = prem.findStrict();
    // If there aren't any, then we just use the simpler method.
    if (strict.size() == 0) {
      return makeRedlogFormula(prem, concl);
    }

    String s = "";
    String cj;
    String a, b;
    InfixReln rel;
    boolean begun_statements = false;
    boolean begun_terms = false;
    HashSet<String> jUsed = new HashSet<>();
    HashSet<String> allc = new HashSet<>();
    Iterator<String> thecj;
    int[] cColumn = new int[J];

    for (int k = 0; k < K; k++) {

      // Populate cColumn.
      for (int j = 0; j < J; j++) {
        cColumn[j] = nextUsableIndex.incrementAndGet();
      }

      // No cj have been used yet, for this k.
      jUsed.clear();

      // I equations:
      for (int i = 0; i < I; i++) {
        // conjoin, if needed:
        if (begun_statements) {
          s += " and ";
        }
        // add J terms:
        for (int j = 0; j < J; j++) {
          a = prem.getCoeff(j, i).toString();
          // Add the term iff it is NOT a 0 prefixed with any number
          // of minus signs.
          if (!a.matches("-*0")) {
            // add, as needed:
            if (begun_terms) {
              s += " + ";
            }
            cj = "c"+Integer.toString(cColumn[j]);
            s += a+"*"+cj;
            // Have added a term, so:
            begun_terms = true;
            jUsed.add(cj);
            allc.add(cj);
          }
        }
        // write 0 if there were no terms:
        if (!begun_terms) {
          s += "0";
        }
        // state the equation:
        b = concl.getCoeff(k, i).toString();
        s += " = "+b;
        // At least one statement has now been made. So:
        begun_statements = true;
        // Finished this statement, so:
        begun_terms = false;
      }

      // 1 inequality:
      // conjoin, if needed:
      if (begun_statements) {
        s += " and ";
      }
      // add J terms:
      for (int j = 0; j < J; j++) {
        a = prem.getRHSCoeff(j).toString();
        if (!a.matches("-*0")) {
          // add, as needed:
          if (begun_terms) {
            s += " + ";
          }
          cj = "c"+Integer.toString(cColumn[j]);
          s += a+"*"+cj;
          // Have added a term, so:
          begun_terms = true;
          jUsed.add(cj);
          allc.add(cj);
        }
      }
      // write 0 if there were no terms:
      if (!begun_terms) {
        s += "0";
      }
      // state the inequality:
      b = concl.getRHSCoeff(k).toString();
      rel = concl.getReln(k);
      // For strict inequalities, we use:
      //   (sum ci ai) + sl = b ^ (c2 > 0 | c7 > 0 | sl > 0)
      // and achieve nonnegativity of sl by adding it to jUsed.
      // For lax inequalities we use:
      //   (sum ci ai) <= b.
      if (rel.equals(InfixReln.LT)) {
        String slackVar = "c"+Integer.toString(nextUsableIndex.incrementAndGet());
        jUsed.add(slackVar);
        s += " + "+slackVar+" = "+b+" and (";
        for (Integer idx : strict) {
          cj = "c"+Integer.toString(cColumn[idx]);
          s += cj+" > 0 or ";
        }
        s += slackVar+" > 0)";
      } else {
        s += " <= "+b;
      }
      // At least one statement has now been made. So:
      begun_statements = true;
      // Finished this statement, so:
      begun_terms = false;

      // Any cj used need to be nonnegative.
      thecj = jUsed.iterator();
      while (thecj.hasNext()) {
        // If any cj have been used, then by now we have
        // added at least one statement, so we should say
        // " and ".
        cj = thecj.next();
        s += " and " + cj + " >= 0";
      }
    }

    if (exqx) {
      Iterator<String> thec = allc.iterator();
      String q = "ex({";
      boolean begun = false;
      while (thec.hasNext()) {
        cj = thec.next();
        if (begun) {
          q += ",";
        }
        q += cj;
        begun = true;
      }
      q += "},";
      s = q+s+")";
    }

    return s;
  }

  /**
   * We return a String representing a formula that can be passed to
   * redlog, and which says that each of the inequalities in concl
   * is a linear combination of those in prem. Actually, it is not
   * quite this, but rather the "matrix" of that existentially
   * quantified formula (i.e. the part without the quantifiers),
   * /unless/ you use the exqx switch (see below), in which case it
   * will also have existential quantifiers on the x's.
   *
   * The quantifiers are left out now, so that you can conjoin
   * (with ' and ') this formula with others, before wraping the
   * whole thing inside 'rlex( ... )'.
   *
   * Also to permit such conjunctions, you must pass a "thirdIndex"
   * argument, which will be used in addition to the ordinary row
   * and column indices of the variables in this formula, so that
   * you may prevent these variables from colliding with those
   * introduced in another formula constructed by this method. You
   * are responsible for choosing those "thirdIndices" (which
   * actually will be used as the first of three) wisely.
   *
   * @param exqx "existentially quantify the x's"
   * Don't use this if you plan to wrap the formula in "rlex()".
   * If set to true, it will prefix the formula with existential
   * quantification of all and only the x variables (i.e. those in
   * the unknown matrix). In other words, everything /except/ the
   * template parameters will be quantified.
   */
  @Deprecated
  public static String makeRedlogFormula(LinearInequality prem,
                  LinearInequality concl, int thirdIndex) {

    int J = prem.getNumIneqs();
    int K = concl.getNumIneqs();
    int I = prem.getNumVars();

    // So if the inequalities are written as columns, then the
    // prem matrix has J columns, the concl matrix has K columns,
    // and they both have I+1 rows, where the last one is the
    // "LEQ" row (whereas all the others are "EQ" rows).

    String s = "";
    String x = "mkid(mkid(mkid(x,"+Integer.toString(thirdIndex)+"),";
    String xjk;
    String a, b;
    boolean begun_statements = false;
    boolean begun_terms = false;
    HashSet<String> jUsed = new HashSet<>();
    HashSet<String> allx = new HashSet<>();
    Iterator<String> thexjk;

    for (int k = 0; k < K; k++) {

      // No xjk have been used yet, for this k.
      jUsed.clear();

      // I equations:
      for (int i = 0; i < I; i++) {
        // conjoin, if needed:
        if (begun_statements) {
          s += " and ";
        }
        // add J terms:
        for (int j = 0; j < J; j++) {
          a = prem.getCoeff(j, i).toString();
          if (!a.matches("-*0")) {
            // add, as needed:
            if (begun_terms) {
              s += " + ";
            }
            xjk = x+Integer.toString(j)+"),"+Integer.toString(k)+")";
            s += a+"*"+xjk;
            // Have added a term, so:
            begun_terms = true;
            jUsed.add(xjk);
            allx.add(xjk);
          }
        }
        // write 0 if there were no terms:
        if (!begun_terms) {
          s += "0";
        }
        // state the equation:
        b = concl.getCoeff(k, i).toString();
        s += " = "+b;
        // At least one statement has now been made. So:
        begun_statements = true;
        // Finished this statement, so:
        begun_terms = false;
      }

      // 1 inequality:
      // conjoin, if needed:
      if (begun_statements) {
        s += " and ";
      }
      // add J terms:
      for (int j = 0; j < J; j++) {
        a = prem.getRHSCoeff(j).toString();
        if (!a.matches("-*0")) {
          // add, as needed:
          if (begun_terms) {
            s += " + ";
          }
          xjk = x+Integer.toString(j)+"),"+Integer.toString(k)+")";
          s += a+"*"+xjk;
          // Have added a term, so:
          begun_terms = true;
          jUsed.add(xjk);
          allx.add(xjk);
        }
      }
      // write 0 if there were no terms:
      if (!begun_terms) {
        s += "0";
      }
      // state the inequality:
      b = concl.getRHSCoeff(k).toString();
      s += " <= "+b;
      // At least one statement has now been made. So:
      begun_statements = true;
      // Finished this statement, so:
      begun_terms = false;

      // Any xjk used need to be nonnegative.
      thexjk = jUsed.iterator();
      while (thexjk.hasNext()) {
        // If any xjk have been used, then by now we have
        // added at least one statement, so we should say
        // " and ".
        xjk = thexjk.next();
        s += " and " + xjk + " >= 0";
      }
    }

    if (exqx) {
      Iterator<String> thex = allx.iterator();
      String q = "ex({";
      boolean begun = false;
      while (thex.hasNext()) {
        xjk = thex.next();
        if (begun) {
          q += ",";
        }
        q += xjk;
        begun = true;
      }
      q += "},";
      s = q+s+")";
    }

    return s;
  }

}