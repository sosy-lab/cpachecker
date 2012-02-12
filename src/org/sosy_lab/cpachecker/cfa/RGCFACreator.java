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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Vector;
import java.util.logging.Level;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration;
import org.sosy_lab.cpachecker.cfa.objectmodel.BlankEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.GlobalDeclarationEdge;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.CFA.Loop;
import org.sosy_lab.cpachecker.util.CFA.NoEnvNodes;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.SortedSetMultimap;

/**
 * Class that encapsulates the whole CFA creation process.
 *
 * It is not thread-safe, but it may be re-used.
 * The get* methods return the result of the last call to {@link #parseFileAndCreateCFA(String)}
 * until this method is called again.
 */
@Options
public class RGCFACreator {

  @Option(name="cfa.relyguarantee.export",
      description="export CFA as .dot file")
  private boolean exportCfa = true;

  @Option(name="cfa.relyguarantee.exportPerFunction",
      description="export individual CFAs for function as .dot files")
  private boolean exportCfaPerFunction = true;

  @Option(name="cfa.relyguarantee.file",
      description="File path template for exporting  thread CFA to .dot files")
  private String exportCfaFile = "test/output/cfa_";

  @Option(name="cfa.relyguarantee.threadFunctions",
      description="Names of main functions for threads. They ought to be uniquee.")
  protected String[] threadFunctions = {"thread0", "thread1", "thread3", "thread4", "thread5", "thread6", "thread7", "thread8", "thread9"};

  private final LogManager logger;
  private final CParser parser;

  private List<CFA> cfas;

  // nodes after global declarations
  private List<CFANode> startNodes;

  private Map<String, CFAFunctionDefinitionNode> functions;
  private List<CFAFunctionDefinitionNode> mainFunctions;
  private List<String> mainFunctionNames;

  //CFAFunctionDefinitionNode mainFunction = null;

  public static List<ImmutableMultimap<String, Loop>> loopList = null;

  public final Timer parserInstantiationTime = new Timer();
  public final Timer totalTime = new Timer();
  public final Timer parsingTime;
  public final Timer conversionTime;
  public final Timer checkTime = new Timer();
  public final Timer processingTime = new Timer();
  public final Timer pruningTime = new Timer();
  public final Timer exportTime = new Timer();

  public RGCFACreator(Configuration config, LogManager logger)
          throws InvalidConfigurationException {
    config.inject(this);

    this.logger = logger;

    parserInstantiationTime.start();
    parser = CParser.Factory.getParser(logger, CParser.Factory.getOptions(config));
    parsingTime = parser.getParseTime();
    conversionTime = parser.getCFAConstructionTime();

    parserInstantiationTime.stop();
  }


  /**
   * Return an immutable map with all function CFAs that are the result of
   * the last call to  {@link #parseFileAndCreateCFA(String)}.
   * @throws IllegalStateException If called before parsing at least once.
   */
  public Map<String, CFAFunctionDefinitionNode> getFunctions() {
    Preconditions.checkState(functions != null);
    return functions;
  }

  /**
   * Return the entry node of the CFA that is the result of
   * the last call to  {@link #parseFileAndCreateCFA(String)}.
   * @throws IllegalStateException If called before parsing at least once.
   */
  /*public List<CFAFunctionDefinitionNode getMainFunction() {
    Preconditions.checkState(mainFunction != null);
    return mainFunction;
  }*/

