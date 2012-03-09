/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa;

import static org.sosy_lab.cpachecker.util.CFA.findLoops;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.ast.IASTSimpleDeclSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.objectmodel.AbstractCFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.BlankEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.GlobalDeclarationEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;
import org.sosy_lab.cpachecker.util.CFA.Loop;
import org.sosy_lab.cpachecker.util.predicates.CtoFormulaConverter;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.SortedSetMultimap;

@Options
public class ParallelCFASCreator implements StatisticsProvider{

  @Option(name="analysis.interprocedural",
      description="run interprocedural analysis")
  private boolean interprocedural = true;

  @Option(name="analysis.useGlobalVars",
      description="add declarations for global variables before entry function")
  private boolean useGlobalVars = true;

  /*@Option(name="cfa.removeIrrelevantForErrorLocations",
      description="remove paths from CFA that cannot lead to a error location")*/
  private boolean removeIrrelevantForErrorLocations = false;

  @Option(name="cfa.rg.export",
      description="export CFA as .dot file")
  private boolean exportCfa = true;

  @Option(name="cfa.rg.file",
      description="File path template for exporting  thread CFA to .dot files")
  private String exportCfaFile = "test/output/cfa_";

  @Option(name="cfa.rg.threadFunctions",
      description="Names of main functions for threads. They ought to be uniquee.")
  protected String[] threadFunctions = {"thread0", "thread1", "thread3", "thread4", "thread5", "thread6", "thread7", "thread8", "thread9"};

  public static final String atomicStart       = "START_ATOMIC";
  public static final String atomicStop        = "END_ATOMIC";
  public static final String functionPrefix   = "tid";

  private final CParser parser;
  private final Stats stats;
  private final LogManager logger;
  private final CFAReduction cfaReduction;




  public ParallelCFASCreator(Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this, ParallelCFASCreator.class);

    this.logger = logger;
    this.stats = new Stats();

    stats.parserInstantiationTime.start();
    parser = CParser.Factory.getParser(logger, CParser.Factory.getOptions(config));
    stats.parsingTimer = parser.getParseTime();
    stats.conversionTimer = parser.getCFAConstructionTime();

    if (removeIrrelevantForErrorLocations) {
      cfaReduction = new CFAReduction(config, logger);
    } else {
      cfaReduction = null;
    }

