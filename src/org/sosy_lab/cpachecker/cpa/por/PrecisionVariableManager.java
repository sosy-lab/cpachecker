// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.core.defaults.precision.ConfigurablePrecision;
import org.sosy_lab.cpachecker.core.defaults.precision.ScopedRefinablePrecision;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

interface PrecisionVariableManager {

  void setNewPrecision(Precision pPrecision);

  boolean contains(MemoryLocation pMemoryLocation);

  class CompositePrecisionVariableManager implements PrecisionVariableManager {

    private final ImmutableCollection<PrecisionVariableManager> variableManagers;

    CompositePrecisionVariableManager(
        ImmutableCollection<PrecisionVariableManager> pVariableManagers) {
      variableManagers = pVariableManagers;
    }

    @Override
    public void setNewPrecision(Precision pPrecision) {
      for (var manager : variableManagers) {
        manager.setNewPrecision(pPrecision);
      }
    }

    @Override
    public boolean contains(MemoryLocation pMemoryLocation) {
      for (var manager : variableManagers) {
        if (manager.contains(pMemoryLocation)) {
          return true;
        }
      }
      return false;
    }
  }

  class ConfigurablePrecisionVariableManager implements PrecisionVariableManager {

    private ConfigurablePrecision precision = null;

    @Override
    public void setNewPrecision(Precision pPrecision) {
      ConfigurablePrecision newPrecision =
          Precisions.extractPrecisionByType(pPrecision, ConfigurablePrecision.class);
      if (newPrecision == null) {
        throw new IllegalArgumentException(
            "Expected a ConfigurablePrecision, but got: " + pPrecision);
      }

      precision = newPrecision;
    }

    @Override
    public boolean contains(MemoryLocation pMemoryLocation) {
      checkState(precision != null, "PrecisionVariableManager not initialized with a precision");

      return precision.isTracking(pMemoryLocation);
    }
  }

  class ScopedRefinablePrecisionVariableManager implements PrecisionVariableManager {

    private ScopedRefinablePrecision precision = null;

    @Override
    public void setNewPrecision(Precision pPrecision) {
      ScopedRefinablePrecision newPrecision =
          Precisions.extractPrecisionByType(pPrecision, ScopedRefinablePrecision.class);
      if (newPrecision == null) {
        throw new IllegalArgumentException(
            "Expected a ScopedRefinablePrecision, but got: " + pPrecision);
      }

      precision = newPrecision;
    }

    @Override
    public boolean contains(MemoryLocation pMemoryLocation) {
      checkState(precision != null, "PrecisionVariableManager not initialized with a precision");

      boolean result = precision.getRawPrecision().contains(pMemoryLocation);
      if (System.getenv("POR_X") != null
          && pMemoryLocation.getExtendedQualifiedName().equals("x")) {
        System.err.println(
            "[POR_X] contains(x)="
                + result
                + " precSize="
                + precision.getRawPrecision().size()
                + " rawSample="
                + precision.getRawPrecision().stream().limit(12).toList());
      }
      return result;
    }
  }

  class PredicatePrecisionVariableManager implements PrecisionVariableManager {

    private final FormulaManagerView formulaManager;

    private PredicatePrecision lastPrecision = null;
    private ImmutableCollection<String> variables = null;

    public PredicatePrecisionVariableManager(FormulaManagerView pFormulaManager) {
      formulaManager = pFormulaManager;
    }

    @Override
    public void setNewPrecision(Precision pPrecision) {
      PredicatePrecision predicatePrecision =
          Precisions.extractPrecisionByType(pPrecision, PredicatePrecision.class);
      if (predicatePrecision == null) {
        throw new IllegalArgumentException("Expected a PredicatePrecision, but got: " + pPrecision);
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
          && precision.getLocationInstancePredicates().isEmpty();
    }
  }
}
