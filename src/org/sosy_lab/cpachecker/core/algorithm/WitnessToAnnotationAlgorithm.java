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
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
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
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonGraphmlParser;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.WitnessInvariantsExtractor;
import org.sosy_lab.cpachecker.util.automaton.AutomatonGraphmlCommon;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.invariantwitness.InvariantWitness;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.entryimport.InvariantWitnessProvider;

@Options(prefix = "wacsl")
public class WitnessToAnnotationAlgorithm implements Algorithm {

  private enum AnnotationLanguage {
    ACSL,
    DIRECT_ASSERTIONS,
    VERCORS
  }

  private enum LocationMatching {
    LINES,
    OFFSET
  }

  @Option(
      secure = true,
      required = true,
      description = "The witness from which annotations should be generated.")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path witness;

  @Option(
      secure = true,
      description = "The directory where generated annotated programs should be stored.")
  @FileOption(FileOption.Type.OUTPUT_DIRECTORY)
  private Path outDir = Path.of("annotated");

  @Option(secure = true, description = "How invariants shall be matched to locations.")
  private LocationMatching locationMatching = LocationMatching.LINES;

  @Option(secure = true, description = "The format in which annotations shall be exported.")
  private AnnotationLanguage lang = AnnotationLanguage.ACSL;

  @Option(
      secure = true,
      description = "Makes the annotated file's name identical to the original source file's name.")
  private boolean useSameFileName = false;

  private final Configuration config;
  private final LogManager logger;
  private final CFA cfa;
  private final ShutdownNotifier shutdownNotifier;
  private final boolean isGraphmlWitness;

  public WitnessToAnnotationAlgorithm(
      Configuration pConfig, LogManager pLogger, ShutdownNotifier pShutdownNotifier, CFA pCfa)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    config = pConfig;
    logger = pLogger;
    cfa = pCfa;
    shutdownNotifier = pShutdownNotifier;
    isGraphmlWitness = AutomatonGraphmlParser.isGraphmlAutomatonFromConfiguration(witness);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    if (!isGraphmlWitness) {
      createAnnotationsFromInvariantWitness();
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    }

    // Collect invariants from the witness
    Set<ExpressionTreeLocationInvariant> invariants;
    try {
      WitnessInvariantsExtractor invariantsExtractor =
          new WitnessInvariantsExtractor(config, logger, cfa, shutdownNotifier, witness);
      invariants = invariantsExtractor.extractInvariantsFromReachedSet();
    } catch (InvalidConfigurationException pE) {
      throw new CPAException("Invalid configuration while analyzing witness", pE);
    }

    // Determine referenced program files
    Multimap<Path, ExpressionTreeLocationInvariant> programsToInvariants =
        LinkedHashMultimap.create();
    for (ExpressionTreeLocationInvariant invariant : invariants) {
      Path file = invariant.getLocation().getFunction().getFileLocation().getFileName();
      if (Files.isRegularFile(file)) {
        programsToInvariants.put(file, invariant);
      }
    }

    // Annotate programs
    for (Entry<Path, Collection<ExpressionTreeLocationInvariant>> entry :
        programsToInvariants.asMap().entrySet()) {
      annotateProgram(entry.getKey(), entry.getValue());
    }

    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  private void createAnnotationsFromInvariantWitness() throws CPAException, InterruptedException {
    Collection<InvariantWitness> invariantWitnesses;
    try {
      final Path tmpDir = Files.createTempDirectory("witnessStore");
      Files.copy(witness, tmpDir.resolve(witness.getFileName()));
      Configuration invariantWitnessConfig =
          Configuration.builder()
              .copyFrom(config)
              .setOption("invariantStore.import.storeDirectory", tmpDir.toString())
              .build();
      InvariantWitnessProvider witnessProvider =
          InvariantWitnessProvider.getNewFromDiskWitnessProvider(
              invariantWitnessConfig, cfa, logger, shutdownNotifier);
      invariantWitnesses = witnessProvider.getCurrentWitnesses();
    } catch (InvalidConfigurationException pE) {
      throw new CPAException("Invalid configuration while analyzing witness", pE);
    } catch (IOException pE) {
      throw new CPAException("Failed to access witness", pE);
    }

    // Determine referenced program files
    Multimap<Path, InvariantWitness> programsToInvariants = LinkedHashMultimap.create();
    for (InvariantWitness invariantWitness : invariantWitnesses) {
      Path file = invariantWitness.getLocation().getFileName();
      if (Files.isRegularFile(file)) {
        programsToInvariants.put(file, invariantWitness);
      } else {
        // Path in the witness might be different, so check whether one of the input files matches
        // by hash
        Optional<String> hash = invariantWitness.getFileHash();
        if (hash.isPresent()) {
          for (Path inputFile : cfa.getFileNames()) {
            try {
              String inputFileHash = AutomatonGraphmlCommon.computeHash(inputFile);
              if (inputFileHash.equals(hash.orElseThrow())) {
                programsToInvariants.put(inputFile, invariantWitness);
              }
            } catch (IOException pE) {
              continue;
            }
          }
        }
      }
    }

    // Annotate programs
    for (Entry<Path, Collection<InvariantWitness>> entry :
        programsToInvariants.asMap().entrySet()) {
      annotateProgramFromInvariantWitnesses(entry.getKey(), entry.getValue());
    }
  }

