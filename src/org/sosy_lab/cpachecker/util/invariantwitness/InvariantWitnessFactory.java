// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;

public class InvariantWitnessFactory {
  private final LogManager logger;

  @SuppressWarnings("unused")
  private final CFA cfa;

  private InvariantWitnessFactory(LogManager pLogger, CFA pCfa) {
    logger = pLogger;
    cfa = pCfa;
  }

  /* Returns a factory - possibly always the same possibly always a different one. Consider calling this method sparingly, since creation of a factory could be expensive. */
  public static InvariantWitnessFactory getFactory(LogManager pLogger, CFA pCFA) {
    // TODO Possibly Cache factory by pCfa
    return new InvariantWitnessFactory(pLogger, pCFA);
  }

  public Collection<InvariantWitness> fromNodeAndInvariant(
      CFANode node, ExpressionTree<Object> invariant) {
    Set<FileLocation> effectiveLocations = getEffectiveLocations(node);
    ImmutableSet.Builder<InvariantWitness> result = ImmutableSet.builder();
    if (effectiveLocations.isEmpty()) {
      logger.logf(
          Level.FINER, "Could not determine a location for invariant %s, skipping.", invariant);
    }

    for (FileLocation invariantLocation : effectiveLocations) {
      InvariantWitness invariantWitness = new InvariantWitness(invariant, invariantLocation, node);

      result.add(invariantWitness);
    }

    return result.build();
  }

  public InvariantWitness fromLocationAndInvariant(
      FileLocation fileLocation, CFANode node, ExpressionTree<Object> invariant) {
    return new InvariantWitness(invariant, fileLocation, node);
  }

  private Set<FileLocation> getEffectiveLocations(CFANode node) {
    ImmutableSet.Builder<FileLocation> locations = ImmutableSet.builder();

    if (node instanceof FunctionEntryNode || node instanceof FunctionExitNode) {
      // Cannot map to a position
      return locations.build();
    }

    for (int i = 0; i < node.getNumLeavingEdges(); i++) {
      CFAEdge edge = node.getLeavingEdge(i);
      if (!edge.getFileLocation().equals(FileLocation.DUMMY)
          && !edge.getDescription().contains("CPAchecker_TMP")
          && !(edge instanceof AssumeEdge)) {
        locations.add(edge.getFileLocation());
      }
    }

    // for (int i = 0; i < node.getNumEnteringEdges(); i++) {
    //   CFAEdge edge = node.getEnteringEdge(i);
    //   if (!edge.getFileLocation().equals(FileLocation.DUMMY)
    //       && !edge.getDescription().contains("CPAchecker_TMP")
    //       && !(edge instanceof AssumeEdge)) {
    //     locations.add(edge.getFileLocation().getEndingLineNumber());
    //   }
    // }

    return locations.build();
  }
}
