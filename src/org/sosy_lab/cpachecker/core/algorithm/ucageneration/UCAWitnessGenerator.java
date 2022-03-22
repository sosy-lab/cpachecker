// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.ucageneration;

import java.io.IOException;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class UCAWitnessGenerator {

  private final UCAVioWitGenerator vioWitGenerator;
  private final UCACorWitGenerator corWitGenerator;

  public UCAWitnessGenerator(
      UCACorWitGenerator pUCACorWitGenerator, UCAVioWitGenerator pUCAVioWitGenerator) {
    this.corWitGenerator = pUCACorWitGenerator;
    this.vioWitGenerator = pUCAVioWitGenerator;
  }

  public int produceUCA4Witness(Appendable pOutput, UnmodifiableReachedSet pReached)
      throws CPAException, IOException {
    boolean error = false;
    for (AbstractState state :pReached.asCollection()){
      if ( AbstractStates.extractStateByType(state, ARGState.class).isTarget()){
        error=true;

      }
    }
    if (error || pReached.wasTargetReached()) {
      return vioWitGenerator.produceUCA4ViolationWitness(pOutput, pReached);
    } else {
      return corWitGenerator.produceUCA4CorrectnessWitness(pOutput, pReached);
    }
  }
}
