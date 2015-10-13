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

import static org.sosy_lab.solver.z3.Z3NativeApiConstants.*;

import java.util.Collections;
import java.util.List;

import org.sosy_lab.solver.SolverException;
import org.sosy_lab.solver.basicimpl.AbstractQuantifiedFormulaManager;

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

  @Override
  protected boolean isQuantifier(Long pExtractInfo) {
    return Z3NativeApi.get_ast_kind(z3context, pExtractInfo) == Z3_QUANTIFIER_AST;
  }

  @Override
  protected boolean isForall(Long pExtractInfo) {
    return Z3NativeApi.is_quantifier_forall(z3context, pExtractInfo);
  }

  @Override
  protected boolean isExists(Long pExtractInfo) {
    return isQuantifier(pExtractInfo) && !isForall(pExtractInfo);
  }

  @Override
  protected int numQuantifierBound(Long pExtractInfo) {
    return Z3NativeApi.get_quantifier_num_bound(z3context, pExtractInfo);
  }

  @Override
  protected Long getQuantifierBody(Long pExtractInfo) {
    return Z3NativeApi.get_quantifier_body(z3context, pExtractInfo);
  }

  @Override
  public boolean isBoundByQuantifier(Long pF) {
    return Z3NativeApi.get_ast_kind(z3context, pF) == Z3_VAR_AST;
  }

  @Override
  protected Long eliminateQuantifiers(Long pExtractInfo) throws SolverException, InterruptedException {
    // It is recommended (personal communication with Nikolaj Bjorner) to run "qe-light" before "qe".
    //  "qe" does not perform a "qe-light" as a preprocessing on its own!

    // You might want to run the tactic "ctx-solver-simplify" on the result...

    return Z3NativeApiHelpers.applyTactics(z3context, pExtractInfo, "qe-light",
        "qe");
  }
}
