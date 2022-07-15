// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.channels.ClosedByInterruptException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JArrayType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.MultiStatistics;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

@Options(prefix = "heuristicSelection")
public class SelectionAlgorithm extends NestingAlgorithm {

  private static class SelectionAlgorithmCFAVisitor implements CFAVisitor {

    private final Set<String> functionNames = new HashSet<>();
    private final Set<String> arrayVariables = new HashSet<>();
    private final Set<String> floatVariables = new HashSet<>();

    @Override
    public TraversalProcess visitEdge(CFAEdge pEdge) {
      switch (pEdge.getEdgeType()) {
        case StatementEdge:
          {
            final AStatementEdge edge = (AStatementEdge) pEdge;
            if (edge.getStatement() instanceof AFunctionCall) {
              final AFunctionCall call = (AFunctionCall) edge.getStatement();
              final AExpression exp = call.getFunctionCallExpression().getFunctionNameExpression();
              if (exp instanceof AIdExpression) {
                final AIdExpression id = (AIdExpression) exp;
                functionNames.add(id.getName());
              }
            }
            break;
          }
        case DeclarationEdge:
          {
            final ADeclarationEdge declarationEdge = (ADeclarationEdge) pEdge;
            ADeclaration declaration = declarationEdge.getDeclaration();
            Type declType = declaration.getType();
            Queue<Type> types = new ArrayDeque<>();
            Set<Type> visitedTypes = new HashSet<>();
            types.add(declType);
            while (!types.isEmpty()) {
              Type type = types.poll();
              if (type instanceof CType) {
                type = ((CType) type).getCanonicalType();
              }
              if (visitedTypes.add(type)) {
                if (type instanceof CCompositeType) {
                  CCompositeType compositeType = (CCompositeType) type;
                  for (CCompositeTypeMemberDeclaration member : compositeType.getMembers()) {
                    types.offer(member.getType());
                  }
                }
                if (type instanceof CArrayType || type instanceof JArrayType) {
                  arrayVariables.add(declaration.getQualifiedName());
                } else if (type instanceof CSimpleType) {
                  CSimpleType simpleType = (CSimpleType) type;
                  if (simpleType.getType().isFloatingPointType()) {
                    floatVariables.add(declaration.getQualifiedName());
                  }
                } else if (type instanceof JSimpleType) {
                  JSimpleType simpleType = (JSimpleType) type;
                  if (simpleType.getType().isFloatingPointType()) {
                    floatVariables.add(declaration.getQualifiedName());
                  }
                }
              }
            }
            break;
          }
        case FunctionCallEdge:
        case FunctionReturnEdge:
        case CallToReturnEdge:
        default:
      }
      return TraversalProcess.CONTINUE;
    }

    @Override
    public TraversalProcess visitNode(CFANode pNode) {
      return TraversalProcess.CONTINUE;
    }
  }

  private static class SelectionAlgorithmStatistics extends MultiStatistics {

    private int sizeOfPreAnaReachedSet = 0;
    private String chosenConfig = "";
    private double relevantBoolRatio = 0.0;
    private double relevantAddressedRatio = 0.0;
    private boolean containsExternalFunctionCalls = false;
    private int numberOfAllRightFunctions = 0;
    private boolean requiresOnlyRelevantBoolsHandling = false;
    private boolean requiresAliasHandling = false;
    private boolean requiresLoopHandling = false;
    private boolean requiresCompositeTypeHandling = false;
    private boolean requiresArrayHandling = false;
    private boolean requiresFloatHandling = false;
    private boolean requiresRecursionHandling = false;
    private boolean hasSingleLoop = false;

    SelectionAlgorithmStatistics(LogManager pLogger) {
      super(pLogger);
    }

    @Override
    public String getName() {
      return "Selection Algorithm";
    }

