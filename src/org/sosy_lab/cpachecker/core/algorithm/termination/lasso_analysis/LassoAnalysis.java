// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.java_smt.SolverContextFactory.Solvers.SMTINTERPOL;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.uni_freiburg.informatik.ultimate.core.lib.exceptions.ToolchainCanceledException;
import de.uni_freiburg.informatik.ultimate.icfgtransformer.transformulatransformers.TermException;
import de.uni_freiburg.informatik.ultimate.lassoranker.AnalysisType;
import de.uni_freiburg.informatik.ultimate.lassoranker.DefaultLassoRankerPreferences;
import de.uni_freiburg.informatik.ultimate.lassoranker.Lasso;
import de.uni_freiburg.informatik.ultimate.lassoranker.LassoRankerPreferences;
import de.uni_freiburg.informatik.ultimate.lassoranker.nontermination.DefaultNonTerminationAnalysisSettings;
import de.uni_freiburg.informatik.ultimate.lassoranker.nontermination.NonTerminationAnalysisSettings;
import de.uni_freiburg.informatik.ultimate.lassoranker.nontermination.NonTerminationArgument;
import de.uni_freiburg.informatik.ultimate.lassoranker.nontermination.NonTerminationArgumentSynthesizer;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.DefaultTerminationAnalysisSettings;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.TerminationAnalysisSettings;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.TerminationArgument;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.TerminationArgumentSynthesizer;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.templates.AffineTemplate;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.templates.NestedTemplate;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.templates.RankingTemplate;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.NativeLibraries;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.RankingRelationBuilder.RankingRelationException;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.construction.LassoBuilder;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.toolchain.LassoRankerToolchainStorage;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.basicimpl.AbstractFormulaManager;

@Options(prefix = "termination.lassoAnalysis")
public class LassoAnalysis {

  // The configuration library does not support small letters in enum constants.
  public enum LassoAnalysisType {

    /** See {@link AnalysisType#DISABLED}. */
    DISABLED(AnalysisType.DISABLED),

    /** See {@link AnalysisType#LINEAR}. */
    LINEAR(AnalysisType.LINEAR),

    /** See {@link AnalysisType#LINEAR_WITH_GUESSES}. */
    LINEAR_WITH_GUESSES(AnalysisType.LINEAR_WITH_GUESSES),

    /** See {@link AnalysisType#NONLINEAR}. */
    NONLINEAR(AnalysisType.NONLINEAR);

    private final AnalysisType analysisType;

    LassoAnalysisType(AnalysisType pAnalysisType) {
      analysisType = pAnalysisType;
    }

    private AnalysisType toAnalysisType() {
      return analysisType;
    }
  }

  @Option(
      secure = true,
      description =
          "Number of non-strict supporting invariants for each Motzkin transformation "
              + "during synthesis of termination arguments.")
  @IntegerOption(min = 1)
  private int nonStrictInvariants = 3;

  @Option(
      secure = true,
      description = "Number of generalized eigenvectors in the geometric nontermination argument.")
  @IntegerOption(min = 0)
  private int eigenvectors = 3;

  @Option(
      secure = true,
      description =
          "Number of strict supporting invariants for each Motzkin transformation "
              + "during synthesis of termination arguments.")
  @IntegerOption(min = 0)
  private int strictInvariants = 2;

  @Option(
      name = "linear.analysisType",
      secure = true,
      description = "Analysis type used for synthesis of linear termination arguments.")
  private LassoAnalysisType linearAnalysisType = LassoAnalysisType.LINEAR_WITH_GUESSES;

  @Option(
      name = "linear.externalSolver",
      secure = true,
      description =
          "If true, an external tool is used as SMT solver instead of SMTInterpol. "
              + "This affects only synthesis of linear termination arguments.")
  private boolean linearExternalSolver = false;

  @Option(
      secure = true,
      name = "nonlinear.analysisType",
      description = "Analysis type used for synthesis of non-linear termination arguments.")
  private LassoAnalysisType nonlinearAnalysisType = LassoAnalysisType.LINEAR_WITH_GUESSES;

  @Option(
      name = "nonlinear.externalSolver",
      secure = true,
      description =
          "If true, an external tool is used as SMT solver instead of SMTInterpol. "
              + "This affects only synthesis of non-linear termination arguments and "
              + "non-termination arguments.")
  private boolean nonlinearExternalSolver = false;

  @Option(description = "Shell command used to call the external SMT solver.")
  private String externalSolverCommand =
      NativeLibraries.getNativeLibraryPath().resolve("z3") + " -smt2 -in SMTLIB2_COMPLIANT=true ";

  @Option(
      secure = true,
      description = "Maximal number of functions used in a ranking function template.")
  @IntegerOption(min = 1)
  private int maxTemplateFunctions = 3;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final LassoAnalysisStatistics statistics;

