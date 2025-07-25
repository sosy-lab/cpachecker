// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.common.ShutdownNotifier.interruptCurrentThreadOnShutdown;

import com.google.common.base.Joiner;
import com.google.common.base.StandardSystemProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.logging.Level;
import org.sosy_lab.common.AbstractMBean;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.Optionals;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cmdline.CPAMain;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.impact.ImpactAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpv.MPVAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ResultProviderReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProviderImpl;

@Options
public class CPAchecker {

  public interface CPAcheckerMXBean {
    int getReachedSetSize();

    void stop();
  }

  private static class CPAcheckerBean extends AbstractMBean implements CPAcheckerMXBean {

    private final ReachedSet reached;
    private final ShutdownManager shutdownManager;

    CPAcheckerBean(ReachedSet pReached, LogManager logger, ShutdownManager pShutdownManager) {
      super("org.sosy_lab.cpachecker:type=CPAchecker", logger);
      reached = pReached;
      shutdownManager = pShutdownManager;
    }

    @Override
    public int getReachedSetSize() {
      return reached.size();
    }

    @Override
    public void stop() {
      shutdownManager.requestShutdown("A stop request was received via the JMX interface.");
    }
  }

  @Option(
      secure = true,
      name = "analysis.stopAfterError",
      description = "stop after the first error has been found")
  private boolean stopAfterError = true;

  public enum InitialStatesFor {
    /** Function entry node of the entry function */
    ENTRY,

    /** Set of function entry nodes of all functions. */
    FUNCTION_ENTRIES,

    /** All locations that are possible targets of the analysis. */
    TARGET,

    /** Function exit node of the entry function. */
    EXIT,

    /** All function exit nodes of all functions and all loop heads of endless loops. */
    FUNCTION_SINKS,

    /** All function exit nodes of the entry function, and all loop heads of endless loops. */
    PROGRAM_SINKS
  }

  @Option(
      secure = true,
      name = "analysis.initialStatesFor",
      description = "What CFA nodes should be the starting point of the analysis?")
  private Set<InitialStatesFor> initialStatesFor = ImmutableSet.of(InitialStatesFor.ENTRY);

  @Option(
      secure = true,
      name = "analysis.partitionInitialStates",
      description =
          "Partition the initial states based on the type of location they were created for (see"
              + " 'initialStatesFor')")
  private boolean partitionInitialStates = false;

