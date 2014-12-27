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

import java.util.Collection;
import java.util.Collections;

import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

/**
 * The result of a pattern matching call -- see {@link SmtAstMatcher}.
 */
public interface SmtAstMatchResult {

  // TODO: Reduce this interface to functions that are really used

  /**
   * @return  Was there a match for the pattern?
   */
  public boolean matches();

  /**
   * @return  Collection of variables that were bound to a Formula.
   */
  public Collection<String> getBoundVariables();

  /**
   * @param pVariableName   Name of a variable.
   * @return  Collection of {@link Formula} were bound during the matching process to pVariableName.
   */
  public Collection<Formula> getVariableBindings(String pVariableName);

  /**
   * @return  Get the Formula that represents the root of the AST of the matching formula.
   */
  public Optional<Formula> getMatchingRootFormula();

  /**
   * @param pForArgumentPattern   (Sub-)pattern that was used for the matching process.
   * @return  Collection of {@link Formula} for that pForArgumentPattern matched.
   */
  public Collection<Formula> getMatchingArgumentFormula(SmtAstPatternSelectionElement pForArgumentPattern);

  /**
   * @param pFormula  A SMT Formula.
   * @return  Get the collection of variables that were bound to the Formula pFormula.
   */
  public Collection<String> getFormulaBindings(Formula pFormula);

  /**
   * @return A MultiMap that describes what SmtAstPatternSelectionElement matched to which Formulas.
   */
  public ImmutableMultimap<SmtAstPatternSelectionElement, Formula> getMatchings();

  /**
   * Append the bindings from variable names to formula to the Multimap pTarget.
   */
  public void appendBindingsTo(Multimap<String, Formula> pTarget);

  public final static SmtAstMatchResult NOMATCH_RESULT = new SmtAstMatchResult() {

    @Override
    public boolean matches() {
      return false;
    }

    @Override
    public Optional<Formula> getMatchingRootFormula() {
      return Optional.absent();
    }

    @Override
    public Collection<Formula> getMatchingArgumentFormula(SmtAstPatternSelectionElement pMatcher) {
      return Collections.emptySet();
    }

    @Override
    public Collection<Formula> getVariableBindings(String pString) {
      return Collections.emptySet();
    }

    @Override
    public Collection<String> getBoundVariables() {
      return Collections.emptySet();
    }

    @Override
    public void appendBindingsTo(Multimap<String, Formula> pTarget) {}

    @Override
    public Collection<String> getFormulaBindings(Formula pFormula) {
      return Collections.emptySet();
    }

    @Override
    public ImmutableMultimap<SmtAstPatternSelectionElement, Formula> getMatchings() {
      return ImmutableMultimap.of();
    }
  };

}
