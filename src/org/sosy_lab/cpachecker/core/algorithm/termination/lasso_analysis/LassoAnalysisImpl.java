/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.WARNING;
import static org.sosy_lab.java_smt.SolverContextFactory.Solvers.SMTINTERPOL;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.IntegerOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.MoreFiles;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.algorithm.termination.TerminationStatistics;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.RankingRelationBuilder.RankingRelationException;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.construction.LassoBuilder;
import org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.toolchain.LassoRankerToolchainStorage;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.SolverContextFactory;
import org.sosy_lab.java_smt.api.ProverEnvironment;
import org.sosy_lab.java_smt.api.SolverContext;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.basicimpl.AbstractFormulaManager;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import de.uni_freiburg.informatik.ultimate.lassoranker.AnalysisType;
import de.uni_freiburg.informatik.ultimate.lassoranker.Lasso;
import de.uni_freiburg.informatik.ultimate.lassoranker.LassoRankerPreferences;
import de.uni_freiburg.informatik.ultimate.lassoranker.exceptions.TermException;
import de.uni_freiburg.informatik.ultimate.lassoranker.nontermination.NonTerminationAnalysisSettings;
import de.uni_freiburg.informatik.ultimate.lassoranker.nontermination.NonTerminationArgument;
import de.uni_freiburg.informatik.ultimate.lassoranker.nontermination.NonTerminationArgumentSynthesizer;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.TerminationAnalysisSettings;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.TerminationArgument;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.TerminationArgumentSynthesizer;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.templates.AffineTemplate;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.templates.NestedTemplate;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.templates.RankingTemplate;
import de.uni_freiburg.informatik.ultimate.logic.SMTLIBException;
import de.uni_freiburg.informatik.ultimate.logic.Script.LBool;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.util.ToolchainCanceledException;

@Options(prefix = "termination.lassoAnalysis")
public class LassoAnalysisImpl implements LassoAnalysis {

  // The configuration library does not support small letters in enum constants.
  public enum LassoAnalysisType {

    /**
     * @see AnalysisType#Disabled
     */
    DISABLED(AnalysisType.Disabled),

    /**
     * @see AnalysisType#Linear
     */
    LINEAR(AnalysisType.Linear),

    /**
     * @see AnalysisType#Linear_with_guesses
     */
    LINEAR_WITH_GUESSES(AnalysisType.Linear_with_guesses),

    /**
     * @see AnalysisType#Nonlinear
     */
    NONLINEAR(AnalysisType.Nonlinear);

    private final AnalysisType analysisType;

    private LassoAnalysisType(AnalysisType pAnalysisType) {
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
            + "during synthesis of termination arguments."
  )
  @IntegerOption(min = 1)
  private int nonStrictInvariants = 3;

  @Option(
    secure = true,
    description = "Number of generalized eigenvectors in the geometric nontermination argument."
  )
  @IntegerOption(min = 0)
  private int eigenvectors = 3;

  @Option(
    secure = true,
    description =
        "Number of strict supporting invariants for each Motzkin transformation "
            + "during synthesis of termination arguments."
  )
  @IntegerOption(min = 0)
  private int strictInvariants = 2;

  @Option(
    name = "linear.analysisType",
    secure = true,
    description = "Analysis type used for synthesis of linear termination arguments."
  )
  private LassoAnalysisType linearAnalysisType = LassoAnalysisType.LINEAR_WITH_GUESSES;

  @Option(
    name = "linear.externalSolver",
    secure = true,
    description =
        "If true, an external tool is used as SMT solver instead of SMTInterpol. "
            + "This affects only synthesis of linear termination arguments."
  )
  private boolean linearExternalSolver = false;

  @Option(
    secure = true,
    name = "nonlinear.analysisType",
    description = "Analysis type used for synthesis of non-linear termination arguments."
  )
  private LassoAnalysisType nonlinearAnalysisType = LassoAnalysisType.LINEAR_WITH_GUESSES;

  @Option(
    name = "nonlinear.externalSolver",
    secure = true,
    description =
        "If true, an external tool is used as SMT solver instead of SMTInterpol. "
            + "This affects only synthesis of non-linear termination arguments and "
            + "non-termination arguments."
  )
  private boolean nonlinearExternalSolver = false;

  @Option(secure = true, description = "Shell command used to call the external SMT solver.")
  private String externalSolverCommand =
      "./lib/native/x86_64-linux/z3 -smt2 -in SMTLIB2_COMPLIANT=true ";

