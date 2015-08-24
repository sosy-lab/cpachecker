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
package org.sosy_lab.cpachecker.util.precondition.segkro;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.sosy_lab.solver.SolverException;
import org.sosy_lab.cpachecker.util.precondition.segkro.interfaces.Rule;
import org.sosy_lab.cpachecker.util.precondition.segkro.rules.EliminationRule;
import org.sosy_lab.cpachecker.util.precondition.segkro.rules.EquivalenceRule;
import org.sosy_lab.cpachecker.util.precondition.segkro.rules.InEqualityRule;
import org.sosy_lab.cpachecker.util.precondition.segkro.rules.RuleEngine;
import org.sosy_lab.solver.Solver;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;

/**
 * This class uses a set of inference rules (for range predicates).
 * Taken from Seghir and Kroening, 2013, Counterexample-guided Precondition Inference
 */
public class ExtractNewPreds {

  private final FormulaManagerView mgrv;
  private final RuleEngine ruleEngine;

  private Ordering<Integer> ordering = new Ordering<Integer>() {
    @Override
    public int compare(Integer left, Integer right) {
        return Ints.compare(left, right);
    }
  };

  public ExtractNewPreds(Solver pSolver, RuleEngine ruleEngine) {
    this.ruleEngine = Preconditions.checkNotNull(ruleEngine);
    this.mgrv = pSolver.getFormulaManager();
  }

  public List<BooleanFormula> extractNewPreds(Collection<BooleanFormula> pBasePredicates) throws SolverException, InterruptedException {
    final List<BooleanFormula> resultPredicates = Lists.newArrayList();
    final LinkedList<BooleanFormula> resultPredicatesPrime = Lists.newLinkedList();

    // Start with the list of basic predicates.
    //    This predicates have (initially) the LOWEST PRIORITY!!!

    resultPredicatesPrime.addAll(ruleEngine.canonicalize(pBasePredicates));

    // Keep applying the rules until no new predicates get produced
    do {
      resultPredicates.clear();
      resultPredicates.addAll(resultPredicatesPrime);

      for (Rule rule: ruleEngine.getRules()) {
        // We have to iterate over a tuple that is element of resultPredicates^premiseCount.
        //
        // Example: For a rule with 3 premises, we iterate over
        //    the cross product resultPredicates^3:
        //      resultPredicates × resultPredicates × resultPredicates
        //
        final int premiseCount = rule.getPremises().size();
        assert premiseCount != 0;
        final List<List<BooleanFormula>> dimensions = new ArrayList<>(premiseCount);
        for (int i=0; i<premiseCount; i++) {
          dimensions.add(resultPredicates);
        }

        for (List<BooleanFormula> tuple: Cartesian.product(dimensions)) {
          boolean containsFormulasNotFrom = false;
          for (BooleanFormula f: tuple) {
            if (!pBasePredicates.contains(f)) {
              containsFormulasNotFrom = true; // TODO: Test this!!
              break;
            }
          }
          final boolean tupleContainsOnlyBasePredicates = !containsFormulasNotFrom;

          // The rules ELIM and EQ are only applied to the base predicates!!
          final boolean isBasicPredicatesOnlyRule = false
              || rule instanceof InEqualityRule
              || rule instanceof EliminationRule
              || rule instanceof EquivalenceRule;

          if ((!isBasicPredicatesOnlyRule) || tupleContainsOnlyBasePredicates) {
            // Conclude new, general, predicates.
            Collection<BooleanFormula> concluded = rule.applyWithInputRelatingPremises(tuple);

            if (!concluded.isEmpty()) {

              // Store predicates according to their priority.
              //    Put the new predicates (that are more general than the predicates in the premise)
              //    after the predicates that were used as premise
              //  (the predicate with the highest priority is on the end of the list)

              // Maximal position of a predicate from the tuple in 'resultPredicates'
              List<Integer> positions = Lists.newArrayList();
              for (int posInResult=0; posInResult<resultPredicates.size(); posInResult++) {
                for (BooleanFormula tf: tuple) {
                  if (resultPredicates.get(posInResult).equals(tf)) {
                    positions.add(posInResult);
                  }
                }
              }
              final int maxPosInResult = ordering.max(positions);

              for (BooleanFormula p: concluded) {
                if (!resultPredicatesPrime.contains(p)) {
                  // insert p after position pos in lPrime
                  resultPredicatesPrime.add(maxPosInResult+1, p);
                }
              }
            }
          }
        }
      }

      // Fix-point iteration: Until now new pre dicates are produced.
    } while(!resultPredicates.equals(resultPredicatesPrime)); // TODO: Does this compare what was intended?

    // Store new predicates (instantiated!!) according to their priority;
    return resultPredicates;
  }

  /**
   * Produces a priorised set of predicates from a conjunctive formula.
   *
   *    Idea: Predicates that are more general have a higher priority.
   *
   *    Given a rule R, with a premise P and a conclusion C,
   *    and two predicates p1 and p2.
   *      Priority(p1) is higher than Priority(p2), if p1 ∈  C and p2 ∈  P.
   *
   * @param pConjunctiveFormula
   *          Formula consists of a conjunction of atoms; disjunctions are not considered explicitly.
   *
   * @return  List of predicates in ascending order (predicates with higher priority first)
   * @throws InterruptedException
   * @throws SolverException
   */
  public List<BooleanFormula> extractNewPreds(BooleanFormula pConjunctiveFormula) throws SolverException, InterruptedException {
    // Start with the list of basic predicates
    //  (extracted from the conjunctive formula)
    // TODO: check whether to use extractDisjuncts instead
    Collection<BooleanFormula> literals = mgrv.extractLiterals(pConjunctiveFormula);
    return extractNewPreds(literals);
  }

}
