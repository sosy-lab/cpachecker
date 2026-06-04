// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import java.util.Map;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.location.LocationStateFactory;

public record TailRecursionEliminationRecovery(
    Map<CFANode, CFANode> nodeMap,
    int parameterNum,
    CFAEdge varDeclarationEdge,
    CFAEdge varAssignmentEdge,
    CFAEdge varReturnEdge
) implements ProgramTransformationRecovery {

  @Override
  public void revertProgramTransformation(
      AbstractState pBeforeState,
      AbstractState pInitialState,
      SubCFA pBeforeProgramTransformation,
      SubCFA pAfterProgramTransformation,
      ReachedSet reached,
      LocationStateFactory pLocationStateFactory) {
    // TODO
  }
}