  @Option(
    secure = true,
    description = "Maximal number of functions used in a ranking function template."
  )
  @IntegerOption(min = 1)
  private int maxTemplateFunctions = 3;

  @Option(
    secure = true,
    description =
        "A human readable representation of the synthesized (non-)termination arguments is "
            + "exported to this file."
  )
  @FileOption(Type.OUTPUT_FILE)
  private Path resultFile = Paths.get("terminationAnalysisResult.txt");

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final TerminationStatistics statistics;

  private final SolverContext solverContext;

  private final org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.construction.LassoBuilder lassoBuilder;
  private final RankingRelationBuilder rankingRelationBuilder;

  private final LassoRankerPreferences linearLassoRankerPreferences;
  private final LassoRankerPreferences nonlinearLassoRankerPreferences;
  private final NonTerminationAnalysisSettings nonTerminationAnalysisSettings;
  private final TerminationAnalysisSettings linearTerminationAnalysisSettings;
  private final TerminationAnalysisSettings nonlinearTerminationAnalysisSettings;

  private final LassoRankerToolchainStorage toolchainStorage;

  private final Collection<RankingTemplate> rankingTemplates;

  private final SetMultimap<Loop, TerminationArgument> terminationArguments;
  private final Map<Loop, NonTerminationArgument> nonTerminationArguments;

  @SuppressWarnings("unchecked")
  public LassoAnalysisImpl(
      LogManager pLogger,
      Configuration pConfig,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      TerminationStatistics pStatistics)
      throws InvalidConfigurationException {

    pConfig.inject(this);
    logger = checkNotNull(pLogger);
    shutdownNotifier = checkNotNull(pShutdownNotifier);
    statistics = checkNotNull(pStatistics);
    solverContext =
        SolverContextFactory.createSolverContext(pConfig, logger, shutdownNotifier, SMTINTERPOL);
    AbstractFormulaManager<Term, ?, ?, ?> formulaManager =
        (AbstractFormulaManager<Term, ?, ?, ?>) solverContext.getFormulaManager();
    Configuration solverConfig = Configuration.defaultConfiguration();
    FormulaManagerView formulaManagerView =
        new FormulaManagerView(formulaManager, solverConfig, pLogger);
    PathFormulaManager pathFormulaManager =
        new PathFormulaManagerImpl(
            formulaManagerView,
            pConfig,
            pLogger,
            pShutdownNotifier,
            pCfa,
            AnalysisDirection.FORWARD);

    lassoBuilder = new LassoBuilder(
            pConfig,
            pLogger,
            shutdownNotifier,
            formulaManager,
            formulaManagerView,
            () -> solverContext.newProverEnvironment(), // Eclipse compiler crashes if a method reference is used here.
            pathFormulaManager);
    rankingRelationBuilder =
        new RankingRelationBuilder(
            pCfa.getMachineModel(),
            pLogger,
            formulaManagerView,
            formulaManager.getFormulaCreator());

    linearLassoRankerPreferences = new LassoRankerPreferences();
    linearLassoRankerPreferences.smt_solver_command = externalSolverCommand;
    linearLassoRankerPreferences.externalSolver = linearExternalSolver;

    nonlinearLassoRankerPreferences = new LassoRankerPreferences();
    nonlinearLassoRankerPreferences.smt_solver_command = externalSolverCommand;
    nonlinearLassoRankerPreferences.externalSolver = nonlinearExternalSolver;

    nonTerminationAnalysisSettings = new NonTerminationAnalysisSettings();
    nonTerminationAnalysisSettings.number_of_gevs = eigenvectors;

    linearTerminationAnalysisSettings = new TerminationAnalysisSettings();
    linearTerminationAnalysisSettings.analysis = linearAnalysisType.toAnalysisType();
    linearTerminationAnalysisSettings.numnon_strict_invariants = nonStrictInvariants;
    linearTerminationAnalysisSettings.numstrict_invariants = strictInvariants;

    nonlinearTerminationAnalysisSettings = new TerminationAnalysisSettings();
    nonlinearTerminationAnalysisSettings.analysis = nonlinearAnalysisType.toAnalysisType();
    nonlinearTerminationAnalysisSettings.numnon_strict_invariants = nonStrictInvariants;
    nonlinearTerminationAnalysisSettings.numstrict_invariants = strictInvariants;

    toolchainStorage = new LassoRankerToolchainStorage(pLogger, pShutdownNotifier);

    rankingTemplates = createTemplates(maxTemplateFunctions);
    terminationArguments = MultimapBuilder.linkedHashKeys().linkedHashSetValues().build();
    nonTerminationArguments = Maps.newLinkedHashMap();
  }

