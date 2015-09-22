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

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.solver.z3.Z3NativeApi.*;
import static org.sosy_lab.solver.z3.Z3NativeApiConstants.*;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Map;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.solver.api.ArrayFormula;
import org.sosy_lab.solver.api.BitvectorFormula;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FormulaType;
import org.sosy_lab.solver.api.FormulaType.ArrayFormulaType;
import org.sosy_lab.solver.basicimpl.FormulaCreator;
import org.sosy_lab.solver.z3.Z3Formula.Z3ArrayFormula;
import org.sosy_lab.solver.z3.Z3Formula.Z3BitvectorFormula;
import org.sosy_lab.solver.z3.Z3Formula.Z3BooleanFormula;
import org.sosy_lab.solver.z3.Z3Formula.Z3IntegerFormula;
import org.sosy_lab.solver.z3.Z3Formula.Z3RationalFormula;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;

@Options(prefix="solver.z3")
class Z3FormulaCreator extends FormulaCreator<Long, Long, Long> {

  @Option(secure=true, description="Whether to use PhantomReferences for discarding Z3 AST")
  private boolean usePhantomReferences = false;

  private final Z3SmtLogger smtLogger;

  private final Table<Long, Long, Long> allocatedArraySorts = HashBasedTable.create();

  private final ReferenceQueue<Z3Formula> referenceQueue =
      new ReferenceQueue<>();
  private final Map<PhantomReference<Z3Formula>, Long> referenceMap =
      Maps.newIdentityHashMap();

  // todo: getters for statistic.
  private final Timer cleanupTimer = new Timer();

  Z3FormulaCreator(
      long pEnv,
      long pBoolType,
      long pIntegerType,
      long pRealType,
      Z3SmtLogger smtLogger,
      Configuration config) throws InvalidConfigurationException {
    super(pEnv, pBoolType, pIntegerType, pRealType);
    config.inject(this);

    this.smtLogger = smtLogger;
  }

  @Override
  public Long makeVariable(Long type, String varName) {
    long z3context = getEnv();
    long symbol = mk_string_symbol(z3context, varName);
    long var = mk_const(z3context, symbol, type);

    smtLogger.logVarDeclaration(var, type);

    return var;
  }

