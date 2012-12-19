/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.mathsat5;

import static org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5NativeApi.*;

import org.sosy_lab.cpachecker.util.predicates.interfaces.BitvectorFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractFormulaCreator;


public class Mathsat5FormulaCreator extends AbstractFormulaCreator<Long, Long, Long>{

  public Mathsat5FormulaCreator(
      final Long msatEnv) {
    super(msatEnv,
        msat_get_bool_type(msatEnv),
        msat_get_rational_type(msatEnv),
        new AbstractFormulaCreator.CreateBitType<Long>() {
          @Override
          public Long fromSize(int pSize) {
            return msat_get_bv_type(msatEnv, pSize);
          }
        });
  }

  @Override
  public Long makeVariable(Long type, String varName) {
    long funcDecl = msat_declare_function(getEnv(), varName, type);
    return msat_make_constant(getEnv(), funcDecl);
  }

  @Override
  public <T extends Formula> Long extractInfo(T pT) {
    return Mathsat5FormulaManager.getTerm(pT);
  }

  public Formula encapsulateUnsafe(Long pTerm){
    return new Mathsat5Formula(pTerm);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Formula> T encapsulate(Class<T> pClazz, Long pTerm) {
    Mathsat5Formula f;
    if (pClazz == BitvectorFormula.class){
      f = new Mathsat5BitvectorFormula(pTerm);
    }else if (pClazz == RationalFormula.class){
      f = new Mathsat5RationalFormula(pTerm);
    }else if (pClazz == BooleanFormula.class){
      f = new Mathsat5BooleanFormula(pTerm);
    }else {
      throw new IllegalArgumentException("invalid interface type");
    }
    return (T)f;
  }

}
