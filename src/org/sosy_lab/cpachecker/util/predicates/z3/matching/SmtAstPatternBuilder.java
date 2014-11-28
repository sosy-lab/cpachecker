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
package org.sosy_lab.cpachecker.util.predicates.z3.matching;

import java.util.Arrays;
import java.util.Collections;

import org.sosy_lab.cpachecker.util.predicates.z3.matching.SmtAstPatternImpl.PatternLogic;

import com.google.common.base.Optional;



public class SmtAstPatternBuilder {

  // Idea: Large parts of SMT formulas are described using functions.
  //  --> We can simplify the matching problem!!
  //
  //  Considering let-statements is not necessary because the are "only"
  //    used for representing common sub formulas, i.e., with multiple entry-edges (DAG)
  //
  //  We operate on the DAG

  /**
   * Match a specific function. This might be either a
   *  ... nullary (constant, variable)
   *  ... unary ('boolean not', ...)
   *  ... binary (equality, implication, different arithmetic operations, ...
   *  ... ternary (ITE, ...)
   *  ... n-ary (and, or, ... user-defined functions, ...)
   *
   * @param pFunction         String symbol that represents the function (as declared)
   * @param argumentMatchers  Matchers for the arguments of the function
   *
   * @return  A description of the pattern that matches the specified sub-formula within the AST
   */
  public static SmtAstPattern match(Comparable<?> pFunction, SmtAstPattern... argumentMatchers) {
    return new SmtAstPatternImpl(
        Optional.<Comparable<?>>of(pFunction),
        Optional.<String>absent(),
        Arrays.asList(argumentMatchers),
        PatternLogic.ALL);
  }

  /**
   * Matches only if all argument patterns of an arbitrary function match.
   *
   * The root node of the AST is also considered as a function with childs!!
   *
   * @param argumentMatchers  The child patterns.
   */
  public static SmtAstPattern match(SmtAstPattern... argumentMatchers) {
    return new SmtAstPatternImpl(
        Optional.<Comparable<?>>absent(),
        Optional.<String>absent(),
        Arrays.asList(argumentMatchers),
        PatternLogic.ALL);
  }

  /**
   * The same as described in {@link #match}, but binds the matching formula to a variable.
   */
  public static SmtAstPattern matchBind(Comparable<?> pFunction, String pBindMatchTo, SmtAstPattern... argumentMatchers) {
    return new SmtAstPatternImpl(
        Optional.<Comparable<?>>of(pFunction),
        Optional.of(pBindMatchTo),
        Arrays.asList(argumentMatchers),
        PatternLogic.ALL);
  }

  /**
   * Matches any function application.
   *
   * @return  Pattern.
   */
  public static SmtAstPattern matchAny() {
    return new SmtAstPatternImpl(
        Optional.<Comparable<?>>absent(),
        Optional.<String>absent(),
        Collections.<SmtAstPattern>emptyList(),
        PatternLogic.ANY);
  }

  /**
   * The same as described in {@link #matchAny}, but binds the matching formula to a variable.
   */
  public static SmtAstPattern matchAnyBind(String pBindMatchTo) {
    return new SmtAstPatternImpl(
        Optional.<Comparable<?>>absent(),
        Optional.<String>of(pBindMatchTo),
        Collections.<SmtAstPattern>emptyList(),
        PatternLogic.ALL);
  }

  /**
   * Matches only if NONE of the patterns matches on the arguments of the specific function application.
   *
   * @param pFunction
   * @param argumentMatchers
   * @return
   */
  public static SmtAstPattern matchIfNot(Comparable<?> pFunction, SmtAstPattern... argumentMatchers) {
    return new SmtAstPatternImpl(
        Optional.<Comparable<?>>of(pFunction),
        Optional.<String>absent(),
        Arrays.asList(argumentMatchers),
        PatternLogic.NONE);
  }

  /**
   * Matches only if NONE of the patterns matches an arbitrary function application.
   *
   * @param argumentMatchers
   * @return
   */
  public static SmtAstPattern matchIfNot(SmtAstPattern... pMatchers) {
    return new SmtAstPatternImpl(
        Optional.<Comparable<?>>absent(),
        Optional.<String>absent(),
        Arrays.asList(pMatchers),
        PatternLogic.NONE);
  }

  public static SmtAstPattern matchNullaryBind(String pBindMatchTo) {
    return new SmtAstPatternImpl(
        Optional.<Comparable<?>>absent(),
        Optional.<String>of(pBindMatchTo),
        Collections.<SmtAstPattern>emptyList(), // This also means, that a matching function must have no parameters! (PatternLogic.ALL can be taken into account)
        PatternLogic.ALL);
  }


}
