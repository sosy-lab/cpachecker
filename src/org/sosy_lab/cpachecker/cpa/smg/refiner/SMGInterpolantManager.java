/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
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