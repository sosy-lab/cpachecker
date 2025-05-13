// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.predicates.AbstractionLemma;
import org.sosy_lab.java_smt.api.Formula;

public class LemmaPrecision implements Precision {
  private final ImmutableMap<String, AbstractionLemma> lemmaSet;
  private ImmutableMap<Formula, Formula> valueMap;

  /* Initialize empty LemmaPrecision */
  public static final LemmaPrecision EMPTY = new LemmaPrecision(ImmutableMap.of());

  public LemmaPrecision(
      ImmutableMap<String, AbstractionLemma> pLemmas, ImmutableMap<Formula, Formula> pValueMap) {
    lemmaSet = pLemmas;
    valueMap = pValueMap;
  }

  public LemmaPrecision(ImmutableMap<String, AbstractionLemma> pLemmaSet) {
    this(pLemmaSet, ImmutableMap.of());
  }

  public ImmutableMap<String, AbstractionLemma> getLemmas() {
    return lemmaSet;
  }

  public ImmutableMap<Formula, Formula> getValues() {
    return valueMap;
  }

  public void setValueMap(ImmutableMap<Formula, Formula> pMap) {
    valueMap = pMap;
  }
}