    @Override
    public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
      out.println("Size of preliminary analysis reached set:      " + sizeOfPreAnaReachedSet);
      out.println("Used algorithm property:                       " + chosenConfig);
      out.println(
          "Program containing only relevant bools:        "
              + (requiresOnlyRelevantBoolsHandling ? 1 : 0));
      out.println(
          String.format("Relevant boolean vars / relevant vars ratio:   %.4f", relevantBoolRatio));
      out.println(
          "Requires alias handling:                       " + (requiresAliasHandling ? 1 : 0));
      out.println(
          "Requires loop handling:                        " + (requiresLoopHandling ? 1 : 0));
      out.println(
          "Requires composite-type handling:              "
              + (requiresCompositeTypeHandling ? 1 : 0));
      out.println(
          "Requires array handling:                       " + (requiresArrayHandling ? 1 : 0));
      out.println(
          "Requires float handling:                       " + (requiresFloatHandling ? 1 : 0));
      out.println(
          "Requires recursion handling:                   " + (requiresRecursionHandling ? 1 : 0));
      out.println(
          String.format(
              "Relevant addressed vars / relevant vars ratio: %.4f", relevantAddressedRatio));
      out.println(
          "Program containing external functions:         " + containsExternalFunctionCalls);
      out.println("Number of all righthand side functions:        " + numberOfAllRightFunctions);
      out.println();

