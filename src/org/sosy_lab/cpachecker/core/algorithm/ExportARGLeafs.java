// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
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
import org.sosy_lab.cpachecker.core.algorithm.equivalence.EquivalenceRunner;
import org.sosy_lab.cpachecker.core.algorithm.equivalence.EquivalenceRunner.SafeAndUnsafeConstraints;
import org.sosy_lab.cpachecker.core.algorithm.equivalence.ExistsEquivalenceCheck;
import org.sosy_lab.cpachecker.core.algorithm.equivalence.JoinEquivalenceCheck;
import org.sosy_lab.cpachecker.core.algorithm.equivalence.LeafStrategy;
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
    ARG
  }

  enum EquivalenceStrategies {
    EXISTS,
    JOIN
  }

  @Option(description = "Which strategy to use for leaf export")
  private LeafStrategies strategy = LeafStrategies.SYMBOLIC_EXECUTION;

  @Option(description = "Which strategy to use for equivalence checking")
  private EquivalenceStrategies equivalenceStrategy = EquivalenceStrategies.EXISTS;

  @Option(description = "All equivalent programs")
  @FileOption(Type.OPTIONAL_INPUT_FILE)
  private List<Path> programs = ImmutableList.of();

  @Option(description = "Which abstraction to use")
  @FileOption(Type.OPTIONAL_INPUT_FILE)
  private Path configuration = Path.of("config/svcomp25.properties");

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

  private LeafStrategy getStrategy() {
    return switch (strategy) {
      case SYMBOLIC_EXECUTION ->
          new SymbolicExecutionLeafStrategy(config, logger, shutdownNotifier, solver);
      case ARG -> new ARGLeafStrategy(config, logger, shutdownNotifier, solver);
      default -> throw new AssertionError("Unknown strategy: " + strategy);
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

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    pReachedSet.clear();
    try {
      ImmutableList.Builder<SafeAndUnsafeConstraints> data = ImmutableList.builder();
      data.add(equivalenceRunner.runStrategy(inputProgram, getStrategy()));
      for (Path program : programs) {
        data.add(equivalenceRunner.runStrategy(createCfa(program), getStrategy()));
      }
      ImmutableList<SafeAndUnsafeConstraints> result = data.build();
      if (result.isEmpty()) {
        logger.log(Level.WARNING, "No result was computed");
        return AlgorithmStatus.NO_PROPERTY_CHECKED;
      }
      AlgorithmStatus status = result.get(0).status();
      for (SafeAndUnsafeConstraints firstConstraint : result) {
        status = status.update(firstConstraint.status());
        for (SafeAndUnsafeConstraints secondConstraint : result) {
          if (secondConstraint == firstConstraint) {
            continue;
          }
          if (getEquivalenceCheck().isEquivalent(secondConstraint.safe(), firstConstraint.safe())) {
            logger.log(Level.INFO, "Safe paths of programs are equivalent");
          } else {
            logger.log(Level.INFO, "Safe paths of programs are not equivalent");
          }
          if (getEquivalenceCheck()
              .isEquivalent(secondConstraint.unsafe(), firstConstraint.unsafe())) {
            logger.log(Level.INFO, "Unafe paths of programs are equivalent");
          } else {
            logger.log(Level.INFO, "Unsafe paths of programs are not equivalent");
          }
        }
      }
      return status;
    } catch (ParserException pE) {
      throw new CPAException("Parsing the program is not possible", pE);
    } catch (IOException pE) {
      throw new CPAException("Reading the program file is not possible", pE);
    } catch (InvalidConfigurationException pE) {
      throw new CPAException("Configuration not correct", pE);
    } catch (SolverException pE) {
      throw new CPAException("Solver crashed", pE);
    }
  }
}