  /**
   * Parse a file and create a CFA, including all post-processing etc.
   *
   * @param filename  The file to parse.
   * @throws InvalidConfigurationException If the main function that was specified in the configuration is not found.
   * @throws IOException If an I/O error occurs.
   * @throws ParserException If the parser or the CFA builder cannot handle the C code.
   * @throws InterruptedException
   */
  public void parseFileAndCreateCFA(String filename)
          throws InvalidConfigurationException, IOException, ParserException, InterruptedException {

    totalTime.start();
    try {

      logger.log(Level.FINE, "Starting parsing of file");
      CFA c = parser.parseFile(filename);
      // remember the cfa
      //cfa = c;

      logger.log(Level.FINE, "Parser Finished");

      final Map<String, CFAFunctionDefinitionNode> cfaFunctions = c.getFunctions();
      final SortedSetMultimap<String, CFANode> cfaNodes = c.getCFANodes();

      this.functions = ImmutableMap.copyOf(cfaFunctions);

      // collect main thread functions
      mainFunctions     =  new Vector<CFAFunctionDefinitionNode>(threadFunctions.length);
      mainFunctionNames =  new Vector<String>(threadFunctions.length);
      loopList          =  new Vector<ImmutableMultimap<String, Loop>>(threadFunctions.length);

      for (int i=0; i<threadFunctions.length; i++){
        CFAFunctionDefinitionNode func = cfaFunctions.get(threadFunctions[i]);
        if (func != null){
          mainFunctions.add(func);
          mainFunctionNames.add(threadFunctions[i]);
        }
      }

      if (mainFunctions.isEmpty()) {
        throw new InvalidConfigurationException("No thread function name found. Check if option 'cfa.relyguarantee.threadFunctions' is set correctly.");
      }

      checkTime.start();


      assert cfaFunctions.keySet().equals(cfaNodes.keySet());
      assert mainFunctions.size() == mainFunctionNames.size();

      // check the CFA of each function
      for (CFAFunctionDefinitionNode cfa : cfaFunctions.values()) {
        assert CFACheck.check(cfa, cfaNodes.get(cfa.getFunctionName()));
      }

      checkTime.stop();
      processingTime.start();

      // get loop information
        try {
         ImmutableMultimap.Builder<String, Loop> loops = ImmutableMultimap.builder();
         for (String functionName : cfaNodes.keySet()) {
           SortedSet<CFANode> nodes = cfaNodes.get(functionName);
           loops.putAll(functionName, findLoops(nodes));
         }
         CFACreator.loops = loops.build();
       } catch (ParserException e) {
         // don't abort here, because if the analysis doesn't need the loop information, we can continue
         logger.log(Level.WARNING, e.getMessage());
       }


      // process per main thread function
      cfas = new Vector<CFA>(mainFunctions.size());
      startNodes = new Vector<CFANode>(mainFunctions.size());


      for (int i=0; i<mainFunctions.size(); i++){

        CFAFunctionDefinitionNode mainFunction = mainFunctions.get(i);
        String  mainFunctionName  = mainFunctionNames.get(i);

        // make a cfa for each thread
        CFA tCfa = new CFA(c);
        cfas.add(tCfa);
        Map<String, CFAFunctionDefinitionNode> tCfaFunctions = tCfa.getFunctions();
        SortedSetMultimap<String, CFANode> tCfaNodes = tCfa.getCFANodes();

        // annotate CFA nodes with topological information for later use
        for(CFAFunctionDefinitionNode cfa : tCfaFunctions.values()){
          CFATopologicalSort topSort = new CFATopologicalSort();
          topSort.topologicalSort(cfa);
        }

        // get information about about nodes between _START_NOENV and _END_NOENV tags
        Set<CFANode> noenvNodes = NoEnvNodes.getNoEnvNodes(mainFunction);
        for (CFANode node : noenvNodes){
          node.setEnvAllowed(false);
        }

        // create a supergraph for each thread
        logger.log(Level.FINE, "Adding super edges");

        CFASecondPassBuilder spbuilder = new CFASecondPassBuilder(tCfaFunctions);
        Set<String> calledFunctions = spbuilder.insertCallEdgesRecursively(mainFunctionName);

        // remove subtrees which are not called by the function
        Set<String> funcToRemove = new HashSet<String>(tCfaFunctions.keySet());
        funcToRemove.removeAll(calledFunctions);
        for (String name : funcToRemove){
          CFAFunctionDefinitionNode node = tCfaFunctions.get(name);
          // remove incoming edges
          for (int j=0; j<node.getNumEnteringEdges(); j++){
            CFAEdge edge = node.getEnteringEdge(j);
            CFANode pred = edge.getPredecessor();
            pred.removeLeavingEdge(edge);
          }
          // remove the node
          tCfaNodes.removeAll(name);
        }
        tCfaFunctions.keySet().retainAll(calledFunctions);

        // add global declarations
        insertGlobalDeclarations(tCfa, mainFunction, i, logger);

        //Files.writeFile(exportCfaFile, DOTBuilder.generateDOT(tCfa.getFunctions().values(), mainFunction));
      }

      processingTime.stop();

      assert cfas.size() == mainFunctions.size();

      // check the super CFAs starting at the main function
      checkTime.start();
      for (CFAFunctionDefinitionNode mainFunction : mainFunctions){
        assert CFACheck.check(mainFunction, null);
      }
      checkTime.stop();

      // export
      if ((exportCfaFile != null) && (exportCfa || exportCfaPerFunction)) {
        exportTime.start();
        // execute asynchronously, this may take several seconds for large programs on slow disks
        for (int i=0; i<mainFunctions.size(); i++){
          String fileName = exportCfaFile+mainFunctionNames.get(i)+".dot";
          File path = new File(fileName);
          exportCfaThread thread = new exportCfaThread(cfas.get(i).getFunctions().values(), mainFunctions.get(i), path);
          new Thread(thread).run();
        }
        exportTime.stop();
      }



    } finally {
      totalTime.stop();
    }
  }



