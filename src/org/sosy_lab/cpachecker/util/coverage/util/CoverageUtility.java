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
import org.sosy_lab.cpachecker.cpa.coverage.analysisindependent.AnalysisIndependentCoverageCPA;
import org.sosy_lab.cpachecker.cpa.coverage.predicate.PredicateCoverageCPA;
import org.sosy_lab.cpachecker.util.coverage.collectors.CoverageCollectorHandler;

public class CoverageUtility {
  public static CoverageCollectorHandler extractCoverageCollector(
      CoverageCollectorHandler covCollectorHandler, ConfigurableProgramAnalysis cpa) {
    if (cpa instanceof ARGCPA) {
      ImmutableList<ConfigurableProgramAnalysis> cpas = ((ARGCPA) cpa).getWrappedCPAs();
      for (var compositeCPA : cpas) {
        if (compositeCPA instanceof CompositeCPA) {
          ImmutableList<ConfigurableProgramAnalysis> wrappedCPAs =
              ((CompositeCPA) compositeCPA).getWrappedCPAs();
          for (var wrappedCPA : wrappedCPAs) {
            if (wrappedCPA instanceof PredicateCoverageCPA) {
              return ((PredicateCoverageCPA) wrappedCPA).getCoverageCollectorHandler();
            } else if (wrappedCPA instanceof AnalysisIndependentCoverageCPA) {
              return ((AnalysisIndependentCoverageCPA) wrappedCPA).getCoverageCollectorHandler();
            }
          }
        }
      }
    }
    return covCollectorHandler;
  }

  public static CoverageCollectorHandler getCoverageCollectorHandlerFromReachedSet(
      UnmodifiableReachedSet pReached, CFA cfa) {
    CoverageCollectorHandler covCollectorHandler = new CoverageCollectorHandler(cfa);
    if (pReached instanceof PartitionedReachedSet) {
      ConfigurableProgramAnalysis cpa = ((PartitionedReachedSet) pReached).getCPA();
      covCollectorHandler = CoverageUtility.extractCoverageCollector(covCollectorHandler, cpa);
    }
    return covCollectorHandler;
  }

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
}
