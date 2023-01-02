// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.residualprogram;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.export.CFAToPixelsWriter;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.residualprogram.ConditionFolder.FOLDER_TYPE;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackCPA;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackStateEqualsWrapper;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.cwriter.ARGToCTranslator;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@Options(prefix = "residualprogram")
public class ResidualProgramConstructionAlgorithm implements Algorithm, StatisticsProvider {

  public enum ResidualGenStrategy {
    REACHABILITY,
    SLICING,
    CONDITION,
    CONDITION_PLUS_FOLD,
    COMBINATION
  }

  @Option(
      secure = true,
      name = "strategy",
      description = "which strategy to use to generate the residual program")
  private ResidualGenStrategy constructionStrategy = ResidualGenStrategy.CONDITION;

  @Option(secure = true, name = "file", description = "write residual program to file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path residualProgram = Path.of("residualProgram.c");

  @Option(
      secure = true,
      name = "assumptionGuider",
      description =
          "set specification file to automaton which guides analysis along assumption produced by"
              + " incomplete analysis,e.g., config/specification/AssumptionGuidingAutomaton.spc, to"
              + " enable residual program from combination of program and assumption condition")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private @Nullable Path conditionSpec = null;

  @Option(
      secure = true,
      name = "assumptionFile",
      description = "set path to file which contains the condition")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private @Nullable Path condition = null;

  @Option(
      secure = true,
      name = "cfa.pixelGraphicFile",
      description =
          "Export CFA of residual program as pixel graphic to the given file name. The suffix is"
              + " added corresponding to the value of option pixelgraphic.export.formatIf set to"
              + " 'null', no pixel graphic is exported.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path exportPixelFile = Path.of("residProgPixel");

  @Option(
      secure = true,
      name = "export.pixel",
      description = "Export residual program as pixel graphic")
  private boolean exportPixelGraphic = false;

  @Option(
      secure = true,
      name = "statistics.size",
      description = "Collect statistical data about size of residual program")
  private boolean collectResidualProgramSizeStatistics = false;

  private final CFA cfa;
  private final Specification spec;
  private final Configuration configuration;
  protected final LogManager logger;
  protected final ShutdownNotifier shutdown;

  private @Nullable CPAAlgorithm cpaAlgorithm;

  private final ARGToCTranslator translator;
  private final @Nullable ConditionFolder folder;

  protected final ProgramGenerationStatistics statistic = new ProgramGenerationStatistics();

  public ResidualProgramConstructionAlgorithm(
      final CFA pCfa,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdown,
      final Specification pSpec,
      final ConfigurableProgramAnalysis pCpa,
      final Algorithm pInnerAlgorithm)
      throws InvalidConfigurationException {
    this(pCfa, pConfig, pLogger, pShutdown, pSpec);

    if (pInnerAlgorithm instanceof CPAAlgorithm) {
      cpaAlgorithm = (CPAAlgorithm) pInnerAlgorithm;
    } else {
      throw new InvalidConfigurationException(
          "For residual program generation, only the CPAAlgorithm is required.");
    }

    checkCPAConfiguration(pCpa);
  }

  protected ResidualProgramConstructionAlgorithm(
      final CFA pCfa,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdown,
      final Specification pSpec)
      throws InvalidConfigurationException {
    pConfig.inject(this, ResidualProgramConstructionAlgorithm.class);

    cfa = pCfa;
    logger = pLogger;
    shutdown = pShutdown;
    spec = pSpec;
    configuration = pConfig;
    translator = new ARGToCTranslator(logger, pConfig, cfa.getMachineModel());

    checkConfiguration();

    if (getStrategy() == ResidualGenStrategy.CONDITION_PLUS_FOLD) {
      folder = ConditionFolder.createFolder(pConfig, cfa);
    } else {
      folder = null;
    }
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    Preconditions.checkState(
        checkInitialState(pReachedSet.getFirstState()),
        "CONDITION, CONDITION_PLUS_FOLD, and COMBINATION strategy require assumption automaton"
            + " (condition) and assumption guiding automaton in specification");
    Preconditions.checkNotNull(cpaAlgorithm);

    logger.log(Level.INFO, "Start construction of residual program.");
    try {
      statistic.modelBuildTimer.start();
      cpaAlgorithm.run(pReachedSet);
    } finally {
      statistic.modelBuildTimer.stop();
    }

    ARGState argRoot = (ARGState) pReachedSet.getFirstState();

    CFANode mainFunction = AbstractStates.extractLocation(argRoot);
    assert mainFunction != null;

    if (pReachedSet.hasWaitingState()) {
      logger.log(
          Level.SEVERE,
          "Analysis run to get structure of residual program is incomplete. ",
          "Ensure that you use cpa.automaton.breakOnTargetState=-1 in your configuration.");
      throw new CPAException("Failed to construct residual program");
    }

    Set<ARGState> addPragma;
    try {
      statistic.collectPragmaPointsTimer.start();
      switch (constructionStrategy) {
        case COMBINATION:
          addPragma = getAllTargetStates(pReachedSet);
          break;
        case SLICING:
          addPragma = getAllTargetStatesNotFullyExplored(pReachedSet);
          break;
        default: // CONDITION, CONDITION_PLUS_FOLD no effect
          addPragma = null;
      }
    } finally {
      statistic.collectPragmaPointsTimer.stop();
    }

    logger.log(Level.INFO, "Write residual program to file.");
    if (!writeResidualProgram(argRoot, addPragma)) {
      try {
        Files.deleteIfExists(residualProgram);
      } catch (IOException e) {
        // ignore error on deleting file
      }
      throw new CPAException("Failed to write residual program.");
    }

    logger.log(
        Level.INFO,
        "Finished construction of residual program. ",
        "If the selected strategy is SLICING or COMBINATION, please continue with the slicing tool"
            + " (Frama-C)");

    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  protected Set<ARGState> getAllTargetStates(final ReachedSet pReachedSet) {
    logger.log(
        Level.INFO,
        "All target states in residual program are relevant and will be considered in slicing.");
    return Sets.newHashSet(
        Iterables.filter(Iterables.filter(pReachedSet, ARGState.class), state -> state.isTarget()));
  }

  private Set<ARGState> getAllTargetStatesNotFullyExplored(final ReachedSet pNodesOfInlinedProg) {
    logger.log(
        Level.INFO,
        "Identify all target states in original program which are not fully explored according to"
            + " condition and are relevant for slicing.");
    Multimap<CFANode, CallstackStateEqualsWrapper> unexploredTargetStates =
        getUnexploredTargetStates(
            AbstractStates.extractLocation(pNodesOfInlinedProg.getFirstState()));
    if (unexploredTargetStates == null) {
      logger.log(
          Level.WARNING,
          "Failed to identify target locations in program which have not been explored completely."
              + " ",
          "Assume that all target locations are unexplored.");
      return getAllTargetStates(pNodesOfInlinedProg);
    }
    return Sets.newHashSet(
        Iterables.filter(
            Iterables.filter(pNodesOfInlinedProg, ARGState.class),
            state ->
                unexploredTargetStates.containsEntry(
                    AbstractStates.extractLocation(state),
                    new CallstackStateEqualsWrapper(
                        AbstractStates.extractStateByType(state, CallstackState.class)))));
  }

  private @Nullable Multimap<CFANode, CallstackStateEqualsWrapper> getUnexploredTargetStates(
      final CFANode mainFunction) {
    Preconditions.checkState(
        condition != null, "Please set option residualprogram.assumptionFile.");
    try {
      ConfigurationBuilder configBuilder = Configuration.builder();
      configBuilder.setOption("cpa", "cpa.arg.ARGCPA");
      configBuilder.setOption("ARGCPA.cpa", "cpa.composite.CompositeCPA");
      configBuilder.setOption(
          "CompositeCPA.cpas", "cpa.location.LocationCPA,cpa.callstack.CallstackCPA");
      configBuilder.setOption("cpa.automaton.breakOnTargetState", "-1");
      Configuration config = configBuilder.build();

      CoreComponentsFactory coreComponents =
          new CoreComponentsFactory(config, logger, shutdown, AggregatedReachedSets.empty());

      final Specification constrSpec =
          spec.withAdditionalSpecificationFile(
              ImmutableSet.of(conditionSpec, condition), cfa, config, logger, shutdown);

      ConfigurableProgramAnalysis cpa = coreComponents.createCPA(cfa, constrSpec);

      ReachedSet reached = coreComponents.createReachedSet(cpa);
      reached.add(
          cpa.getInitialState(mainFunction, StateSpacePartition.getDefaultPartition()),
          cpa.getInitialPrecision(mainFunction, StateSpacePartition.getDefaultPartition()));

      Algorithm algo = CPAAlgorithm.create(cpa, logger, config, shutdown);
      algo.run(reached);

      if (reached.hasWaitingState()) {
        logger.log(Level.SEVERE, "Analysis run to get structure of residual program is incomplete");
        return null;
      }

      Multimap<CFANode, CallstackStateEqualsWrapper> result =
          HashMultimap.create(cfa.getAllNodes().size(), cfa.getNumberOfFunctions());

      for (AbstractState targetState : AbstractStates.getTargetStates(reached)) {
        result.put(
            AbstractStates.extractLocation(targetState),
            new CallstackStateEqualsWrapper(
                AbstractStates.extractStateByType(targetState, CallstackState.class)));
      }
      return result;
    } catch (InvalidConfigurationException
        | CPAException
        | IllegalArgumentException
        | InterruptedException e1) {
      logger.log(Level.SEVERE, "Analysis to build structure of residual program failed", e1);
      return null;
    }
  }

  private String getResidualProgramText(
      final ARGState pARGRoot, @Nullable final Set<ARGState> pAddPragma)
      throws CPAException, IOException {
    ARGState root = pARGRoot;
    if (constructionStrategy == ResidualGenStrategy.CONDITION_PLUS_FOLD) {
      Preconditions.checkState(pAddPragma == null);
      Preconditions.checkNotNull(folder);

      try {
        statistic.foldTimer.start();
        statistic.modelBuildTimer.start();
        root = folder.foldARG(pARGRoot);
      } finally {
        statistic.modelBuildTimer.stop();
        statistic.foldTimer.stop();
      }
    }
    try {
      statistic.translationTimer.start();
      return translator.translateARG(root, pAddPragma, hasDeclarationGotoProblem());
    } finally {
      statistic.translationTimer.stop();
    }
  }

  private boolean hasDeclarationGotoProblem() {
    return constructionStrategy != ResidualGenStrategy.CONDITION_PLUS_FOLD
        || folder.getType() != FOLDER_TYPE.CFA;
  }

  protected boolean writeResidualProgram(
      final ARGState pArgRoot, @Nullable final Set<ARGState> pAddPragma)
      throws InterruptedException {
    logger.log(Level.INFO, "Generate residual program");
    try (Writer writer = IO.openOutputFile(residualProgram, Charset.defaultCharset())) {
      writer.write(getResidualProgramText(pArgRoot, pAddPragma));
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write residual program to file");
      return false;
    } catch (CPAException e) {
      logger.logException(Level.SEVERE, e, "Failed to generate residual program.");
      return false;
    }
    String mainFunction = AbstractStates.extractLocation(pArgRoot).getFunctionName();
    if (!translator.addsIncludeDirectives()) {
      assert isValidResidualProgram(mainFunction);
    }
    return true;
  }

  private boolean isValidResidualProgram(String mainFunction) throws InterruptedException {
    try {
      CFACreator cfaCreator =
          new CFACreator(
              Configuration.builder()
                  .setOption("analysis.entryFunction", mainFunction)
                  .setOption("analysis.useLoopStructure", "false")
                  .build(),
              logger,
              shutdown);
      cfaCreator.parseFileAndCreateCFA(Lists.newArrayList(residualProgram.toString()));
    } catch (InvalidConfigurationException e) {
      logger.log(Level.SEVERE, "Default configuration unsuitable for parsing residual program.", e);
      return false;
    } catch (IOException | ParserException e) {
      logger.log(Level.SEVERE, "No valid residual program generated. ", e);
      return false;
    }
    return true;
  }

  protected void checkConfiguration() throws InvalidConfigurationException {
    if (constructionStrategy == ResidualGenStrategy.SLICING) {
      if (conditionSpec == null || condition == null) {
        throw new InvalidConfigurationException(
            "When selection SLICING strategy, also the options residualprogram.assumptionGuider and"
                + " residualprogram.assumptionFile must be set.");
      }
    }
  }

  private void checkCPAConfiguration(final ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    if (pCpa instanceof ARGCPA && ((ARGCPA) pCpa).getWrappedCPAs().get(0) instanceof CompositeCPA) {
      CompositeCPA comCpa = (CompositeCPA) ((ARGCPA) pCpa).getWrappedCPAs().get(0);

      boolean considersLocation = false, considersCallstack = false;
      for (ConfigurableProgramAnalysis innerCPA : comCpa.getWrappedCPAs()) {
        if (innerCPA instanceof LocationCPA) {
          considersLocation = true;
        } else if (innerCPA instanceof CallstackCPA) {
          considersCallstack = true;
        }
      }

      if (!considersLocation || !considersCallstack) {
        throw new InvalidConfigurationException(
            "For residual program generation location and callstack information is required.");
      }

    } else {
      throw new InvalidConfigurationException(
          "Require an ARGCPA which wraps a CompositeCPA for residual program generation.");
    }
  }

  private boolean checkInitialState(final AbstractState initState) {
    if (usesParallelCompositionOfProgramAndCondition()) {
      boolean considersAssumption = false, considersAssumptionGuider = false;

      for (AbstractState component : AbstractStates.asIterable(initState)) {
        if (component instanceof AutomatonState) {
          if (((AutomatonState) component).getOwningAutomatonName().equals("AssumptionAutomaton")) {
            considersAssumption = true;
          }
          if (((AutomatonState) component)
              .getOwningAutomatonName()
              .equals("AssumptionGuidingAutomaton")) {
            considersAssumptionGuider = true;
          }
        }
      }
      if (!considersAssumption || !considersAssumptionGuider) {
        return false;
      }
    }

    return true;
  }

  protected boolean usesParallelCompositionOfProgramAndCondition() {
    return getStrategy() == ResidualGenStrategy.CONDITION
        || getStrategy() == ResidualGenStrategy.COMBINATION
        || getStrategy() == ResidualGenStrategy.CONDITION_PLUS_FOLD;
  }

  protected ResidualGenStrategy getStrategy() {
    return constructionStrategy;
  }

  protected Specification getSpecification() {
    return spec;
  }

  protected @Nullable Path getAssumptionGuider() {
    return conditionSpec;
  }

  protected class ProgramGenerationStatistics implements Statistics {

    private final Timer translationTimer = new Timer();
    private final Timer foldTimer = new Timer();
    protected final Timer modelBuildTimer = new Timer();
    protected final Timer collectPragmaPointsTimer = new Timer();

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      StatisticsWriter statWriter = StatisticsWriter.writingStatisticsTo(pOut);

      statWriter.put("Time for residual program model construction", modelBuildTimer);
      if (getStrategy() == ResidualGenStrategy.CONDITION_PLUS_FOLD) {
        statWriter = statWriter.beginLevel();
        statWriter.put("Time for folding", foldTimer);
        statWriter = statWriter.endLevel();
      }

      if (getStrategy() == ResidualGenStrategy.SLICING
          || getStrategy() == ResidualGenStrategy.COMBINATION) {
        statWriter.put("Time for identifying pragma locations", collectPragmaPointsTimer);
      }

      statWriter.put("Time for C translation", translationTimer);

      if (collectResidualProgramSizeStatistics || (exportPixelGraphic && exportPixelFile != null)) {
        CFA residProg = getResidualProgram(pReached.getFirstState());

        if (residProg != null) {
          if (collectResidualProgramSizeStatistics) {
            int residProgSize = residProg.getAllNodes().size();
            statWriter.put("Original program size (#loc)", cfa.getAllNodes().size());
            statWriter.put("Generated program size (#loc)", residProgSize);
            statWriter.put("Size increase", ((double) residProgSize / cfa.getAllNodes().size()));
          }
          if (exportPixelGraphic && exportPixelFile != null) {
            try {
              new CFAToPixelsWriter(configuration)
                  .write(residProg.getMainFunction(), exportPixelFile);
            } catch (IOException | InvalidConfigurationException e) {
              logger.logUserException(Level.WARNING, e, "Pixel export of residual program failed.");
            }
          }
        }
      }
    }

    private @Nullable CFA getResidualProgram(final AbstractState root) {
      try {
        CFACreator cfaCreator =
            new CFACreator(
                Configuration.builder()
                    .setOption(
                        "analysis.entryFunction",
                        AbstractStates.extractLocation(root).getFunctionName())
                    .setOption("parser.usePreprocessor", "true")
                    .setOption("parser.useClang", "true")
                    .setOption("analysis.useLoopStructure", "false")
                    .build(),
                logger,
                shutdown);

        CFA residProg =
            cfaCreator.parseFileAndCreateCFA(Lists.newArrayList(residualProgram.toString()));

        return residProg;

      } catch (InterruptedException
          | InvalidConfigurationException
          | IOException
          | ParserException e) {
        // ignore
        return null;
      }
    }

    @Override
    public @Nullable String getName() {
      return "Residual Program Generation";
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    cpaAlgorithm.collectStatistics(pStatsCollection);

    pStatsCollection.add(statistic);
  }
}
