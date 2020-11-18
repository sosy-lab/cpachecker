// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
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

@Options(prefix = "wacsl")
public class WitnessToACSLAlgorithm implements Algorithm {

  @Option(
      secure = true,
      required = true,
      description = "The witness from which ACSL annotations should be generated.")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path witness;

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

    Set<String> files = new HashSet<>();
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
      // TODO: Could be in a dummy function
      files.add(c.getLocation().getFunction().getFileLocation().getFileName());
    }

    for (String file : files) {
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

      String fileContent = "";
      try {
        fileContent = Files.asCharSource(new File(file), Charsets.UTF_8).read();
      } catch (IOException pE) {
        logger.logfUserException(Level.SEVERE, pE, "Could not read file %s", file);
      }

      List<Integer> sortedLocations = new ArrayList<>(locationsToInvariants.keySet());
      Collections.sort(sortedLocations);
      Iterator<Integer> invariantLocationsIterator = sortedLocations.iterator();
      Integer currentLocation;
      if (invariantLocationsIterator.hasNext()) {
        currentLocation = invariantLocationsIterator.next();
      } else {
        currentLocation = null;
      }

      List<String> output = new ArrayList<>();

      List<String> splitContent = Splitter.onPattern("\\r?\\n").splitToList(fileContent);
      for (int i = 0; i < splitContent.size(); i++) {
        assert currentLocation == null || currentLocation >= i;
        while (currentLocation != null && currentLocation == i) {
          for (ExpressionTreeLocationInvariant inv : locationsToInvariants.get(currentLocation)) {
            String annotation = makeACSLAnnotation(inv);
            String indentation = i > 0 ? getIndentation(splitContent.get(i - 1)) : "";
            output.add(indentation.concat(annotation));
          }
          if (invariantLocationsIterator.hasNext()) {
            currentLocation = invariantLocationsIterator.next();
          } else {
            currentLocation = null;
          }
        }
        output.add(splitContent.get(i));
      }

      while (invariantLocationsIterator.hasNext()) {
        currentLocation = invariantLocationsIterator.next();
        for (ExpressionTreeLocationInvariant inv : locationsToInvariants.get(currentLocation)) {
          String annotation = makeACSLAnnotation(inv);
          output.add(annotation);
        }
      }

      try {
        writeToFile(file, output);
      } catch (IOException pE) {
        logger.logfUserException(Level.SEVERE, pE, "Could not write annotations for file %s", file);
      }
    }
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  private void writeToFile(String pathToOriginalFile, List<String> newContent) throws IOException {
    Path path = Path.of(pathToOriginalFile);
    Path directory = path.getParent();
    assert directory != null;
    Path oldFileName = path.getFileName();
    String newFileName = makeNameForAnnotatedFile(oldFileName.toString());

    File outFile = new File(Path.of(directory.toString(), newFileName).toUri());
    assert outFile.createNewFile() : String.format("File %s already exists!", outFile);
    try (Writer out = Files.newWriter(outFile, StandardCharsets.UTF_8)) {
      for (String line : newContent) {
        out.append(line.concat("\n"));
      }
      out.flush();
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
    return "annotated_".concat(nameWithoutExtension).concat(timestamp).concat(extension);
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

  private String makeACSLAnnotation(ExpressionTreeLocationInvariant inv) {
    return "/*@ assert " + inv.asExpressionTree() + "; */";
  }

  private Set<Integer> getEffectiveLocations(ExpressionTreeLocationInvariant inv) {
    CFANode node = inv.getLocation();
    Set<Integer> locations = new HashSet<>(node.getNumLeavingEdges());

    if (node instanceof FunctionEntryNode || node instanceof FunctionExitNode) {
      // Cannot map to a position
      return locations;
    }

    for (int i = 0; i < node.getNumLeavingEdges(); i++) {
      CFAEdge edge = node.getLeavingEdge(i);
      if (!edge.getFileLocation().equals(FileLocation.DUMMY)
          && !edge.getDescription().contains("CPAchecker_TMP")
          && !(edge instanceof AssumeEdge)) {
        locations.add(edge.getFileLocation().getStartingLineNumber() - 1);
      }
    }

    for (int i = 0; i < node.getNumEnteringEdges(); i++) {
      CFAEdge edge = node.getEnteringEdge(i);
      if (!edge.getFileLocation().equals(FileLocation.DUMMY)
          && !edge.getDescription().contains("CPAchecker_TMP")
          && !(edge instanceof AssumeEdge)) {
        locations.add(edge.getFileLocation().getEndingLineNumber());
      }
    }

    return locations;
  }
}
