/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.modifications;

import com.google.common.collect.FluentIterable;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.conditions.AvoidanceReportingState;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public final class ModificationsState
    implements AvoidanceReportingState, AbstractQueryableState,
                                           Graphable {

  private boolean hasModification;
  private CFANode locationInGivenCfa;
  private CFANode locationInOriginalCfa;

  public ModificationsState(CFANode pLocationInGivenCfa, CFANode pLocationInOriginalCfa) {
    this(pLocationInGivenCfa, pLocationInOriginalCfa, false);
  }

  public ModificationsState(
      CFANode pLocationInGivenCfa, CFANode pLocationInOriginalCfa, boolean pHasModification) {
    locationInGivenCfa = pLocationInGivenCfa;
    locationInOriginalCfa = pLocationInOriginalCfa;
    hasModification = pHasModification;
  }

  public CFANode getLocationInOriginalCfa() {
    return locationInOriginalCfa;
  }

  public CFANode getLocationInGivenCfa() {
    return locationInGivenCfa;
  }

  public boolean hasModification() {
    return hasModification;
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    if (pO == null || getClass() != pO.getClass()) {
      return false;
    }
    ModificationsState that = (ModificationsState) pO;
    return Objects.equals(locationInOriginalCfa, that.locationInOriginalCfa)
        && Objects.equals(locationInGivenCfa, that.locationInGivenCfa)
        && Objects.equals(hasModification, that.hasModification);
  }

  @Override
  public int hashCode() {

    return Objects.hash(locationInOriginalCfa, locationInGivenCfa, hasModification);
  }

  @Override
  public boolean mustDumpAssumptionForAvoidance() {
    return hasModification;
  }

  @Override
  public BooleanFormula getReasonFormula(FormulaManagerView pMgr) {
    if (mustDumpAssumptionForAvoidance()) {
      return pMgr.getBooleanFormulaManager().makeFalse();
    }
    return pMgr.getBooleanFormulaManager().makeTrue();
  }

  @Override
  public String getCPAName() {
    return ModificationsCPA.class.getSimpleName();
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    switch (pProperty) {
      case "is_modified":
        return hasModification;
      default:
        throw new InvalidQueryException(
            "Unknown query to " + getClass().getSimpleName() + ": " + pProperty);
    }
  }

  @Override
  public String toDOTLabel() {
    StringBuilder sb = new StringBuilder();
    if (hasModification) {
      sb.append("Misfit: ");
      FluentIterable<CFAEdge> edgesInOrig = CFAUtils.enteringEdges(locationInOriginalCfa);
      sb.append("{");
      for (CFAEdge e : edgesInOrig) {
        sb.append(e);
        sb.append(", ");
      }
      sb.append("}");
    } else {
      sb.append("No modification");
    }
    return sb.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return hasModification;
  }
}
