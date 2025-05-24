// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.equivalence;

import java.util.List;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public interface EquivalenceCheck {

  record EquivalenceData(
      boolean equivalent,
      int sizeOriginal,
      int sizeMutant,
      int totalChecks,
      int checkedSafe,
      int checkedUnsafe,
      int checkedUnknown,
      int falseOriginal,
      int falseMutant) {

    public String toCsvString() {
      return String.format(
          "%b\t%d\t%d\t%d\t%d\t%d\t%d\t%d\t%d",
          equivalent,
          sizeOriginal,
          sizeMutant,
          totalChecks,
          checkedSafe,
          checkedUnsafe,
          checkedUnknown,
          falseOriginal,
          falseMutant);
    }

    public static String getCsvHeader() {
      return "equivalent\tsizeOriginal\tsizeMutant\ttotalChecks\tcheckedSafe\tcheckedUnsafe"
          + "\tcheckedUnknown\tfalseOriginal\tfalseMutant";
    }
  }

  EquivalenceData isEquivalent(List<BooleanFormula> original, List<BooleanFormula> mutant)
      throws InterruptedException, SolverException;
}
