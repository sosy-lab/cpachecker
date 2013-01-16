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

import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractBooleanFormulaManager;

import com.microsoft.z3.Native;
import com.microsoft.z3.Z3Exception;
import com.microsoft.z3.enumerations.Z3_decl_kind;


public class Z3BooleanFormulaManager extends AbstractBooleanFormulaManager<Long> {

  private Z3FormulaCreator creator;
  private long ctx;

  protected Z3BooleanFormulaManager(Z3FormulaCreator pCreator) {
    super(pCreator);
    this.creator = pCreator;
    this.ctx = creator.getEnv();
  }

  public static Z3BooleanFormulaManager create(Z3FormulaCreator creator) {
    return new Z3BooleanFormulaManager(creator);
  }

  @Override
  protected Long makeVariableImpl(String pVar) {
    try {
      return Native.mkConst(ctx, Native.mkStringSymbol(ctx, pVar), creator.getBoolType());
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long makeBooleanImpl(boolean pValue) {
    try {
      return pValue ? Native.mkTrue(ctx) : Native.mkFalse(ctx);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long not(Long pParam1) {
    try {
      return Native.mkNot(ctx, pParam1);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long and(Long pParam1, Long pParam2) {
    try {
      return Native.mkAnd(ctx, 2, new long[] { pParam1, pParam2 });
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long or(Long pParam1, Long pParam2) {
    try {
      return Native.mkOr(ctx, 2, new long[] { pParam1, pParam2 });
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long xor(Long pParam1, Long pParam2) {
    try {
      return Native.mkXor(ctx, pParam1, pParam2);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isNot(Long pParam) {
    try {
      return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_NOT.toInt();
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isAnd(Long pParam) {
    try {
      return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_AND.toInt();
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isOr(Long pParam) {
    try {
      return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_OR.toInt();
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isXor(Long pParam) {
    try {
      return Native.getDeclKind(ctx, pParam) == Z3_decl_kind.Z3_OP_XOR.toInt();
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long equivalence(Long pBits1, Long pBits2) {
    try {
      return Native.mkIff(ctx, pBits1, pBits2);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isTrue(Long pBits) {
    try {
      return Native.getDeclKind(ctx, pBits) == Z3_decl_kind.Z3_OP_TRUE.toInt();
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isFalse(Long pBits) {
    try {
      return Native.getDeclKind(ctx, pBits) == Z3_decl_kind.Z3_OP_FALSE.toInt();
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected Long ifThenElse(Long pCond, Long pF1, Long pF2) {
    try {
      return Native.mkIte(ctx, pCond, pF1, pF2);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isEquivalence(Long pBits) {
    try {
      return Native.getDeclKind(ctx, pBits) == Z3_decl_kind.Z3_OP_IFF.toInt();
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

  @Override
  protected boolean isIfThenElse(Long pBits) {
    try {
      return Native.getDeclKind(ctx, pBits) == Z3_decl_kind.Z3_OP_ITE.toInt();
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }



}
