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

import java.util.Collection;

import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractBooleanFormulaManager;

import com.google.common.primitives.Longs;

class Z3BooleanFormulaManager extends AbstractBooleanFormulaManager<Long, Long, Long> {

  private final long z3context;

  Z3BooleanFormulaManager(Z3FormulaCreator creator) {
    super(creator);
    this.z3context = creator.getEnv();
  }

  @Override
  protected Long makeVariableImpl(String varName) {
    long type = getFormulaCreator().getBoolType();
    return getFormulaCreator().makeVariable(type, varName);
  }

  @Override
  protected Long makeBooleanImpl(boolean pValue) {
    if (pValue) {
      return mk_true(z3context);
    } else {
      return mk_false(z3context);
    }
  }

  @Override
  protected Long not(Long pParam) {
    return mk_not(z3context, pParam);
  }

  @Override
  protected Long and(Long pParam1, Long pParam2) {
    return mk_and(z3context, pParam1, pParam2);
  }

  @Override
  protected Long or(Long pParam1, Long pParam2) {
    return mk_or(z3context, pParam1, pParam2);
  }

  @Override
  protected Long orImpl(Collection<Long> params) {
    return mk_or(z3context, params.size(), Longs.toArray(params));
  }

  @Override
  protected Long andImpl(Collection<Long> params) {
    return mk_and(z3context, params.size(), Longs.toArray(params));
  }

  @Override
  protected Long xor(Long pParam1, Long pParam2) {
    return mk_xor(z3context, pParam1, pParam2);
  }

  @Override
  protected boolean isNot(Long pParam) {
    return isOP(z3context, pParam, Z3_OP_NOT);
  }

  @Override
  protected boolean isAnd(Long pParam) {
    return isOP(z3context, pParam, Z3_OP_AND);
  }

  @Override
  protected boolean isOr(Long pParam) {
    return isOP(z3context, pParam, Z3_OP_OR);
  }

  @Override
  protected boolean isXor(Long pParam) {
    return isOP(z3context, pParam, Z3_OP_XOR);
  }

  @Override
  protected Long equivalence(Long pBits1, Long pBits2) {
    return mk_eq(z3context, pBits1, pBits2);
  }

  @Override
  protected Long implication(Long pBits1, Long pBits2) {
    return mk_implies(z3context, pBits1, pBits2);
  }

  @Override
  protected boolean isTrue(Long pParam) {
    return isOP(z3context, pParam, Z3_OP_TRUE);
  }

  @Override
  protected boolean isFalse(Long pParam) {
    return isOP(z3context, pParam, Z3_OP_FALSE);
  }

  @Override
  protected Long ifThenElse(Long pCond, Long pF1, Long pF2) {
    return mk_ite(z3context, pCond, pF1, pF2);
  }

  @Override
  protected boolean isEquivalence(Long pParam) {
    return isOP(z3context, pParam, Z3_OP_EQ)
        && get_app_num_args(z3context,pParam) == 2
        && get_sort(z3context, get_app_arg(z3context, pParam, 0)) == Z3_BOOL_SORT
        && get_sort(z3context, get_app_arg(z3context, pParam, 1)) == Z3_BOOL_SORT;
  }

  @Override
  protected boolean isImplication(Long pParam) {
    return isOP(z3context, pParam, Z3_OP_IMPLIES);
  }

  @Override
  protected boolean isIfThenElse(Long pParam) {
    return isOP(z3context, pParam, Z3_OP_ITE);
  }

  /**
   * @param pParam Z3_ast
   * @return Z3_ast with the tactic applied.
   */
  @Override
  public Long applyTacticImpl(Long pParam, Tactic tactic) {
    long tseitinTactic = mk_tactic(z3context, tactic.getTacticName());
    tactic_inc_ref(z3context, tseitinTactic);

    long goal = mk_goal(z3context, true, false, false);
    goal_inc_ref(z3context, goal);
    goal_assert(z3context, goal, pParam);

    long result = tactic_apply(z3context, tseitinTactic, goal);
    apply_result_inc_ref(z3context, result);

    try {
      return applyResultToAST(result);
    } finally {
      apply_result_dec_ref(z3context, result);
      goal_dec_ref(z3context, goal);
      tactic_dec_ref(z3context, tseitinTactic);
    }
  }

  private long applyResultToAST(long applyResult) {
    int no_subgoals = apply_result_get_num_subgoals(z3context, applyResult);
    long[] goal_formulas = new long[no_subgoals];

    for (int i=0; i<no_subgoals; i++) {
      long subgoal = apply_result_get_subgoal(z3context, applyResult, i);
      goal_inc_ref(z3context, subgoal);
      long subgoal_ast = goalToAST(subgoal);
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

  private long goalToAST(long goal) {
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