  @Override
  public Long extractInfo(Formula pT) {
    return Z3FormulaManager.getZ3Expr(pT);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Formula> FormulaType<T> getFormulaType(T pFormula) {
    if (pFormula instanceof ArrayFormula<?,?>
    || pFormula instanceof BitvectorFormula) {
      long term = extractInfo(pFormula);
      return (FormulaType<T>) getFormulaType(term);
    }

    return super.getFormulaType(pFormula);
  }

  public FormulaType<?> getFormulaTypeFromSort(Long pSort) {
    long z3context = getEnv();
    long sortKind = get_sort_kind(z3context, pSort);
    if (sortKind == Z3_BOOL_SORT) {
      return FormulaType.BooleanType;
    } else if (sortKind == Z3_INT_SORT) {
      return FormulaType.IntegerType;
    } else if (sortKind == Z3_ARRAY_SORT) {
      long domainSort = get_array_sort_domain(z3context, pSort);
      long rangeSort = get_array_sort_range(z3context, pSort);
      return FormulaType.getArrayType(
          getFormulaTypeFromSort(domainSort),
          getFormulaTypeFromSort(rangeSort));
    } else if (sortKind == Z3_REAL_SORT) {
      return FormulaType.RationalType;
    } else if (sortKind == Z3_BV_SORT) {
      return FormulaType.getBitvectorTypeWithSize(get_bv_sort_size(z3context, pSort));
    }
    throw new IllegalArgumentException("Unknown formula type");
  }

  @Override
  public FormulaType<?> getFormulaType(Long pFormula) {
    long sort = get_sort(getEnv(), pFormula);
    return getFormulaTypeFromSort(sort);
  }

  @Override
  protected <TD extends Formula, TR extends Formula>
  FormulaType<TR> getArrayFormulaElementType(ArrayFormula<TD, TR> pArray) {
    return ((Z3ArrayFormula<TD,TR>) pArray).getElementType();
  }

  @Override
  protected <TD extends Formula, TR extends Formula>
  FormulaType<TD> getArrayFormulaIndexType(ArrayFormula<TD, TR> pArray) {
    return ((Z3ArrayFormula<TD,TR>) pArray).getIndexType();
  }

  @Override
  protected <TD extends Formula, TR extends Formula> ArrayFormula<TD, TR> encapsulateArray(Long pTerm,
      FormulaType<TD> pIndexType, FormulaType<TR> pElementType) {
    cleanupReferences();
    return storePhantomReference(
        new Z3ArrayFormula<>(getEnv(), pTerm, pIndexType, pElementType),
        pTerm);
  }

  @SuppressWarnings("unchecked")
  private <T extends Z3Formula> T storePhantomReference(T out, Long pTerm) {
    if (usePhantomReferences) {
      PhantomReference<T> ref = new PhantomReference<>(out, referenceQueue);
      referenceMap.put((PhantomReference<Z3Formula>)ref, pTerm);
    }
    return out;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends Formula> T encapsulate(FormulaType<T> pType, Long pTerm) {
    cleanupReferences();
    if (pType.isBooleanType()) {
      return (T)storePhantomReference(new Z3BooleanFormula(getEnv(), pTerm),
          pTerm);
    } else if (pType.isIntegerType()) {
      return (T)storePhantomReference(new Z3IntegerFormula(getEnv(), pTerm),
          pTerm);
    } else if (pType.isRationalType()) {
      return (T)storePhantomReference(new Z3RationalFormula(getEnv(), pTerm),
          pTerm);
    } else if (pType.isBitvectorType()) {
      return (T)storePhantomReference(new Z3BitvectorFormula(getEnv(), pTerm),
          pTerm);
    } else if (pType.isArrayType()) {
      ArrayFormulaType<?, ?> arrFt = (ArrayFormulaType<?, ?>) pType;
      return (T) storePhantomReference(new Z3ArrayFormula<>(
          getEnv(),
          pTerm,
          arrFt.getIndexType(),
          arrFt.getElementType()), pTerm);
    }

    throw new IllegalArgumentException("Cannot create formulas of type " + pType + " in Z3");
  }

  @Override
  public BooleanFormula encapsulateBoolean(Long pTerm) {
    cleanupReferences();
    return storePhantomReference(new Z3BooleanFormula(getEnv(), pTerm), pTerm);
  }

  @Override
  public BitvectorFormula encapsulateBitvector(Long pTerm) {
    cleanupReferences();
    return storePhantomReference(new Z3BitvectorFormula(getEnv(), pTerm), pTerm);
  }

  @Override
  public Long getArrayType(Long pIndexType, Long pElementType) {
    Long allocatedArraySort = allocatedArraySorts.get(pIndexType, pElementType);
    if (allocatedArraySort == null) {
      allocatedArraySort = Z3NativeApi.mk_array_sort(getEnv(), pIndexType, pElementType);
      Z3NativeApi.inc_ref(getEnv(), allocatedArraySort);
      allocatedArraySorts.put(pIndexType, pElementType, allocatedArraySort);
    }
    return allocatedArraySort;
  }

  @Override
  public Long getBitvectorType(int pBitwidth) {
    checkArgument(pBitwidth > 0, "Cannot use bitvector type with size %s", pBitwidth);
    long bvSort = mk_bv_sort(getEnv(), pBitwidth);
    inc_ref(getEnv(), sort_to_ast(getEnv(), bvSort));
    return bvSort;
  }

  @Override
  public Long getFloatingPointType(FormulaType.FloatingPointType type) {
    throw new UnsupportedOperationException("FloatingPoint theory is not supported by Z3");
  }


  private void cleanupReferences() {
    if (!usePhantomReferences) {
      return;
    }
    cleanupTimer.start();
    try {
      PhantomReference<? extends Z3Formula> ref;
      while ((ref =
          (PhantomReference<? extends Z3Formula>)referenceQueue
              .poll()) != null) {

        Long z3ast = referenceMap.remove(ref);
        assert z3ast != null;
        dec_ref(getEnv(), z3ast);
      }
    } finally {
      cleanupTimer.stop();
    }
  }
}
