// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

interface PrecisionVariableManager {

  void setNewPrecision(Precision precision);

  boolean contains(MemoryLocation pMemoryLocation);

  class PredicatePrecisionVariableManager implements PrecisionVariableManager {

    private final FormulaManagerView formulaManager;

    private PredicatePrecision lastPrecision = null;
    private ImmutableCollection<String> variables = null;

    public PredicatePrecisionVariableManager(FormulaManagerView pFormulaManager) {
      formulaManager = pFormulaManager;
    }

    @Override
    public void setNewPrecision(Precision precision) {
      PredicatePrecision predicatePrecision =
          Precisions.extractPrecisionByType(precision, PredicatePrecision.class);
      if (predicatePrecision == null) {
        throw new IllegalArgumentException("Expected a PredicatePrecision, but got: " + precision);
      }

      if (predicatePrecision.equals(lastPrecision)) {
        return;
      }

      if (!checkSupportedPrecision(predicatePrecision)) {
        throw new IllegalArgumentException(
            "Only global predicates are allowed in abstraction-aware POR: " + predicatePrecision);
      }

      ImmutableSet.Builder<String> variableBuilder = ImmutableSet.builder();
      for (var predicate : predicatePrecision.getGlobalPredicates()) {
        var vars = formulaManager.extractVariables(predicate.getSymbolicAtom());
        variableBuilder.addAll(vars.keySet());
      }

      lastPrecision = predicatePrecision;
      variables = variableBuilder.build();
    }

    @Override
    public boolean contains(MemoryLocation pMemoryLocation) {
      return variables.contains(pMemoryLocation.getExtendedQualifiedName());
    }

    private boolean checkSupportedPrecision(PredicatePrecision precision) {
      return precision.getLocalPredicates().isEmpty()
          && precision.getFunctionPredicates().isEmpty()
          && precision.getLocationInstancePredicates().isEmpty();
    }
  }
}
