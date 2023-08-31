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
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.Edge;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.Witness;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.KeyDef;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon.WitnessType;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;

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

      // True invariants do not add any information in order to proof the program
      if (invariantExpression.equals(ExpressionTrees.getTrue())) {
        continue;
      }

      boolean isLoopHead =
          pWitness.getEnteringEdges().get(invexpstate).stream()
              .anyMatch(
                  x ->
                      "true".equalsIgnoreCase(x.getLabel().getMapping().get(KeyDef.ENTERLOOPHEAD)));

      Collection<Edge> edges;
      if (!isLoopHead && writeLocationInvariants) {
        // For Location invariants, we need to consider certain
        edges = pWitness.getEnteringEdges().get(invexpstate);
      } else if (!isLoopHead && !writeLocationInvariants) {
        continue;
      } else {
        edges = pWitness.getLeavingEdges().get(invexpstate);
      }

      Set<FileLocation> exportedInvariantsAtFilelocation = new HashSet<>();
      for (Edge e : edges) {
        Collection<CFAEdge> cfaEdges = pWitness.getCFAEdgeFor(e);
        ImmutableSet<CFANode> cfaNodes;
        if (isLoopHead) {
          // There is a blank edge between the loop start node and the actual node in the CFA, which
          // is different to the witness automaton which does not consider these.
          // This makes it such that CFANode is the loop start node in the CFA
          cfaNodes =
              cfaEdges.stream()
                  .map(CFAEdge::getPredecessor)
                  .map(CFAUtils::enteringEdges)
                  .flatMap(x -> x.stream())
                  .map(CFAEdge::getPredecessor)
                  .collect(ImmutableSet.toImmutableSet());
        } else {
          cfaNodes =
              cfaEdges.stream().map(CFAEdge::getSuccessor).collect(ImmutableSet.toImmutableSet());
        }
        if (cfaNodes.size() != 1) {
          logger.logf(
              Level.WARNING,
              "Expected one CFA node matching invariant in witness, but identified %d!",
              cfaNodes.size());
        }
        CFANode firstNode = cfaNodes.asList().get(0);

        int startline = Integer.parseInt(e.getLabel().getMapping().get(KeyDef.STARTLINE));
        if (isLoopHead) {
          // The witness removes the actual loop start node opting for the edge to already contain
          // the
          // next actual statement being executed and not the loop start node
          startline -= 1;
        }

        int startoffset = Integer.parseInt(e.getLabel().getMapping().get(KeyDef.OFFSET));
        FileLocation loc =
            new FileLocation(
                Path.of(pWitness.getOriginFile()), startoffset, 0, startline, startline);

        // See if we already exported the invariant for this location, else export it
        if (exportedInvariantsAtFilelocation.contains(loc)) {
          continue;
        } else {
          exportedInvariantsAtFilelocation.add(loc);
        }

        builder.add(new InvariantWitness(invariantExpression, loc, firstNode));
      }
    }

    return builder.build().asList();
  }
}