  public List<CFA> getCfas() {
    return cfas;
  }

  public List<CFANode> getStartNodes(){
    return startNodes;
  }


  public List<CFAFunctionDefinitionNode> getMainFunctions() {
    return mainFunctions;
  }


  /**
   * Insert nodes for global declarations after first node of CFA.
   * @param cfa
   * @param i
   */
  public void insertGlobalDeclarations(CFA cfa, final CFAFunctionDefinitionNode mainFunction, int i, LogManager logger) {
    List<IASTDeclaration> globalVars = cfa.getGlobalDeclarations();

    if (globalVars.isEmpty()) {
      return;
    }

    // split off first node of CFA
    assert mainFunction.getNumLeavingEdges() == 1;
    CFAEdge firstEdge = mainFunction.getLeavingEdge(0);
    assert firstEdge instanceof BlankEdge && !firstEdge.isJumpEdge();
    CFANode secondNode = firstEdge.getSuccessor();

    mainFunction.removeLeavingEdge(firstEdge);
    secondNode.removeEnteringEdge(firstEdge);

    mainFunction.setGeneratesEnv(false);

    // insert one node to start the series of declarations
    CFANode cur = new CFANode(0, mainFunction.getFunctionName());
    cur.setGeneratesEnv(false);
    cfa.addNode(cur);
    BlankEdge be = new BlankEdge("INIT GLOBAL VARS", 0, mainFunction, cur);

    addToCFA(be);


    // create a series of GlobalDeclarationEdges, one for each declaration
    for (IASTDeclaration d : globalVars) {
      assert d.isGlobal();

      CFANode n = new CFANode(d.getFileLocation().getStartingLineNumber(), cur.getFunctionName());
      n.setGeneratesEnv(false);
      cfa.addNode(n);
      GlobalDeclarationEdge e = new GlobalDeclarationEdge(d,
          d.getFileLocation().getStartingLineNumber(), cur, n);
      addToCFA(e);
      cur = n;
    }

    // and a blank edge connecting the declarations with the second node of CFA
    be = new BlankEdge(firstEdge.getRawStatement(), firstEdge.getLineNumber(), cur, secondNode);
    addToCFA(be);

    // rember where the program  starts
    startNodes.add(i, cur);
  }

  private static void addToCFA(CFAEdge edge) {
    edge.getPredecessor().addLeavingEdge(edge);
    edge.getSuccessor().addEnteringEdge(edge);
  }



  private class exportCfaThread implements Runnable{

    private Collection<CFAFunctionDefinitionNode> functions;
    private CFAFunctionDefinitionNode mainFunction;
    private File path;

    public exportCfaThread(Collection<CFAFunctionDefinitionNode> functions, CFAFunctionDefinitionNode mainFunction, File path){
      this.functions = functions;
      this.mainFunction = mainFunction;
      this.path = path;
    }

    @Override
    public void run() {
      // running the following in parallel is thread-safe
      // because we don't modify the CFA from this point on

      // write CFA to a file
      if (exportCfa) {
        try {
          Files.writeFile(path,
              DOTBuilder.generateDOT(functions, mainFunction));
        } catch (IOException e) {
          logger.log(Level.WARNING,
            "Could not write CFA to dot file, check configuration option cfa.file! (",
            e.getMessage() + ")");
          // continue with analysis
        }
      }

      // write the CFA to files (one file per function + some metainfo)
      if (exportCfaPerFunction) {
        try {
          File outdir = path.getParentFile();
          DOTBuilder2.writeReport(mainFunction, outdir);
        } catch (IOException e) {
          logger.log(Level.WARNING,
            "Could not write CFA to dot and json files, check configuration option cfa.file! (",
            e.getMessage() + ")");
          // continue with analysis
        }
      }
    }

  }
}

