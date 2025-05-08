// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates;

import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class AbstractionLemma {
  private final BooleanFormula formula;
  private final BitvectorFormula signature;
  private final BitvectorFormula body;

  public AbstractionLemma(
      BooleanFormula pFormula, BitvectorFormula pSignature, BitvectorFormula pBody) {
    formula = pFormula;
    signature = pSignature;
    body = pBody;
  }

  public BooleanFormula getFormula() {
    return formula;
  }

  public BitvectorFormula getSignature() {
    return signature;
  }

  public BitvectorFormula getBody() {
    return body;
  }
}