  private void annotateProgramFromInvariantWitnesses(
      Path file, Collection<InvariantWitness> invariantWitnesses) {
    // Read in program
    String fileContent;
    try {
      fileContent = Files.readString(file);
    } catch (IOException pE) {
      logger.logfUserException(Level.WARNING, pE, "Could not read file %s", file);
      return;
    }

    // Create and insert annotations at correct locations in the program
    String output = fileContent;
    PriorityQueue<InvariantWitness> sortedInvariantWitnesses =
        new PriorityQueue<>(
            invariantWitnesses.size(),
            Comparator.comparingInt(i -> -i.getLocation().getNodeOffset()));
    sortedInvariantWitnesses.addAll(invariantWitnesses);
    while (!sortedInvariantWitnesses.isEmpty()) {
      InvariantWitness currentWitness = sortedInvariantWitnesses.poll();
      int currentLocation = currentWitness.getLocation().getNodeOffset();
      String annotation =
          makeAnnotation(currentWitness.getFormula(), isAtLoopStart(currentWitness.getNode()));
      output =
          output.substring(0, currentLocation) + annotation + output.substring(currentLocation);
    }

    // Write out annotated program
    try {
      writeToFile(file, output);
    } catch (IOException pE) {
      logger.logfUserException(Level.WARNING, pE, "Could not write annotations for file %s", file);
    }
  }

  /**
   * Converts the given invariants into annotations and inserts them at fitting locations in the
   * program.
   *
   * @param file The program file.
   * @param invariants A set of invariants for the program.
   */
  private void annotateProgram(Path file, Collection<ExpressionTreeLocationInvariant> invariants) {
    // Read in program
    String fileContent;
    try {
      fileContent = Files.readString(file);
    } catch (IOException pE) {
      logger.logfUserException(Level.WARNING, pE, "Could not read file %s", file);
      return;
    }

    // Sort invariants by location
    Multimap<Integer, ExpressionTreeLocationInvariant> locationsToInvariants =
        LinkedHashMultimap.create();
    for (ExpressionTreeLocationInvariant inv : invariants) {
      assert inv.getLocation().getFunction().getFileLocation().getFileName().equals(file)
          : "Invariant belongs to another program";
      Set<Integer> effectiveLocations = getEffectiveLocations(inv);
      if (effectiveLocations.isEmpty()) {
        logger.logf(
            Level.INFO,
            "Could not determine a location for invariant %s, skipping.",
            inv.asExpressionTree());
      }
      for (Integer location : effectiveLocations) {
        locationsToInvariants.put(location, inv);
      }
    }

    // Create and insert annotations at correct locations in the program
    String output =
        switch (locationMatching) {
          case LINES -> createAndInsertAnnotationsAtLines(fileContent, locationsToInvariants);
          case OFFSET -> createAndInsertAnnotationsAtOffsets(fileContent, locationsToInvariants);
        };

    // Write out annotated program
    try {
      writeToFile(file, output);
    } catch (IOException pE) {
      logger.logfUserException(Level.WARNING, pE, "Could not write annotations for file %s", file);
    }
  }

