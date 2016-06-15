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
package org.sosy_lab.cpachecker.cpa.predicate.relevantpredicates;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.errorprone.annotations.ForOverride;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public abstract class AbstractRelevantPredicatesComputer<T> implements RelevantPredicatesComputer {

  protected final FormulaManagerView fmgr;

  private final Table<T, AbstractionPredicate, Boolean> relevantPredicates = HashBasedTable.create();

  protected AbstractRelevantPredicatesComputer(FormulaManagerView pFmgr) {
    fmgr = pFmgr;
  }

  @Override
  public final Set<AbstractionPredicate> getRelevantPredicates(Block context, Collection<AbstractionPredicate> predicates) {
    Set<AbstractionPredicate> result = new HashSet<>(predicates.size());

    T precomputeResult = precompute(context, predicates);

    for (AbstractionPredicate predicate : predicates) {
      if (isRelevant0(precomputeResult, predicate)) {
        result.add(predicate);
      }
    }
    return result;
  }

  private boolean isRelevant0(T pPrecomputeResult, AbstractionPredicate pPredicate) {

    // lookup in cache
    Boolean cacheResult = relevantPredicates.get(pPrecomputeResult, pPredicate);
    if (cacheResult != null) {
      return cacheResult;
    }

    boolean result;
    if (fmgr.getBooleanFormulaManager().isFalse(pPredicate.getSymbolicAtom())
        || fmgr.extractVariableNames(pPredicate.getSymbolicAtom()).isEmpty()) {
      result = true;
    } else {
      String predicateString = pPredicate.getSymbolicAtom().toString();
      if (predicateString.contains("false") || predicateString.contains("retval")  || predicateString.contains("nondet")) {
        result = true;
      } else {
        result = isRelevant(pPrecomputeResult, pPredicate);
      }
    }

    relevantPredicates.put(pPrecomputeResult, pPredicate, result);
    return result;
  }

  protected abstract boolean isRelevant(T pPrecomputeResult, AbstractionPredicate pPredicate);

  @ForOverride
  protected abstract T precompute(Block pContext, Collection<AbstractionPredicate> pPredicates);

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof AbstractRelevantPredicatesComputer) {
      AbstractRelevantPredicatesComputer<?> other = (AbstractRelevantPredicatesComputer<?>) o;
      return fmgr.equals(other.fmgr) && relevantPredicates.equals(other.relevantPredicates);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(fmgr, relevantPredicates);
  }

}
