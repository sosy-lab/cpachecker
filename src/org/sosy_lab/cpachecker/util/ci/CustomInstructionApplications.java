/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.ci;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

import com.google.common.collect.ImmutableMap;


public class CustomInstructionApplications {

  private final ImmutableMap<CFANode, AppliedCustomInstruction> cis;

  public CustomInstructionApplications(final ImmutableMap<CFANode, AppliedCustomInstruction> pCis) {
    cis = pCis;
  }

  public boolean isStartState(final AbstractState pState) throws CPAException {
    CFANode locState = AbstractStates.extractLocation(pState);
    if (locState == null) {
      throw new CPAException("TheState " + pState+ " has to contain a location state!");
    }
    return cis.containsKey(locState);
  }

  public boolean isEndState(final AbstractState pIsEnd, final AbstractState pCIStart) throws CPAException {
    return isEndState(pIsEnd, AbstractStates.extractLocation(pCIStart));
  }

  public boolean isEndState(final AbstractState pIsEnd, final CFANode pCIStart) throws CPAException {
    return cis.get(pCIStart).isEndState(pIsEnd);
  }

}