  private String createAndInsertAnnotationsAtLines(
      String fileContent,
      Multimap<Integer, ExpressionTreeLocationInvariant> locationsToInvariants) {
    List<String> output = new ArrayList<>();
    PriorityQueue<Integer> sortedLocations = new PriorityQueue<>(locationsToInvariants.keySet());
    Integer currentLocation = sortedLocations.poll();
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
            if (isAtLoopStart(inv.getLocation())) {
              collectedLoopInvariants.add(inv.asExpressionTree());
              continue;
            }
            String annotation =
                makeAnnotation(inv.asExpressionTree(), isAtLoopStart(inv.getLocation()));
            String indentation = i > 0 ? getIndentation(splitContent.get(i - 1)) : "";
            output.add(indentation + annotation);
          }
        }
        currentLocation = sortedLocations.poll();
      }
      if (!collectedLoopInvariants.isEmpty() && !splitContent.get(i).isBlank()) {
        ExpressionTree<Object> conjunctedInvariants = And.of(collectedLoopInvariants);
        String annotation = makeAnnotation(conjunctedInvariants, true);
        String indentation = i > 0 ? getIndentation(splitContent.get(i - 1)) : "";
        output.add(indentation + annotation);
      }
      output.add(splitContent.get(i));
    }

    // Check if any invariants were skipped
    if (!sortedLocations.isEmpty()) {
      logger.logf(
          Level.WARNING,
          "Not all invariants where used for annotations, we still have %d invariants left!",
          sortedLocations.size());
    }

    return String.join("\n", output) + "\n";
  }

  private String createAndInsertAnnotationsAtOffsets(
      String fileContent,
      Multimap<Integer, ExpressionTreeLocationInvariant> locationsToInvariants) {
    String output = fileContent;
    PriorityQueue<Integer> sortedLocations =
        new PriorityQueue<>(locationsToInvariants.keySet().stream().map(x -> -x).toList());
    while (!sortedLocations.isEmpty()) {
      int currentLocation = -sortedLocations.poll();
      for (ExpressionTreeLocationInvariant inv : locationsToInvariants.get(currentLocation)) {
        String annotation =
            makeAnnotation(inv.asExpressionTree(), isAtLoopStart(inv.getLocation()));
        output =
            output.substring(0, currentLocation) + annotation + output.substring(currentLocation);
      }
    }
    return output;
  }

  private void writeToFile(Path pathToOriginalFile, String newContent) throws IOException {
    String newFileName = makeNameForAnnotatedFile(pathToOriginalFile.getFileName().toString());
    Path outFile = outDir.resolve(newFileName);
    try (Writer writer = IO.openOutputFile(outFile, Charset.defaultCharset())) {
      writer.write(newContent);
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

  private String makeAnnotation(ExpressionTree<Object> inv, boolean asLoopInvariant) {
    switch (lang) {
      case ACSL -> {
        if (asLoopInvariant) {
          return "/*@ loop invariant " + inv + "; */";
        } else {
          return "/*@ assert " + inv + "; */";
        }
      }
      case DIRECT_ASSERTIONS -> {
        return "if (!(" + inv + ")) reach_error();";
      }
      case VERCORS -> {
        if (asLoopInvariant) {
          return "/*@ loop_invariant " + inv + "; @*/";
        } else {
          return "/*@ assert " + inv + "; @*/";
        }
      }
      default -> throw new AssertionError("Unhandled assertion language: " + lang);
    }
  }

  private boolean isAtLoopStart(CFANode node) {
    Optional<LoopStructure> loopStructure = cfa.getLoopStructure();
    if (loopStructure.isPresent()) {
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
    return switch (lang) {
      case ACSL -> getEffectiveLocationsForACSL(inv);
      case DIRECT_ASSERTIONS -> getEffectiveLocationsForDirectAssertions(inv);
      case VERCORS -> getEffectiveLocationsForVerCors(inv);
    };
  }

  private Set<Integer> getEffectiveLocationsForACSL(ExpressionTreeLocationInvariant inv) {
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
        switch (locationMatching) {
          case LINES -> locations.add(edge.getFileLocation().getStartingLineNumber() - 1);
          case OFFSET -> locations.add(edge.getFileLocation().getNodeOffset());
          default -> throw new AssertionError(
              "Unhandled method for location matching: " + locationMatching);
        }
      }
    }

    for (CFAEdge edge : CFAUtils.enteringEdges(node)) {
      if (!edge.getFileLocation().equals(FileLocation.DUMMY)
          && !edge.getDescription().contains("CPAchecker_TMP")
          && !(edge instanceof AssumeEdge)) {
        switch (locationMatching) {
          case LINES -> locations.add(edge.getFileLocation().getEndingLineNumber());
          case OFFSET -> locations.add(
              edge.getFileLocation().getNodeOffset() + edge.getFileLocation().getNodeLength());
          default -> throw new AssertionError(
              "Unhandled method for location matching: " + locationMatching);
        }
      }
    }

    return locations.build();
  }

  private Set<Integer> getEffectiveLocationsForDirectAssertions(
      ExpressionTreeLocationInvariant inv) {
    // TODO
    return getEffectiveLocationsForACSL(inv);
  }

  private Set<Integer> getEffectiveLocationsForVerCors(ExpressionTreeLocationInvariant inv) {
    // TODO
    return getEffectiveLocationsForACSL(inv);
  }
}
