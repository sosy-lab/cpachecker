// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.core.algorithm.equivalence.ARGLeafStrategy;
import org.sosy_lab.cpachecker.core.algorithm.equivalence.EquivalenceCheck;
import org.sosy_lab.cpachecker.core.algorithm.equivalence.EquivalenceCheck.EquivalenceData;
import org.sosy_lab.cpachecker.core.algorithm.equivalence.EquivalenceRunner;
import org.sosy_lab.cpachecker.core.algorithm.equivalence.EquivalenceRunner.SafeAndUnsafeConstraints;
import org.sosy_lab.cpachecker.core.algorithm.equivalence.ExistsEquivalenceCheck;
import org.sosy_lab.cpachecker.core.algorithm.equivalence.JoinEquivalenceCheck;
import org.sosy_lab.cpachecker.core.algorithm.equivalence.LeafStrategy;
import org.sosy_lab.cpachecker.core.algorithm.equivalence.MixEquivalenceStrategy;
import org.sosy_lab.cpachecker.core.algorithm.equivalence.SymbolicExecutionLeafStrategy;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "leafExport")
public class ExportARGLeafs implements Algorithm {

  enum LeafStrategies {
    SYMBOLIC_EXECUTION,
    ARG,
    MIX
  }

  enum EquivalenceStrategies {
    EXISTS,
    JOIN
  }

  @Option(description = "Which strategy to use for leaf export")
  private LeafStrategies leafStrategy = LeafStrategies.MIX;

  @Option(description = "Which strategy to use for equivalence checking")
  private EquivalenceStrategies equivalenceStrategy = EquivalenceStrategies.EXISTS;

  @Option(description = "All equivalent programs")
  @FileOption(Type.OPTIONAL_INPUT_FILE)
  private List<Path> mutants = ImmutableList.of();

  @Option(description = "Which abstraction to use")
  @FileOption(Type.OPTIONAL_INPUT_FILE)
  private Path configuration = Path.of("config/svcomp25.properties");

  @Option(description = "Where to store output csv.")
  @FileOption(Type.OUTPUT_FILE)
  private Path csvOut = Path.of("leafs.csv");

  @Option(description = "Where to store touched lines.")
  @FileOption(Type.OUTPUT_DIRECTORY)
  private Path touchedLinesOut = Path.of("touchedLines");

  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final EquivalenceRunner equivalenceRunner;
  private final Solver solver;
  private final CFA inputProgram;

