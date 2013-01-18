/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
import org.sosy_lab.cpachecker.util.predicates.interfaces.RationalFormula;

import com.microsoft.z3.Native;
import com.microsoft.z3.Z3Exception;


public class Z3Formula implements Formula {

  private final long ast;
  private final long ctx;

  public Z3Formula(Z3FormulaCreator pCreator, long pAst) {
    this.ast = pAst;
    this.ctx = pCreator.getEnv();
  }

  @Override
  public String toString() {
    try {
      return Native.astToString(ctx, ast);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Z3Formula))
      return false;
    Z3Formula that = (Z3Formula) o;
    return this.ast == that.ast;
  }

  @Override
  public int hashCode() {
    try {
      return Native.getAstHash(ctx, ast);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  public long getTerm() {
    return ast;
  }

}

class Z3BitvectorFormula extends Z3Formula implements BitvectorFormula {

  public Z3BitvectorFormula(Z3FormulaCreator pCreator, long pAst) {
    super(pCreator, pAst);
  }

}

class Z3RationalFormula extends Z3Formula implements RationalFormula {

  public Z3RationalFormula(Z3FormulaCreator pCreator, long pAst) {
    super(pCreator, pAst);
  }

}

class Z3BooleanFormula extends Z3Formula implements BooleanFormula {

  public Z3BooleanFormula(Z3FormulaCreator pCreator, long pAst) {
    super(pCreator, pAst);
  }

}