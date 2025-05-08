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

public class LemmaPrecision implements Precision {
  private final ImmutableMap<String, AbstractionLemma> lemmaSet;

  /* Initialize empty LemmaPrecision */
  public static final LemmaPrecision EMPTY = new LemmaPrecision(ImmutableMap.of());

  public LemmaPrecision(ImmutableMap<String, AbstractionLemma> pLemmas) {
    lemmaSet = pLemmas;
  }

  public ImmutableMap<String, AbstractionLemma> getLemmas() {
    return lemmaSet;
  }
}
