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

import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.AbstractFormulaCreator;

import com.microsoft.z3.Native;
import com.microsoft.z3.Z3Exception;


public class Z3FormulaCreator extends AbstractFormulaCreator<Long, Long, Long> {

  public Z3FormulaCreator(final Long pCtx) throws Z3Exception {
    super(pCtx, Native.mkBoolSort(pCtx), Native.mkRealSort(pCtx),
        new AbstractFormulaCreator.CreateBitType<Long>() {

          @Override
          public Long fromSize(int pSize) {
            try {
              return Native.mkBvSort(pCtx, pSize);
            } catch (Z3Exception e) {
              throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
            }
          }
        });
  }

  @Override
  public Long makeVariable(Long pType, String pVarName) {
    final long ctx = super.getEnv();
    try {
      return Native.mkConst(ctx, Native.mkStringSymbol(ctx, pVarName), pType);
    } catch (Z3Exception e) {
      throw new RuntimeException(e); // XXX: Z3Exception should be a runtime exception
    }
  }

}
