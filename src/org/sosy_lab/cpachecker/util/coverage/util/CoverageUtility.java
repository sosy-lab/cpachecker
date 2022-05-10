// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.util;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.coverage.CoverageCPA;
import org.sosy_lab.cpachecker.util.coverage.collectors.CoverageCollectorHandler;

/**
 * Utility class for a collection of static methods which are used mostly in the util.coverage
 * package.
 */
public class CoverageUtility {
  /* ##### Public Methods ##### */
  /**
   * Checks if the given CFA edge can be considered for coverage or has code which represents a
   * dummy location or function declaration.
   *
   * @param pEdge CFA edge to be checked if it is useful to be considered for coverage
   * @return true if the given edge is capable to be considered for coverage
   */
  public static boolean coversLine(CFAEdge pEdge) {
    FileLocation loc = pEdge.getFileLocation();
    if (loc.getStartingLineNumber() == 0) {
      // dummy location
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
   * @param cfa CFA of the program which is used for building a backup CoverageCollectorHandler.
   * @return either a backup handler or a unique specified CoverageCollectorHandler instance.
   */
  public static CoverageCollectorHandler getCoverageCollectorHandlerFromReachedSet(
      UnmodifiableReachedSet pReached, CFA cfa) {
    CoverageCollectorHandler covCollectorHandler = new CoverageCollectorHandler(cfa);
    if (pReached instanceof PartitionedReachedSet) {
      ConfigurableProgramAnalysis cpa = ((PartitionedReachedSet) pReached).getCPA();
      covCollectorHandler =
          CoverageUtility.extractCoverageCollectorFromCPA(covCollectorHandler, cpa);
    }
    return covCollectorHandler;
  }

  /* ##### Helper Methods ##### */
  private static CoverageCollectorHandler extractCoverageCollectorFromCPA(
      CoverageCollectorHandler covCollectorHandler, ConfigurableProgramAnalysis cpa) {
    if (cpa instanceof ARGCPA) {
      ImmutableList<ConfigurableProgramAnalysis> cpas = ((ARGCPA) cpa).getWrappedCPAs();
      for (var compositeCPA : cpas) {
        if (compositeCPA instanceof CompositeCPA) {
          ImmutableList<ConfigurableProgramAnalysis> wrappedCPAs =
              ((CompositeCPA) compositeCPA).getWrappedCPAs();
          for (var wrappedCPA : wrappedCPAs) {
            if (wrappedCPA instanceof CoverageCPA) {
              return ((CoverageCPA) wrappedCPA).getCoverageCollectorHandler();
            }
          }
        }
      }
    }
    return covCollectorHandler;
  }
}
