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

import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;

public class Z3Formula implements Formula {

  public final long z3expr;
  public final long z3context;

  public Z3Formula(long z3context, long z3expr) {
    this.z3expr = z3expr;
    this.z3context = z3context;
    Z3NativeApi.inc_ref(z3context, z3expr); // TODO check and delete with garbage collection
  }

  @Override
  public String toString() {
    return Z3NativeApi.ast_to_string(z3context, z3expr);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof Z3Formula)) { return false; }
    Z3Formula other = (Z3Formula) obj;
    return (z3context == other.z3context)
        && Z3NativeApi.is_eq_ast(z3context, z3expr, other.z3expr);
  }

  @Override
  public int hashCode() {
    return Z3NativeApi.get_ast_hash(z3context, z3expr);
  }

  public long getExpr() {
    return z3expr;
  }
}

class Z3BitvectorFormula extends Z3Formula implements BitvectorFormula {

  public Z3BitvectorFormula(long z3context, long z3expr) {
    super(z3context, z3expr);
  }
}

class Z3IntegerFormula extends Z3Formula implements IntegerFormula {

  public Z3IntegerFormula(long z3context, long z3expr) {
    super(z3context, z3expr);
  }
}

class Z3RationalFormula extends Z3Formula implements RationalFormula {

  public Z3RationalFormula(long z3context, long z3expr) {
    super(z3context, z3expr);
  }
}

class Z3BooleanFormula extends Z3Formula implements BooleanFormula {

  public Z3BooleanFormula(long z3context, long z3expr) {
    super(z3context, z3expr);
  }
}
