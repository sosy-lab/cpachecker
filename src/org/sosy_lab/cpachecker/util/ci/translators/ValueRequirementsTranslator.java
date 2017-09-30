/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.ci.translators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.interval.NumberInterface;
import org.sosy_lab.cpachecker.cpa.interval.UnifyAnalysisState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;


public class ValueRequirementsTranslator extends CartesianRequirementsTranslator<UnifyAnalysisState> {

  public ValueRequirementsTranslator(final LogManager pLog) {
    super(UnifyAnalysisState.class, pLog);
  }

  @Override
  protected List<String> getVarsInRequirements(final UnifyAnalysisState pRequirement) {
    List<String> list = new ArrayList<>(pRequirement.getConstantsMapView().size());
    for (MemoryLocation memLoc : pRequirement.getConstantsMapView().keySet()) {
      list.add(memLoc.getAsSimpleString());
    }
    return list;
  }

  @Override
  protected List<String> getListOfIndependentRequirements(final UnifyAnalysisState pRequirement,
      final SSAMap pIndices, final @Nullable Collection<String> pRequiredVars) {
    List<String> list = new ArrayList<>();
    for (MemoryLocation memLoc : pRequirement.getConstantsMapView().keySet()) {
      NumberInterface integerValue = pRequirement.getConstantsMapView().get(memLoc);
        if (!integerValue.isNumericValue() || !(integerValue.getNumber() instanceof Integer)) {
          logger.log(Level.SEVERE, "The value " + integerValue + " of the MemoryLocation " + memLoc + " is not an Integer.");
        } else {
          if (pRequiredVars == null || pRequiredVars.contains(memLoc.getAsSimpleString())) {
            list.add("(= " + getVarWithIndex(memLoc.getAsSimpleString(), pIndices) + " "
                + integerValue.getNumber() + ")");
          }
        }
    }
    // TODO getRequirement(..) hinzufuegen
    return list;
  }
}
