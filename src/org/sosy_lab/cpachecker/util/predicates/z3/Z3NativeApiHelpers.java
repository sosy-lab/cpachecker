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
import static org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApiConstants.*;

import org.sosy_lab.cpachecker.exceptions.SolverException;


public class Z3NativeApiHelpers {

  public static long applyTactic(long z3context, Long pF, String pTactic) {
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
        long[] goalResultItems = new long[Math.max(1, goalResultItemCount)];
        if (goalResultItemCount == 0) {
          goalResultItems[0] = mk_true(z3context); // TODO: This is just a hack. Z3 provides the results for TRUE/FALSE in a different structure...
        } else {
          for(int i=0; i<goalResultItemCount; i++) {
            long subGoalFormula = goal_formula(z3context, resultSubGoal, i);
            inc_ref(z3context, subGoalFormula);

            goalResultItems[i] = subGoalFormula;
          }
        }

        goal_dec_ref(z3context, resultSubGoal);

        long result;
        if (goalResultItemCount > 1) {
          result = mk_and(z3context, goalResultItems);
          inc_ref(z3context, result);

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

  public static long applyTactics(long z3context, final Long pF, String ... pTactics) throws InterruptedException, SolverException {
    long overallResult = pF;
    for (String tactic: pTactics) {
      long tacticResult = applyTactic(z3context, overallResult, tactic);
      if (overallResult != pF) {
        dec_ref(z3context, overallResult);
      }
      overallResult = tacticResult;
    }
    return overallResult;
  }

  public static String getDeclarationSymbolText(long pContext, long pDeclaration) {
    long symbol = get_decl_name(pContext, pDeclaration);
    switch (get_symbol_kind(pContext, symbol)) {
    case Z3_INT_SYMBOL:
      return Integer.toString(get_symbol_int(pContext, symbol));
    case Z3_STRING_SYMBOL:
      return get_symbol_string(pContext, symbol);
    default:
      throw new UnsupportedOperationException("getDeclarationSymbolText: Kind of symbol not yet supported! Implement it!");
    }
  }

}