  @Option(
      secure = true,
      name = "specification",
      description =
          "Comma-separated list of files with specifications that should be checked (cf."
              + " config/specification/ for examples). Property files as used in SV-COMP can also"
              + " be used here, but when these are specified inside a configuration file instead of"
              + " on the command line, CPAchecker"
              + " will ignore the entry function in the property file.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private List<Path> specificationFiles = ImmutableList.of();

  @Option(
      secure = true,
      name = "backwardSpecification",
      description =
          "comma-separated list of files with specifications that should be used "
              + "\nin a backwards analysis; used if the analysis starts at the target states!"
              + "\n(see config/specification/ for examples)")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private List<Path> backwardSpecificationFiles = ImmutableList.of();

  @Option(
      secure = true,
      name = "analysis.unknownAsTrue",
      description = "Do not report unknown if analysis terminated, report true (UNSOUND!).")
  private boolean unknownAsTrue = false;

  @Option(
      secure = true,
      name = "analysis.counterexampleLimit",
      description = "Maximum number of counterexamples to be created.")
  private int cexLimit = 0;

  private final LogManager logger;
  private final Configuration config;
  private final ShutdownManager shutdownManager;
  private final ShutdownNotifier shutdownNotifier;
  private final CoreComponentsFactory factory;

  // The content of this String is read from a file that is created by the
  // ant task "init".
  // To change the version, update the property in build.xml.
  private static final String version;

  static {
    String v = "(unknown version)";
    try {
      URL url =
          CPAchecker.class.getClassLoader().getResource("org/sosy_lab/cpachecker/VERSION.txt");
      if (url != null) {
        String content = Resources.toString(url, StandardCharsets.US_ASCII).trim();
        if (content.matches("[a-zA-Z0-9 ._+:-]+")) {
          v = content;
        }
      }
    } catch (IOException e) {
      // Ignore exception, no better idea what to do here.
    }
    version = v;
  }

  /**
   * This class is responsible for retrieving the name of the approach CPAchecker was configured to
   * run with from the {@link Configuration}.
   */
  @Options
  private static final class ApproachNameInformation {
    @Option(
        secure = true,
        name = CPAMain.APPROACH_NAME_OPTION,
        description = "Name of the used analysis, defaults to the name of the used configuration")
    private String approachName;

    private ApproachNameInformation(Configuration pConfig) throws InvalidConfigurationException {
      pConfig.inject(this);
    }

    private String getApproachName() {
      return approachName;
    }
  }

  /**
   * Returns a string that contains the version of CPAchecker as well as information on which
   * analysis is executed.
   */
  public static String getVersion(Configuration pConfig) {
    StringJoiner joiner = new StringJoiner(" / ");
    joiner.add("CPAchecker " + getPlainVersion());
    String analysisName = getApproachName(pConfig);
    if (analysisName != null) {
      joiner.add(analysisName);
    }
    return joiner.toString();
  }

  public static String getPlainVersion() {
    return version;
  }

  /**
   * Returns a string that represents the aproach that CPAchecker runs (typically the name of the
   * properties file).
   *
   * <p>Result can be null if name can not be determined.
   *
   * @param pConfig current config
   * @return approach name
   */
  public static String getApproachName(Configuration pConfig) {
    try {
      return new ApproachNameInformation(pConfig).getApproachName();
    } catch (InvalidConfigurationException e) {
      // Injecting a non-required "secure" String option without restrictions on allowed values
      // actually never fails, and avoiding a throws clause simplifies callers of this method.
      throw new AssertionError(e);
    }
  }

  public static String getJavaInformation() {
    return StandardSystemProperty.JAVA_VM_NAME.value()
        + " "
        + StandardSystemProperty.JAVA_VERSION.value();
  }

  public CPAchecker(
      Configuration pConfiguration, LogManager pLogManager, ShutdownManager pShutdownManager)
      throws InvalidConfigurationException {
    config = pConfiguration;
    logger = pLogManager;
    shutdownManager = pShutdownManager;
    shutdownNotifier = pShutdownManager.getNotifier();

    config.inject(this);
    factory =
        new CoreComponentsFactory(
            pConfiguration, pLogManager, shutdownNotifier, AggregatedReachedSets.empty());
  }

  public CPAcheckerResult run(List<String> programDenotation) {
    checkArgument(!programDenotation.isEmpty());

    logger.logf(Level.INFO, "%s (%s) started", getVersion(config), getJavaInformation());

    MainCPAStatistics stats = null;
    CFA cfa = null;

    final ShutdownRequestListener interruptThreadOnShutdown = interruptCurrentThreadOnShutdown();
    shutdownNotifier.register(interruptThreadOnShutdown);

    try {
      stats = new MainCPAStatistics(config, logger, shutdownNotifier);
      stats.creationTime.start();

      cfa = parse(programDenotation, stats);
      shutdownNotifier.shutdownIfNecessary();

      return run0(cfa, stats);

    } catch (InvalidConfigurationException
        | ParserException
        | IOException
        | InterruptedException e) {
      logErrorMessage(e, logger);
      return new CPAcheckerResult(Result.NOT_YET_STARTED, "", null, cfa, stats);
    } finally {
      shutdownNotifier.unregister(interruptThreadOnShutdown);
    }
  }

  public CPAcheckerResult run(CFA cfa, MainCPAStatistics stats) {
    logger.logf(Level.INFO, "%s (%s) started", getVersion(config), getJavaInformation());

    final ShutdownRequestListener interruptThreadOnShutdown = interruptCurrentThreadOnShutdown();
    shutdownNotifier.register(interruptThreadOnShutdown);

    try {
      return run0(cfa, stats);
    } finally {
      shutdownNotifier.unregister(interruptThreadOnShutdown);
    }
  }

  private CPAcheckerResult run0(CFA cfa, MainCPAStatistics stats) {

    Algorithm algorithm = null;
    ReachedSet reached = null;
    Result result = Result.NOT_YET_STARTED;
    String targetDescription = "";
    Specification specification;

    try {

      // create reached set, cpa, algorithm
      ConfigurableProgramAnalysis cpa;

      // When the run method is called from the main entry run method, the creationTime
      // is already running. In this case, we do not need to start it again.
      if (!stats.creationTime.isRunning()) {
        stats.creationTime.start();
      }
      stats.cpaCreationTime.start();
      try {
        logAboutSpecification();
        specification =
            Specification.fromFiles(specificationFiles, cfa, config, logger, shutdownNotifier);
        cpa = factory.createCPA(cfa, specification);
      } finally {
        stats.cpaCreationTime.stop();
      }
      stats.setCPA(cpa);

      if (cpa instanceof StatisticsProvider statisticsProvider) {
        statisticsProvider.collectStatistics(stats.getSubStatistics());
      }

      algorithm = factory.createAlgorithm(cpa, cfa, specification);

      if (algorithm instanceof MPVAlgorithm && !stopAfterError) {
        // sanity check
        throw new InvalidConfigurationException(
            "Cannot use option 'analysis.stopAfterError' along with "
                + "multi-property verification algorithm. "
                + "Please use option 'mpv.findAllViolations' instead");
      }

      if (algorithm instanceof StatisticsProvider) {
        ((StatisticsProvider) algorithm).collectStatistics(stats.getSubStatistics());
      }

      reached = factory.createReachedSet(cpa);
      if (algorithm instanceof ImpactAlgorithm mcmillan) {
        reached.add(
            mcmillan.getInitialState(cfa.getMainFunction()),
            mcmillan.getInitialPrecision(cfa.getMainFunction()));
      } else {
        initializeReachedSet(reached, cpa, cfa.getMainFunction(), cfa);
      }

      printConfigurationWarnings();

      stats.creationTime.stop();
      shutdownNotifier.shutdownIfNecessary();

      // now everything necessary has been instantiated: run analysis

      result = Result.UNKNOWN; // set to unknown so that the result is correct in case of exception

      AlgorithmStatus status = runAlgorithm(algorithm, reached, stats);

      if (status.wasPropertyChecked()) {
        stats.resultAnalysisTime.start();
        if (reached.wasTargetReached()) {
          targetDescription = Joiner.on(", ").join(reached.getTargetInformation());

          if (!status.isPrecise()) {
            result = Result.UNKNOWN;
          } else {
            result = Result.FALSE;
          }
        } else {
          result = analyzeResult(reached, status.isSound());
          if (unknownAsTrue && result == Result.UNKNOWN) {
            result = Result.TRUE;
          }
        }
        stats.resultAnalysisTime.stop();
      } else {
        result = Result.DONE;
      }
    } catch (InvalidConfigurationException | InterruptedException | CPAException e) {
      logErrorMessage(e, logger);
    } finally {
      CPAs.closeIfPossible(algorithm, logger);
    }
    return new CPAcheckerResult(result, targetDescription, reached, cfa, stats);
  }

  private static void handleParserException(ParserException e, LogManager pLogger) {
    pLogger.logUserException(Level.SEVERE, e, "Parsing failed");
    StringBuilder msg = new StringBuilder();
    msg.append("Please make sure that the code can be compiled by a compiler.\n");
    switch (e.getLanguage()) {
      case C ->
          msg.append(
              """
              If the code was not preprocessed, please use a C preprocessor
              or specify the --preprocess command-line argument.
              """);
      case LLVM ->
          msg.append(
              """
              If you want to use the LLVM frontend, please make sure that
              the code can be compiled by clang or input valid LLVM code.
              """);
      default -> {
        // do not log additional messages
      }
    }
    msg.append(
        """
        If the error still occurs, please send this error message
        together with the input file to cpachecker-users@googlegroups.com.
        """);
    pLogger.log(Level.INFO, msg);
  }

  private static void logErrorMessage(Exception e, LogManager pLogger) {
    if (e instanceof IOException) {
      pLogger.logUserException(Level.SEVERE, e, "Could not read file");
    } else if (e instanceof InvalidConfigurationException) {
      pLogger.logUserException(Level.SEVERE, e, "Invalid configuration");
    } else if (e instanceof ParserException parserException) {
      handleParserException(parserException, pLogger);
    } else if (e instanceof InterruptedException) {
      // CPAchecker must exit because it was asked to
      // we return normally instead of propagating the exception
      // so we can return the partial result we have so far
      pLogger.logUserException(Level.WARNING, e, "Analysis interrupted");
    } else if (e instanceof CPAException) {
      pLogger.logUserException(Level.SEVERE, e, null);
    } else {
      throw new AssertionError("unexpected exception type", e);
    }
  }

  public CFA parse(List<String> fileNames, MainCPAStatistics stats)
      throws InvalidConfigurationException, IOException, ParserException, InterruptedException {

    logger.logf(Level.INFO, "Parsing CFA from file(s) \"%s\"", Joiner.on(", ").join(fileNames));
    CFACreator cfaCreator = new CFACreator(config, logger, shutdownNotifier);
    stats.setCFACreator(cfaCreator);
    final CFA cfa = cfaCreator.parseFileAndCreateCFA(fileNames);
    stats.setCFA(cfa);
    return cfa;
  }

  private void printConfigurationWarnings() {
    Set<String> unusedProperties = config.getUnusedProperties();
    if (!unusedProperties.isEmpty()) {
      logger.log(
          Level.WARNING,
          "The following configuration options were specified but are not used:\n",
          Joiner.on("\n ").join(unusedProperties),
          "\n");
    }
    Set<String> deprecatedProperties = config.getDeprecatedProperties();
    if (!deprecatedProperties.isEmpty()) {
      logger.log(
          Level.WARNING,
          "The following options are deprecated and will be removed in the future:\n",
          Joiner.on("\n ").join(deprecatedProperties),
          "\n");
    }
  }

  private void logAboutSpecification() {
    try {
      Path defaultSpec =
          Classes.getCodeLocation(CPAchecker.class)
              .resolveSibling("config/specification/default.spc");
      if (specificationFiles.size() == 1
          && Files.isSameFile(specificationFiles.get(0), defaultSpec)) {
        logger.log(
            Level.INFO,
            "Using default specification, which checks for assertion failures and error labels.");
      }
    } catch (IOException e) {
      // This method only logs, we do not want this to disturb CPAchecker execution on failure.
      logger.logDebugException(e, "Failed to check whether given spec is default spec.");
    }
  }

  private AlgorithmStatus runAlgorithm(
      final Algorithm algorithm, final ReachedSet reached, final MainCPAStatistics stats)
      throws CPAException, InterruptedException {

    logger.log(Level.INFO, "Starting analysis ...");

    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;

    // register management interface for CPAchecker
    CPAcheckerBean mxbean = new CPAcheckerBean(reached, logger, shutdownManager);
    mxbean.register();

    stats.startAnalysisTimer();
    try {
      int counterExampleCount = 0;
      do {
        status = status.update(algorithm.run(reached));

        if (cexLimit > 0) {
          counterExampleCount =
              Optionals.presentInstances(
                      from(reached)
                          .filter(AbstractStates::isTargetState)
                          .filter(ARGState.class)
                          .transform(ARGState::getCounterexampleInformation))
                  .size();
        }
        // either run only once (if stopAfterError == true)
        // or until the waitlist is empty
        // or until maximum number of counterexamples is reached
      } while (!stopAfterError
          && reached.hasWaitingState()
          && (cexLimit == 0 || cexLimit > counterExampleCount));

      logger.log(Level.INFO, "Stopping analysis ...");
      return status;

    } finally {
      stats.stopAnalysisTimer();

      // unregister management interface for CPAchecker
      mxbean.unregister();
    }
  }

  private Result analyzeResult(final ReachedSet reached, boolean isSound) {
    if (reached instanceof ResultProviderReachedSet resultProviderReachedSet) {
      return resultProviderReachedSet.getOverallResult();
    }
    if (reached.hasWaitingState()) {
      logger.log(Level.WARNING, "Analysis not completed: there are still states to be processed.");
      return Result.UNKNOWN;
    }

    if (!isSound) {
      logger.log(
          Level.WARNING,
          "Analysis incomplete: no errors found, but not everything could be checked.");
      return Result.UNKNOWN;
    }

    return Result.TRUE;
  }

  private void addToInitialReachedSet(
      final Set<? extends CFANode> pLocations,
      final Object pPartitionKey,
      final ReachedSet pReached,
      final ConfigurableProgramAnalysis pCpa)
      throws InterruptedException {

    for (CFANode loc : pLocations) {
      StateSpacePartition putIntoPartition =
          partitionInitialStates
              ? StateSpacePartition.getPartitionWithKey(pPartitionKey)
              : StateSpacePartition.getDefaultPartition();

      AbstractState initialState = pCpa.getInitialState(loc, putIntoPartition);
      Precision initialPrecision = pCpa.getInitialPrecision(loc, putIntoPartition);

      pReached.add(initialState, initialPrecision);
    }
  }

  private void initializeReachedSet(
      final ReachedSet pReached,
      final ConfigurableProgramAnalysis pCpa,
      final FunctionEntryNode pAnalysisEntryFunction,
      final CFA pCfa)
      throws InvalidConfigurationException, InterruptedException {

    logger.log(Level.FINE, "Creating initial reached set");

    for (InitialStatesFor isf : initialStatesFor) {
      final ImmutableSet<? extends CFANode> initialLocations =
          switch (isf) {
            case ENTRY -> ImmutableSet.of(pAnalysisEntryFunction);
            case EXIT -> {
              if (pAnalysisEntryFunction.getExitNode().isEmpty()) {
                logger.logf(
                    Level.SEVERE,
                    "Cannot use exit node of '%s' because it never returns in a normal way"
                        + " (because, e.g., it always aborts the program or always executes an"
                        + " obvious infinite loop)",
                    pAnalysisEntryFunction.getFunction().getOrigName());
              }
              yield Optionals.asSet(pAnalysisEntryFunction.getExitNode());
            }
            case FUNCTION_ENTRIES -> ImmutableSet.copyOf(pCfa.entryNodes());
            case FUNCTION_SINKS ->
                ImmutableSet.<CFANode>builder()
                    .addAll(getAllEndlessLoopHeads(pCfa.getLoopStructure().orElseThrow()))
                    .addAll(getAllFunctionExitNodes(pCfa))
                    .build();
            case PROGRAM_SINKS ->
                ImmutableSet.<CFANode>builder()
                    .addAll(
                        CFAUtils.getProgramSinks(
                            pCfa.getLoopStructure().orElseThrow(), pAnalysisEntryFunction))
                    .build();
            case TARGET ->
                new TargetLocationProviderImpl(shutdownNotifier, logger, pCfa)
                    .tryGetAutomatonTargetLocations(
                        pAnalysisEntryFunction,
                        Specification.fromFiles(
                            backwardSpecificationFiles, pCfa, config, logger, shutdownNotifier));
          };
      addToInitialReachedSet(initialLocations, isf, pReached, pCpa);
    }

    if (!pReached.hasWaitingState()
        && !initialStatesFor.equals(Collections.singleton(InitialStatesFor.TARGET))) {
      throw new InvalidConfigurationException(
          "Initialization of the set of initial states failed: No analysis target found!");
    } else {
      logger.logf(
          Level.FINE,
          "Initial reached set has a waitlist of %d states.",
          pReached.getWaitlist().size());
    }
  }

  private Set<CFANode> getAllFunctionExitNodes(CFA cfa) {
    Set<CFANode> functionExitNodes = new HashSet<>();

    for (FunctionEntryNode node : cfa.entryNodes()) {
      node.getExitNode().ifPresent(functionExitNodes::add);
    }
    return functionExitNodes;
  }

  private Collection<CFANode> getAllEndlessLoopHeads(LoopStructure structure) {
    return CFAUtils.getEndlessLoopHeads(structure);
  }
}
