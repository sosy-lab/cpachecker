/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm.bmc;

import com.google.common.collect.FluentIterable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.collect.PersistentSortedMap;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
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
    for (Map.Entry<String, ModelValue> valueAssignment : this.model.entrySet()) {
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
