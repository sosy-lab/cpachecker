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
package org.sosy_lab.cpachecker.util.predicates.interfaces;

import java.util.List;


/**
 * This interface represents the Boolean-Theory
 */
public interface BooleanFormulaManager {
  /**
   * Checks if the given {@link Formula} is a boolean.
   *
   * @param pF the <code>Formula</code> to check
   * @return <code>true</code> if the given <code>Formula</code> is boolean,
   *         <code>false</code> otherwise
   */
  public boolean isBoolean(Formula pF);

  /**
   * Returns a {@link BooleanFormula} representing the given value.
   *
   * @param value the boolean value the returned <code>Formula</code> should represent
   * @return a Formula representing the given value
   */
  public BooleanFormula makeBoolean(boolean value);

  public BooleanFormula makeVariable(String pVar);

  /**
   * Creates a formula representing an equivalence of the two arguments.
   * @param formula1 a Formula
   * @param formula2 a Formula
   * @return (formula1 <-> formula2)
   */
  public BooleanFormula equivalence(BooleanFormula formula1, BooleanFormula formula2);

  /** Check, if the formula is of the form "a==b" with two boolean args. */
  public boolean isEquivalence(BooleanFormula formula);

  /** Check, if the formula is of the form "a=>b" with two boolean args. */
  public boolean isImplication(BooleanFormula formula);

  /**
   * Check, if the formula is the formula "TRUE".
   * This does not include a satisfiability check,
   * but only a syntactical matching.
   * However, depending on the SMT solver,
   * there might be some pre-processing of formulas such that trivial cases
   * like "1==1" are recognized and rewritten as "TRUE",
   * and thus such formulas might also be matched.
   */
  public boolean isTrue(BooleanFormula formula);

  /**
   * Check, if the formula is the formula "FALSE".
   * This does not include a satisfiability check,
   * but only a syntactical matching.
   * However, depending on the SMT solver,
   * there might be some pre-processing of formulas such that trivial cases
   * like "1==2" are recognized and rewritten as "FALSE",
   * and thus such formulas might also be matched.
   */
  public boolean isFalse(BooleanFormula formula);


  /**
   * Creates a formula representing "IF cond THEN f1 ELSE f2"
   * @param cond a Formula
   * @param f1 a Formula
   * @param f2 a Formula
   * @return (IF cond THEN f1 ELSE f2)
   */
  public <T extends Formula> T ifThenElse(BooleanFormula cond, T f1, T f2);

  /** Check, if the formula matches ITE(cond,t1,t2) with three args. */
  public <T extends Formula> boolean isIfThenElse(T f);

  /**
   * Creates a formula representing a negation of the argument.
   * @param bits a Formula
   * @return (!bits)
   */
  public BooleanFormula not(BooleanFormula bits);

  /**
   * Creates a formula representing an AND of the two arguments.
   * @param bits1 a Formula
   * @param bits2 a Formula
   * @return (bits1 & bits2)
   */
  public BooleanFormula and(BooleanFormula bits1, BooleanFormula bits2);
  public BooleanFormula and(List<BooleanFormula> bits);

  /**
   * Creates a formula representing an OR of the two arguments.
   * @param bits1 a Formula
   * @param bits2 a Formula
   * @return (bits1 | bits2)
   */
  public BooleanFormula or(BooleanFormula bits1, BooleanFormula bits2);
  public BooleanFormula or(List<BooleanFormula> bits);

  public BooleanFormula xor(BooleanFormula bits1, BooleanFormula bits2);


  /** Check, if the formula matches NOT(f) with one boolean arg. */
  public boolean isNot(BooleanFormula bits);

  /** Check, if the formula matches AND(a,b) with two (or more) boolean args. */
  public boolean isAnd(BooleanFormula bits);

  /** Check, if the formula matches OR(a,b) with two (or more) boolean args. */
  public boolean isOr(BooleanFormula bits);

  /** Check, if the formula matches XOR(a,b) with two (or more) boolean args. */
  public boolean isXor(BooleanFormula bits);

  /** Apply a tactic which performs formula transformation */
  public BooleanFormula applyTactic(BooleanFormula input, Tactic tactic);

  /** Strategies for transforming the formula AST. */
  enum Tactic {
    NNF("nnf"),
    CNF("tseitin-cnf")
    ;

    private final String name;

    Tactic(String pName) {
      name = pName;
    }

    public String getTacticName() {
      return name;
    }
  }
}
