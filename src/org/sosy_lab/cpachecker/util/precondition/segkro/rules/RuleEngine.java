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
package org.sosy_lab.cpachecker.util.precondition.segkro.rules;

import java.io.PrintStream;
import java.util.Collection;
import java.util.List;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.solver.SolverException;
import org.sosy_lab.cpachecker.util.precondition.segkro.interfaces.Canonicalizer;
import org.sosy_lab.cpachecker.util.precondition.segkro.interfaces.Concluding;
import org.sosy_lab.cpachecker.util.precondition.segkro.interfaces.Rule;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstMatcher;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.statistics.AbstractStatistics;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class RuleEngine implements Concluding, StatisticsProvider, Canonicalizer {

  private final List<Rule> rules;

  private class RuleEngineStatistics extends AbstractStatistics {
    @Override
    public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
      for (Rule r: rules) {
        if (r instanceof PatternBasedRule) {
          PatternBasedRule pr = (PatternBasedRule) r;
          put(pOut, 1, "Rule", pr.getRuleName());
          put(pOut, 2, pr.overallTimer.getTitle(), pr.overallTimer.toString());
          put(pOut, 3, "Number of derived conclusions", pr.conclusionTimer.getUpdateCount());
          put(pOut, 3, pr.conclusionTimer.getTitle(), pr.conclusionTimer.toString());
          put(pOut, 3, pr.constraintCheckTimer.getTitle(), pr.constraintCheckTimer.toString());
          put(pOut, 3, pr.matchingTimer.getTitle(), pr.matchingTimer.toString());
          put(pOut, 3, pr.conclusionValidationTimer.getTitle(), pr.conclusionValidationTimer.toString());
        }
      }
    }
  }
  private final RuleEngineStatistics stats = new RuleEngineStatistics();
  private final Canonicalizer canon;

  public RuleEngine(LogManager pLogger, Solver pSolver) {
    final SmtAstMatcher matcher = pSolver.getSmtAstMatcher();

    // ATTENTION: The ordering of the rules is important!!!!!!
    rules = Lists.newArrayList();
    rules.add(new InEqualityRule(pSolver, matcher));
    rules.add(new LinCombineRule(pSolver, matcher));
    rules.add(new EliminationRule(pSolver, matcher));
    rules.add(new EquivalenceRule(pSolver, matcher));
    rules.add(new UniversalizeRule(pSolver, matcher));
    rules.add(new SubstitutionRule(pSolver, matcher));
    rules.add(new LinkRule(pSolver, matcher));
    rules.add(new ExistentialRule(pSolver, matcher));
    rules.add(new ExtendLeftRule(pSolver, matcher));
    rules.add(new ExtendRightRule(pSolver, matcher));
    // ATTENTION: The ordering of the rules is important!!!!!!

    this.canon = new DefaultCanonicalizer(pSolver, matcher);
  }

  public ImmutableList<Rule> getRules() {
    return ImmutableList.copyOf(rules);
  }

  @Override
  public BooleanFormula concludeFromAtoms(List<BooleanFormula> pAtomPredicates) {
    return null;
  }

  /**
   *
   * @param pAtomPredicates
   * @param pRule
   * @return    List of atomic predicates
   */
  public List<BooleanFormula> concludeWithSingleRule(List<BooleanFormula> pAtomPredicates, Rule pRule) {
    return null;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }

  @Override
  public BooleanFormula canonicalize(BooleanFormula pPredicate)
      throws SolverException, InterruptedException {

    return canon.canonicalize(pPredicate);
  }

  @Override
  public Collection<BooleanFormula> canonicalize(Collection<BooleanFormula> pPredicates)
      throws SolverException, InterruptedException {

    return canon.canonicalize(pPredicates);
  }

}
