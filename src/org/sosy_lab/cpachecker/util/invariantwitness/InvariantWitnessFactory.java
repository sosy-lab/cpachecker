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
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;

/**
 * Creates InvariantWitnesses. The class serves as a data-interface to the invariant store. As such
 * it translates from CPAchecker-internal data structures (e.g. CFANodes, ExpressionTrees) to
 * InvariantWitnesses.
 */
public class InvariantWitnessFactory {
  private final LogManager logger;

  @SuppressWarnings("unused")
  private final CFA cfa;

  private InvariantWitnessFactory(LogManager pLogger, CFA pCfa) {
    logger = pLogger;
    cfa = pCfa;
  }

  /** Returns a new instance of this class. */
  public static InvariantWitnessFactory getFactory(LogManager pLogger, CFA pCFA) {
    // TODO Possibly Cache factory by pCfa
    return new InvariantWitnessFactory(pLogger, pCFA);
  }

  /**
   * Generates witnesses from a node and an expression tree. The file location is determined from
   * the node heuristically. The mapping is therefore imprecise and thus this method produces a
   * collection. Moreover, the collection might be empty, even if the input is valid.
   *
   * @param node Node where the invariant holds
   * @param invariant Invariant formula
   * @return immutable collection of invariants
   */
  public Collection<InvariantWitness> fromNodeAndInvariant(
      CFANode node, ExpressionTree<Object> invariant) {
    Set<FileLocation> effectiveLocations = getEffectiveLocations(node);
    ImmutableSet.Builder<InvariantWitness> result = ImmutableSet.builder();
    if (effectiveLocations.isEmpty()) {
      logger.logf(
          Level.FINEST, "Could not determine a location for invariant %s, skipping.", invariant);
    }

    for (FileLocation invariantLocation : effectiveLocations) {
      InvariantWitness invariantWitness = new InvariantWitness(invariant, invariantLocation, node);

      result.add(invariantWitness);
    }

    return result.build();
  }

  /** Generates a witness from the given fileLocation, node and expression tree. */
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

    for (CFAEdge edge : CFAUtils.leavingEdges(node)) {
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
