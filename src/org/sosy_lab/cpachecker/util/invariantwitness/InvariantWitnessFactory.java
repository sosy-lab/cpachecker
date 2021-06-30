// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;

public class InvariantWitnessFactory {
  private final LogManager logger;
  private final CFA cfa;
  private final Table<String, Integer, Integer> lineOffsetsByFile;

  public InvariantWitnessFactory(LogManager pLogger, CFA pCfa) {
    logger = pLogger;
    cfa = pCfa;
    lineOffsetsByFile = getLineOffsetsByFile(cfa, logger);
  }

  /* Returns a factory - possibly always the same possibly always a different one. Consider calling this method sparingly, since creation of a factory could be expensive. */
  public static InvariantWitnessFactory getFactory(LogManager pLogger, CFA pCfa) {
    // TODO Possibly Cache factory by pCfa
    return new InvariantWitnessFactory(pLogger, pCfa);
  }

  public Set<InvariantWitness> fromLocationAndInvariant(
      CFANode location, ExpressionTree<Object> invariant) {
    Set<FileLocation> effectiveLocations = getEffectiveLocations(location);
    ImmutableSet.Builder<InvariantWitness> result = ImmutableSet.builder();
    if (effectiveLocations.isEmpty()) {
      logger.logf(
          Level.FINER, "Could not determine a location for invariant %s, skipping.", invariant);
    }

    for (FileLocation invariantLocation : effectiveLocations) {
      final String fileName = invariantLocation.getFileName();
      final int lineNumber = invariantLocation.getStartingLineInOrigin();
      final int lineOffset = lineOffsetsByFile.get(fileName, lineNumber);
      final int offsetInLine = invariantLocation.getNodeOffset() - lineOffset;

      InvariantWitness invariantWitness =
          InvariantWitness.builder()
              .formula(invariant)
              .location(fileName, "file_hash", lineNumber, offsetInLine, location.getFunctionName())
              .build();
      // TODO extract meta-data

      result.add(invariantWitness);
    }

    return result.build();
  }

  /* Assumes that CPAchecker is the producer of this invariant */
  public Set<InvariantWitness> fromLocationInvariant(ExpressionTreeLocationInvariant invariant) {
    return fromLocationAndInvariant(invariant.getLocation(), invariant.asExpressionTree());
  }

  private static Table<String, Integer, Integer> getLineOffsetsByFile(CFA cfa, LogManager logger) {
    ImmutableTable.Builder<String, Integer, Integer> result = ImmutableTable.builder();

    for (Path filename : cfa.getFileNames()) {
      if (Files.isRegularFile(filename)) {
        String fileContent;
        try {
          fileContent = Files.readString(filename);
        } catch (IOException pE) {
          logger.logfUserException(Level.WARNING, pE, "Could not read file %s", filename);
          continue;
        }

        List<String> sourceLines = Splitter.onPattern("\\n").splitToList(fileContent);
        int currentOffset = 0;
        for (int lineNumber = 0; lineNumber < sourceLines.size(); lineNumber++) {
          result.put(filename.toString(), lineNumber + 1, currentOffset);
          currentOffset += sourceLines.get(lineNumber).length() + 1;
        }
      }
    }
    return result.build();
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
