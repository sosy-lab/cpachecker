// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates;

import org.sosy_lab.java_smt.api.Formula;

public class AbstractionLemma {
  final Formula formula;

  public AbstractionLemma(Formula pFormula) {
    formula = pFormula;
  }

  public Formula getFormula() {
    return formula;
  }
}
