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
package org.sosy_lab.cpachecker.util.predicates.mathsat5;

import static org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5NativeApi.*;

import java.util.List;

import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractUnsafeFormulaManager;

import com.google.common.primitives.Longs;

class Mathsat5UnsafeFormulaManager extends AbstractUnsafeFormulaManager<Long> {

  private final long msatEnv;
  private final Mathsat5FormulaCreator creator;

  public Mathsat5UnsafeFormulaManager(Mathsat5FormulaCreator pCreator) {
    super(pCreator);
    this.msatEnv = pCreator.getEnv();
    this.creator = pCreator;
  }

  @Override
  public Formula encapsulateUnsafe(Long pL) {
    return creator.encapsulateUnsafe(pL);
  }

  @Override
  public boolean isAtom(Long t) {
    return msat_term_is_atom(msatEnv, t);
  }

  @Override
  public int getArity(Long pT) {
    return msat_term_arity(pT);
  }

  @Override
  public Long getArg(Long t, int n) {
    return msat_term_get_arg(t, n);
  }

  @Override
  public boolean isVariable(Long t) {
    return msat_term_is_constant(msatEnv, t);
  }

  @Override
  public boolean isUF(Long t) {
    return msat_term_is_uf(msatEnv, t);
  }

  @Override
  public String getName(Long t) {
    if (isUF(t)) {
      return msat_decl_get_name(msat_term_get_decl(t));
    } else if (isVariable(t)) {
      return msat_term_repr(t);
    } else {
      throw new IllegalArgumentException("Can't get the name from the given formula!");
    }
  }

  @Override
  public Long replaceArgs(Long t, List<Long> newArgs) {
    long tDecl = msat_term_get_decl(t);
    return msat_make_term(msatEnv, tDecl, Longs.toArray(newArgs));
  }

  @Override
  public Long replaceName(Long t, String newName) {
    if (isUF(t)) {
      long decl = msat_term_get_decl(t);
      int arity = msat_decl_get_arity(decl);
      long retType = msat_decl_get_return_type(decl);
      long[] argTypes = new long[arity];
      for (int i = 0; i < argTypes.length; i++) {
        argTypes[i] = msat_decl_get_arg_type(decl, i);
      }
      long funcType = msat_get_function_type(msatEnv, argTypes, argTypes.length, retType);
      long funcDecl = msat_declare_function(msatEnv, newName, funcType);
      return msat_make_uf(msatEnv, funcDecl, Longs.toArray(getArguments(t)));
    } else if (isVariable(t)) {
      return creator.makeVariable(msat_term_get_type(t), newName);
    } else {
      throw new IllegalArgumentException("Can't set the name from the given formula!");
    }
  }

  @Override
  public boolean isNumber(Long pT) {
    return msat_term_is_number(msatEnv, pT);
  }

}
