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

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionBlock;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;

import java.util.Set;

public class SMGInterpolantManager {

  private final LogManager logger;
  private final MachineModel model;
  private final SMGInterpolant initalInterpolant;

  public SMGInterpolantManager(MachineModel pModel, LogManager pLogger, CFA pCfa,
      boolean pTrackPredicates, int pExternalAllocationSize) {
    logger = pLogger;
    model = pModel;
    initalInterpolant = SMGInterpolant.createInitial(logger, model, pCfa.getMainFunction(),
        pTrackPredicates, pExternalAllocationSize);
  }

  public SMGInterpolant createInitialInterpolant() {
    return initalInterpolant;
  }

  public SMGInterpolant createInterpolant(SMGState pState) {
    return pState.createInterpolant();
  }

  public SMGInterpolant getFalseInterpolant() {
    return SMGInterpolant.getFalseInterpolant();
  }

  public SMGInterpolant getTrueInterpolant(SMGInterpolant pTemplate) {
    return SMGInterpolant.getTrueInterpolant(pTemplate);
  }

  public SMGInterpolant createInterpolant(SMGState pState,
      Set<SMGAbstractionBlock> pAbstractionBlocks) {
    return pState.createInterpolant(pAbstractionBlocks);
  }
}