    stats.parserInstantiationTime.stop();
  }


  /**
   * Creates a CFA for each thread. Thread CFA have disjoint node number and their function have unique names.
   *
   * @param filename  The file to parse.
   * @throws InvalidConfigurationException If the main function that was specified in the configuration is not found.
   * @throws IOException If an I/O error occurs.
   * @throws ParserException If the parser or the CFA builder cannot handle the C code.
   * @throws InterruptedException
   * @throws UnrecognizedCFAEdgeException
   */
  public ParallelCFAS createParallelCFAS(String filename) throws InvalidConfigurationException, IOException, ParserException, InterruptedException, UnrecognizedCFAEdgeException {

    stats.totalTimer.start();

    // get the first cfa
    CFA firstCFA = parser.parseFile(filename);
    stats.checkTime.start();
    checkCFAFunctions(firstCFA);
    stats.checkTime.stop();
    List<String> threadNamesList = getThreadNames(firstCFA);
    stats.threadNo = threadNamesList.size();

    // create one CFA per thread
    List<CFA> cfas = new Vector<CFA>(threadNamesList.size());
    cfas.add(firstCFA);
    for (int tid=1; tid<threadNamesList.size(); tid++){
      firstCFA = parser.parseFile(filename);
      cfas.add(firstCFA);
    }

    stats.processingTime.start();

    // get data on global and local variables
    Set<String> globalVars = getGlobalVars(firstCFA);

    // process and create RGCFAs
    List<ThreadCFA> rgcfas = new Vector<ThreadCFA>(threadNamesList.size());

    for (int tid=0; tid<threadNamesList.size(); tid++){
      String threadName = threadNamesList.get(tid);
      CFA cfa = cfas.get(tid);
      addLoopInfo(cfa);
      ThreadCFA tcfa = buildThreadCFA(cfa, threadName, tid);
      addTopologicalInfo(cfa);
      checkCFA(tcfa);
      Pair<ImmutableMultimap<CFAEdge, String>, ImmutableMultimap<CFAEdge, String>> pair = getReadWriteAllEdges(tcfa);
      setOperationTypes(tcfa, globalVars, pair.getFirst(), pair.getSecond());
      rgcfas.add(tcfa);
    }

    stats.processingTime.stop();

    checkNodeNumbers(rgcfas);

    ParallelCFAS pcfa = new ParallelCFAS(rgcfas, globalVars);
    stats.totalTimer.stop();
    return pcfa;
  }


  private List<String> getThreadNames(CFA cfa) throws InvalidConfigurationException {
    // get sorted thread names
    Set<String> threadNames = new HashSet<String>(threadFunctions.length);
    for (int i=0; i<threadFunctions.length; i++){
      threadNames.add(threadFunctions[i]);
    }
    Set<String> funcNames = cfa.getFunctions().keySet();
    threadNames = Sets.intersection(funcNames, threadNames);
    threadNames = new TreeSet(threadNames);
    List<String> threadNamesList = new Vector<String>(threadNames);
    if (threadNames.isEmpty()) {
      throw new InvalidConfigurationException("No thread found");
    }

    return threadNamesList;
  }


  private Set<String> getGlobalVars(CFA cfa) {
    HashSet<String> gvars = new HashSet<String>();

    for (IASTDeclaration gd : cfa.getGlobalDeclarations()){
      if (gd.getDeclSpecifier() != null && gd.getDeclSpecifier() instanceof IASTSimpleDeclSpecifier){
        gvars.add(gd.getName());
      }
    }

    return gvars;
  }



  private Set<String> getLocalVars(CFA cfa) {

    Set<String> lvars = new HashSet<String>();

    for (CFANode node : cfa.cfaNodes.values()){
      for (int i=0; i<node.getNumLeavingEdges(); i++) {
        CFAEdge edge = node.getLeavingEdge(i);

        if (edge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
          DeclarationEdge de = (DeclarationEdge) edge;

          if (!de.isGlobal() && de.getDeclSpecifier() instanceof IASTSimpleDeclSpecifier){
            // TODO use some method
            String function = edge.getPredecessor().getFunctionName();
            String name = CtoFormulaConverter.scoped(de.getName(), function);
            lvars.add(name);
          }
        }
      }
    }

    return lvars;
  }

  /**
   * Returns two mappings from all edges in the cfa: to rhs variables and two lhs variables.
   * @param pCfa
   * @return
   * @throws UnrecognizedCFAEdgeException
   */
  private Pair<ImmutableMultimap<CFAEdge, String>, ImmutableMultimap<CFAEdge, String>> getReadWriteAllEdges(CFA cfa) throws UnrecognizedCFAEdgeException {

    com.google.common.collect.ImmutableMultimap.Builder<CFAEdge, String> lhsBldr = ImmutableMultimap.builder();
    com.google.common.collect.ImmutableMultimap.Builder<CFAEdge, String> rhsBldr = ImmutableMultimap.builder();

    for (CFANode node : cfa.cfaNodes.values()){
      for (int i=0; i<node.getNumLeavingEdges(); i++) {
        CFAEdge edge = node.getLeavingEdge(i);

        CFAEdgeType edgeType = edge.getEdgeType();
        // TODO is declaration a write?
        if (edgeType == CFAEdgeType.StatementEdge    || edgeType == CFAEdgeType.AssumeEdge ||
            edgeType == CFAEdgeType.FunctionCallEdge ) {
          IASTNode iast = edge.getRawAST();
          Pair<Set<String>, Set<String>> vars = getReadWriteVariables(node.getFunctionName(), iast);
          lhsBldr = lhsBldr.putAll(edge, vars.getFirst());
          rhsBldr = rhsBldr.putAll(edge, vars.getSecond());
        }
      }
    }

    return Pair.of(lhsBldr.build(), rhsBldr.build());
  }


  /**
   * Mark edges as global, local, reads and writes.
   * @param cfa
   * @param globalVars
   * @param lhsVars
   * @param rhsVars
   */
  private void setOperationTypes(ThreadCFA cfa, Collection<String> globalVars, Multimap<CFAEdge, String> lhsVars, Multimap<CFAEdge, String> rhsVars) {

    for (CFANode node : cfa.getCFANodes().values()){

      for (int i=0; i<node.getNumLeavingEdges(); i++){
        AbstractCFAEdge edge = (AbstractCFAEdge) node.getLeavingEdge(i);

        Collection<String> lhsNames = lhsVars.get(edge);

        for (String lhsName : lhsNames){
          if (globalVars.contains(lhsName)){
            edge.setGlobalWrite(true);
          } else {
            edge.setLocalWrite(true);
          }
        }

        Collection<String> rhsNames = rhsVars.get(edge);

        for (String rhsName : rhsNames){
          if (globalVars.contains(rhsName)){
            edge.setGlobalRead(true);
          } else {
            edge.setLocalRead(true);
          }
        }

        assert !edge.isGlobalWrite() || !edge.isLocalWrite();
      }

    }

  }

  /**
   * Returns rhs and lhs variables of the expression.
   * @param node
   * @param exp
   * @return
   * @throws UnrecognizedCFAEdgeException
   */
  private Pair<Set<String>, Set<String>> getReadWriteVariables(String function, IASTNode exp) throws UnrecognizedCFAEdgeException {
    Vector<IASTExpression> toProcess = new Vector<IASTExpression>();
    Set<String> rhsVars = new LinkedHashSet<String>();
    Set<String> lhsVars = new LinkedHashSet<String>();

    if (exp instanceof IASTExpressionAssignmentStatement){
      IASTExpressionAssignmentStatement e = (IASTExpressionAssignmentStatement) exp;
      IASTExpression rhs = e.getRightHandSide();
      IASTExpression lhs = e.getLeftHandSide();
      IASTIdExpression lhsId = (IASTIdExpression) lhs;
      String lhsVar = CtoFormulaConverter.scopedIfNecessary(lhsId, function);
      lhsVars.add(lhsVar);
      toProcess.add(rhs);
    } else if (exp instanceof IASTBinaryExpression) {
      toProcess.add((IASTBinaryExpression)exp);
    } else if (exp instanceof IASTFunctionCallAssignmentStatement) {
      IASTFunctionCallAssignmentStatement e = (IASTFunctionCallAssignmentStatement) exp;
      IASTExpression lhs = e.getLeftHandSide();
      IASTIdExpression lhsId = (IASTIdExpression) lhs;
      String lhsVar = CtoFormulaConverter.scopedIfNecessary(lhsId, function);
      lhsVars.add(lhsVar);
    }

    while(! toProcess.isEmpty()){
      IASTExpression e = toProcess.remove(0);
      if (e instanceof IASTIdExpression){
        IASTIdExpression eid = (IASTIdExpression) e;
        String var = CtoFormulaConverter.scopedIfNecessary(eid, function);
        rhsVars.add(var);
      }
      else if (e instanceof IASTBinaryExpression){
        IASTBinaryExpression bin = (IASTBinaryExpression) e;
        toProcess.add(bin.getOperand1());
        toProcess.add(bin.getOperand2());
      } else if (e instanceof IASTUnaryExpression){
        IASTUnaryExpression unary = (IASTUnaryExpression) e;
        toProcess.add(unary.getOperand());
      }
      else if (e instanceof IASTIntegerLiteralExpression){}
      else if (e instanceof IASTCastExpression){
        IASTCastExpression cast = (IASTCastExpression) e;
        toProcess.add(cast.getOperand());
      }
      else {
        throw new UnrecognizedCFAEdgeException("Unrecognized AST type: "+e.getClass());
      }
    }

    return Pair.of(lhsVars, rhsVars);
  }


  private void checkCFAFunctions(CFA c) {
    stats.checkTime.start();

    final Map<String, CFAFunctionDefinitionNode> cfas = c.getFunctions();
    final SortedSetMultimap<String, CFANode> cfaNodes = c.getCFANodes();
    assert cfas.keySet().equals(cfaNodes.keySet());

    // check the CFA of each function
    for (CFAFunctionDefinitionNode cfa : cfas.values()) {
      assert CFACheck.check(cfa, cfaNodes.get(cfa.getFunctionName()));
    }

    stats.checkTime.stop();
  }


  /**
   * Get loop information.
   * @param c
   * @return
   */
  private ImmutableMultimap<String, Loop> addLoopInfo(CFA c) {
    final SortedSetMultimap<String, CFANode> cfaNodes = c.getCFANodes();

    ImmutableMultimap<String, Loop> loopInfo = null;
    try {
      ImmutableMultimap.Builder<String, Loop> loops = ImmutableMultimap.builder();
      for (String functionName : cfaNodes.keySet()) {
        SortedSet<CFANode> nodes = cfaNodes.get(functionName);
        loops.putAll(functionName, findLoops(nodes));
      }
      loopInfo = loops.build();
    } catch (ParserException e) {
      // don't abort here, because if the analysis doesn't need the loop information, we can continue
      logger.log(Level.WARNING, e.getMessage());
    }

    return loopInfo;
  }

  /**
   * Adds a thread prefix to all non-main functions and their nodes.
   * NOTE: it does not change the IAST expression of the edge, which may
   * cause  problems.
   * @param pC
   * @param threadName
   * @param pTid
   */
  private void prefixFunctions(CFA cfa, String threadName, int tid) {

    Collection<String> oldFuncs = new Vector<String>(cfa.getFunctions().keySet());
    oldFuncs.remove(threadName);

    for (String func : oldFuncs){

      String newName = functionPrefix + tid + func;
      // change nodes
      SortedSet<CFANode> nodes = cfa.getCFANodes().get(func);
      nodes = new TreeSet<CFANode>(nodes);

      for (CFANode node : nodes){
        node.setFunctionName(newName);
      }

      // change node set
      cfa.getCFANodes().removeAll(func);
      cfa.getCFANodes().putAll(newName, nodes);

      // change function map
      CFAFunctionDefinitionNode node = cfa.getFunctions().remove(func);
      cfa.getFunctions().put(newName, node);
    }

  }

  /**
   * Annotate CFA nodes with topological information for later use
   * @param c
   */
  private void addTopologicalInfo(CFA c) {
    final Map<String, CFAFunctionDefinitionNode> cfas = c.getFunctions();

    for(CFAFunctionDefinitionNode cfa : cfas.values()){
      CFATopologicalSort topSort = new CFATopologicalSort();
      topSort.topologicalSort(cfa);
    }
  }


  private void checkCFA(ThreadCFA cfa){
    stats.checkTime.start();
    CFAFunctionDefinitionNode threadInit = cfa.getInitalNode();
    assert CFACheck.check(threadInit, null);
    stats.checkTime.stop();

  }


  /**
   * Creates a CFA representing the given thread.
   * @param c
   * @param threadName
   * @param lastNodeNo
   * @return
   * @throws InterruptedException
   * @throws ParserException
   */
  private ThreadCFA buildThreadCFA(CFA c, String threadName, int tid) throws InterruptedException, ParserException {

    Map<String, CFAFunctionDefinitionNode> cfas = c.getFunctions();
    SortedSetMultimap<String, CFANode> nodes = c.getCFANodes();
    CFAFunctionDefinitionNode threadInit = cfas.get(threadName);
    assert threadInit != null;

    if (interprocedural) {
      logger.log(Level.FINE, "Analysis is interprocedural, adding super edges");

      CFASecondPassBuilder spbuilder = new CFASecondPassBuilder(cfas);
      Set<String> calledFunctions = spbuilder.insertCallEdgesRecursively(threadName);

      // remove all functions which are never reached from cfas
      cfas.keySet().retainAll(calledFunctions);
      Set<String> funcToRemove = new HashSet<String>(nodes.keySet());
      funcToRemove.removeAll(calledFunctions);

      for (String func : funcToRemove){
        nodes.removeAll(func);
      }
    }

    List<CFANode> globalDeclNodes;
    CFANode executionStart;

    if (useGlobalVars){
      // add global variables at the beginning of main
      Pair<List<CFANode>, CFANode> pair = insertGlobalDeclarations(c, threadInit, c.getGlobalDeclarations(), logger);
      globalDeclNodes = pair.getFirst();
      executionStart = pair.getSecond();
    } else {
      globalDeclNodes = Collections.emptyList();
      executionStart = null;
    }

    // remove irrelevant locations
    // TODO not working now;
    if (cfaReduction != null) {
      stats.pruningTime.start();
      cfaReduction.removeIrrelevantForErrorLocations(threadInit);
      stats.pruningTime.stop();

      if (threadInit.getNumLeavingEdges() == 0) {
        logger.log(Level.INFO, "No error locations reachable from " + threadInit.getFunctionName()
              + ", analysis not necessary. "
              + "If the code contains no error location named ERROR, set the option cfa.removeIrrelevantForErrorLocations to false.");

        // TODO could do sth, e.g. return empty CFA
      }
    }

    prefixFunctions(c, threadName, tid);
    export(c, threadName);
    Set<String> localVars = getLocalVars(c);
    Collection<CFANode> atomic = getAtomicNodes(executionStart);

    // wrap the CFA as a RGCFA
    ThreadCFA rgCFA = new ThreadCFA(c, tid, globalDeclNodes, atomic, localVars, threadInit, executionStart, threadName);

    return rgCFA;
  }


  /**
   * Export the cfa to a DOT file.
   * @param cfa
   * @param threadName
   */
  private void export(CFA cfa, final String threadName) {

    final Map<String, CFAFunctionDefinitionNode> cfas = cfa.getFunctions();
    final CFAFunctionDefinitionNode threadStart = cfas.get(threadName);
    assert threadStart != null;

    if ((exportCfaFile != null) && (exportCfa)) {
      stats.exportTime.start();


      // execute asynchronously, this may take several seconds for large programs on slow disks
      new Thread(new Runnable() {
        @Override
        public void run() {
          // running the following in parallel is thread-safe
          // because we don't modify the CFA from this point on

          // write CFA to file
          File threadCFAFile = new File(exportCfaFile+threadName+".dot");

          if (exportCfa) {
            try {
              Files.writeFile(threadCFAFile,
                  DOTBuilder.generateDOT(cfas.values(), threadStart));
            } catch (IOException e) {
              logger.log(Level.WARNING,
                  "Could not write CFA to dot file, check configuration option cfa.file! (",
                  e.getMessage() + ")");
              // continue with analysis
            }
          }
        }
      }).start();

      stats.exportTime.stop();
    }

  }


  /**
   * Insert nodes for global declarations after first node of CFA. Returns the nodes of global declarations
   * and the first node after the global declarations.
   * @param cfa
   * @param threadName
   * @param initalNode
   * @param globalVars
   * @param logger
   * @return
   */
  public Pair<List<CFANode>, CFANode> insertGlobalDeclarations(CFA cfa, final CFAFunctionDefinitionNode initalNode, List<IASTDeclaration> globalVars, LogManager logger) {
    if (globalVars.isEmpty()) {
      return null;
    }

    List<CFANode> globalDeclNodes = new Vector<CFANode>();

    // split off first node of CFA
    assert initalNode.getNumLeavingEdges() == 1;
    CFAEdge firstEdge = initalNode.getLeavingEdge(0);
    assert firstEdge instanceof BlankEdge && !firstEdge.isJumpEdge();
    CFANode secondNode = firstEdge.getSuccessor();
    globalDeclNodes.add(initalNode);
    initalNode.removeLeavingEdge(firstEdge);
    secondNode.removeEnteringEdge(firstEdge);



    // insert one node to start the series of declarations
    CFANode cur = new CFANode(0, initalNode.getFunctionName());
    BlankEdge be = new BlankEdge("INIT GLOBAL VARS", 0, initalNode, cur);
    addToCFA(be);

    if (globalVars.isEmpty()){
      globalDeclNodes.add(cur);
    }

    // create a series of GlobalDeclarationEdges, one for each declaration
    for (IASTDeclaration d : globalVars) {
      assert d.isGlobal();
      globalDeclNodes.add(cur);
      CFANode n = new CFANode(d.getFileLocation().getStartingLineNumber(), cur.getFunctionName());
      GlobalDeclarationEdge e = new GlobalDeclarationEdge(d,
          d.getFileLocation().getStartingLineNumber(), cur, n);
      addToCFA(e);

      cur = n;
    }

    // and a blank edge connecting the declarations with the second node of CFA
    be = new BlankEdge(firstEdge.getRawStatement(), firstEdge.getLineNumber(), cur, secondNode);
    addToCFA(be);

    return Pair.of(globalDeclNodes, cur);
  }

  private static void addToCFA(CFAEdge edge) {
    edge.getPredecessor().addLeavingEdge(edge);
    edge.getSuccessor().addEnteringEdge(edge);
  }

 /* Finds and marks nodes where env. transition cannot be applied.
  * private void markNodes(CFA cfa, String threadName, ImmutableMultimap<CFAEdge, String> pImmutableMultimap, ImmutableMultimap<CFAEdge, String> pImmutableMultimap2) throws ParserException {
    stats.processingTimer.start();

    CFAFunctionDefinitionNode threadStart = cfa.getFunctions().get(threadName);
    assert threadStart != null;
    Collection<CFANode> noenvNodes = findNoEnvNodes(threadStart);

    for (CFANode node : noenvNodes){
      node.setEnvApplicationAllowed(false);
    }

    if (useGlobalVars){
      Collection<CFANode> globalDeclNodes = getGlobalDeclNodes(threadStart);

      for (CFANode node : globalDeclNodes){
        node.setEnvApplicationAllowed(false);
      }
    }

    stats.processingTimer.stop();
  }*/


  /**
   * Returns nodes before global declarations.
   * @param pMainFunction
   * @return
   */
  private Collection<CFANode> getGlobalDeclNodes(CFAFunctionDefinitionNode threadStart) {

    List<CFANode> gNodes = new Vector<CFANode>();
    CFANode node = threadStart;
    BlankEdge blankEdge = null;;

    while (blankEdge == null || blankEdge.getRawStatement().equals("INIT GLOBAL VARS")){
      gNodes.add(node);


      assert node.getNumLeavingEdges() == 1;
      CFAEdge edge = node.getLeavingEdge(0);

      if (edge.getEdgeType() == CFAEdgeType.BlankEdge){
        blankEdge = (BlankEdge) edge;
      } else {
        blankEdge = null;
      }

      node = edge.getSuccessor();
    }

    return gNodes;
  }


  /**
   * Gathers information about CFANodes that are between statement edges {@link #atomicStart} and {@link #atomicStop}.
   * In rely-guarantee analysis, no enviormental information can be applied to such nodes. These tags cannot
   * be nested and have to properly enclose nodes.
   * @param start
   * @return
   * @throws ParserException
   */
  private Collection<CFANode> getAtomicNodes(CFANode start) throws ParserException{

    // edges before "_START_NOENV" or after "_END_NOENV"
    Deque<CFANode> toProcessEnv = new ArrayDeque<CFANode>();
    Set<CFANode> seenEnv = new HashSet<CFANode>();
    // edges after "_START_NOENV", but before "_END_NOENV".
    Deque<CFANode> toProcessNoEnv = new ArrayDeque<CFANode>();
    Set<CFANode> seenNoEnv = new HashSet<CFANode>();

    toProcessEnv.push(start);
    while (!toProcessEnv.isEmpty() || !toProcessNoEnv.isEmpty()){
      // search "no env" edges
      while (!toProcessNoEnv.isEmpty()) {
        CFANode n  = toProcessNoEnv.pop();
        seenNoEnv.add(n);
        if (seenEnv.contains(n)){
          throw new ParserException("Mismatched "+atomicStart+ " and "+atomicStop+" tags.");
        }

        for (int i = 0; i < n.getNumLeavingEdges(); ++i) {
          CFAEdge e = n.getLeavingEdge(i);
          CFANode s = e.getSuccessor();
          if (!seenNoEnv.contains(s)){
            if (e.getEdgeType() == CFAEdgeType.StatementEdge){
              if (e.getRawStatement().contains(atomicStop)){
                toProcessEnv.push(s);
              } else if (e.getRawStatement().contains(atomicStart)){
                throw new ParserException("Mismatched "+atomicStart+ " and "+atomicStop+" tags.");
              } else {
                toProcessNoEnv.push(s);
              }
            } else {
              toProcessNoEnv.push(s);
            }

          }
        }
        if (n.getLeavingSummaryEdge() != null) {
          CFANode s = n.getLeavingSummaryEdge().getSuccessor();
          if (!seenEnv.contains(s)) {
            toProcessNoEnv.push(s);
          }
        }
      }
      // search for edges outside the keywords
      while (!toProcessEnv.isEmpty()) {
        CFANode n = toProcessEnv.pop();
        seenEnv.add(n);
        if (seenNoEnv.contains(n)){
          throw new ParserException("Mismatched "+atomicStart+ " and "+atomicStop+" tags.");
        }

        for (int i = 0; i < n.getNumLeavingEdges(); ++i) {
          CFAEdge e = n.getLeavingEdge(i);
          CFANode s = e.getSuccessor();
          if (!seenEnv.contains(s)){
            if (e.getEdgeType() == CFAEdgeType.StatementEdge){
              if (e.getRawStatement().contains(atomicStart)){
                toProcessNoEnv.push(s);
              } else if (e.getRawStatement().contains(atomicStop)){
                throw new ParserException("Mismatched "+atomicStart+ " and "+atomicStop+" tags.");
              } else {
                toProcessEnv.push(s);
              }
            } else {
              toProcessEnv.push(s);
            }
          }


        }
        if (n.getLeavingSummaryEdge() != null) {
          CFANode s = n.getLeavingSummaryEdge().getSuccessor();
          if (!seenEnv.contains(s)) {
            toProcessEnv.push(s);
          }
        }
      }
    }

    return seenNoEnv;
  }




  /**
   * Check if node nodes are unique.
   * @param cfas
   */
  private void checkNodeNumbers(List<ThreadCFA> cfas) {
    stats.checkTime.start();
    Set<Integer> allNodes = new TreeSet<Integer>();

    for (ThreadCFA cfa : cfas){
      Collection<CFANode> nodes = cfa.getCFANodes().values();

      for (CFANode node : nodes){
        int nodeId = node.getNodeNumber();
        assert !allNodes.contains(nodeId);
        allNodes.add(nodeId);
      }
    }

    stats.checkTime.stop();
  }

  /**
   * Renames nodes, such that their numbers start from the given number
   * @param pTcfa
   * @param pLastNodeNo
   */
  private void renamedNodes(CFA tcfa, int startNo) {

    if (startNo == 0){
      return;
    }

    for (CFANode node : tcfa.getCFANodes().values()){
      int newNo = node.getNodeNumber()+startNo;
      node.setNodeNumber(newNo);
    }
  }

  @Override
  public void collectStatistics(Collection<Statistics> statsColl) {
    statsColl.add(stats);
  }


  public static class Stats implements Statistics {

    public final Timer totalTimer               = new Timer();
    public Timer conversionTimer;
    public Timer parsingTimer;
    public final Timer parserInstantiationTime  = new Timer();
    public final Timer checkTime                = new Timer();
    public final Timer pruningTime              = new Timer();
    public final Timer exportTime               = new Timer();
    public final Timer processingTime           = new Timer();
    public int threadNo               = 0;

    @Override
    public void printStatistics(PrintStream out, Result pResult,ReachedSet pReached) {
      out.println("total time on threads CFAs creation:   " + totalTimer);
      out.println("no of thread CFAs:                     " + formatInt(threadNo));
      out.println();
      out.println("time for loading C parser:             " + parserInstantiationTime);
      out.println("time for parsing C file:               " + parsingTimer);
      out.println("time for AST to CFA:                   " + conversionTimer);
      out.println("time for CFA sanity checks:            " + checkTime);
      out.println("time for other processing:             " + processingTime);

  }

  private String formatInt(int val){
    return String.format("  %7d", val);
  }

    @Override
    public String getName() {
      return "RGCFACreator";
    }
  }
}

