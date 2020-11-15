// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.slicing;

import java.util.Collection;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;

/**
 * Static program slicer that represents the "identity slice". This slicer always creates a program
 * slice that contains all edges of the given CFA (i.e., it doesn't slice anything).
 *
 * @see SlicerFactory
 */
public class IdentitySlicer extends AbstractSlicer {

  IdentitySlicer(
      SlicingCriteriaExtractor pExtractor,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Configuration pConfig)
      throws InvalidConfigurationException {
    super(pExtractor, pLogger, pShutdownNotifier, pConfig);
  }

  @Override
  public Slice getSlice0(CFA pCfa, Collection<CFAEdge> pSlicingCriteria) {
    EdgeCollectingCFAVisitor visitor = new EdgeCollectingCFAVisitor();
    CFATraversal.dfs().traverseOnce(pCfa.getMainFunction(), visitor);
    return new Slice(pCfa, visitor.getVisitedEdges(), pSlicingCriteria);
  }
}
