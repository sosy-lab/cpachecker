// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.tests;

import com.google.common.collect.ImmutableMap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPORUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.GAPNode;
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
      ImmutableMap<CFAEdge, GAPNode> pGlobalAccesses)
      throws CPATransferException, InterruptedException {

    for (var entryA : pGlobalAccesses.entrySet()) {
      for (var entryB : pGlobalAccesses.entrySet()) {
        if (!entryA.equals(entryB)) {
          CFAEdge edgeA = entryA.getKey();
          CFAEdge edgeB = entryB.getKey();
          if (MPORUtil.doEdgesCommute(pPtr, pAbstractState, edgeA, edgeB)) {
            pLogManager.log(
                Level.INFO, "TRUE commute - " + edgeA.getCode() + " - " + edgeB.getCode());
          } else {
            pLogManager.log(
                Level.INFO, "FALSE commute - " + edgeA.getCode() + " - " + edgeB.getCode());
          }
        }
      }
    }
  }
}
