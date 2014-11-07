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
package org.sosy_lab.cpachecker.util.predicates.z3;

import static org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApi.*;

import java.util.Collections;
import java.util.List;

import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractQuantifiedFormulaManager;

import com.google.common.primitives.Longs;


class Z3QuantifiedFormulaManager extends AbstractQuantifiedFormulaManager<Long, Long, Long> {

  private final long z3context;

  Z3QuantifiedFormulaManager(Z3FormulaCreator creator) {
    super(creator);
    this.z3context = creator.getEnv();
  }

  @Override
  protected Long exists(List<Long> pVariables, Long pBody) {
    return Z3NativeApi.mk_exists_const(
        z3context,
        0,
        pVariables.size(),
        Longs.toArray(pVariables),
        0,
        Longs.toArray(Collections.<Long>emptyList()),
        pBody);
  }

  @Override
  protected Long forall(List<Long> pVariables, Long pBody) {
    return Z3NativeApi.mk_forall_const(
        z3context,
        0,
        pVariables.size(),
        Longs.toArray(pVariables),
        0,
        Longs.toArray(Collections.<Long>emptyList()),
        pBody);
  }

  private long applyTactic(Long pF, String pTactic) throws InterruptedException, SolverException {
    long tactic_qe = mk_tactic(z3context, pTactic);
    tactic_inc_ref(z3context, tactic_qe);

    long goal = mk_goal(z3context, true, true, false);
    goal_inc_ref(z3context, goal);

    try {
      goal_assert(z3context, goal, pF);

      long applyResult = tactic_apply(z3context, tactic_qe, goal);
      apply_result_inc_ref(z3context, applyResult);

      try {
        long resultSubGoal = apply_result_get_subgoal(z3context, applyResult, 0);
        goal_inc_ref(z3context, resultSubGoal);

        // TODO: Check the reference-counting!!

        // CNF!
        int goalResultItemCount = goal_size(z3context, resultSubGoal);
        long[] goalResultItems = new long[goalResultItemCount];
        for(int i=0; i<goalResultItemCount; i++) {
          long subGoalFormula = goal_formula(z3context, resultSubGoal, i);
          inc_ref(z3context, subGoalFormula);

          goalResultItems[i] = subGoalFormula;
        }

        goal_dec_ref(z3context, resultSubGoal);

        assert goalResultItemCount > 0;

        long result;
        if (goalResultItemCount > 1) {
          result = mk_and(z3context, goalResultItems);

          for(int i=0; i<goalResultItems.length; i++) {
            dec_ref(z3context, goalResultItems[i]);
          }

        } else {
          result = goalResultItems[0];
        }

        return result;

      } finally {
        apply_result_dec_ref(z3context, applyResult);
      }

    } finally {
      goal_dec_ref(z3context, goal);
      tactic_dec_ref(z3context, tactic_qe);
    }
  }

  @Override
  protected Long eliminateQuantifiers(Long pExtractInfo) throws SolverException, InterruptedException {
    // It is recommended (personal communication with Nikolaj Bjorner) to run "qe-light" before "qe".
    //  "qe" does not perform a "qe-light" as a preprocessing on its own!

    // You might want to run the tactic "ctx-solver-simplify" on the result...

    return applyTactic(applyTactic(pExtractInfo, "qe-light"), "qe");
  }

}
