// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.alternative_error_witness;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CounterexampleStoreAlgorithm;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.PartitionedReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaStrategy;
import org.sosy_lab.cpachecker.cpa.predicate.BlockFormulaStrategy.BlockFormulas;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPARefiner;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

@Options(prefix = "cpa.spexport")
public class AlternativeErrorWitnessExport implements Algorithm, StatisticsProvider {

  private static final String PREFIX_FILENAME = "formula_";

  private static final String SUFFIX_BRANCHING_FORMULA = "branching";

  @Option(
      secure = true,
      description =
          "Create a file that contains the StrongestPost for each loop in the program in this directory.(with a concluding '/')")
  private String outdirForExport = "output/";

  private final LogManager logger;

  private final CFA cfa;
  private final Algorithm algorithm;
  private PredicateCPA predicateCPA;

  private @NonNull ARGCPA argCpa;

  private AssumptionToEdgeAllocator allocator;

  public AlternativeErrorWitnessExport(
      Configuration config,
      Algorithm pAlgorithm,
      LogManager pLogger,
      CFA pCfa,
      ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    algorithm = pAlgorithm;
    cfa = Objects.requireNonNull(pCfa);
    logger = Objects.requireNonNull(pLogger);

    argCpa = CPAs.retrieveCPAOrFail(pCpa, ARGCPA.class, CounterexampleStoreAlgorithm.class);
    allocator = AssumptionToEdgeAllocator.create(config, pLogger, cfa.getMachineModel());
    predicateCPA =
        CPAs.retrieveCPAOrFail(pCpa, PredicateCPA.class, CounterexampleStoreAlgorithm.class);
    config.inject(this, AlternativeErrorWitnessExport.class);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReached) throws CPAException, InterruptedException {
    if (!(pReached instanceof PartitionedReachedSet)) {
      throw new CPAException("Expecting a partioned reached set");
    }
    PartitionedReachedSet reached = (PartitionedReachedSet) pReached;

    AlgorithmStatus status = algorithm.run(reached);

    if (reached.hasViolatedProperties()) {
      List<CounterexampleInfo> ceInfos = new ArrayList<>();
      Map<ARGState, CounterexampleInfo> cexs = getAllCounterexamples(reached);

      for (Map.Entry<ARGState, CounterexampleInfo> e : cexs.entrySet()) {
        ARGState targetState = e.getKey();
        if (!targetState.getCounterexampleInformation().isPresent()) {
          targetState.addCounterexampleInformation(e.getValue());
        }
        ceInfos.add(e.getValue());
      }

      PathFormulaManager pathFormulaManager = predicateCPA.getPathFormulaManager();
      BlockFormulaStrategy blockFormulaStrategy = new BlockFormulaStrategy();
      List<BlockFormulas> blockFolmulas = new ArrayList<>();
      for (CounterexampleInfo info : ceInfos) {

        // TODO: This code is adapted from from
        // PredicateStaticRefiner#performStaticRefinementForPath
        Set<ARGState> elementsOnPath =
            ARGUtils.getAllStatesOnPathsTo(info.getTargetPath().getLastState());
        // No branches/merges in path, it is precise.
        // We don't need to care about creating extra predicates for branching etc.

        if (elementsOnPath.size() == info.getTargetPath().size()) {
          elementsOnPath = ImmutableSet.of();
        }

        // create path with all abstraction location elements (excluding the initial element)
        // the last element is the element corresponding to the error location
        final List<ARGState> abstractionStatesTrace =
            PredicateCPARefiner.filterAbstractionStates(info.getTargetPath());

        BlockFormulas formulas =
            blockFormulaStrategy.getFormulasForPath(
                info.getTargetPath().getFirstState(), abstractionStatesTrace);
        if (!formulas.hasBranchingFormula()) {
          formulas =
              formulas.withBranchingFormula(
                  pathFormulaManager.buildBranchingFormula(elementsOnPath));
        }
        blockFolmulas.add(formulas);
      }
     try {
        Path path = Paths.get(this.outdirForExport + "errorPaths/");
      Path rootDir = Files.createDirectory(path);
        FormulaManagerView fmgr = predicateCPA.getSolver().getFormulaManager();

        for (int cntBFs = 0; cntBFs < blockFolmulas.size(); cntBFs++) {
          Path dir =
              Files.createDirectory(
                  Paths.get(rootDir.toAbsolutePath() + String.format("/path_%d", cntBFs)));
          BlockFormulas bf = blockFolmulas.get(cntBFs);
          ImmutableList<BooleanFormula> formulaList = bf.getFormulas();
          for (int cntFormula = 0; cntFormula < bf.getSize(); cntFormula++) {
            Path f =
                Files.createFile(
                    Paths.get(dir.toAbsolutePath() + "/" + PREFIX_FILENAME + cntFormula));
            fmgr.dumpFormulaToFile(formulaList.get(cntFormula), f);
          }
          if (bf.hasBranchingFormula()) {
            Path f =
                Files.createFile(
                    Paths.get(
                        dir.toAbsolutePath() + "/" + PREFIX_FILENAME + SUFFIX_BRANCHING_FORMULA));
            fmgr.dumpFormulaToFile(bf.getBranchingFormula(), f);
          }
        }
      } catch (IOException e) {
        logger.log(Level.SEVERE, String.format("An error occured while stroring the output-files. Errro: %s", Throwables.getStackTraceAsString(e)));
      }
    }
    logger.log(Level.INFO, "All counterexamples are succesfully stored as formulae!");
    return status;
  }

  // TODO: This is duplicate code with ARGStatistics#getAllCounterexamples
  private Map<ARGState, CounterexampleInfo> getAllCounterexamples(
      final UnmodifiableReachedSet pReached) {
    ImmutableMap.Builder<ARGState, CounterexampleInfo> counterexamples = ImmutableMap.builder();

    for (AbstractState targetState : from(pReached).filter(AbstractStates::isTargetState)) {
      ARGState s = (ARGState) targetState;
      CounterexampleInfo cex =
          ARGUtils.tryGetOrCreateCounterexampleInformation(s, argCpa, allocator).orElse(null);
      if (cex != null) {
        counterexamples.put(s, cex);
      }
    }

    Map<ARGState, CounterexampleInfo> allCounterexamples = counterexamples.build();
    final Map<ARGState, CounterexampleInfo> preciseCounterexamples =
        Maps.filterValues(allCounterexamples, cex -> cex.isPreciseCounterExample());
    return preciseCounterexamples.isEmpty() ? allCounterexamples : preciseCounterexamples;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) algorithm).collectStatistics(statsCollection);
    }
  }

  private Optional<PathFormula> getPathFormulaOfNode(
      CFANode pPredOfLoopHead, PartitionedReachedSet pReached) {
    Collection<AbstractState> toProcess = filter(pPredOfLoopHead, pReached);

    for (AbstractState s : toProcess) {
      PredicateAbstractState pred =
          AbstractStates.extractStateByType(s, PredicateAbstractState.class);

      return Optional.of(pred.getPathFormula());
    }
    return Optional.empty();
  }

  private Set<AbstractState> filter(CFANode pPredOfLoopHead, PartitionedReachedSet pReached) {
    return pReached
        .asCollection()
        .stream()
        .filter(s -> AbstractStates.extractLocation(s).equals(pPredOfLoopHead))
        .collect(Collectors.toSet());
  }
}
