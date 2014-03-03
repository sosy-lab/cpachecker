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

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;
import static org.sosy_lab.cpachecker.util.AbstractStates.isTargetState;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisPrecision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.StaticRefiner;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

public class ValueAnalysisStaticRefiner extends StaticRefiner {

  private final ValueAnalysisPrecision valueAnalysisPrecision;

  public ValueAnalysisStaticRefiner(
      Configuration pConfig,
      LogManager pLogger,
      ValueAnalysisPrecision initialPrecision) throws InvalidConfigurationException {
    super(pConfig, pLogger);

    valueAnalysisPrecision = initialPrecision;
  }

  public ValueAnalysisPrecision extractPrecisionFromCfa(UnmodifiableReachedSet pReached,
      ARGPath pPath) throws CPATransferException {
    logger.log(Level.INFO, "Extracting precision from CFA...");

    ARGState targetState = Iterables.getLast(pPath).getFirst();
    assert isTargetState(targetState);
    CFANode targetNode = extractLocation(targetState);
    Collection<CFANode> targetNodes = ImmutableList.of(targetNode);
    Set<AssumeEdge> assumeEdges = new HashSet<>(getTargetLocationAssumes(targetNodes).values());
    Multimap<CFANode, MemoryLocation> increment = HashMultimap.create();

    for (AssumeEdge assume : assumeEdges) {
      for (CIdExpression idExpr : getVariablesOfAssume(assume)) {
        MemoryLocation memoryLocation = MemoryLocation.valueOf(idExpr.getDeclaration().getQualifiedName());
        increment.put(assume.getSuccessor(), memoryLocation);
      }
    }

    return new ValueAnalysisPrecision(valueAnalysisPrecision, increment);
  }
}
