// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import com.google.common.collect.FluentIterable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.SingleLocationFormulaInvariant;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

public class CounterexampleToInductivity {

  private final CFANode location;

  private final PersistentSortedMap<String, ModelValue> model;

  public CounterexampleToInductivity(CFANode pLocation, Map<String, ModelValue> pModel) {
    location = Objects.requireNonNull(pLocation);
    model = PathCopyingPersistentTreeMap.copyOf(pModel);
  }

  public BooleanFormula getFormula(FormulaManagerView pFMGR) {
    BooleanFormulaManager bfmgr = pFMGR.getBooleanFormulaManager();
    BooleanFormula modelFormula = bfmgr.makeTrue();
    for (Map.Entry<String, ModelValue> valueAssignment : model.entrySet()) {
      String variableName = valueAssignment.getKey();
      ModelValue v = valueAssignment.getValue();
      assert variableName.equals(v.getVariableName());
      modelFormula = bfmgr.and(modelFormula, v.toAssignment(pFMGR));
    }
    return modelFormula;
  }

  public Map<String, ModelValue> getAssignments() {
    return model;
  }

  public CounterexampleToInductivity dropLiteral(String pVarName) {
    PersistentSortedMap<String, ModelValue> reducedModel = model.removeAndCopy(pVarName);
    if (reducedModel.size() == model.size()) {
      throw new IllegalArgumentException(pVarName + " is not part of this CTI");
    }
    return new CounterexampleToInductivity(getLocation(), reducedModel);
  }

  public CFANode getLocation() {
    return location;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther instanceof CounterexampleToInductivity) {
      CounterexampleToInductivity other = (CounterexampleToInductivity) pOther;
      return model.equals(other.model) && getLocation().equals(other.getLocation());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(model, getLocation());
  }

  @Override
  public String toString() {
    return model.values() + " at " + getLocation();
  }

  public FluentIterable<CandidateInvariant> splitLiterals(
      FormulaManagerView pFMGR, boolean pSplitEquals) {
    return FluentIterable.from(model.values())
        .transformAndConcat(
            v -> {
              if (pSplitEquals) {
                return FluentIterable.from(
                        pFMGR.splitNumeralEqualityIfPossible(v.toAssignment(pFMGR)))
                    .transform(
                        f ->
                            SingleLocationFormulaInvariant.makeLocationInvariant(
                                getLocation(), f, pFMGR));
              }
              return Collections.singleton(
                  SingleLocationFormulaInvariant.makeLocationInvariant(
                      getLocation(), v.toAssignment(pFMGR), pFMGR));
            });
  }
}
