// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.alternative_error_witness;

import static org.sosy_lab.cpachecker.cpa.arg.ARGUtils.getAllStatesOnPathsTo;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CounterexampleStoreAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPARefiner;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

@Options(prefix = "cpa.alternativeWitnessExport")
public class AlternativeErrorWitnessExport implements Algorithm, StatisticsProvider {

  @Option(
      secure = true,
      name = "outdirForExport",
      description =
          "Create a file that contains the StrongestPost for each loop in the program in this directory.(with a concluding '/')")
  @FileOption(FileOption.Type.OUTPUT_DIRECTORY)
  private Path outdirForExport = Paths.get("errorPaths/");

  private final LogManager logger;

  private final Algorithm algorithm;
  private PredicateCPA predicateCPA;



  public AlternativeErrorWitnessExport(
      Configuration config,
      Algorithm pAlgorithm,
      LogManager pLogger,
      ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    algorithm = pAlgorithm;

    logger = Objects.requireNonNull(pLogger);

    predicateCPA =
        CPAs.retrieveCPAOrFail(pCpa, PredicateCPA.class, CounterexampleStoreAlgorithm.class);
    config.inject(this, AlternativeErrorWitnessExport.class);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReached) throws CPAException, InterruptedException {
    if (!(pReached instanceof PartitionedReachedSet)) {
      throw new CPAException("Expecting a partioned reached set");
    }
    PartitionedReachedSet reachedSet = (PartitionedReachedSet) pReached;

    AlgorithmStatus status = algorithm.run(reachedSet);

    if (reachedSet.wasTargetReached()) {

      final ARGState lastElement = (ARGState) pReached.getLastState();
      assert lastElement.isTarget()
          : "Last element in reached is not a target state before refinement";

      final @Nullable ARGPath allStatesTrace = ARGUtils.getOnePathTo(lastElement);


      Set<ARGState> elementsOnPath = getAllStatesOnPathsTo(allStatesTrace.getLastState());
      // No branches/merges in path, it is precise.
      // We don't need to care about creating extra predicates for branching etc.

      if (elementsOnPath.size() == allStatesTrace.size()
          && !PredicateCPARefiner.containsBranchingInPath(elementsOnPath)) {
        elementsOnPath = ImmutableSet.of();
      }

      // create path with all abstraction location elements (excluding the initial element)
      // the last element is the element corresponding to the error location
      List<ARGState> abstractionStatesTrace =
          PredicateCPARefiner.filterAbstractionStates(allStatesTrace);

      logger.log(Level.ALL, "Abstraction trace is", abstractionStatesTrace);

      PathFormulaManager pathFormulaManager = predicateCPA.getPathFormulaManager();
      LocationAwareBlockFormulaStrategy blockFormulaStrategy =
          new LocationAwareBlockFormulaStrategy();

      LocationAwareBlockFormulas formulas =
          blockFormulaStrategy.getLocatinoAwareFormulasForPath(
              allStatesTrace.getFirstState(), abstractionStatesTrace);
      if (!formulas.hasBranchingFormula()) {
        formulas =
            formulas.withBranchingFormula(pathFormulaManager.buildBranchingFormula(elementsOnPath));
      }
      try {

        Path rootDir;
        try {
          rootDir = Files.createDirectory(outdirForExport);
        } catch (FileAlreadyExistsException e) {
          rootDir = outdirForExport;
          cleanRecursively(rootDir);
        }
        @SuppressWarnings("resource")
        Solver solver = predicateCPA.getSolver();
        FormulaManagerView fmgr = solver.getFormulaManager();
        formulas.dumpToFolder(rootDir, logger, fmgr);

      } catch (IOException e) {
        logger.log(
            Level.SEVERE,
            String.format(
                "An error occured while stroring the output-files. Errro: %s",
                Throwables.getStackTraceAsString(e)));
      }
    }
    logger.log(Level.INFO, "All counterexamples are succesfully stored as formulae!");
    return status;
  }

  private void cleanRecursively(Path pRootDir) throws IOException {
    File root = pRootDir.toFile();
    if (root.isDirectory()) {
      for (File f : pRootDir.toFile().listFiles()) {
        if (f.isDirectory()) {
          cleanRecursively(f.toPath());
        }
        f.delete();
      }
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) algorithm).collectStatistics(statsCollection);
    }
  }
}
