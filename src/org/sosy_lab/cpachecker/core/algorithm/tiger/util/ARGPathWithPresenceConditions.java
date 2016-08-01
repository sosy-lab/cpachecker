package org.sosy_lab.cpachecker.core.algorithm.tiger.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.testing.google.ListGenerators.ImmutableListOfGenerator;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.presence.interfaces.PresenceCondition;

import java.util.List;

public class ARGPathWithPresenceConditions extends ARGPath {

  private final ImmutableList<PresenceCondition> presenceConditions;

  public ARGPathWithPresenceConditions(
      List<ARGState> pStates,
      List<PresenceCondition> pPresenceConditions,
      List<CFAEdge> pEdges) {
    super(pStates, pEdges);
    presenceConditions = ImmutableList.copyOf(Preconditions.checkNotNull(pPresenceConditions));
    //Preconditions.checkArgument(pPresenceConditions.size() == pStates.size());
  }
}