  private final SolverContext solverContext;

  private final LassoBuilder lassoBuilder;
  private final RankingRelationBuilder rankingRelationBuilder;

  private final LassoRankerPreferences linearLassoRankerPreferences;
  private final LassoRankerPreferences nonlinearLassoRankerPreferences;
  private final NonTerminationAnalysisSettings nonTerminationAnalysisSettings;
  private final TerminationAnalysisSettings linearTerminationAnalysisSettings;
  private final TerminationAnalysisSettings nonlinearTerminationAnalysisSettings;

  private final LassoRankerToolchainStorage toolchainStorage;

  private final ImmutableList<RankingTemplate> rankingTemplates;

  @SuppressWarnings({"resource", "unchecked"})
  public static LassoAnalysis create(
      LassoBuilder pLassoBuilder,
      RankingRelationBuilder pRankingRelationBuilder,
      SolverContext pSolverContext,
      LogManager pLogger,
      Configuration pConfig,
      ShutdownNotifier pShutdownNotifier,
      LassoAnalysisStatistics pStatistics)
      throws InvalidConfigurationException {
    return new LassoAnalysis(
        pLassoBuilder,
        pRankingRelationBuilder,
        pSolverContext,
        pLogger,
        pConfig,
        pShutdownNotifier,
        pStatistics);
  }

  @SuppressWarnings({"resource", "unchecked"})
  public static LassoAnalysis create(
      LogManager pLogger,
      Configuration pConfig,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      LassoAnalysisStatistics pStatistics)
      throws InvalidConfigurationException {
    pConfig = Solver.adjustConfigForSolver(pConfig);
    SolverContext solverContext =
        SolverContextFactory.createSolverContext(pConfig, pLogger, pShutdownNotifier, SMTINTERPOL);
    AbstractFormulaManager<Term, ?, ?, ?> formulaManager =
        (AbstractFormulaManager<Term, ?, ?, ?>) solverContext.getFormulaManager();
    FormulaManagerView formulaManagerView =
        new FormulaManagerView(formulaManager, pConfig, pLogger);
    PathFormulaManager pathFormulaManager =
        new PathFormulaManagerImpl(
            formulaManagerView,
            pConfig,
            pLogger,
            pShutdownNotifier,
            pCfa,
            AnalysisDirection.FORWARD);

    LassoBuilder lassoBuilder =
        new LassoBuilder(
            pConfig,
            pLogger,
            pShutdownNotifier,
            formulaManager,
            formulaManagerView,
            () ->
                solverContext
                    .newProverEnvironment(), // Eclipse compiler crashes if a method reference is
            // used here.
            pathFormulaManager,
            pStatistics);
    RankingRelationBuilder rankingRelationBuilder =
        new RankingRelationBuilder(
            pCfa.getMachineModel(),
            pLogger,
            formulaManagerView,
            formulaManager.getFormulaCreator());

    return new LassoAnalysis(
        lassoBuilder,
        rankingRelationBuilder,
        solverContext,
        pLogger,
        pConfig,
        pShutdownNotifier,
        pStatistics);
  }

  private LassoAnalysis(
      LassoBuilder pLassoBuilder,
      RankingRelationBuilder pRankingRelationBuilder,
      SolverContext pSolverContext,
      LogManager pLogger,
      Configuration pConfig,
      ShutdownNotifier pShutdownNotifier,
      LassoAnalysisStatistics pStatistics)
      throws InvalidConfigurationException {

    pConfig.inject(this);
    logger = checkNotNull(pLogger);
    shutdownNotifier = checkNotNull(pShutdownNotifier);
    statistics = checkNotNull(pStatistics);
    lassoBuilder = checkNotNull(pLassoBuilder);
    rankingRelationBuilder = checkNotNull(pRankingRelationBuilder);
    solverContext = checkNotNull(pSolverContext);

    linearLassoRankerPreferences =
        new LassoRankerPreferences(
            new DefaultLassoRankerPreferences() {

              @Override
              public String getExternalSolverCommand() {
                return externalSolverCommand;
              }

              @Override
              public boolean isExternalSolver() {
                return linearExternalSolver;
              }
            });

    nonlinearLassoRankerPreferences =
        new LassoRankerPreferences(
            new DefaultLassoRankerPreferences() {

              @Override
              public String getExternalSolverCommand() {
                return externalSolverCommand;
              }

              @Override
              public boolean isExternalSolver() {
                return nonlinearExternalSolver;
              }
            });

    nonTerminationAnalysisSettings =
        new NonTerminationAnalysisSettings(
            new DefaultNonTerminationAnalysisSettings() {

              @Override
              public int getNumberOfGevs() {
                return eigenvectors;
              }
            });

    linearTerminationAnalysisSettings =
        new TerminationAnalysisSettings(
            new DefaultTerminationAnalysisSettings() {

              @Override
              public AnalysisType getAnalysis() {
                return linearAnalysisType.toAnalysisType();
              }

              @Override
              public int getNumNonStrictInvariants() {
                return nonStrictInvariants;
              }

              @Override
              public int getNumStrictInvariants() {
                return strictInvariants;
              }

              @Override
              public boolean isNonDecreasingInvariants() {
                return false;
              }
            });

    nonlinearTerminationAnalysisSettings =
        new TerminationAnalysisSettings(
            new DefaultTerminationAnalysisSettings() {

              @Override
              public AnalysisType getAnalysis() {
                return nonlinearAnalysisType.toAnalysisType();
              }

              @Override
              public int getNumNonStrictInvariants() {
                return nonStrictInvariants;
              }

              @Override
              public int getNumStrictInvariants() {
                return strictInvariants;
              }

              @Override
              public boolean isNonDecreasingInvariants() {
                return false;
              }
            });

    toolchainStorage = new LassoRankerToolchainStorage(pLogger, pShutdownNotifier);

    rankingTemplates = createTemplates(maxTemplateFunctions);
  }

