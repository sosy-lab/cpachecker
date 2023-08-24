// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.nio.file.Path;
import java.util.Collection;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.Edge;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.Witness;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.WitnessType;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;

public class WitnessToYamlWitnessConverter {
  private final LogManager logger;
  private final boolean writeLocationInvariants;

  public WitnessToYamlWitnessConverter(LogManager pLogger) {
    this(pLogger, false);
  }

  public WitnessToYamlWitnessConverter(LogManager pLogger, boolean pWriteLocationInvariants) {
    logger = pLogger;
    writeLocationInvariants = pWriteLocationInvariants;
  }

  public ImmutableList<InvariantWitness> convertProofWitness(Witness pWitness) {
    Preconditions.checkState(pWitness.getWitnessType().equals(WitnessType.CORRECTNESS_WITNESS));
    ImmutableSet.Builder<InvariantWitness> builder = ImmutableSet.builder();
    for (String invexpstate : pWitness.getInvariantExportStates()) {
      ExpressionTree<Object> invariantExpression = pWitness.getStateInvariant(invexpstate);

      boolean isLoopHead =
          pWitness.getEnteringEdges().get(invexpstate).stream()
              .anyMatch(
                  x ->
                      "true".equalsIgnoreCase(x.getLabel().getMapping().get(KeyDef.ENTERLOOPHEAD)));

      if (!isLoopHead && !writeLocationInvariants) {
        continue;
        // TODO: we might want to export regular invariants as location invariant at some point.
      }

      Collection<Edge> edges = pWitness.getLeavingEdges().get(invexpstate);
      for (Edge e : edges) {
        Collection<CFAEdge> cfaEdges = pWitness.getCFAEdgeFor(e);
        ImmutableSet<CFANode> cfaNodes =
            cfaEdges.stream().map(CFAEdge::getSuccessor).collect(ImmutableSet.toImmutableSet());
        if (cfaNodes.size() != 1) {
          logger.logf(
              Level.WARNING,
              "Expected one CFA node matching invariant in witness, but identified %d!",
              cfaNodes.size());
        }
        CFANode firstNode = cfaNodes.asList().get(0);
        int startline = Integer.parseInt(e.getLabel().getMapping().get(KeyDef.STARTLINE));
        int startoffset = Integer.parseInt(e.getLabel().getMapping().get(KeyDef.OFFSET));
        FileLocation loc =
            new FileLocation(
                Path.of(pWitness.getOriginFile()), startoffset, 0, startline, startline);
        builder.add(new InvariantWitness(invariantExpression, loc, firstNode));
      }
    }

    return builder.build().asList();
  }
}
