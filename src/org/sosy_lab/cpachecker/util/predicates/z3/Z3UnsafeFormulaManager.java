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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractUnsafeFormulaManager;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;

public class Z3UnsafeFormulaManager extends AbstractUnsafeFormulaManager<Long, Long, Long> {

  private Set<Long> uifs = new HashSet<>();
  private long z3context;

  public Z3UnsafeFormulaManager(
      Z3FormulaCreator pCreator) {
    super(pCreator);
    this.z3context = pCreator.getEnv();
  }

  private final static Collection<Integer> nonAtomicOpTypes =
      Sets.newHashSet(Z3_OP_AND, Z3_OP_OR, Z3_OP_IMPLIES, Z3_OP_ITE, Z3_OP_NOT);

  @Override
  public Formula encapsulateUnsafe(Long pL) {
    return new Z3Formula(z3context, pL);
  }

  @Override
  public boolean isAtom(Long t) {
    long decl = get_app_decl(z3context, t);
    return !nonAtomicOpTypes.contains(get_decl_kind(z3context, decl));
  }

  @Override
  public int getArity(Long t) {
    return get_app_num_args(z3context, t);
  }

  @Override
  public Long getArg(Long t, int n) {
    return get_app_arg(z3context, t, n);
  }

  @Override
  public boolean isVariable(Long t) {
    if (isOP(z3context, t, Z3_OP_TRUE) || isOP(z3context, t, Z3_OP_FALSE)) { return false; }
    int astKind = get_ast_kind(z3context, t);
    return (astKind == Z3_APP_AST) && (getArity(t) == 0);
  }

  @Override
  public boolean isUF(Long t) {
    return uifs.contains(t);
  }

  @Override
  public String getName(Long t) {
    long funcDecl = get_app_decl(z3context, t);
    long symbol = get_decl_name(z3context, funcDecl);
    switch (get_symbol_kind(z3context, symbol)) {
    case Z3_INT_SYMBOL:
      return Integer.toString(get_symbol_int(z3context, symbol));
    case Z3_STRING_SYMBOL:
      return get_symbol_string(z3context, symbol);
    default:
      throw new AssertionError();
    }
  }

  @Override
  public Long replaceArgs(Long t, List<Long> newArgs) {
    Preconditions.checkState(get_app_num_args(z3context, t) == newArgs.size());
    long[] newParams = Longs.toArray(newArgs);
    // TODO check equality of sort of each oldArg and newArg
    long funcDecl = get_app_decl(z3context, t);
    return mk_app(z3context, funcDecl, newParams);
  }

  @Override
  public Long replaceName(Long t, String pNewName) {
    if (isVariable(t)) {
      long sort = get_sort(z3context, t);
      return getFormulaCreator().makeVariable(sort, pNewName);

    } else if (uifs.contains(t)) {
      int n = get_app_num_args(z3context, t);
      long[] args = new long[n];
      long[] sorts = new long[n];
      for (int i = 0; i < sorts.length; i++) {
        args[i] = get_app_arg(z3context, t, i);
        inc_ref(z3context, args[i]);
        sorts[i] = get_sort(z3context, args[i]);
        inc_ref(z3context, sorts[i]);
      }
      long symbol = mk_string_symbol(z3context, pNewName);
      long retSort = get_sort(z3context, t);
      long newFunc = mk_func_decl(z3context, symbol, sorts, retSort);
      inc_ref(z3context, newFunc);

      //      creator.getSmtLogger().logDeclaration(newFunc, retSort, sorts); // TODO necessary???

      long uif = createUIFCallImpl(newFunc, args);

      for (int i = 0; i < sorts.length; i++) {
        dec_ref(z3context, args[i]);
        dec_ref(z3context, sorts[i]);
      }
      return uif;

    } else {
      throw new IllegalArgumentException("The Term " + t + " has no name!");
    }
  }

  public long createUIFCallImpl(long pNewFunc, long[] args) {
    long ufc = mk_app(z3context, pNewFunc, args);
    uifs.add(ufc);
    return ufc;
  }

  @Override
  public boolean isNumber(Long t) {
    return is_numeral_ast(z3context, t);
  }
}