  public ExportARGLeafs(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Specification pSpecification,
      CFA pCFA)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    try {
      config = Configuration.builder().loadFromFile(configuration).build();
    } catch (IOException e) {
      throw new InvalidConfigurationException(
          "Could not load configuration file: " + configuration, e);
    }
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    equivalenceRunner = new EquivalenceRunner(pSpecification, config, logger, shutdownNotifier);
    solver = Solver.create(config, logger, shutdownNotifier);
    inputProgram = pCFA;
  }

  private LeafStrategy getLeafStrategy() {
    return switch (leafStrategy) {
      case SYMBOLIC_EXECUTION ->
          new SymbolicExecutionLeafStrategy(config, logger, shutdownNotifier, solver);
      case ARG -> new ARGLeafStrategy(config, logger, shutdownNotifier, solver);
      case MIX -> new MixEquivalenceStrategy(config, logger, shutdownNotifier, solver);
      default -> throw new AssertionError("Unknown strategy: " + leafStrategy);
    };
  }

  private EquivalenceCheck getEquivalenceCheck() {
    return switch (equivalenceStrategy) {
      case EXISTS -> new ExistsEquivalenceCheck(solver);
      case JOIN -> new JoinEquivalenceCheck(solver);
      default -> throw new AssertionError("Unknown strategy: " + equivalenceStrategy);
    };
  }

  private CFA createCfa(Path program)
      throws IOException, ParserException, InterruptedException, InvalidConfigurationException {
    final CFACreator cfaCreator = new CFACreator(config, logger, shutdownNotifier);
    return cfaCreator.parseFileAndCreateCFA(ImmutableList.of(program.toAbsolutePath().toString()));
  }

  record EquivalenceResult(Path original, Path mutant, EquivalenceData equivalent) {}

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    logger.logf(
        Level.INFO,
        "Running leaf strategy %s with the %s equivalence check",
        leafStrategy,
        equivalenceStrategy);
    pReachedSet.clear();
    try {
      ImmutableMap.Builder<Path, SafeAndUnsafeConstraints> data = ImmutableMap.builder();
      data.put(
          inputProgram.getFileNames().get(0),
          equivalenceRunner.runStrategy(inputProgram, getLeafStrategy()));
      for (Path program : mutants) {
        data.put(program, equivalenceRunner.runStrategy(createCfa(program), getLeafStrategy()));
      }
      ImmutableMap<Path, SafeAndUnsafeConstraints> constraints = data.buildOrThrow();
      if (constraints.isEmpty()) {
        logger.log(Level.WARNING, "No result was computed");
        return AlgorithmStatus.NO_PROPERTY_CHECKED;
      }
      AlgorithmStatus status =
          Objects.requireNonNull(constraints.get(inputProgram.getFileNames().get(0))).status();
      ImmutableList.Builder<EquivalenceResult> safeResults = ImmutableList.builder();
      ImmutableList.Builder<EquivalenceResult> unsafeResults = ImmutableList.builder();
      int index = 0;
      Files.createDirectories(touchedLinesOut);
      for (Entry<Path, SafeAndUnsafeConstraints> firstConstraint : constraints.entrySet()) {
        index++;
        status = status.update(firstConstraint.getValue().status());
        Files.writeString(
            touchedLinesOut.resolve(
                "export-" + index + "_" + firstConstraint.getKey().getFileName()),
            Joiner.on("\n").join(firstConstraint.getValue().touchedLines()),
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE);
        for (Entry<Path, SafeAndUnsafeConstraints> secondConstraint : constraints.entrySet()) {
          if (secondConstraint.getKey().equals(firstConstraint.getKey())) {
            continue;
          }
          safeResults.add(
              new EquivalenceResult(
                  firstConstraint.getKey(),
                  secondConstraint.getKey(),
                  getEquivalenceCheck()
                      .isEquivalent(
                          secondConstraint.getValue().safe(), firstConstraint.getValue().safe())));
          unsafeResults.add(
              new EquivalenceResult(
                  firstConstraint.getKey(),
                  secondConstraint.getKey(),
                  getEquivalenceCheck()
                      .isEquivalent(
                          secondConstraint.getValue().unsafe(),
                          firstConstraint.getValue().unsafe())));
        }
      }

      if (!status.equals(AlgorithmStatus.SOUND_AND_PRECISE)) {
        logger.logf(Level.WARNING, "Analysis was not sound and precise, but %s", status);
      }

      StringBuilder csvBuilder = new StringBuilder();
      csvBuilder
          .append(String.format("Original\tMutant\t%s\tSafe", EquivalenceData.getCsvHeader()))
          .append('\n');
      for (EquivalenceResult result : safeResults.build()) {
        csvBuilder
            .append(
                String.format(
                    "%s\t%s\t%s\ttrue",
                    result.original(), result.mutant(), result.equivalent().toCsvString()))
            .append('\n');
      }
      for (EquivalenceResult result : unsafeResults.build()) {
        csvBuilder
            .append(
                String.format(
                    "%s\t%s\t%s\tfalse",
                    result.original(), result.mutant(), result.equivalent().toCsvString()))
            .append('\n');
      }
      Files.writeString(
          csvOut, csvBuilder.toString(), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
      logger.logf(Level.INFO, "Exported results to %s", csvOut);
      return status;
    } catch (ParserException e) {
      throw new CPAException("Parsing the program is not possible", e);
    } catch (IOException e) {
      throw new CPAException("Reading the program file is not possible", e);
    } catch (InvalidConfigurationException e) {
      throw new CPAException("Configuration not correct", e);
    } catch (SolverException e) {
      throw new CPAException("Solver crashed", e);
    }
  }
}
