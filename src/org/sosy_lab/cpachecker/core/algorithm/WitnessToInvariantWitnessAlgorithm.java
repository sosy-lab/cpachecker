// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAEnabledAnalysisPropertyViolationException;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.WitnessInvariantsExtractor;
import org.sosy_lab.cpachecker.util.invariantwitness.InvariantWitness;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.InvariantWitnessWriter;

@Options(prefix = "invariantStore")
public class WitnessToInvariantWitnessAlgorithm implements Algorithm {

  @Option(
      secure = true,
      required = true,
      description = "The witness from which invariants should be generated.")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path witness;

  @Option(
      secure = true,
      required = true,
      description = "The directory where the invariant is stored.")
  @FileOption(FileOption.Type.OUTPUT_DIRECTORY)
  private Path outDir;

  private final Configuration config;
  private final LogManager logger;
  private final CFA cfa;
  private final ShutdownNotifier shutdownNotifier;

  public WitnessToInvariantWitnessAlgorithm(
      Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier, CFA pCfa)
      throws InvalidConfigurationException {
    config = pConfig;
    config.inject(this);
    logger = pLogger;
    cfa = pCfa;
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet)
      throws CPAException, InterruptedException, CPAEnabledAnalysisPropertyViolationException {

    Set<ExpressionTreeLocationInvariant> invariants;
    try {
      WitnessInvariantsExtractor invariantsExtractor =
          new WitnessInvariantsExtractor(config, logger, cfa, shutdownNotifier, witness);
      invariants = invariantsExtractor.extractInvariantsFromReachedSet();
    } catch (InvalidConfigurationException pE) {
      throw new CPAException(
          "Invalid Configuration while analyzing witness:\n" + pE.getMessage(), pE);
    }

    Table<String, Integer, Integer> lineOffsetsByFile = getLineOffsetsByFile(invariants);

    Set<InvariantWitness> invariantWitnesses = new HashSet<>();
    for (ExpressionTreeLocationInvariant invariant : invariants) {
      invariantWitnesses.addAll(convertInvariantToInvariantWitnesses(invariant, lineOffsetsByFile));
    }

    InvariantWitnessWriter witnessWriter = InvariantWitnessWriter.getWriter();
    int witnessIndex = 0;
    for (InvariantWitness invariantWitness : invariantWitnesses) {
      String entry = witnessWriter.invariantWitnessToYamlEntry(invariantWitness);
      Path outFile =
          outDir.resolve(witness.getFileName() + "_" + witnessIndex + ".invariantwitness.yaml");

      try {
        writeToFile(entry, outFile);
      } catch (IOException e) {
        logger.log(Level.WARNING, "Could not write to file");
      }

      witnessIndex++;
    }
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  private void writeToFile(String content, Path filepath) throws IOException {
    try (Writer writer = IO.openOutputFile(filepath, Charset.defaultCharset())) {
      writer.write(content);
    }
  }

  private Set<InvariantWitness> convertInvariantToInvariantWitnesses(
      ExpressionTreeLocationInvariant invariant,
      Table<String, Integer, Integer> lineOffsetsByFile) {
    Set<FileLocation> effectiveLocations = getEffectiveLocations(invariant);
    ImmutableSet.Builder<InvariantWitness> result = ImmutableSet.builder();
    if (effectiveLocations.isEmpty()) {
      logger.logf(
          Level.INFO,
          "Could not determine a location for invariant %s, skipping.",
          invariant.asExpressionTree());
    }

    for (FileLocation invariantLocation : effectiveLocations) {
      final String fileName = invariantLocation.getFileName();
      final int lineNumber = invariantLocation.getStartingLineInOrigin();
      final int lineOffset = lineOffsetsByFile.get(fileName, lineNumber);
      final int offsetInLine = invariantLocation.getNodeOffset() - lineOffset;

      InvariantWitness invariantWitness =
          InvariantWitness.builder()
              .formula(invariant.asExpressionTree())
              .location(
                  fileName,
                  "file_hash",
                  lineNumber,
                  offsetInLine,
                  invariant.getLocation().getFunctionName())
              .build();
      // TODO extract meta-data

      result.add(invariantWitness);
    }

    return result.build();
  }

  private Table<String, Integer, Integer> getLineOffsetsByFile(
      Set<ExpressionTreeLocationInvariant> invariants) {
    ImmutableTable.Builder<String, Integer, Integer> result = ImmutableTable.builder();
    Set<String> filenames =
        invariants.stream()
            .map((inv) -> inv.getLocation().getFunction().getFileLocation().getFileName())
            .collect(Collectors.toSet());

    for (String filename : filenames) {
      if (Files.isRegularFile(Path.of(filename))) {
        String fileContent;
        try {
          fileContent = Files.readString(Path.of(filename));
        } catch (IOException pE) {
          logger.logfUserException(Level.WARNING, pE, "Could not read file %s", filename);
          continue;
        }

        List<String> sourceLines = Splitter.onPattern("\\n").splitToList(fileContent);
        int currentOffset = 0;
        for (int lineNumber = 0; lineNumber < sourceLines.size(); lineNumber++) {
          result.put(filename, lineNumber + 1, currentOffset);
          currentOffset += sourceLines.get(lineNumber).length() + 1;
        }
      }
    }
    return result.build();
  }

  private Set<FileLocation> getEffectiveLocations(ExpressionTreeLocationInvariant inv) {
    CFANode node = inv.getLocation();
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
