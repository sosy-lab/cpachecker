// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.io.MoreFiles;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.Appender;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CPAcheckerResult;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.counterexamples.CEXExportOptions;
import org.sosy_lab.cpachecker.cpa.arg.counterexamples.CEXExporter;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.ExtendedWitnessExporter;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.Witness;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.WitnessExporter;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.WitnessToOutputFormatsUtils;
import org.sosy_lab.cpachecker.cpa.automaton.ARGToAutomatonConverter;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.partitioning.PartitioningCPA.PartitionState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.BiPredicates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.cwriter.ARGToCTranslator;

@Options(prefix = "cpa.arg")
public class ARGStatistics implements Statistics {

  @Option(
      secure = true,
      name = "dumpAfterIteration",
      description =
          "Dump all ARG related statistics files after each iteration of the CPA algorithm? (for"
              + " debugging and demonstration)")
  private boolean dumpArgInEachCpaIteration = false;

  @Option(secure = true, name = "export", description = "export final ARG as .dot file")
  private boolean exportARG = true;

  @Option(secure = true, name = "file", description = "export final ARG as .dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path argFile = Path.of("ARG.dot");

  @Option(
      secure = true,
      name = "pixelGraphicFile",
      description =
          "Export final ARG as pixel graphic to the given file name. The suffix is added "
              + " corresponding"
              + " to the value of option pixelgraphic.export.format"
              + "If set to 'null', no pixel graphic is exported.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path pixelGraphicFile = Path.of("ARG");

  @Option(secure = true, name = "proofWitness", description = "export a proof as .graphml file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path proofWitness = null;

  @Option(
      secure = true,
      name = "proofWitness.dot",
      description = "export a proof as dot/graphviz file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path proofWitnessDot = null;

  @Option(
      secure = true,
      name = "compressWitness",
      description = "compress the produced correctness-witness automata using GZIP compression.")
  private boolean compressWitness = true;

  @Option(
      secure = true,
      name = "simplifiedARG.file",
      description =
          "export final ARG as .dot file, showing only loop heads and function entries/exits")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path simplifiedArgFile = Path.of("ARGSimplified.dot");

  @Option(
      secure = true,
      name = "refinements.file",
      description = "export simplified ARG that shows all refinements to .dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path refinementGraphFile = Path.of("ARGRefinements.dot");

  @Option(secure = true, name = "translateToC", description = "translate final ARG into C program")
  private boolean translateARG = false;

  @Option(
      secure = true,
      name = "CTranslation.file",
      description = "translate final ARG into this C file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path argCFile = Path.of("ARG.c");

  @Option(
      secure = true,
      name = "automaton.export",
      description = "translate final ARG into an automaton")
  private boolean exportAutomaton = false;

  @Option(
      secure = true,
      name = "automaton.exportSpcZipFile",
      description = "translate final ARG into an automaton, depends on 'automaton.export=true'")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path automatonSpcZipFile = Path.of("ARG_parts.zip");

  @Option(
      secure = true,
      name = "automaton.exportSpcFile",
      description = "translate final ARG into an automaton, depends on 'automaton.export=true'")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate automatonSpcFile = PathTemplate.ofFormatString("ARG_parts/ARG.%06d.spc");

  @Option(
      secure = true,
      name = "automaton.exportDotFile",
      description = "translate final ARG into an automaton, depends on 'automaton.export=true'")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate automatonSpcDotFile =
      PathTemplate.ofFormatString("ARG_parts/ARG.%06d.spc.dot");

  @Option(
      secure = true,
      name = "automaton.exportCompressed",
      description = "export as zip-files, depends on 'automaton.export=true'")
  private boolean exportAutomatonCompressed = true;

  @Option(
      secure = true,
      name = "automaton.exportZipped",
      description = "export all automata into one zip-file, depends on 'automaton.export=true'")
  private boolean exportAutomatonZipped = true;

  protected final ConfigurableProgramAnalysis cpa;

  private final CEXExportOptions counterexampleOptions;
  private Writer refinementGraphUnderlyingWriter = null;
  private ARGToDotWriter refinementGraphWriter = null;
  private final @Nullable CEXExporter cexExporter;
  private final WitnessExporter argWitnessExporter;
  private final AssumptionToEdgeAllocator assumptionToEdgeAllocator;
  private final ARGToCTranslator argToCExporter;
  private final ARGToPixelsWriter argToBitmapExporter;
  private ARGToAutomatonConverter argToAutomatonSplitter;
  protected final LogManager logger;

  public ARGStatistics(
      Configuration config,
      LogManager pLogger,
      ConfigurableProgramAnalysis pCpa,
      Specification pSpecification,
      CFA cfa)
      throws InvalidConfigurationException {
    config.inject(this, ARGStatistics.class); // needed for sub-classes

    counterexampleOptions = new CEXExportOptions(config);
    argToBitmapExporter = new ARGToPixelsWriter(config);
    logger = pLogger;
    cpa = pCpa;
    assumptionToEdgeAllocator =
        AssumptionToEdgeAllocator.create(config, logger, cfa.getMachineModel());

    if (argFile == null
        && simplifiedArgFile == null
        && refinementGraphFile == null
        && proofWitness == null
        && proofWitnessDot == null
        && pixelGraphicFile == null
        && (!exportAutomaton || (automatonSpcFile == null && automatonSpcDotFile == null))) {
      exportARG = false;
    }

    argWitnessExporter = new WitnessExporter(config, logger, pSpecification, cfa);

    if (counterexampleOptions.disabledCompletely()) {
      cexExporter = null;
    } else {
      ExtendedWitnessExporter extendedWitnessExporter =
          new ExtendedWitnessExporter(config, logger, pSpecification, cfa);
      cexExporter =
          new CEXExporter(
              config,
              counterexampleOptions,
              logger,
              cfa,
              cpa,
              argWitnessExporter,
              extendedWitnessExporter);
    }

    argToCExporter = new ARGToCTranslator(logger, config, cfa.getMachineModel());
    argToAutomatonSplitter = new ARGToAutomatonConverter(config, cfa.getMachineModel(), logger);

    if (argCFile == null) {
      translateARG = false;
    }
  }

  ARGToDotWriter getRefinementGraphWriter() {
    if (!exportARG || refinementGraphFile == null) {
      return null;
    }

    if (refinementGraphWriter == null) {
      // Open output file for refinement graph,
      // we continuously write into this file during analysis.
      // We do this lazily so that the file is written only if there are refinements.
      try {
        refinementGraphUnderlyingWriter =
            IO.openOutputFile(refinementGraphFile, Charset.defaultCharset());
        refinementGraphWriter = new ARGToDotWriter(refinementGraphUnderlyingWriter);
      } catch (IOException e) {
        if (refinementGraphUnderlyingWriter != null) {
          try {
            refinementGraphUnderlyingWriter.close();
          } catch (IOException innerException) {
            e.addSuppressed(innerException);
          }
        }

        logger.logUserException(Level.WARNING, e, "Could not write refinement graph to file");

        refinementGraphFile = null; // ensure we won't try again
        refinementGraphUnderlyingWriter = null;
        refinementGraphWriter = null;
      }
    }

    // either both are null or none
    assert (refinementGraphUnderlyingWriter == null) == (refinementGraphWriter == null);
    return refinementGraphWriter;
  }

  @Override
  public String getName() {
    return null; // return null because we do not print statistics
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {}

  @Override
  public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {
    if ((counterexampleOptions.disabledCompletely()
            || counterexampleOptions.dumpErrorPathImmediately())
        && !exportARG
        && !translateARG) {
      return;
    }

    Map<ARGState, CounterexampleInfo> counterexamples = getAllCounterexamples(pReached);

    if (!counterexampleOptions.disabledCompletely()
        && !counterexampleOptions.dumpErrorPathImmediately()
        && pResult == Result.FALSE) {
      for (Map.Entry<ARGState, CounterexampleInfo> cex : counterexamples.entrySet()) {
        cexExporter.exportCounterexample(cex.getKey(), cex.getValue());
      }
    }

    if (exportARG) {
      exportARG(pReached, counterexamples, pResult);
    }

    if (translateARG) {
      try {
        final String argAsC =
            argToCExporter.translateARG((ARGState) pReached.getFirstState(), true);
        try (Writer writer = IO.openOutputFile(argCFile, Charset.defaultCharset())) {
          writer.write(argAsC);
        }
      } catch (IOException | CPAException e) {
        logger.logUserException(Level.WARNING, e, "Could not write C translation of ARG to file");
      }
    }
  }

  private Path adjustPathNameForPartitioning(ARGState rootState, Path pPath) {
    if (pPath == null) {
      return null;
    }

    PartitionState partyState = AbstractStates.extractStateByType(rootState, PartitionState.class);
    if (partyState == null) {
      return pPath;
    }

    final String partitionKey = partyState.getStateSpacePartition().getPartitionKey().toString();

    String path = pPath.toString();
    int sepIx = path.lastIndexOf(".");
    String prefix = path.substring(0, sepIx);
    String extension = path.substring(sepIx);
    return Path.of(prefix + "-" + partitionKey + extension);
  }

  private void exportARG(
      UnmodifiableReachedSet pReached,
      final Map<ARGState, CounterexampleInfo> counterexamples,
      Result pResult) {
    final Set<Pair<ARGState, ARGState>> allTargetPathEdges = new HashSet<>();
    for (CounterexampleInfo cex : counterexamples.values()) {
      allTargetPathEdges.addAll(cex.getTargetPath().getStatePairs());
    }

    // The state space might be partitioned ...
    // ... so we would export a separate ARG for each partition ...
    boolean partitionedArg =
        pReached.isEmpty()
            || AbstractStates.extractStateByType(pReached.getFirstState(), PartitionState.class)
                != null;

    final Set<ARGState> rootStates =
        partitionedArg
            ? ARGUtils.getRootStates(pReached)
            : Collections.singleton(
                AbstractStates.extractStateByType(pReached.getFirstState(), ARGState.class));

    for (ARGState rootState : rootStates) {
      exportARG0(rootState, BiPredicates.pairIn(allTargetPathEdges), pResult);
    }
  }

  @SuppressWarnings("try")
  private void exportARG0(
      final ARGState rootState,
      final BiPredicate<ARGState, ARGState> isTargetPathEdge,
      Result pResult) {
    SetMultimap<ARGState, ARGState> relevantSuccessorRelation =
        ARGUtils.projectARG(rootState, ARGState::getChildren, ARGUtils::isRelevantState);
    Function<ARGState, Collection<ARGState>> relevantSuccessorFunction =
        Functions.forMap(relevantSuccessorRelation.asMap(), ImmutableSet.of());

    if (EnumSet.of(Result.TRUE, Result.UNKNOWN).contains(pResult)) {
      try {
        final Witness witness =
            argWitnessExporter.generateProofWitness(
                rootState,
                Predicates.alwaysTrue(),
                BiPredicates.alwaysTrue(),
                argWitnessExporter.getProofInvariantProvider());

        if (proofWitness != null) {
          Path witnessFile = adjustPathNameForPartitioning(rootState, proofWitness);
          WitnessToOutputFormatsUtils.writeWitness(
              witnessFile,
              compressWitness,
              pAppendable -> WitnessToOutputFormatsUtils.writeToGraphMl(witness, pAppendable),
              logger);
        }

        if (proofWitnessDot != null) {
          Path witnessFile = adjustPathNameForPartitioning(rootState, proofWitnessDot);
          WitnessToOutputFormatsUtils.writeWitness(
              witnessFile,
              compressWitness,
              pAppendable -> WitnessToOutputFormatsUtils.writeToDot(witness, pAppendable),
              logger);
        }
      } catch (InterruptedException e) {
        logger.logUserException(Level.WARNING, e, "Could not export witness due to interruption");
      }
    }

    if (argFile != null) {
      try (Writer w =
          IO.openOutputFile(
              adjustPathNameForPartitioning(rootState, argFile), Charset.defaultCharset())) {
        ARGToDotWriter.write(
            w, rootState, ARGState::getChildren, Predicates.alwaysTrue(), isTargetPathEdge);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write ARG to file");
      }
    }

    if (pixelGraphicFile != null) {
      try {
        Path adjustedBitmapFileName = adjustPathNameForPartitioning(rootState, pixelGraphicFile);
        argToBitmapExporter.write(rootState, adjustedBitmapFileName);
      } catch (IOException | InvalidConfigurationException e) {
        logger.logUserException(Level.WARNING, e, "Could not write ARG bitmap to file");
      }
    }

    if (simplifiedArgFile != null) {
      try (Writer w =
          IO.openOutputFile(
              adjustPathNameForPartitioning(rootState, simplifiedArgFile),
              Charset.defaultCharset())) {
        ARGToDotWriter.write(
            w,
            rootState,
            relevantSuccessorFunction,
            Predicates.alwaysTrue(),
            BiPredicates.alwaysFalse());
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write ARG to file");
      }
    }

    assert (refinementGraphUnderlyingWriter == null) == (refinementGraphWriter == null);
    if (refinementGraphUnderlyingWriter != null) {
      try (Writer w = refinementGraphUnderlyingWriter) { // for auto-closing
        // TODO: Support for partitioned state spaces
        refinementGraphWriter.writeSubgraph(
            rootState,
            relevantSuccessorFunction,
            Predicates.alwaysTrue(),
            BiPredicates.alwaysFalse());
        refinementGraphWriter.finish();

      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write refinement graph to file");
      }
    }

    if (exportAutomaton && (automatonSpcFile != null || automatonSpcDotFile != null)) {
      try {
        if (exportAutomatonZipped && automatonSpcZipFile != null) {
          Files.deleteIfExists(automatonSpcZipFile);
        }
        final int baseId = -1; // id for the exported 'complete' automaton
        writeAutomaton(baseId, argToAutomatonSplitter.getAutomaton(rootState, true));
      } catch (IOException io) {
        logger.logUserException(Level.WARNING, io, "Could not write ARG to automata to file");
      }
      try {
        int counterId = 0; // id for each exported 'partial' automata, distinct from 'baseId'
        for (Automaton automaton : argToAutomatonSplitter.getAutomata(rootState)) {
          counterId++;
          writeAutomaton(counterId, automaton);
        }
        logger.log(Level.INFO, "Number of exported automata after splitting:", counterId);
      } catch (IOException io) {
        logger.logUserException(Level.WARNING, io, "Could not write ARG to automata to file");
      }
    }
  }

  private void writeAutomaton(int counterId, Automaton automaton) throws IOException {
    if (automatonSpcFile != null) {
      writeFile(automatonSpcFile.getPath(counterId), automaton);
    }
    if (automatonSpcDotFile != null) {
      Appender app = automaton::writeDotFile;
      writeFile(automatonSpcDotFile.getPath(counterId), app);
    }
  }

  private void writeFile(Path path, Object content) throws IOException {
    if (exportAutomatonZipped && automatonSpcZipFile != null) {
      MoreFiles.createParentDirectories(automatonSpcZipFile);
      try (FileSystem fs =
          FileSystems.newFileSystem(
              URI.create("jar:" + automatonSpcZipFile.toUri()),
              // create zip-file if not existing, else append
              ImmutableMap.of("create", "true"))) {
        Path nf = fs.getPath(path.getFileName().toString());
        IO.writeFile(nf, Charset.defaultCharset(), content);
      }
    } else if (exportAutomatonCompressed) {
      path = path.resolveSibling(path.getFileName() + ".gz");
      IO.writeGZIPFile(path, Charset.defaultCharset(), content);
    } else {
      IO.writeFile(path, Charset.defaultCharset(), content);
    }
  }

  public Map<ARGState, CounterexampleInfo> getAllCounterexamples(
      final UnmodifiableReachedSet pReached) {
    ImmutableMap.Builder<ARGState, CounterexampleInfo> counterexamples = ImmutableMap.builder();

    for (AbstractState targetState : from(pReached).filter(AbstractStates::isTargetState)) {
      ARGState s = (ARGState) targetState;
      CounterexampleInfo cex =
          ARGUtils.tryGetOrCreateCounterexampleInformation(s, cpa, assumptionToEdgeAllocator)
              .orElse(null);
      if (cex != null) {
        counterexamples.put(s, cex);
      }
    }

    Map<ARGState, CounterexampleInfo> allCounterexamples = counterexamples.buildOrThrow();
    final Map<ARGState, CounterexampleInfo> preciseCounterexamples =
        Maps.filterValues(allCounterexamples, cex -> cex.isPreciseCounterExample());
    return preciseCounterexamples.isEmpty() ? allCounterexamples : preciseCounterexamples;
  }

  public void exportCounterexampleOnTheFly(
      ARGState pTargetState, CounterexampleInfo pCounterexampleInfo) throws InterruptedException {
    if (!counterexampleOptions.disabledCompletely()
        && counterexampleOptions.dumpErrorPathImmediately()) {
      cexExporter.exportCounterexampleIfRelevant(pTargetState, pCounterexampleInfo);
    }
  }

  public void exportCounterexampleOnTheFly(UnmodifiableReachedSet pReachedSet)
      throws InterruptedException {
    if (counterexampleOptions.dumpAllFoundErrorPaths()) {
      for (Map.Entry<ARGState, CounterexampleInfo> cex :
          getAllCounterexamples(pReachedSet).entrySet()) {
        exportCounterexampleOnTheFly(cex.getKey(), cex.getValue());
      }
    }
  }

  public void printIterationStatistics(UnmodifiableReachedSet pReached) {
    if (dumpArgInEachCpaIteration) {
      exportARG(pReached, getAllCounterexamples(pReached), CPAcheckerResult.Result.UNKNOWN);
    }
  }
}
