// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.datarace;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.SetMultimap;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithAssumptions;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.exceptions.InvalidQueryException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class DataRaceState
    implements AbstractStateWithAssumptions, AbstractQueryableState, Graphable {

  private static final String PROPERTY_DATA_RACE = "data-race";
  private final boolean hasDataRace;
  private final SetMultimap<MemoryLocation, Integer> writtenBy;

  public DataRaceState(boolean pHasDataRace) {
    hasDataRace = pHasDataRace;
    writtenBy = HashMultimap.create();
  }

  public DataRaceState(boolean pB, SetMultimap<MemoryLocation, Integer> pWrittenBy) {
    hasDataRace = pB;
    writtenBy = pWrittenBy;
  }

  @Override
  public List<? extends AExpression> getAssumptions() {
    return ImmutableList.of();
  }

  @Override
  public String getCPAName() {
    return "DataRaceCPA";
  }

  @Override
  public boolean checkProperty(String pProperty) throws InvalidQueryException {
    if (pProperty.equals(PROPERTY_DATA_RACE)) {
      return hasDataRace;
    }
    throw new InvalidQueryException("Query '" + pProperty + "' is invalid.");
  }

  @Override
  public String toDOTLabel() {
    return writtenBy.toString();
  }

  @Override
  public boolean shouldBeHighlighted() {
    return hasDataRace;
  }
}
