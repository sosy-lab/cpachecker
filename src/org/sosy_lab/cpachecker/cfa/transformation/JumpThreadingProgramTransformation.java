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

public class JumpThreadingProgramTransformation extends ProgramTransformation {

  @Override
  public Optional<ProgramTransformationInformation> transform(CFA pCFA, CFANode pNode) {

    // TODO check conditions

    // TODO perform transformation
    return Optional.empty();
  }
}
