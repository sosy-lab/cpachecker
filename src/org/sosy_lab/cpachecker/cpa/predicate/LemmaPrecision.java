// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.predicates.AbstractionLemma;

public class LemmaPrecision implements Precision {
  private final ImmutableSet<AbstractionLemma> lemmaSet;

  /* Initialize empty LemmaPrecision */
  public static final LemmaPrecision EMPTY = new LemmaPrecision(ImmutableSet.of());

  public LemmaPrecision(ImmutableSet<AbstractionLemma> pLemmas) {
    lemmaSet = pLemmas;
  }

  public ImmutableSet<AbstractionLemma> getLemmas() {
    return lemmaSet;
  }
}
