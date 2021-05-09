// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm;


import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.cfa.model.CFAEdgeType.StatementEdge;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.collect.TreeMultimap;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.Optionals;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.AnnotatedValue;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.counterexample.CFAPathWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.EdgeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.ForwardingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.NodeCollectingCFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultLocalizationInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;
import org.sosy_lab.java_smt.api.SolverException;

@Options(prefix = "programRepair")
public class AutomaticProgramRepair
    implements Algorithm, StatisticsProvider, Statistics {

  private final FaultLocalizationWithTraceFormula algorithm;
  private final Configuration config;
  private final LogManager logger;
  private final CFA cfa;
  private final Specification specification;
  private final ShutdownNotifier shutdownNotifier;


  private final StatTimer totalTime = new StatTimer("Total time for bug repair");

  @Option(
    secure = true,
    required = true,
    description = "Config file of the internal analysis.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private AnnotatedValue<Path> internalAnalysisConfigFile;

  public AutomaticProgramRepair(
      final Algorithm pStoreAlgorithm,
      final Configuration pConfig,
      final LogManager pLogger,
      final CFA pCfa,
      final Specification pSpecification,
      final ShutdownNotifier pShutdownNotifier
  )
      throws InvalidConfigurationException {

    if (!(pStoreAlgorithm instanceof FaultLocalizationWithTraceFormula)){
      throw new InvalidConfigurationException("option FaultLocalizationWithTraceFormula required, found: " + pStoreAlgorithm.getClass());
    }
    config = pConfig;
    algorithm = (FaultLocalizationWithTraceFormula) pStoreAlgorithm;
    cfa = pCfa;
    logger = pLogger;
    specification = pSpecification;
    shutdownNotifier = pShutdownNotifier;
    config.inject(this);
  }

  @Override
  public AlgorithmStatus run(ReachedSet reachedSet) throws CPAException, InterruptedException {
    totalTime.start();
    AlgorithmStatus status = algorithm.getAlgorithm().run(reachedSet);

    try {
      logger.log(Level.INFO, "Starting bug repair...");

      ArrayList<FaultLocalizationInfo> faultLocalizationInfos = localizeFaults(reachedSet);

      faultLocalizationInfos.forEach(faultLocalizationInfo -> {
        try {
          runAlgorithm(faultLocalizationInfo, reachedSet);
        } catch (Exception e) {
          logger.logUserException(Level.SEVERE, e, "Exception");
        }
      });

      logger.log(Level.INFO, "Stopping bug repair...");
    } catch (Exception e) {
      logger.logUserException(Level.SEVERE, e, "Invalid configuration");
    } finally{
      totalTime.stop();
    }

    return status;
  }

  private void runAlgorithm(FaultLocalizationInfo faultLocalizationInfo, ReachedSet reachedSet)
      throws Exception {
    final MutableCFA clonedCFA = cloneCFA();

    logger.log(Level.INFO, "hasViolatedProperties:" + reachedSet.hasViolatedProperties());
    MutableCFA mutatedCFA = clonedCFA;

//    for (Fault fault : faultLocalizationInfo.getRankedList()){

    /* MUTATION START */
    Fault fault = faultLocalizationInfo.getRankedList().get(0);
    FaultContribution faultContribution = fault.iterator().next();
    CFAEdge edge = faultContribution.correspondingEdge();
    CFAEdge newEdge = mutateEdge(edge);

    mutatedCFA = exchangeEdge(clonedCFA, edge, newEdge);
    /* MUTATION END */

//    }

    // this call will use the original reachedSet and return violated properties, regardless of mutation
    //final ReachedSet newReachedSet = rerun(mutatedCFA, reachedSet);

    // this call will generate a new reachedSet and return no violated properties, regardless of mutation

    final ReachedSet newReachedSet = rerun1(mutatedCFA);

    logger.log(Level.INFO, "hasViolatedProperties:" + newReachedSet.hasViolatedProperties());
  }


  private CFAEdge mutateEdge(CFAEdge edge){
    if(edge.getEdgeType() == StatementEdge){
      final CStatementEdge statementEdge = (CStatementEdge) edge;
      CStatement statement = statementEdge.getStatement();

      if(statement instanceof CExpressionAssignmentStatement){

        final CExpressionAssignmentStatement expressionAssignmentStatement = (CExpressionAssignmentStatement) statement;
        final Set<CExpression> expressions = collectExpressions();
        CExpression alternativeExpression = expressionAssignmentStatement.getRightHandSide();

        for(CExpression expression : expressions){
          if(expression.toString().equals("temp")
              && expressionAssignmentStatement.getLeftHandSide().toString().equals("second")){
            alternativeExpression = expression;
          }
        }

        final CStatement modifiedStatement =
            new CExpressionAssignmentStatement(expressionAssignmentStatement.getFileLocation(), expressionAssignmentStatement.getLeftHandSide(), alternativeExpression) ;

        logger.log(Level.INFO, "original Statement: " + statement.toString());
        logger.log(Level.INFO, "modified Statement: " + modifiedStatement.toString());

        return new CStatementEdge(statementEdge.getRawStatement(), modifiedStatement,
            statementEdge.getFileLocation(), statementEdge.getPredecessor(), statementEdge.getSuccessor());
      }
    }

    return edge;
  }

  private ReachedSet rerun(MutableCFA mutatedCFA, ReachedSet reachedSet)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    final ReachedSetFactory reachedSetFactory = new ReachedSetFactory(config, logger);
    final CPABuilder builder = new CPABuilder(config, logger, shutdownNotifier, reachedSetFactory);
    final ConfigurableProgramAnalysis mutatedCPA = builder.buildCPAs(mutatedCFA, specification, new AggregatedReachedSets(Set.of(reachedSet)));
    final CPAAlgorithm algo = CPAAlgorithm.create(mutatedCPA, logger, config, shutdownNotifier);

    algo.run(reachedSet);

    return reachedSet;
  }

  private ReachedSet rerun(MutableCFA mutatedCFA)
      throws CPAException, InterruptedException, InvalidConfigurationException {
    final ReachedSetFactory reachedSetFactory = new ReachedSetFactory(config, logger);
    final CPABuilder builder = new CPABuilder(config, logger, shutdownNotifier, reachedSetFactory);
    ReachedSet reachedSet = reachedSetFactory.create();
    final ConfigurableProgramAnalysis mutatedCPA = builder.buildCPAs(mutatedCFA, specification, new AggregatedReachedSets(Set.of(reachedSet)));
    final CPAAlgorithm algo = CPAAlgorithm.create(mutatedCPA, logger, config, shutdownNotifier);

    algo.run(reachedSet);

    return reachedSet;

  }

  private ReachedSet rerun1(MutableCFA mutatedCFA)
      throws CPAException, InterruptedException, InvalidConfigurationException, IOException {
    Configuration internalAnylysisConfig =
        buildSubConfig(internalAnalysisConfigFile.value());

    CoreComponentsFactory coreComponents =
        new CoreComponentsFactory(
            internalAnylysisConfig,
            logger,
            shutdownNotifier,
            new AggregatedReachedSets());

    ConfigurableProgramAnalysis cpa = coreComponents.createCPA(cfa, specification);
    GlobalInfo.getInstance().setUpInfoFromCPA(cpa);
    // TODO add a proper check
    FaultLocalizationWithTraceFormula algo =
        (FaultLocalizationWithTraceFormula) coreComponents.createAlgorithm(cpa, cfa, specification);
    ReachedSet reached =
        createInitialReachedSet(cpa, cfa.getMainFunction(), coreComponents, logger);

    algo.getAlgorithm().run(reached);

    return reached;

  }

  // TODO temp solution: copied from NestingAlgorithm
  private Configuration buildSubConfig(Path singleConfigFileName)
      throws IOException, InvalidConfigurationException {

    ConfigurationBuilder singleConfigBuilder = Configuration.builder();

    // TODO next line overrides existing options with options loaded from file.
    // Perhaps we want to keep some global options like 'specification'?
    singleConfigBuilder.loadFromFile(singleConfigFileName);

    Configuration singleConfig = singleConfigBuilder.build();
    // checkConfigs(globalConfig, singleConfig, singleConfigFileName, logger);
    return singleConfig;
  }

  // TODO temp solution: copied from NestingAlgorithm
  private ReachedSet createInitialReachedSet(
      ConfigurableProgramAnalysis cpa,
      CFANode mainFunction,
      CoreComponentsFactory pFactory,
      LogManager singleLogger)
      throws InterruptedException {
    singleLogger.log(Level.FINE, "Creating initial reached set");

    AbstractState initialState =
        cpa.getInitialState(mainFunction, StateSpacePartition.getDefaultPartition());
    Precision initialPrecision =
        cpa.getInitialPrecision(mainFunction, StateSpacePartition.getDefaultPartition());

    ReachedSet reached = pFactory.createReachedSet();
    reached.add(initialState, initialPrecision);
    return reached;
  }

  private MutableCFA cloneCFA() {

    final TreeMultimap<String, CFANode> nodes = TreeMultimap.create();

    for (final String function : cfa.getAllFunctionNames()) {
      nodes.putAll(function, CFATraversal.dfs().collectNodesReachableFrom(cfa.getFunctionHead(function)));
    }

    return new MutableCFA(cfa.getMachineModel(),
        cfa.getAllFunctions(),
        nodes,
        cfa.getMainFunction(),
        cfa.getFileNames(),
        cfa.getLanguage());
  }

  private MutableCFA exchangeEdge(MutableCFA currentCFA, CFAEdge edgeToRemove, CFAEdge edgeToInsert){
    final CFANode predecessorNode = edgeToRemove.getPredecessor();
    final CFANode successorNode = edgeToRemove.getSuccessor();

    for(int i = 0; i <  predecessorNode.getNumLeavingEdges(); i++){
      final CFAEdge edge = predecessorNode.getLeavingEdge(i);

      if(edge.getLineNumber() == edgeToInsert.getLineNumber()){
        predecessorNode.removeLeavingEdge(edge);
        predecessorNode.addLeavingEdge(edgeToInsert);
      }
    }

    for(int a = 0; a <  successorNode.getNumEnteringEdges(); a++){
      final CFAEdge edge = successorNode.getEnteringEdge(a);

      if(edge.getLineNumber() == edgeToInsert.getLineNumber()){
        successorNode.removeEnteringEdge(edge);
        successorNode.addEnteringEdge(edgeToInsert);
      }
    }

    return currentCFA;
  }


  private MutableCFA mutateCFA(MutableCFA currentCFA, List<CFANode> repairCandidateNodes)
      throws Exception {

    for (CFANode node : repairCandidateNodes) {
      if (shouldDelete()) {
        deleteNode(currentCFA, node);
      }

      if(shouldInsert()){
        insertNode(currentCFA, node);
      }
    }
    return currentCFA;
  }

  /**
  * Deletes a given node from a cfa, along with all nodes that can be reached from it */
  private void deleteNode(MutableCFA currentCFA, CFANode node){
    final Set<CFANode> reachableNodes = Sets.newHashSet();

    reachableNodes.addAll(CFATraversal.dfs().collectNodesReachableFrom(node));

    for (CFANode reachableNode : reachableNodes) {
      currentCFA.removeNode(reachableNode);
      logger.log(Level.INFO, "Deleted " + reachableNode.toString());
    }
  }

  /**
  * Inserts a random node from a given CFA after a given node. */
  private void insertNode(MutableCFA currentCFA, CFANode predecessorNode) throws Exception {
    final Collection<CFANode> originalNodes = cfa.getAllNodes();
    final int insertionNodeIndex = new Random().nextInt(originalNodes.size());
    final CFANode insertionNode = Iterables.get(originalNodes, insertionNodeIndex);

    CFANode successorNode = null;

    for(int i = 0; i <  predecessorNode.getNumLeavingEdges(); i++){
      final CFAEdge edge = predecessorNode.getLeavingEdge(i);
      predecessorNode.removeLeavingEdge(edge);

      final CFAEdge predecessorNodeNewLeavingEdge = changeConnectedNodes(edge, edge.getPredecessor(), insertionNode);
      predecessorNode.addLeavingEdge(predecessorNodeNewLeavingEdge);

      successorNode = edge.getSuccessor();
    }

    for(int a = 0; a <  insertionNode.getNumLeavingEdges(); a++){
      final CFAEdge edge = insertionNode.getLeavingEdge(a);
      logger.log(Level.INFO, "Edge " + edge.toString());

      insertionNode.removeLeavingEdge(edge);
      final CFAEdge predecessorNodeNewLeavingEdge = changeConnectedNodes(edge, insertionNode, successorNode);
      insertionNode.addLeavingEdge(predecessorNodeNewLeavingEdge);
    }

    currentCFA.addNode(insertionNode);
  }

  /* TODO:
      - it is assumed that the language is C - an error should be thrown if this is not the case
      - In cases where special conditions have to be met, the insertion should not take place if the conditions don't apply, instead of throwing an error
  */

  private CFAEdge changeConnectedNodes(CFAEdge edge, CFANode predecessor , CFANode successor)
      throws Exception {
    switch (edge.getEdgeType()) {

      case AssumeEdge:
        final CAssumeEdge assumeEdge = (CAssumeEdge) edge;
          return new CAssumeEdge(assumeEdge.getRawStatement(), assumeEdge.getFileLocation(), predecessor,
              successor, assumeEdge.getExpression(),assumeEdge.getTruthAssumption());

      case FunctionCallEdge:
        final CFunctionCallEdge functionCallEdge = (CFunctionCallEdge) edge;
        final Optional<CFunctionCall> functionCall = functionCallEdge.getRawAST();

        if (functionCall.isPresent()){
          return new CFunctionCallEdge(functionCallEdge.getRawStatement(),
              functionCallEdge.getFileLocation(), predecessor, (CFunctionEntryNode) successor,
              functionCall.get(), functionCallEdge.getSummaryEdge());
        } else {
          /* TODO throw proper error */
          throw new Exception("Cannot extract functional call: " + successor.getClass());
        }

      case FunctionReturnEdge:
        final CFunctionReturnEdge functionReturnEdge = (CFunctionReturnEdge) edge;
        /* TODO reconsider casting predecessor */
        return new CFunctionReturnEdge(functionReturnEdge.getFileLocation(),
            (FunctionExitNode) predecessor, successor, functionReturnEdge.getSummaryEdge());

      case DeclarationEdge:
        final CDeclarationEdge declarationEdge = (CDeclarationEdge) edge;
        return new CDeclarationEdge(declarationEdge.getRawStatement(), declarationEdge.getFileLocation(),
      declarationEdge.getPredecessor(), declarationEdge.getSuccessor(), declarationEdge.getDeclaration());

      case StatementEdge:
        final CStatementEdge statementEdge = (CStatementEdge) edge;
        return new CStatementEdge(statementEdge.getRawStatement(), statementEdge.getStatement(),
            statementEdge.getFileLocation(), predecessor, successor);

      case ReturnStatementEdge:
        final CReturnStatementEdge returnStatementEdge = (CReturnStatementEdge) edge;
        final Optional<CReturnStatement> optionalReturnStatement = returnStatementEdge.getRawAST();

        if(optionalReturnStatement.isPresent()){
          /* TODO reconsider casting successor */
          return new CReturnStatementEdge(returnStatementEdge.getRawStatement(), optionalReturnStatement.get(),
              returnStatementEdge.getFileLocation(), predecessor, (FunctionExitNode)  successor);
        } else {
          /* TODO throw proper error */
          throw new Exception("Cannot extract return statement");
        }

      case BlankEdge:
        final BlankEdge blankEdge = (BlankEdge) edge;
        return new BlankEdge(blankEdge.getRawStatement(), blankEdge.getFileLocation(),  predecessor,
          successor, blankEdge.getDescription());

      case CallToReturnEdge:
        final CFunctionSummaryEdge functionSummaryEdge = (CFunctionSummaryEdge) edge;

        return new CFunctionSummaryEdge(functionSummaryEdge.getRawStatement(), functionSummaryEdge.getFileLocation(),
            predecessor, successor, functionSummaryEdge.getExpression(),
            functionSummaryEdge.getFunctionEntry());
      default:
        throw new UnrecognizedCFAEdgeException(edge);
    }
  }

  private static class BasicCollector extends ForwardingCFAVisitor {
    Iterable<? extends AAstNode> astNodes = FluentIterable.of() ;


    public BasicCollector() {
      super(new NodeCollectingCFAVisitor());
    }

    @Override
    public TraversalProcess visitEdge(CFAEdge edge) {
      Iterable<? extends AAstNode> candidates =
          FluentIterable.from(CFAUtils.getAstNodesFromCfaEdge(edge))
              .transformAndConcat(CFAUtils::traverseRecursively);

      astNodes =
          Iterables.concat(astNodes, candidates);

      return super.visitEdge(edge);
    }


    private Iterable<? extends AAstNode> getAstNodes() {
      return astNodes;
    }

  }

  private Iterable<? extends AAstNode> collectAstNodes(CFA currentCFA){
    final BasicCollector expressionCollector = new BasicCollector();
    CFATraversal.dfs().ignoreSummaryEdges().traverseOnce(currentCFA.getMainFunction(), expressionCollector);
    final Iterable<? extends AAstNode> expressions =  expressionCollector.getAstNodes();

    for(AAstNode expression : expressions) {
      logger.log(Level.INFO, expression.toString());
    }

    return expressions;
  }


  private static class ExpressionCollector extends ForwardingCFAVisitor {
    private Set<CExpression> expressions = Sets.newHashSet();

    public ExpressionCollector() {
      super(new EdgeCollectingCFAVisitor());
    }

    @Override
    public TraversalProcess visitEdge(CFAEdge edge) {
      ImmutableSet<? extends CExpression> currentExpressions =
          FluentIterable.from(CFAUtils.getAstNodesFromCfaEdge(edge))
              .transformAndConcat(CFAUtils::traverseRecursively)
              .filter(CExpression.class)
              .toSet();


      expressions.addAll(currentExpressions);

      return super.visitEdge(edge);
    }

    private Set<CExpression> getExpressions() {
      return expressions;
    }
  }

  private Set<CExpression> collectExpressions(){
    final ExpressionCollector expressionCollector = new ExpressionCollector();
    CFATraversal.dfs().ignoreSummaryEdges().traverseOnce(cfa.getMainFunction(), expressionCollector);
    final Set<CExpression> expressions =  expressionCollector.getExpressions();

/*
    for(CExpression expression : expressions) {
      logger.log(Level.INFO, expression.toString());
    }
*/

    return expressions;
  }

  private boolean shouldDelete() {
    return Math.random() >= 1;
  }

  private boolean shouldInsert() {
    return false;
  }
  private ArrayList<FaultLocalizationInfo> localizeFaults(ReachedSet reachedSet)
      throws InterruptedException, InvalidConfigurationException, SolverException, CPAException {
    algorithm.checkOptions();

    ArrayList<FaultLocalizationInfo> faultLocalizationInfos = new ArrayList<>();

    FluentIterable<CounterexampleInfo> counterExamples =
        Optionals.presentInstances(
            from(reachedSet)
                .filter(AbstractStates::isTargetState)
                .filter(ARGState.class)
                .transform(ARGState::getCounterexampleInformation));

      // run algorithm for every error
      logger.log(Level.INFO, "Starting fault localization...");
      for (CounterexampleInfo info : counterExamples) {
        logger.log(Level.INFO, "Find explanations for fault #" + info.getUniqueId());

        CFAPathWithAssumptions assumptions = info.getCFAPathWithAssignments();
        Optional<FaultLocalizationInfo>
            optionalFaultLocalizationInfo = algorithm.calcFaultLocalizationInfo(assumptions, info, algorithm.getFaultAlgorithm());

        if(optionalFaultLocalizationInfo.isPresent()){
          faultLocalizationInfos.add(optionalFaultLocalizationInfo.get());
        }

      }
      logger.log(Level.INFO, "Stopping fault localization...");
      return faultLocalizationInfos;
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsCollection) {
    statsCollection.add(this);
    if (algorithm instanceof Statistics) {
      statsCollection.add(algorithm);
    }
    if (algorithm instanceof StatisticsProvider) {
      ((StatisticsProvider) algorithm).collectStatistics(statsCollection);
    }
  }

  @Override
  public void printStatistics(
      PrintStream out, Result result, UnmodifiableReachedSet reached) {
    StatisticsWriter.writingStatisticsTo(out).put(totalTime);
  }

  @Override
  public @Nullable String getName() {
    return getClass().getSimpleName();
  }


}
