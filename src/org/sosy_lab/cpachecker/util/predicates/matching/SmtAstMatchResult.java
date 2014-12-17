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


public interface SmtAstMatchResult {

  public Collection<Formula> getMatchingArgumentFormula(SmtAstPattern pForArgumentPattern);

  public Optional<Formula> getMatchingRootFormula();

  public Collection<String> getBoundVariables();

  public Collection<Formula> getVariableBindings(String pVariableName);

  public Collection<String> getFormulaBindings(Formula pFormula);

  public ImmutableMultimap<SmtAstPatternSelectionElement, Formula> getMatchings();

  public void appendBindingsTo(Multimap<String, Formula> pTarget);

  public boolean matches();

  public final static SmtAstMatchResult NOMATCH_RESULT = new SmtAstMatchResult() {
    @Override
    public boolean matches() { return false; }
    @Override
    public Optional<Formula> getMatchingRootFormula() { return Optional.absent(); }
    @Override
    public Collection<Formula> getMatchingArgumentFormula(SmtAstPattern pMatcher) { return Collections.emptySet(); }
    @Override
    public Collection<Formula> getVariableBindings(String pString) { return Collections.emptySet(); }
    @Override
    public Collection<String> getBoundVariables() { return Collections.emptySet(); }
    @Override
    public void appendBindingsTo(Multimap<String, Formula> pTarget) { }
    @Override
    public Collection<String> getFormulaBindings(Formula pFormula) { return Collections.emptySet(); }
    @Override
    public ImmutableMultimap<SmtAstPatternSelectionElement, Formula> getMatchings() {return ImmutableMultimap.of(); }
  };

}
