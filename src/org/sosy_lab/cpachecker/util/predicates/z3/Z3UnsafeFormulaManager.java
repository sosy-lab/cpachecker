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

import java.util.List;

import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractUnsafeFormulaManager;

import com.microsoft.z3.Native;
import com.microsoft.z3.Z3Exception;
import com.microsoft.z3.enumerations.Z3_ast_kind;
import com.microsoft.z3.enumerations.Z3_sort_kind;


public class Z3UnsafeFormulaManager extends AbstractUnsafeFormulaManager<Long> {

  private Z3FormulaCreator creator;
  private long ctx;

  public Z3UnsafeFormulaManager(Z3FormulaCreator pCreator) {
    super(pCreator);
    this.creator = pCreator;
    this.ctx = pCreator.getEnv();
  }

  @Override
  public Formula encapsulateUnsafe(Long pL) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isAtom(Long pT) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public int getArity(Long pT) {
    try {
      return Native.getArity(ctx, pT);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  public Long getArg(Long pT, int pN) {
    try {
      return Native.getAppArg(ctx, pT, pN);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  public boolean isVariable(Long pT) {
    try {
      return Native.getAstKind(ctx, pT) == Z3_ast_kind.Z3_VAR_AST.toInt();
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  public boolean isNumber(Long pT) {
    try {
      return Native.getAstKind(ctx, pT) == Z3_ast_kind.Z3_NUMERAL_AST.toInt();
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  public boolean isUF(Long pT) {
    try {
      return Native.getSort(ctx,  pT) == Z3_sort_kind.Z3_UNINTERPRETED_SORT.toInt();
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  public String getName(Long pT) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Long replaceArgs(Long pT, List<Long> pNewArgs) {
    int n = pNewArgs.size();
    long[] args = new long[n];
    int i = 0;
    for (long arg : pNewArgs)
      args[i++] = arg;
    try {
      return Native.updateTerm(ctx, pT, n, args);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  public Long replaceName(Long pT, String pNewName) {
    // TODO Auto-generated method stub
    return null;
  }

}
