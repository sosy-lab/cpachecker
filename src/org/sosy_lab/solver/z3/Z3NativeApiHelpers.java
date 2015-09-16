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
package org.sosy_lab.solver.z3;

import static org.sosy_lab.solver.z3.Z3NativeApi.*;

import org.sosy_lab.solver.SolverException;


public class Z3NativeApiHelpers {
  /**
   * Apply multiple tactics in sequence.
   */
  static long applyTactics(long z3context, final Long pF, String ... pTactics) throws InterruptedException, SolverException {
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

  /**
   * Apply tactic on a Z3_ast object, convert the result back to Z3_ast.
   *
   * @param z3context Z3_context
   * @param tactic Z3 Tactic Name
   * @param pF Z3_ast
   * @return Z3_ast
   */
  static long applyTactic(long z3context, long pF, String tactic) {
    long tseitinTactic = mk_tactic(z3context, tactic);
    tactic_inc_ref(z3context, tseitinTactic);

    long goal = mk_goal(z3context, true, false, false);
    goal_inc_ref(z3context, goal);
    goal_assert(z3context, goal, pF);

    long result = tactic_apply(z3context, tseitinTactic, goal);
    apply_result_inc_ref(z3context, result);

    try {
      return applyResultToAST(z3context, result);
    } finally {
      apply_result_dec_ref(z3context, result);
      goal_dec_ref(z3context, goal);
      tactic_dec_ref(z3context, tseitinTactic);
    }
  }

  private static long applyResultToAST(long z3context, long applyResult) {
    int no_subgoals = apply_result_get_num_subgoals(z3context, applyResult);
    long[] goal_formulas = new long[no_subgoals];

    for (int i=0; i<no_subgoals; i++) {
      long subgoal = apply_result_get_subgoal(z3context, applyResult, i);
      goal_inc_ref(z3context, subgoal);
      long subgoal_ast = goalToAST(z3context, subgoal);
      inc_ref(z3context, subgoal_ast);
      goal_formulas[i] = subgoal_ast;
      goal_dec_ref(z3context, subgoal);
    }
    try {
      return goal_formulas.length == 1 ?
          goal_formulas[0] :
          mk_or(z3context, goal_formulas.length, goal_formulas);
    } finally {
      for (int i=0; i<no_subgoals; i++) {
        dec_ref(z3context, goal_formulas[i]);
      }
    }
  }

  private static long goalToAST(long z3context, long goal) {
    int no_subgoal_f = goal_size(z3context, goal);
    long[] subgoal_formulas = new long[no_subgoal_f];
    for (int k=0; k<no_subgoal_f; k++) {
      long f = goal_formula(z3context, goal, k);
      inc_ref(z3context, f);
      subgoal_formulas[k] = f;
    }
    try {
      return subgoal_formulas.length == 1 ?
          subgoal_formulas[0] :
          mk_and(z3context, subgoal_formulas.length, subgoal_formulas);
    } finally {
      for (int k=0; k<no_subgoal_f; k++) {
        dec_ref(z3context, subgoal_formulas[k]);
      }
    }
  }
}
