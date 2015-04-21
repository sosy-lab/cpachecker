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

import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

public class SmtAstMatchResultImpl implements SmtAstMatchResult {

  private final Multimap<SmtAstPatternSelectionElement, Formula> argumentPatternMatches;
  private final Multimap<String, Formula> variableBindings;
  private final Multimap<Formula, String> variableBindingsReverse;
  private Formula matchingRootFormula;

  public SmtAstMatchResultImpl() {
    this.argumentPatternMatches = HashMultimap.create();
    this.variableBindings = HashMultimap.create();
    this.variableBindingsReverse = HashMultimap.create();
  }

  public void putMatchingArgumentFormula(SmtAstPatternSelectionElement pArgumentPattern, Formula pMatchingFormula) {
    argumentPatternMatches.put(pArgumentPattern, pMatchingFormula);
  }

  public void setMatchingRootFormula(Formula pMatchingFormula) {
    matchingRootFormula = pMatchingFormula;
  }

  public void putBoundVaribale(String pVariable, Formula pBoundFormula) {
    variableBindings.put(pVariable, pBoundFormula);
    variableBindingsReverse.put(pBoundFormula, pVariable);
  }

  @Override
  public Collection<Formula> getMatchingArgumentFormula(SmtAstPatternSelectionElement pArgumentPattern) {
    return argumentPatternMatches.get(pArgumentPattern);
  }

  @Override
  public Optional<Formula> getMatchingRootFormula() {
    Preconditions.checkNotNull(matchingRootFormula);
    return Optional.of(matchingRootFormula);
  }

  @Override
  public Collection<Formula> getVariableBindings(String pString) {
    return variableBindings.get(pString);
  }

  @Override
  public boolean matches() {
    Preconditions.checkNotNull(matchingRootFormula);
    return true;
  }

  @Override
  public Collection<String> getBoundVariables() {
    return variableBindings.keySet();
  }

  @Override
  public String toString() {
    return String.format("VarBindings: %s", variableBindings.toString());
  }

  @Override
  public void appendBindingsTo(Multimap<String, Formula> pTarget) {
    pTarget.putAll(variableBindings);
  }

  public void addSubResults (SmtAstMatchResult pSubResults) {
    SmtAstMatchResultImpl sr = (SmtAstMatchResultImpl) pSubResults;

    this.argumentPatternMatches.putAll(sr.argumentPatternMatches);
    this.variableBindings.putAll(sr.variableBindings);
  }

  @Override
  public Collection<String> getFormulaBindings(Formula pFormula) {
    return variableBindingsReverse.get(pFormula);
  }

  @Override
  public ImmutableMultimap<SmtAstPatternSelectionElement, Formula> getMatchings() {
    return ImmutableMultimap.copyOf(argumentPatternMatches);
  }

}
