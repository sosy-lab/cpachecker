// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import com.google.common.base.Predicates;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.TargetLocationCandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.invariants.ExpressionTreeSupplier;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.InvariantProvider;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.Witness;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.WitnessExporter;
import org.sosy_lab.cpachecker.cpa.arg.witnessexport.WitnessToOutputFormatsUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.BiPredicates;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.java_smt.api.BasicProverEnvironment;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

@Options
public class BMCAlgorithm extends AbstractBMCAlgorithm implements Algorithm {

  @Option(
      name = "bmc.checkTargetStates",
      secure = true,
      description =
          "Check reachability of target states after analysis "
              + "(classical BMC). The alternative is to check the reachability "
              + "as soon as the target states are discovered, which is done if "
              + "cpa.predicate.targetStateSatCheck=true.")
  private boolean checkTargetStates = true;

  // Option copied from PathChecker, keep in sync (and hopefully remove at some point)
  @Option(
      name = "counterexample.export.allowImpreciseCounterexamples",
      secure = true,
      description =
          "An imprecise counterexample of the Predicate CPA is usually a bug,"
              + " but expected in some configurations. Should it be treated as a bug or accepted?")
  private boolean allowImpreciseCounterexamples = false;

  @Option(
      name = "bmc.invariantsExport",
      secure = true,
      description = "Export auxiliary invariants used for induction.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  @Nullable
  private Path invariantsExport = null;

  private final Configuration config;
  private final CFA cfa;

  private final WitnessExporter argWitnessExporter;

  public BMCAlgorithm(
      Algorithm pAlgorithm,
      ConfigurableProgramAnalysis pCPA,
      Configuration pConfig,
      LogManager pLogger,
      ReachedSetFactory pReachedSetFactory,
      ShutdownManager pShutdownManager,
      CFA pCFA,
      final Specification specification,
      AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    super(
        pAlgorithm,
        pCPA,
        pConfig,
        pLogger,
        pReachedSetFactory,
        pShutdownManager,
        pCFA,
        specification,
        new BMCStatistics(),
        false /* no invariant generator */,
        pAggregatedReachedSets);
    pConfig.inject(this);

    config = pConfig;
    cfa = pCFA;

    argWitnessExporter = new WitnessExporter(config, logger, specification, cfa);
  }

  @Override
  public AlgorithmStatus run(final ReachedSet reachedSet)
      throws CPAException, InterruptedException {
    try {
      return super.run(reachedSet);
    } catch (SolverException e) {
      throw new CPAException("Solver Failure " + e.getMessage(), e);
    } finally {
      invariantGenerator.cancel();
    }
  }

  @Override
  protected CandidateGenerator getCandidateInvariants() {
    if (getTargetLocations().isEmpty() || !cfa.getAllLoopHeads().isPresent()) {
      return CandidateGenerator.EMPTY_GENERATOR;
    } else {
      return new StaticCandidateProvider(
          Collections.singleton(TargetLocationCandidateInvariant.INSTANCE));
    }
  }

  @Override
  protected boolean boundedModelCheck(
      final ReachedSet pReachedSet,
      final BasicProverEnvironment<?> pProver,
      CandidateInvariant pInductionProblem)
      throws CPATransferException, InterruptedException, SolverException {
    if (!checkTargetStates) {
      return true;
    }

    return super.boundedModelCheck(pReachedSet, pProver, pInductionProblem);
  }

  @Override
  protected void analyzeCounterexample(
      final BooleanFormula pCounterexampleFormula,
      final ReachedSet pReachedSet,
      final BasicProverEnvironment<?> pProver)
      throws CPATransferException, InterruptedException {

    analyzeCounterexample0(pCounterexampleFormula, pReachedSet, pProver)
        .ifPresentOrElse(
            cex -> cex.getTargetState().addCounterexampleInformation(cex),
            () -> {
              if (!allowImpreciseCounterexamples) {
                throw new AssertionError(
                    "Found imprecise counterexample with BMC. "
                        + "If this is expected for this configuration "
                        + "(e.g., because of UF-based heap encoding), "
                        + "set counterexample.export.allowImpreciseCounterexamples=true. "
                        + "Otherwise please report this as a bug.");
              }
            });
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    super.collectStatistics(pStatsCollection);
    pStatsCollection.add(
        new Statistics() {

          @Override
          public void printStatistics(
              PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
            // apparently there is nothing to do here.
          }

          @Override
          public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {
            if (pResult == Result.FALSE) {
              return;
            }
            ARGState rootState =
                AbstractStates.extractStateByType(pReached.getFirstState(), ARGState.class);
            if (rootState != null && invariantsExport != null) {
              ExpressionTreeSupplier tmpExpressionTreeSupplier =
                  ExpressionTreeSupplier.TrivialInvariantSupplier.INSTANCE;
              if (invariantGenerator.isStarted()) {
                try {
                  tmpExpressionTreeSupplier = invariantGenerator.getExpressionTreeSupplier();
                } catch (CPAException | InterruptedException e1) {
                  tmpExpressionTreeSupplier =
                      ExpressionTreeSupplier.TrivialInvariantSupplier.INSTANCE;
                }
              }
              final ExpressionTreeSupplier expSup = tmpExpressionTreeSupplier;
              try (Writer w = IO.openOutputFile(invariantsExport, StandardCharsets.UTF_8)) {
                final Witness generatedWitness =
                    argWitnessExporter.generateProofWitness(
                        rootState,
                        Predicates.alwaysTrue(),
                        BiPredicates.alwaysTrue(),
                        new InvariantProvider() {
                          @Override
                          public ExpressionTree<Object> provideInvariantFor(
                              CFAEdge pCFAEdge,
                              Optional<? extends Collection<? extends ARGState>> pStates)
                              throws InterruptedException {
                            CFANode node = pCFAEdge.getSuccessor();
                            ExpressionTree<Object> result = expSup.getInvariantFor(node);
                            if (ExpressionTrees.getFalse().equals(result) && !pStates.isPresent()) {
                              return ExpressionTrees.getTrue();
                            }
                            return result;
                          }
                        });
                WitnessToOutputFormatsUtils.writeToGraphMl(generatedWitness, w);
              } catch (IOException e) {
                logger.logUserException(
                    Level.WARNING, e, "Could not write invariants to file " + invariantsExport);
              } catch (InterruptedException e) {
                logger.logUserException(
                    Level.WARNING, e, "Could not export witness due to interruption");
              }
            }
          }

          @Override
          public String getName() {
            return null; // return null because we do not print statistics
          }
        });
  }
}
