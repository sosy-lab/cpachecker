// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness;

import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;

public class InvariantWitness {
  private final ExpressionTree<Object> formula;
  private final InvariantWitnessLocation location;

  private InvariantWitness(ExpressionTree<Object> pFormula, InvariantWitnessLocation pLocation) {
    formula = pFormula;
    location = pLocation;
  }

  public static Builder builder() {
    return new Builder();
  }

  static class Builder {
    private InvariantWitnessLocation location;
    private ExpressionTree<Object> formula;

    private Builder() {}

    Builder formula(ExpressionTree<Object> pFormula) {
      formula = pFormula;
      return this;
    }

    Builder location(
        String pFileName, String pFileHash, int pLine, int pColumn, String pFunctionName) {
      location = new InvariantWitnessLocation(pFileName, pFileHash, pFunctionName, pLine, pColumn);
      return this;
    }

    InvariantWitness build() {
      return new InvariantWitness(formula, location);
    }

  }

  public InvariantWitnessLocation getLocation() {
    return location;
  }

  public ExpressionTree<Object> getFormula() {
    return formula;
  }
}
