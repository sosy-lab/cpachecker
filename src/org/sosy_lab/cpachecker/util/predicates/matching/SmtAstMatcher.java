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
package org.sosy_lab.cpachecker.util.predicates.matching;

import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Optional;
import com.google.common.collect.Multimap;


public interface SmtAstMatcher {

  /**
   * Check whether the Formula matches the pattern that
   * is described in the SmtAstPatternSelection.
   *
   * @param pPatternSelection   The pattern.
   * @param pF                  The SMT formula.
   *
   * @return  Resulting {@link SmtAstMatchResult}} of the matching process.
   */
  public SmtAstMatchResult perform(SmtAstPatternSelection pPatternSelection, Formula pF);

  /**
   * Similar to {@link SmtAstMatcher#perform(SmtAstPatternSelection, Formula)}}
   * but it takes an additional map of <variable name to bound formula> that is
   * used to restrict the possible matches.
   *
   * Patterns can describe a binding of formulas to variables; these provided
   * bindings are used to further restrict possible matches.
   *
   */
  public SmtAstMatchResult perform(
      SmtAstPatternSelection pPatternSelection,
      @Nullable Formula pParent,
      Formula pF,
      Optional<Multimap<String, Formula>> bBindingRestrictions);

  /**
   * Define that a specific function (represented by its name/function symbol)
   * is commutative, for example,
   *
   *    a+b  <-->  b+a
   */
  public void defineCommutative(String pFunctionName);

  /**
   * Define that a specific binary function is equivalent to another function,
   * but with swapped operators, for example,
   *
   *    a >= b  <-->  b <= a
   */
  public void defineRotations(String pFunctionName, String pRotationFunctionName);

  /**
   * Define aliases for a given function.
   */
  public void defineFunctionAliases(String pFunctionName, Set<String> pAliases);

  /**
   * This function should only be used to provide conclusions of inference rules.
   *    Such a method could be provided by {@link FormulaManagerView}} in the future.
   *
   * USING THIS METHOD IS UNSAFE: IT DOES NOT GUARANTEE ANYTHING!
   *  IT WILL BE (hopefully) REMOVED WITHIN THE NEXT FEW MONTHS.
   */
  @Deprecated
  public <T1 extends Formula, T2 extends Formula> T1 substitute(T1 f, Map<T2, T2> fromToMapping);
}
