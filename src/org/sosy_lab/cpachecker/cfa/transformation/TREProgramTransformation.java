// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class TREProgramTransformation extends ProgramTransformation{
  public ProgramTransformationBehaviour behaviour = ProgramTransformationBehaviour.PRECISE;

  @Override
  public SubCFA transform(CFA pCFA, CFANode pEntryNode, CFANode pExitNode) {

    //TODO do tail recursion elimination

    return new SubCFA(
        pCFA,
        pEntryNode,
        pExitNode,
        null,
        null,
        ProgramTransformationEnum.TAIL_RECURSION_ELIMINATION,
        ProgramTransformationBehaviour.PRECISE,
        ImmutableSet.of(),
        ImmutableSet.of()
    );
  }
}
