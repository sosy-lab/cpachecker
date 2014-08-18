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

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.OptEnvironment;

import static org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApi.*;
import static org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApiConstants.*;

public class Z3OptProver implements OptEnvironment{

  private Z3FormulaManager mgr;
  private long z3context;
  private long z3optContext;

  public Z3OptProver(Z3FormulaManager mgr) {
    this.mgr = mgr;
    z3context = mgr.getEnvironment();
    z3optContext = mk_optimize(z3context);
    optimize_inc_ref(z3context, z3optContext);
  }

  @Override
  public void addConstraint(BooleanFormula constraint) {
    Z3BooleanFormula z3Constraint = (Z3BooleanFormula) constraint;
    optimize_assert(z3context, z3optContext, z3Constraint.getExpr());
  }

  @Override
  public void setObjective(Formula objective) {
    Z3Formula z3Objective = (Z3Formula) objective;
    Preconditions.checkArgument(mgr.getUnsafeFormulaManager().isVariable(z3Objective),
        "Can only maximize for a single variable.");
    optimize_maximize(z3context, z3optContext, z3Objective.getExpr());
  }

  @Override
  public OptResult maximize() throws InterruptedException {
    int status = optimize_check(z3context, z3optContext);
    if (status == Z3_LBOOL.Z3_L_FALSE.status) {
      return OptResult.UNSAT;
    } else if (status == Z3_LBOOL.Z3_L_UNDEF.status) {
      return OptResult.UNDEF;
    } else {
      return OptResult.OPT;
    }
  }

  @Override
  public Model getModel() throws SolverException {
    long z3model = optimize_get_model(z3context, z3optContext);
    return Z3Model.parseZ3Model(mgr, z3context, z3model);
  }

  @Override
  public void close() throws Exception {
    optimize_dec_ref(z3context, z3optContext);
    z3context = 0;
    z3optContext = 0;
  }
}
