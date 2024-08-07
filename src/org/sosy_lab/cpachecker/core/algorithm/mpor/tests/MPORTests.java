// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.tests;

import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

@SuppressWarnings("unused")
@SuppressFBWarnings({"UUF_UNUSED_FIELD", "URF_UNREAD_FIELD"})
public class MPORTests {

  MPORAlgorithm algorithm;

  public MPORTests(MPORAlgorithm pAlgorithm) {
    algorithm = pAlgorithm;
  }

  public static void testCommutativity(
      LogManager pLogManager,
      PredicateTransferRelation pPtr,
      PredicateAbstractState pAbstractState,
      ImmutableSet<CFAEdge> pGlobalAccesses)
      throws CPATransferException, InterruptedException {

    for (CFAEdge edgeA : pGlobalAccesses) {
      for (CFAEdge edgeB : pGlobalAccesses) {
        if (!edgeA.equals(edgeB)) {
          if (MPORUtil.doEdgesCommute(pPtr, pAbstractState, edgeA, edgeB)) {
            // pLogManager.log(
            // Level.INFO, "TRUE commute - " + edgeA.getCode() + " - " + edgeB.getCode());
          } else {
            // pLogManager.log(
            // Level.INFO, "FALSE commute - " + edgeA.getCode() + " - " + edgeB.getCode());
          }
        }
      }
    }
  }
}