      super.printStatistics(out, result, reached);
    }
  }

  private Algorithm preAnalysisAlgorithm;
  private ReachedSet preAnalysisReachedSet;

  private final SelectionAlgorithmStatistics stats;

  @Option(secure = true, description = "Configuration for preliminary algorithm.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path preAnalysisAlgorithmConfig = null;

  @Option(secure = true, description = "Configuration for programs containing recursion.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path recursionConfig;

  @Option(secure = true, description = "Configuration for loop-free programs.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path loopFreeConfig;

  @Option(secure = true, description = "Configuration for programs with a single loop.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path singleLoopConfig;

  @Option(secure = true, required = true, description = "Configuration for programs with loops.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path loopConfig;

  @Option(
      secure = true,
      description = "Configuration for programs with loops and complex datastructures.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path complexLoopConfig;

  @Option(
      secure = true,
      description = "Configuration for programs containing only relevant bool vars.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path onlyBoolConfig;

  @Option(
      secure = true,
      description =
          "Configuration for programs containing more than @Option adressedRatio addressed vars.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path addressedConfig;

  @Option(secure = true, description = "Configuration for programs containing composite types.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path compositeTypeConfig;

  @Option(secure = true, description = "Configuration for programs containing arrays.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path arrayConfig;

  @Option(
      secure = true,
      description =
          "Ratio of addressed vars. Values bigger than the passed value lead to @option"
              + " addressedConfig.")
  private double addressedRatio = 0;

  private final CFA cfa;

  public SelectionAlgorithm(
      CFA pCfa,
      ShutdownNotifier pShutdownNotifier,
      Configuration pConfig,
      Specification pSpecification,
      LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pShutdownNotifier, pSpecification);
    pConfig.inject(this);
    cfa = pCfa;
    stats = new SelectionAlgorithmStatistics(pLogger);
  }

  private AlgorithmStatus performPreAnalysisAlgorithm() throws CPAException, InterruptedException {

    String info = "Performing preliminary analysis algorithm ...";
    logger.log(Level.INFO, info);

    Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> preAnaAlg;
    final Path preAnalysisConfig = preAnalysisAlgorithmConfig;
    ShutdownManager shutdownManager = ShutdownManager.createWithParent(shutdownNotifier);
    try {
      preAnaAlg = createAlgorithm(preAnalysisConfig, cfa.getMainFunction(), cfa, shutdownManager);
    } catch (InvalidConfigurationException e) {
      logger.logUserException(
          Level.WARNING,
          e,
          "Skipping preAnalysisAlgorithm because the configuration file "
              + preAnalysisConfig
              + " is invalid");
      return AlgorithmStatus.UNSOUND_AND_PRECISE;
    } catch (IOException e) {
      String message =
          "Skipping preAnalysisAlgorithm because the configuration file "
              + preAnalysisConfig
              + " could not be read";
      if (shutdownNotifier.shouldShutdown() && e instanceof ClosedByInterruptException) {
        logger.log(Level.WARNING, message);
      } else {
        logger.logUserException(Level.WARNING, e, message);
      }
      return AlgorithmStatus.UNSOUND_AND_PRECISE;
    }

    preAnalysisAlgorithm = preAnaAlg.getFirst();
    preAnalysisReachedSet = preAnaAlg.getThird();

    return preAnalysisAlgorithm.run(preAnalysisReachedSet);
  }

  @SuppressWarnings({"resource", "null"})
  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    checkArgument(
        pReachedSet instanceof ForwardingReachedSet,
        "SelectionAlgorithm needs ForwardingReachedSet");
    checkArgument(
        pReachedSet.size() <= 1,
        "SelectionAlgorithm does not support being called several times with the same reached set");
    checkArgument(!pReachedSet.isEmpty(), "SelectionAlgorithm needs non-empty reached set");
    checkArgument(
        cfa.getVarClassification().isPresent(),
        "SelectionAlgorithm requires variable classification");

    extractStatisticsFromCfa();

    final Path chosenConfig = chooseConfig();

    return run0(pReachedSet, chosenConfig);
  }

  /** analyze the CFA and extract useful statistics. */
  private void extractStatisticsFromCfa() throws CPAException {
    SelectionAlgorithmCFAVisitor visitor = new SelectionAlgorithmCFAVisitor();
    CFANode startingNode = cfa.getMainFunction();
    CFATraversal.dfs().traverseOnce(startingNode, visitor);
    for (String name : visitor.functionNames) {
      if (!cfa.getAllFunctionNames().contains(name)) {
        stats.containsExternalFunctionCalls = true;
      }
    }
    stats.numberOfAllRightFunctions = visitor.functionNames.size();

    // Preliminary algorithm run
    if (preAnalysisAlgorithmConfig != null) {
      try {
        performPreAnalysisAlgorithm();
      } catch (UnsupportedCodeException e) {
        if (e.getMessage().contains("recursion")) {
          stats.requiresRecursionHandling = true;
        }
      } catch (InterruptedException e) {
        // Caught, so that SelectionAlgorithm continues and does not get interrupted completely
        // after pre analysis
      }
      stats.sizeOfPreAnaReachedSet = preAnalysisReachedSet.size();
    }

    Optional<LoopStructure> loopStructure = cfa.getLoopStructure();
    VariableClassification variableClassification = cfa.getVarClassification().orElseThrow();

    if (!variableClassification.getRelevantVariables().isEmpty()) {
      stats.relevantBoolRatio =
          ((double)
                  Sets.intersection(
                          variableClassification.getIntBoolVars(),
                          variableClassification.getRelevantVariables())
                      .size())
              / (double) variableClassification.getRelevantVariables().size();

      stats.relevantAddressedRatio =
          ((double)
                  Sets.intersection(
                          variableClassification.getAddressedVariables(),
                          variableClassification.getRelevantVariables())
                      .size())
              / (double) variableClassification.getRelevantVariables().size();
    }

    stats.requiresOnlyRelevantBoolsHandling =
        variableClassification
            .getIntBoolVars()
            .containsAll(variableClassification.getRelevantVariables());

    stats.requiresAliasHandling =
        !variableClassification.getAddressedVariables().isEmpty()
            || !variableClassification.getAddressedFields().isEmpty();

    stats.requiresLoopHandling =
        !loopStructure.isPresent() || !loopStructure.orElseThrow().getAllLoops().isEmpty();

    stats.requiresCompositeTypeHandling = !variableClassification.getRelevantFields().isEmpty();

    stats.requiresArrayHandling =
        !Collections.disjoint(variableClassification.getRelevantVariables(), visitor.arrayVariables)
            || !Collections.disjoint(
                variableClassification.getAddressedFields().values(), visitor.arrayVariables);

    stats.requiresFloatHandling =
        !Collections.disjoint(variableClassification.getRelevantVariables(), visitor.floatVariables)
            || !Collections.disjoint(
                variableClassification.getAddressedFields().values(), visitor.floatVariables);

    stats.hasSingleLoop =
        loopStructure.isPresent() && loopStructure.orElseThrow().getAllLoops().size() == 1;
  }

  /** use statistical data and choose a configuration for further analysis. */
  private Path chooseConfig() {
    final Path chosenConfig;

    // Perform heuristic
    String info = "Performing heuristic ...";
    logger.log(Level.INFO, info);

    if (stats.requiresRecursionHandling && recursionConfig != null) {
      // Run recursion config
      chosenConfig = recursionConfig;
    } else if (!stats.requiresLoopHandling && loopFreeConfig != null) {
      // Run standard loop-free config
      chosenConfig = loopFreeConfig;
    } else if (stats.requiresOnlyRelevantBoolsHandling && onlyBoolConfig != null) {
      // Run bool only config
      chosenConfig = onlyBoolConfig;
    } else if (stats.relevantAddressedRatio > addressedRatio && addressedConfig != null) {
      chosenConfig = addressedConfig;
      // EXCHANGED
    } else if (stats.requiresCompositeTypeHandling && compositeTypeConfig != null) {
      chosenConfig = compositeTypeConfig;
    } else if (stats.requiresArrayHandling && arrayConfig != null) {
      chosenConfig = arrayConfig;
    } else if ((stats.requiresFloatHandling
            || stats.requiresArrayHandling
            || stats.requiresCompositeTypeHandling)
        && complexLoopConfig != null) {
      // Run complex loop config
      chosenConfig = complexLoopConfig;
    } else if (stats.hasSingleLoop && singleLoopConfig != null) {
      // Run single loop config
      chosenConfig = singleLoopConfig;
    } else {
      // Run standard loop config
      chosenConfig = loopConfig;
    }
    stats.chosenConfig = chosenConfig.toString();
    return chosenConfig;
  }

  /** build all components for the analysis and run the further analysis. */
  private AlgorithmStatus run0(ReachedSet pReachedSet, final Path chosenConfig)
      throws CPAException, InterruptedException {
    Algorithm chosenAlgorithm;
    Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> currentAlg;
    ShutdownManager shutdownManager = ShutdownManager.createWithParent(shutdownNotifier);
    try {
      currentAlg = createAlgorithm(chosenConfig, cfa.getMainFunction(), cfa, shutdownManager);
    } catch (InvalidConfigurationException e) {
      logger.logUserException(
          Level.WARNING,
          e,
          "Skipping SelectionAlgorithm because the configuration file "
              + chosenConfig
              + " is invalid");
      return AlgorithmStatus.UNSOUND_AND_PRECISE;
    } catch (IOException e) {
      String message =
          "Skipping SelectionAlgorithm because the configuration file "
              + chosenConfig
              + " could not be read";
      if (shutdownNotifier.shouldShutdown() && e instanceof ClosedByInterruptException) {
        logger.log(Level.WARNING, message);
      } else {
        logger.logUserException(Level.WARNING, e, message);
      }
      return AlgorithmStatus.UNSOUND_AND_PRECISE;
    }

    chosenAlgorithm = currentAlg.getFirst();
    // ConfigurableProgramAnalysis chosenCpa = currentAlg.getSecond();
    ReachedSet reachedSetForChosenAnalysis = currentAlg.getThird();

    ForwardingReachedSet reached = (ForwardingReachedSet) pReachedSet;
    reached.setDelegate(reachedSetForChosenAnalysis);

    return chosenAlgorithm.run(reachedSetForChosenAnalysis);
  }

  private Triple<Algorithm, ConfigurableProgramAnalysis, ReachedSet> createAlgorithm(
      Path singleConfigFileName,
      CFANode pInitialNode,
      CFA pCfa,
      ShutdownManager singleShutdownManager)
      throws InvalidConfigurationException, CPAException, IOException, InterruptedException {
    AggregatedReachedSets aggregateReached = AggregatedReachedSets.empty();
    return super.createAlgorithm(
        singleConfigFileName,
        pInitialNode,
        pCfa,
        singleShutdownManager,
        aggregateReached,
        ImmutableSet.of("analysis.selectAnalysisHeuristically"),
        stats.getSubStatistics());
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
  }
}
