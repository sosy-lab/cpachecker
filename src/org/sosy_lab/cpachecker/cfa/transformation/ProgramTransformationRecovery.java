// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.location.LocationStateFactory;

/**
 * Class for reverting a program transformation after a ProgramTransformationCEGARAlgorithm. The
 * needed information is set when inserting program transformations during the creation of the CFA.
 */
public interface ProgramTransformationRecovery {

  /**
   * Change all states in the reached set, so that no LocationState belonging to
   * pAfterProgramTransformation remains.
   *
   * @param pBeforeState the state before entering the program transformation
   * @param pAfterProgramTransformation the SubCFA of the entered program transformation
   * @param reached the ReachedSet/ARG after ProgramTransformationCEGARAlgorithm
   * @param pLocationStateFactory used for access to LocationStates
   */
  public void revertProgramTransformation(
      AbstractState pBeforeState,
      SubCFA pAfterProgramTransformation,
      ReachedSet reached,
      LocationStateFactory pLocationStateFactory);
}
