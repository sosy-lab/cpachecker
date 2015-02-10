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
package org.sosy_lab.cpachecker.cpa.value.refiner;

import static org.sosy_lab.cpachecker.util.AbstractStates.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.StaticRefiner;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

@Options(prefix="cpa.value.refiner")
public class ValueAnalysisStaticRefiner extends StaticRefiner {

  @Option(secure=true, description="use heuristic to extract a precision from the CFA statically on first refinement")
  private boolean performStaticRefinement = false;

  public ValueAnalysisStaticRefiner(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pLogger);

    pConfig.inject(this);
  }

  public boolean performRefinement(final ARGReachedSet pReached, final ARGPath pErrorPath) throws CPAException {
    if(performStaticRefinement) {
      logger.log(Level.INFO, "Performing a single static refinement for path based on CFA.");

      VariableTrackingPrecision targetStatePrecision = extractTargetStatePrecision(pReached, pErrorPath);

      VariableTrackingPrecision refinedPrecision = targetStatePrecision.withIncrement(HashMultimap.create(extractIncrementForPathFromCfa(pErrorPath)));

      for (ARGState childOfRoot : Sets.newHashSet(pErrorPath.getFirstState().getChildren())) {
        pReached.removeSubtree(childOfRoot, refinedPrecision, VariableTrackingPrecision.isMatchingCPAClass(ValueAnalysisCPA.class));
      }

      performStaticRefinement = false;

      return true;
    }

    return false;
  }

  private VariableTrackingPrecision extractTargetStatePrecision(final ARGReachedSet pReached, final ARGPath pErrorPath) {
    Precision compositePrecision = pReached.asReachedSet().getPrecision(pErrorPath.getLastState());

    FluentIterable<Precision> precisions = Precisions.asIterable(compositePrecision);

    return (VariableTrackingPrecision) precisions.filter(VariableTrackingPrecision.isMatchingCPAClass(ValueAnalysisCPA.class)).get(0);
  }

  private Multimap<CFANode, MemoryLocation> extractIncrementForPathFromCfa(final ARGPath pPath)
      throws CPATransferException {

    ARGState targetState = pPath.getLastState();
    assert isTargetState(targetState);

    Multimap<CFANode, MemoryLocation> pathIncrement = HashMultimap.create();
    for (AssumeEdge assume : getTargetLocationAssumes(targetState)) {
      for (CIdExpression identifier : getVariablesOfAssume(assume)) {
        pathIncrement.put(assume.getSuccessor(), MemoryLocation.valueOf(identifier.getDeclaration().getQualifiedName()));
      }
    }

    return pathIncrement;
  }

  private Set<AssumeEdge> getTargetLocationAssumes(ARGState targetState) {
    Collection<CFANode> targetNodes = ImmutableList.of(extractLocation(targetState));

    return new HashSet<>(getTargetLocationAssumes(targetNodes).values());
  }
}
