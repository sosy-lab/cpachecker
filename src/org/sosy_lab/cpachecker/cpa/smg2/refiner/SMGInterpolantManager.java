// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.refiner;

import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.util.refinement.InterpolantManager;

/** InterpolantManager for interpolants of {@link SMGState}. */
public class SMGInterpolantManager implements InterpolantManager<SMGState, SMGInterpolant> {

  private static final SMGInterpolantManager SINGLETON = new SMGInterpolantManager();

  private SMGInterpolantManager() {
    // DO NOTHING
  }

  public static SMGInterpolantManager getInstance() {
    return SINGLETON;
  }

  @Override
  public SMGInterpolant createInitialInterpolant() {
    return SMGInterpolant.createInitial();
  }

  @Override
  public SMGInterpolant createInterpolant(SMGState state) {
    return state.createInterpolant();
  }

  @Override
  public SMGInterpolant getTrueInterpolant() {
    return SMGInterpolant.createTRUE();
  }

  @Override
  public SMGInterpolant getFalseInterpolant() {
    return SMGInterpolant.createFALSE();
  }
}
