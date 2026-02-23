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

public abstract class ProgramTransformation {
  private ProgramTransformationBehaviour behaviour;

  /**
   * Checks if a specific ProgramTransformation can be applied on a CFA.
   *
   * @param pCFA CFA
   * @return Either the information needed for the program transformation is present or empty.
   */
  public abstract Optional<ProgramTransformationInformation> canBeApplied(CFA pCFA);

  /**
   * Perform the program transformation and return the resulting SubCFA.
   * @param pCFA CFA
   * @param pInfo ProgramTransformationInformation
   * @return SubCFA
   */
  public abstract SubCFA transform(CFA pCFA, ProgramTransformationInformation pInfo);
}
