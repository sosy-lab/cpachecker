//// This file is part of CPAchecker,
//// a tool for configurable software verification:
//// https://cpachecker.sosy-lab.org
////
//// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
////
//// SPDX-License-Identifier: Apache-2.0
//
// package org.sosy_lab.cpachecker.core.algorithm.giageneration;
//
// import java.io.IOException;
// import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
// import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
// import org.sosy_lab.cpachecker.cpa.arg.ARGState;
// import org.sosy_lab.cpachecker.exceptions.CPAException;
// import org.sosy_lab.cpachecker.util.AbstractStates;
//
// public class GIAWitnessGenerator {
//
//  private final GIAVioWitGenerator vioWitGenerator;
////  private final GIACorWitGenerator corWitGenerator;
//
//  public GIAWitnessGenerator(
//      GIACorWitGenerator pGIACorWitGenerator, GIAVioWitGenerator pGIAVioWitGenerator) {
//    this.corWitGenerator = pGIACorWitGenerator;
//    this.vioWitGenerator = pGIAVioWitGenerator;
//  }
//
//  public int produceGIA4Witness(Appendable pOutput, UnmodifiableReachedSet pReached)
//      throws CPAException, IOException {
//    boolean error = false;
//    for (AbstractState state : pReached.asCollection()) {
//      final ARGState argState = AbstractStates.extractStateByType(state, ARGState.class);
//      if (argState != null && argState.isTarget()) {
//        error = true;
//        break;
//      }
//    }
//    if (error || pReached.wasTargetReached()) {
//      return vioWitGenerator.produceGIA4ViolationWitness(pOutput, pReached);
//    } else {
//      return corWitGenerator.produceGIA4CorrectnessWitness(pOutput, pReached);
//    }
//  }
// }
