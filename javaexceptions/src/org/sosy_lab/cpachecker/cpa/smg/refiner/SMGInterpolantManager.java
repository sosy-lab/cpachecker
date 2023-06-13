// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.refiner;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionBlock;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.UnmodifiableSMGState;
import org.sosy_lab.cpachecker.util.refinement.InterpolantManager;

public class SMGInterpolantManager
    implements InterpolantManager<Collection<SMGState>, SMGInterpolant> {

  private final SMGInterpolant initalInterpolant;

  public SMGInterpolantManager(LogManager pLogger, CFA pCfa, SMGOptions options)
      throws SMGInconsistentException {
    initalInterpolant =
        SMGInterpolant.createInitial(
            pLogger, pCfa.getMachineModel(), pCfa.getMainFunction(), options);
  }

  @Override
  public SMGInterpolant createInitialInterpolant() {
    return initalInterpolant;
  }

  @Override
  public SMGInterpolant createInterpolant(Collection<SMGState> pStates) {
    return new SMGInterpolant(pStates);
  }

  @Override
  public SMGInterpolant getTrueInterpolant() {
    return new SMGInterpolant(ImmutableSet.of());
  }

  @Override
  public SMGInterpolant getFalseInterpolant() {
    return SMGInterpolant.getFalseInterpolant();
  }

  public SMGInterpolant createInterpolant(
      UnmodifiableSMGState pState, Set<SMGAbstractionBlock> pAbstractionBlocks) {
    return new SMGInterpolant(ImmutableSet.of(pState), pAbstractionBlocks);
  }
}
