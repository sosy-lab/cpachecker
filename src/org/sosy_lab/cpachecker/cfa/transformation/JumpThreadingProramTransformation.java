// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class JumpThreadingProramTransformation extends ProgramTransformation {
  public ProgramTransformationBehaviour behaviour = ProgramTransformationBehaviour.PRECISE;

  @Override
  public Optional<SubCFA> transform(CFA pCFA, CFANode pNode) {
    return Optional.empty();
  }
}
