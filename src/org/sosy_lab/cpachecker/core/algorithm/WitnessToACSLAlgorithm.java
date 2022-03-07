// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.logging.Level;
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
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.WitnessInvariantsExtractor;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;

@Options(prefix = "wacsl")
public class WitnessToACSLAlgorithm implements Algorithm {

  @Option(
      secure = true,
      required = true,
      description = "The witness from which ACSL annotations should be generated.")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path witness;

  @Option(
      secure = true,
      description = "The directory where generated, ACSL annotated programs are stored.")
  @FileOption(FileOption.Type.OUTPUT_DIRECTORY)
  private Path outDir = Path.of("annotated");

  @Option(
      secure = true,
      description =
          "Instead of comments, output the assertions into the original program as violations to unreach_call.prp")
  private boolean makeDirectAssertions = false;

  @Option(
      secure = true,
      description = "Makes the annotated file's name identical to the original source file's name.")
  private boolean useSameFileName = false;

  private final Configuration config;
  private final LogManager logger;
  private final CFA cfa;
  private final ShutdownNotifier shutdownNotifier;

  public WitnessToACSLAlgorithm(
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

    Set<Path> files = new HashSet<>();
    Set<ExpressionTreeLocationInvariant> invariants;
    try {
      WitnessInvariantsExtractor invariantsExtractor =
          new WitnessInvariantsExtractor(config, logger, cfa, shutdownNotifier, witness);
      invariants = invariantsExtractor.extractInvariantsFromReachedSet();
    } catch (InvalidConfigurationException pE) {
      throw new CPAException(
          "Invalid Configuration while analyzing witness:\n" + pE.getMessage(), pE);
    }

    for (ExpressionTreeLocationInvariant c : invariants) {
      Path file = c.getLocation().getFunction().getFileLocation().getFileName();
      if (Files.isRegularFile(file)) {
        files.add(file);
      }
    }

    for (Path file : files) {
      // Sort invariants by location
      Multimap<Integer, ExpressionTreeLocationInvariant> locationsToInvariants =
          LinkedHashMultimap.create();
      for (ExpressionTreeLocationInvariant inv : invariants) {
        CFANode node = inv.getLocation();
        if (!node.getFunction().getFileLocation().getFileName().equals(file)) {
          // Current invariant belongs to another program
          continue;
        }
        Set<Integer> effectiveLocations = getEffectiveLocations(inv);
        if (!effectiveLocations.isEmpty()) {
          for (Integer location : effectiveLocations) {
            locationsToInvariants.put(location, inv);
          }
        } else {
          logger.logf(
              Level.INFO,
              "Could not determine a location for invariant %s, skipping.",
              inv.asExpressionTree());
        }
      }

      String fileContent;
      try {
        fileContent = Files.readString(file);
      } catch (IOException pE) {
        logger.logfUserException(Level.WARNING, pE, "Could not read file %s", file);
        continue;
      }

      PriorityQueue<Integer> sortedLocations = new PriorityQueue<>(locationsToInvariants.keySet());
      Integer currentLocation = sortedLocations.poll();

      List<String> output = new ArrayList<>();

      List<String> splitContent = Splitter.onPattern("\\r?\\n").splitToList(fileContent);
      for (int i = 0; i < splitContent.size(); i++) {
        assert currentLocation == null || currentLocation >= i;
        List<ExpressionTree<Object>> collectedLoopInvariants = new ArrayList<>();
        while (currentLocation != null && currentLocation == i) {
          String lineBefore = splitContent.get(i - 1).strip();
          String lineAfter = splitContent.get(i).strip();
          if ((lineBefore.endsWith(")") || lineBefore.endsWith("else"))
              && lineAfter.startsWith("{")) {
            // Add annotation inside the following block, not between braces
            sortedLocations.offer(currentLocation + 1);
            for (ExpressionTreeLocationInvariant inv : locationsToInvariants.get(currentLocation)) {
              locationsToInvariants.put(currentLocation + 1, inv);
            }
            locationsToInvariants.removeAll(currentLocation);
          } else {
            for (ExpressionTreeLocationInvariant inv : locationsToInvariants.get(currentLocation)) {
              if (isAtLoopStart(inv)) {
                collectedLoopInvariants.add(inv.asExpressionTree());
                continue;
              }
              String annotation = makeACSLAnnotation(inv.asExpressionTree(), isAtLoopStart(inv));
              String indentation = i > 0 ? getIndentation(splitContent.get(i - 1)) : "";
              output.add(indentation + annotation);
            }
          }
          currentLocation = sortedLocations.poll();
        }
        if (!collectedLoopInvariants.isEmpty() && !splitContent.get(i).isBlank()) {
          ExpressionTree<Object> conjunctedInvariants = And.of(collectedLoopInvariants);
          String annotation = makeACSLAnnotation(conjunctedInvariants, true);
          String indentation = i > 0 ? getIndentation(splitContent.get(i - 1)) : "";
          output.add(indentation + annotation);
          collectedLoopInvariants.clear();
        }
        output.add(splitContent.get(i));
      }

      long count = 0;
      while (!sortedLocations.isEmpty()) {
        currentLocation = sortedLocations.poll();
        count++;
      }
      if (count != 0) {
        logger.logf(
            Level.WARNING,
            "Not all invariants where used for annotations, we still have %d invariants left!",
            count);
      }
      try {
        writeToFile(file, output);
      } catch (IOException pE) {
        logger.logfUserException(
            Level.WARNING, pE, "Could not write annotations for file %s", file);
        continue;
      }
    }
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  private void writeToFile(Path pathToOriginalFile, List<String> newContent) throws IOException {
    String newFileName = makeNameForAnnotatedFile(pathToOriginalFile.getFileName().toString());
    Path outFile = outDir.resolve(newFileName);
    try (Writer writer = IO.openOutputFile(outFile, Charset.defaultCharset())) {
      writer.write(String.join("\n", newContent) + "\n");
    }
  }

  /**
   * Creates a name for the file containing the annotated code based on the original's name. The new
   * name has the prefix "annotated" and ends in a timestamp.
   *
   * @param oldFileName The name of the original source file.
   * @return A name based on the given file name.
   */
  private String makeNameForAnnotatedFile(String oldFileName) {
    if (useSameFileName) {
      return oldFileName;
    }
    int indexOfFirstPeriod = oldFileName.indexOf('.');
    String nameWithoutExtension = oldFileName;
    String extension = "";
    if (indexOfFirstPeriod != -1) {
      nameWithoutExtension = oldFileName.substring(0, indexOfFirstPeriod);
      extension = oldFileName.substring(indexOfFirstPeriod);
    }
    String timestamp =
        LocalDateTime.now(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss"));
    return "annotated_" + nameWithoutExtension + timestamp + extension;
  }

  /**
   * Returns a String containing only space chars the same length as the given parameters
   * indentation. Note that length refers to the length of the printed whitespace and not
   * necessarily the value returned by <code>String.length()</code>.
   *
   * @param correctlyIndented A String of which the indentation should be matched.
   * @return the longest whitespace-only prefix of the given String.
   */
  private String getIndentation(String correctlyIndented) {
    String indentation = null;
    for (int i = 0; i < correctlyIndented.length(); i++) {
      if (!Character.isSpaceChar(correctlyIndented.charAt(i))) {
        indentation = correctlyIndented.substring(0, i);
        break;
      }
    }
    return indentation == null ? correctlyIndented : indentation;
  }

  private String makeACSLAnnotation(ExpressionTree<Object> inv, boolean asLoopInvariant) {
    if (!makeDirectAssertions) {
      if (asLoopInvariant) {
        return "/*@ loop invariant " + inv + "; */";
      } else {
        return "/*@ assert " + inv + "; */";
      }
    } else {
      return "if (!(" + inv + ")) reach_error();";
    }
  }

  private boolean isAtLoopStart(ExpressionTreeLocationInvariant inv) {
    Optional<LoopStructure> loopStructure = cfa.getLoopStructure();
    if (loopStructure.isPresent()) {
      CFANode node = inv.getLocation();
      for (Loop loop : loopStructure.orElseThrow().getLoopsForFunction(node.getFunctionName())) {
        for (CFAEdge edge : loop.getIncomingEdges()) {
          if (edge.getPredecessor().equals(node)) {
            String description = edge.getDescription();
            if (description.equals("while")
                || description.equals("for")
                || description.equals("do")
                || (edge.getPredecessor().getNumEnteringEdges() == 1
                    && edge.getPredecessor().getEnteringEdge(0).getDescription().equals("for"))) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }

  private Set<Integer> getEffectiveLocations(ExpressionTreeLocationInvariant inv) {
    CFANode node = inv.getLocation();
    ImmutableSet.Builder<Integer> locations = ImmutableSet.builder();

    if (node instanceof FunctionEntryNode || node instanceof FunctionExitNode) {
      // Cannot map to a position
      return locations.build();
    }

    for (CFAEdge edge : CFAUtils.leavingEdges(node)) {
      if (!edge.getFileLocation().equals(FileLocation.DUMMY)
          && !edge.getDescription().contains("CPAchecker_TMP")
          && !(edge instanceof AssumeEdge)) {
        locations.add(edge.getFileLocation().getStartingLineNumber() - 1);
      }
    }

    for (CFAEdge edge : CFAUtils.enteringEdges(node)) {
      if (!edge.getFileLocation().equals(FileLocation.DUMMY)
          && !edge.getDescription().contains("CPAchecker_TMP")
          && !(edge instanceof AssumeEdge)) {
        locations.add(edge.getFileLocation().getEndingLineNumber());
      }
    }

    return locations.build();
  }
}