  private static ImmutableList<RankingTemplate> createTemplates(int pMaxTemplateFunctions) {
    ImmutableList.Builder<RankingTemplate> rankingTemplates = ImmutableList.builder();

    rankingTemplates.add(new AffineTemplate());

    for (int i = 2; i <= pMaxTemplateFunctions; i++) {
      rankingTemplates.add(new NestedTemplate(i));
    }

    return rankingTemplates.build();
  }

  /** Frees all created resources and the solver context. */
  public void close() {
    toolchainStorage.clear();
    solverContext.close();
  }

  /**
   * Tries to prove (non)-termination of a lasso given as {@link CounterexampleInfo}.
   *
   * @param pLoop the Loop the is currently analyzed
   * @param pCounterexample the {@link CounterexampleInfo} representing the potentially
   *     non-terminating lasso
   * @param pRelevantVariables all variables that might be relevant to prove (non-)termination
   * @return the {@link LassoAnalysisResult}
   * @throws CPATransferException if the extraction of stem or loop fails
   * @throws InterruptedException if a shutdown was requested
   */
  public LassoAnalysisResult checkTermination(
      Loop pLoop, CounterexampleInfo pCounterexample, Set<CVariableDeclaration> pRelevantVariables)
      throws CPATransferException, InterruptedException {
    statistics.analysisOfLassosStarted();
    try {
      return checkTermination0(pLoop, pCounterexample, pRelevantVariables);
    } finally {
      statistics.analysisOfLassosFinished();
    }
  }

  private LassoAnalysisResult checkTermination0(
      Loop pLoop, CounterexampleInfo pCounterexample, Set<CVariableDeclaration> pRelevantVariables)
      throws CPATransferException, InterruptedException {
    Collection<Lasso> lassos;
    statistics.lassoConstructionStarted();
    try {
      lassos = lassoBuilder.buildLasso(pCounterexample, pRelevantVariables);
      statistics.lassosConstructed(pLoop, lassos.size());

    } catch (TermException | SolverException e) {
      logger.logUserException(Level.WARNING, e, "Could not extract lasso (" + pLoop + ").");
      return LassoAnalysisResult.unknown();
    } finally {
      statistics.lassoConstructionFinished();
    }

    try {
      return checkTermination(pLoop, lassos, pRelevantVariables);

    } catch (IOException | SMTLIBException | TermException | SolverException e) {
      logger.logUserException(
          Level.WARNING, e, "Could not check (non)-termination of lasso (" + pLoop + ").");
      return LassoAnalysisResult.unknown();
    } catch (ToolchainCanceledException e) {
      throw new InterruptedException(e.getMessage());
    }
  }

  public LassoAnalysisResult checkTermination(
      Loop pLoop, Collection<Lasso> lassos, Set<CVariableDeclaration> pRelevantVariables)
      throws IOException, SMTLIBException, TermException, InterruptedException, SolverException {

    LassoAnalysisResult result = LassoAnalysisResult.unknown();

    // Try to synthesize non-termination arguments first because it is much cheaper
    // than synthesizing termination arguments.
    for (Lasso lasso : lassos) {
      shutdownNotifier.shutdownIfNecessary();
      logger.logf(Level.FINER, "Synthesizing non-termination argument for lasso:\n%s.", lasso);
      LassoAnalysisResult resultFromLasso = synthesizeNonTerminationArgument(pLoop, lasso);
      result = result.update(resultFromLasso);

      // Stop and return result if non-termination could be proved.
      if (result.hasNonTerminationArgument()) {
        return result;
      }
    }

    // Synthesize termination arguments
    for (Lasso lasso : lassos) {
      shutdownNotifier.shutdownIfNecessary();
      logger.logf(Level.FINER, "Synthesizing termination argument for lasso:\n%s.", lasso);
      LassoAnalysisResult resultFromLasso =
          synthesizeTerminationArgument(pLoop, lasso, pRelevantVariables);
      result = result.update(resultFromLasso);
    }

    return result;
  }

