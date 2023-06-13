// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.slicing;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

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

    EdgeCollectingCFAVisitor cfaVisitor = new EdgeCollectingCFAVisitor();
    CFATraversal.dfs().traverseOnce(pCfa.getMainFunction(), cfaVisitor);

    List<CFAEdge> relevantEdges = cfaVisitor.getVisitedEdges();
    ImmutableSet<ASimpleDeclaration> relevantDeclarations =
        AbstractSlice.computeRelevantDeclarations(relevantEdges, declaration -> true);

    return new AbstractSlice(pCfa, pSlicingCriteria, relevantEdges, relevantDeclarations) {

      @Override
      public boolean isRelevantDef(CFAEdge pEdge, MemoryLocation pMemoryLocation) {

        checkNotNull(pEdge, "pEdge must not be null");
        checkNotNull(pMemoryLocation, "pEdge must not be null");
        checkArgument(
            getRelevantEdges().contains(pEdge), "pEdge is not relevant to this program slice");

        return true;
      }

      @Override
      public boolean isRelevantUse(CFAEdge pEdge, MemoryLocation pMemoryLocation) {

        checkNotNull(pEdge, "pEdge must not be null");
        checkNotNull(pMemoryLocation, "pEdge must not be null");
        checkArgument(
            getRelevantEdges().contains(pEdge), "pEdge is not relevant to this program slice");

        return true;
      }
    };
  }
}
