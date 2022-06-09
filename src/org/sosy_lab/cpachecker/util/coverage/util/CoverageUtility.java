// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.util;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.coverage.CoverageCPA;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.coverage.collectors.CoverageCollectorHandler;

/**
 * Utility class for a collection of static methods which are used mostly in the util.coverage
 * package.
 */
public class CoverageUtility {
  /**
   * Checks if the given CFA edge can be considered for coverage or has code which represents a
   * dummy location or function declaration.
   *
   * @param pEdge CFA edge to be checked if it is useful to be considered for coverage
   * @return true if the given edge is capable to be considered for coverage
   */
  public static boolean coversLine(CFAEdge pEdge) {
    if (!pEdge.getFileLocation().isRealLocation()) {
      return false;
    }
    if (pEdge instanceof ADeclarationEdge
        && (((ADeclarationEdge) pEdge).getDeclaration() instanceof AFunctionDeclaration)) {
      // Function declarations span the complete body, this is not desired.
      return false;
    }
    return true;
  }

  /**
   * Extracts a CPA instance from the reached set. Depending on the specified CPAs, this method
   * extracts the relevant CoverageCollectorHandler instance and returns it. If none was found
   * because there was no CoverageCPA specified, a new CoverageCollectorHandler instance is
   * returned.
   *
   * @param pReached the reached set.
   * @return either a backup handler or a unique specified CoverageCollectorHandler instance.
   */
  public static CoverageCollectorHandler getCoverageCollectorHandlerFromReachedSet(
      UnmodifiableReachedSet pReached, CFA cfa) {
    if (pReached instanceof PartitionedReachedSet) {
      CoverageCPA cpa =
          CPAs.retrieveCPA(((PartitionedReachedSet) pReached).getCPA(), CoverageCPA.class);
      if (cpa != null) {
        return cpa.getCoverageCollectorHandler();
      }
    }
    return new CoverageCollectorHandler(cfa);
  }
}