  private LassoAnalysisResult synthesizeNonTerminationArgument(Loop pLoop, Lasso lasso)
      throws IOException, SMTLIBException, TermException {

    statistics.nonTerminationAnalysisOfLassoStarted();
    NonTerminationArgument nonTerminationArgument = null;
    try (NonTerminationArgumentSynthesizer nonTerminationArgumentSynthesizer =
        createNonTerminationArgumentSynthesizer(lasso)) {

      LBool result = nonTerminationArgumentSynthesizer.synthesize();
      if (result.equals(LBool.SAT) && nonTerminationArgumentSynthesizer.synthesisSuccessful()) {
        nonTerminationArgument = nonTerminationArgumentSynthesizer.getArgument();
        logger.logf(Level.FINE, "Proved non-termination: %s", nonTerminationArgument);
        statistics.synthesizedNonTerminationArgument(pLoop, nonTerminationArgument);
        return LassoAnalysisResult.fromNonTerminationArgument(nonTerminationArgument);

      } else {
        return LassoAnalysisResult.unknown();
      }

    } finally {
      statistics.nonTerminationAnalysisOfLassoFinished();
    }
  }

  private LassoAnalysisResult synthesizeTerminationArgument(
      Loop pLoop, Lasso lasso, Set<CVariableDeclaration> pRelevantVariables)
      throws IOException, SMTLIBException, TermException, InterruptedException, SolverException {

    statistics.terminationAnalysisOfLassoStarted();
    try {
      for (RankingTemplate rankingTemplate : rankingTemplates) {
        shutdownNotifier.shutdownIfNecessary();

        try (TerminationArgumentSynthesizer terminationArgumentSynthesizer =
            createTerminationArgumentSynthesizer(lasso, rankingTemplate)) {
          LBool result = null;
          try {
            result = terminationArgumentSynthesizer.synthesize();
          } catch (AssertionError e) {
            // Workaround for a bug in LassoRanker (terminationArgumentSynthesizer.synthesize()):
            // An assertion is violated if the time limit is reached.
            if ("not yet implemented".equals(e.getMessage())) {
              shutdownNotifier.shutdownIfNecessary();
            }
            throw e;
          }
          if (result.equals(LBool.SAT) && terminationArgumentSynthesizer.synthesisSuccessful()) {
            TerminationArgument terminationArgument = terminationArgumentSynthesizer.getArgument();
            logger.logf(Level.FINE, "Found termination argument: %s", terminationArgument);

            try (ProverEnvironment proverEnv = solverContext.newProverEnvironment()) {
              RankingRelation rankingRelation =
                  rankingRelationBuilder.fromTerminationArgument(
                      terminationArgument, pRelevantVariables);

              proverEnv.push(rankingRelation.asFormula());
              if (!proverEnv.isUnsat()) {
                statistics.synthesizedTerminationArgument(pLoop, terminationArgument);
                return LassoAnalysisResult.fromTerminationArgument(rankingRelation);
              }

            } catch (RankingRelationException e) {
              logger.logUserException(
                  Level.INFO, e, "Could not create ranking relation from " + terminationArgument);
              return LassoAnalysisResult.unknown();
            }
          }
        }
      }

    } finally {
      statistics.terminationAnalysisOfLassoFinished();
    }

    return LassoAnalysisResult.unknown();
  }

  private TerminationArgumentSynthesizer createTerminationArgumentSynthesizer(
      Lasso lasso, RankingTemplate template) throws IOException {
    LassoRankerPreferences lassoRankerPreferences;
    TerminationAnalysisSettings terminationAnalysisSettings;

    if (template.getDegree() == 0) {
      lassoRankerPreferences = linearLassoRankerPreferences;
      terminationAnalysisSettings = linearTerminationAnalysisSettings;

    } else {
      lassoRankerPreferences = nonlinearLassoRankerPreferences;
      terminationAnalysisSettings = nonlinearTerminationAnalysisSettings;
    }

    return new TerminationArgumentSynthesizer(
        lasso,
        template,
        lassoRankerPreferences,
        terminationAnalysisSettings,
        ImmutableSet.of(),
        toolchainStorage);
  }

  private NonTerminationArgumentSynthesizer createNonTerminationArgumentSynthesizer(Lasso lasso)
      throws IOException {
    return new NonTerminationArgumentSynthesizer(
        lasso, nonlinearLassoRankerPreferences, nonTerminationAnalysisSettings, toolchainStorage);
  }
}
