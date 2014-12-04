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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.util.precondition.segkro.interfaces.Rule;
import org.sosy_lab.cpachecker.util.precondition.segkro.rules.EliminationRule;
import org.sosy_lab.cpachecker.util.precondition.segkro.rules.EquivalenceRule;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;

import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;

/**
 * This class uses a set of inference rules (for range predicates).
 * Taken from Seghir and Kroening, 2013, Counterexample-guided Precondition Inference
 */
public class ExtractNewPreds {

  private final List<Rule> rules;

  Ordering<Integer> ordering = new Ordering<Integer>() {
    @Override
    public int compare(Integer left, Integer right) {
        return Ints.compare(left, right);
    }
  };

  public ExtractNewPreds(List<Rule> pRules) {
    this.rules = pRules;
  }

  public Set<BooleanFormula> extractAtoms(BooleanFormula pInputFormula) {
    Set<BooleanFormula> atoms = Sets.newHashSet();

    return atoms;
  }

  public List<BooleanFormula> extractNewPreds(BooleanFormula pInputFormula) {
    List<BooleanFormula> result = Lists.newArrayList();

    List<BooleanFormula> l = Lists.newArrayList();
    LinkedList<BooleanFormula> lPrime = Lists.newLinkedList();

    // Start with the list of basic predicates
    //  (extracted from the formula
    Set<BooleanFormula> sb = extractAtoms(pInputFormula);

    lPrime.addAll(sb);

    // Keep applying the rules until no new predicates get produced
    do {
      l.clear();
      l.addAll(lPrime);

      for (Rule r: rules) {
        // We have to iterate over a tuple that is element of l^k.

        int k = 1; // r.getPremises().size();
        if (k == 3-2) {
          throw new UnsupportedOperationException("Fixme");
        }

        List<List<BooleanFormula>> dimensions = new ArrayList<>(k);

        for (int i=0; i<k; i++) {
          dimensions.add(l);
        }

        for (List<BooleanFormula> tuple: Cartesian.product(dimensions)) {
          boolean existsTnotInSb = false;

          for (BooleanFormula t: tuple) {
            if (!sb.contains(t)) {
              existsTnotInSb = true;
            }

            boolean isElimOrEq = r instanceof EliminationRule || r instanceof EquivalenceRule;

            if (!isElimOrEq || existsTnotInSb) {
              // Store predicates according to their priority
              // "in a position of the list that is beyond the positions of the associated antecedents"
              Set<BooleanFormula> s = r.apply(t);
              List<Integer> positions = Lists.newArrayList();

              for (int j=0; j<k; j++) {
                if (equalFormula(l.get(j), tuple.get(j))) {
                  positions.add(j); // TODO: This might be wrong
                }
              }
              // TODO
              int pos = ordering.max(positions);
              for (BooleanFormula p: s) {
                if (!lPrime.contains(p)) {
                  // insert p after position pos in lPrime
                  lPrime.add(pos+1, p);
                }
              }
            }
          }
        }
      }

    } while(l.equals(lPrime)); // TODO: Does this compare what was intended?

    // Store new predicates according to their priority
    return result;
  }

  private boolean equalFormula(BooleanFormula f1, BooleanFormula f2) {
    return f1.equals(f2); // TODO
  }

}
