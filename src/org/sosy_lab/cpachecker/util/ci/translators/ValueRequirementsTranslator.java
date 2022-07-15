// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.ci.translators;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class ValueRequirementsTranslator
    extends CartesianRequirementsTranslator<ValueAnalysisState> {

  public ValueRequirementsTranslator(final LogManager pLog) {
    super(ValueAnalysisState.class, pLog);
  }

  @Override
  protected List<String> getVarsInRequirements(final ValueAnalysisState pRequirement) {
    List<String> list = new ArrayList<>(pRequirement.getConstants().size());
    for (MemoryLocation memLoc : pRequirement.getTrackedMemoryLocations()) {
      list.add(memLoc.getExtendedQualifiedName());
    }
    return list;
  }

  @Override
  protected List<String> getListOfIndependentRequirements(
      final ValueAnalysisState pRequirement,
      final SSAMap pIndices,
      final @Nullable Collection<String> pRequiredVars) {
    List<String> list = new ArrayList<>();
    for (Entry<MemoryLocation, ValueAndType> e : pRequirement.getConstants()) {
      MemoryLocation memLoc = e.getKey();
      Value integerValue = e.getValue().getValue();
      if (!integerValue.isNumericValue()
          || !(integerValue.asNumericValue().getNumber() instanceof Integer
              || integerValue.asNumericValue().getNumber() instanceof Long
              || integerValue.asNumericValue().getNumber() instanceof BigInteger)) {
        logger.log(
            Level.SEVERE,
            "The value "
                + integerValue
                + " of the MemoryLocation "
                + memLoc
                + " is not an Integer.");
      } else {
        if (pRequiredVars == null || pRequiredVars.contains(memLoc.getExtendedQualifiedName())) {
          list.add(
              "(= "
                  + getVarWithIndex(memLoc.getExtendedQualifiedName(), pIndices)
                  + " "
                  + integerValue.asNumericValue().getNumber()
                  + ")");
        }
      }
    }
    // TODO getRequirement(..) hinzufuegen
    return list;
  }
}