  private static Collection<RankingTemplate> createTemplates(int pMaxTemplateFunctions) {
    ImmutableList.Builder<RankingTemplate> rankingTemplates = ImmutableList.builder();

    rankingTemplates.add(new AffineTemplate());

    for (int i = 2; i <= pMaxTemplateFunctions; i++) {
      rankingTemplates.add(new NestedTemplate(i));
    }

    return rankingTemplates.build();
  }

  @Override
  public void close() {
    toolchainStorage.clear();
    solverContext.close();
  }

  @Override
  public void writeOutputFiles() {
    if (resultFile != null) {
      logger.logf(FINER, "Writing result of termination analysis into %s.", resultFile);

      try (Writer writer = MoreFiles.openOutputFile(resultFile, UTF_8)) {
        writer.append("Non-termination arguments:\n");
        for (Entry<Loop, NonTerminationArgument> nonTerminationArgument :
            nonTerminationArguments.entrySet()) {
          writer.append(nonTerminationArgument.getKey().toString());
          writer.append(":\n");
          writer.append(nonTerminationArgument.getValue().toString());
          writer.append('\n');
        }

        writer.append("\n\nTermination arguments:\n");
        for (Loop loop : terminationArguments.keySet()) {
          for (TerminationArgument terminationArgument : terminationArguments.get(loop)) {
            writer.append(loop.toString());
            writer.append(":\n");
            writer.append(terminationArgument.toString());
            writer.append('\n');
          }
          writer.append('\n');
        }

      } catch (IOException e) {
        logger.logException(WARNING, e, "Could not export (non-)termination arguments.");
      }
    }
  }

  @Override
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
          WARNING, e, "Could not check (non)-termination of lasso (" + pLoop + ").");
      return LassoAnalysisResult.unknown();
    } catch (ToolchainCanceledException e) {
      throw new InterruptedException(e.getMessage());
    }
  }

  private LassoAnalysisResult checkTermination(
      Loop pLoop, Collection<Lasso> lassos, Set<CVariableDeclaration> pRelevantVariables)
      throws IOException, SMTLIBException, TermException, InterruptedException, SolverException {

    LassoAnalysisResult result = LassoAnalysisResult.unknown();

    // Try to synthesize non-termination arguments first because it is much cheaper
    // than synthesizing termination arguments.
    for (Lasso lasso : lassos) {
      shutdownNotifier.shutdownIfNecessary();
      logger.logf(FINER, "Synthesizing non-termination argument for lasso:\n%s.", lasso);
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
      logger.logf(FINER, "Synthesizing termination argument for lasso:\n%s.", lasso);
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
        logger.logf(FINE, "Proved non-termintion: %s", nonTerminationArgument);
        nonTerminationArguments.put(pLoop, nonTerminationArgument);
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

          LBool result = terminationArgumentSynthesizer.synthesize();
          if (result.equals(LBool.SAT) && terminationArgumentSynthesizer.synthesisSuccessful()) {
            TerminationArgument terminationArgument = terminationArgumentSynthesizer.getArgument();
            logger.logf(Level.FINE, "Found termination argument: %s", terminationArgument);

            try (ProverEnvironment proover = solverContext.newProverEnvironment()) {
              RankingRelation rankingRelation =
                  rankingRelationBuilder.fromTerminationArgument(
                      terminationArgument, pRelevantVariables);

              proover.push(rankingRelation.asFormula());
              if (!proover.isUnsat()) {
                terminationArguments.put(pLoop, terminationArgument);
                return LassoAnalysisResult.fromTerminationArgument(rankingRelation);
              }

            } catch (RankingRelationException e) {
              logger.logUserException(
                  Level.INFO, e, "Could not create ranking relation from " + terminationArgument);
              return LassoAnalysisResult.unknown();
            }
          }

        } catch (AssertionError e) {
          // Workaround for a bug in LassoRanker (terminationArgumentSynthesizer.synthesize()):
          // An assertion is violated if the time limit is reached.
          if (e.getMessage().equals("not yet implemented")) {
            shutdownNotifier.shutdownIfNecessary();
          }
          throw e;
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
        Collections.emptySet(),
        toolchainStorage,
        toolchainStorage);
  }

  private NonTerminationArgumentSynthesizer createNonTerminationArgumentSynthesizer(Lasso lasso)
      throws IOException {
    return new NonTerminationArgumentSynthesizer(
        lasso,
        nonlinearLassoRankerPreferences,
        nonTerminationAnalysisSettings,
        toolchainStorage,
        toolchainStorage);
  }
}
