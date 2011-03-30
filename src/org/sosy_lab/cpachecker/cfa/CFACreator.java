/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.logging.Level;

import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CParser.Dialect;
import org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration;
import org.sosy_lab.cpachecker.cfa.objectmodel.BlankEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.GlobalDeclarationEdge;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.CFA.Loop;

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
public class CFACreator {

  @Option(name="analysis.entryFunction", regexp="^[_a-zA-Z][_a-zA-Z0-9]*$")
  private String mainFunctionName = "main";

  @Option(name="analysis.interprocedural")
  private boolean interprocedural = true;

  @Option(name="analysis.useGlobalVars")
  private boolean useGlobalVars = true;

  @Option(name="cfa.removeIrrelevantForErrorLocations")
  private boolean removeIrrelevantForErrorLocations = false;

  @Option(name="cfa.export")
  private boolean exportCfa = true;

  @Option(name="cfa.exportPerFunction")
  private boolean exportCfaPerFunction = true;  
  
  @Option(name="cfa.file", type=Option.Type.OUTPUT_FILE)
  private File exportCfaFile = new File("cfa.dot");

  private final LogManager logger;
  private final Configuration config;
  private final CParser parser;
  
  private Map<String, CFAFunctionDefinitionNode> functions;
  private CFAFunctionDefinitionNode mainFunction;
  
  public static ImmutableMultimap<String, Loop> loops = null;

  public final Timer parserInstantiationTime = new Timer();
  public final Timer parsingTime;
  public final Timer conversionTime;
  public final Timer checkTime = new Timer();
  public final Timer processingTime = new Timer();
  public final Timer pruningTime = new Timer();
  public final Timer exportTime = new Timer();
  
  public CFACreator(Dialect dialect, Configuration config, LogManager logger)
          throws InvalidConfigurationException {
    config.inject(this);
    
    this.config = config;
    this.logger = logger;
    
    parserInstantiationTime.start();
    parser = CParser.Factory.getParser(logger, dialect);
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
  public CFAFunctionDefinitionNode getMainFunction() {
    Preconditions.checkState(mainFunction != null);
    return mainFunction;
  }
  
  /**
   * Parse a file and create a CFA, including all post-processing etc.
   * 
   * @param filename  The file to parse.
   * @throws InvalidConfigurationException If the main function that was specified in the configuration is not found. 
   * @throws IOException If an I/O error occurs.
   * @throws ParserException If the parser or the CFA builder cannot handle the C code.
   */
  public void parseFileAndCreateCFA(String filename)
          throws InvalidConfigurationException, IOException, ParserException {

    logger.log(Level.FINE, "Starting parsing of file");
    CFA c = parser.parseFile(filename);
    logger.log(Level.FINE, "Parser Finished");
    
    final Map<String, CFAFunctionDefinitionNode> cfas = c.getFunctions();
    final SortedSetMultimap<String, CFANode> cfaNodes = c.getCFANodes();
    final CFAFunctionDefinitionNode mainFunction = cfas.get(mainFunctionName);
    
    if (mainFunction == null) {
      throw new InvalidConfigurationException("Function " + mainFunctionName + " not found!");
    }

    checkTime.start();
    assert cfas.keySet().equals(cfaNodes.keySet());

    // check the CFA of each function
    for (CFAFunctionDefinitionNode cfa : cfas.values()) {
      assert CFACheck.check(cfa, cfaNodes.get(cfa.getFunctionName()));
    }
    checkTime.stop();
    
    processingTime.start();
    
    // annotate CFA nodes with topological information for later use
    for(CFAFunctionDefinitionNode cfa : cfas.values()){
      CFATopologicalSort topSort = new CFATopologicalSort();
      topSort.topologicalSort(cfa);
    }
    
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
  
    // Insert call and return edges and build the supergraph
    if (interprocedural) {
      logger.log(Level.FINE, "Analysis is interprocedural, adding super edges");
  
      CFASecondPassBuilder spbuilder = new CFASecondPassBuilder(cfas);
      Set<String> calledFunctions = spbuilder.insertCallEdgesRecursively(mainFunctionName);
  
      // remove all functions which are never reached from cfas
      cfas.keySet().retainAll(calledFunctions);
    }
  
    if (useGlobalVars){
      // add global variables at the beginning of main
      insertGlobalDeclarations(mainFunction, c.getGlobalDeclarations(), logger);
    }
    
    processingTime.stop();

    // remove irrelevant locations
    if (removeIrrelevantForErrorLocations) {
      pruningTime.start();
      CFAReduction coi =  new CFAReduction(config, logger);
      coi.removeIrrelevantForErrorLocations(mainFunction);
      pruningTime.stop();
  
      if (mainFunction.getNumLeavingEdges() == 0) {
        logger.log(Level.INFO, "No error locations reachable from " + mainFunction.getFunctionName()
              + ", analysis not necessary. "
              + "If the code contains no error location named ERROR, set the option cfa.removeIrrelevantForErrorLocations to false.");
        
        this.functions = ImmutableMap.of();
        this.mainFunction = null;
        return;
      }
    }
  
    // check the super CFA starting at the main function
    checkTime.start();
    assert CFACheck.check(mainFunction, null);
    checkTime.stop();
 
    if ((exportCfaFile != null) && (exportCfa || exportCfaPerFunction)) {
      exportTime.start();
   
      // execute asynchronously, this may take several seconds for large programs on slow disks
      new Thread(new Runnable() {
        @Override
        public void run() {
          // running the following in parallel is thread-safe
          // because we don't modify the CFA from this point on
          
          // write CFA to file
          if (exportCfa) {
            try {
              Files.writeFile(exportCfaFile,
                  DOTBuilder.generateDOT(cfas.values(), mainFunction));
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
              File outdir = exportCfaFile.getParentFile();        
              DOTBuilder2.writeReport(mainFunction, outdir);
            } catch (IOException e) {        
              logger.log(Level.WARNING,
                "Could not write CFA to dot and json files, check configuration option cfa.file! (",
                e.getMessage() + ")");
              // continue with analysis
            }
          }
        }
      }).start();
      
      exportTime.stop();
    }

    logger.log(Level.FINE, "DONE, CFA for", cfas.size(), "functions created");
    
    this.functions = ImmutableMap.copyOf(cfas);
    this.mainFunction = mainFunction;
  }


  /**
   * Insert nodes for global declarations after first node of CFA.
   */
  public static void insertGlobalDeclarations(final CFAFunctionDefinitionNode cfa, List<IASTDeclaration> globalVars, LogManager logger) {
    if (globalVars.isEmpty()) {
      return;
    }

    // split off first node of CFA
    assert cfa.getNumLeavingEdges() == 1;
    CFAEdge firstEdge = cfa.getLeavingEdge(0);
    assert firstEdge instanceof BlankEdge && !firstEdge.isJumpEdge();
    CFANode secondNode = firstEdge.getSuccessor();

    cfa.removeLeavingEdge(firstEdge);
    secondNode.removeEnteringEdge(firstEdge);
    
    // insert one node to start the series of declarations
    CFANode cur = new CFANode(0, cfa.getFunctionName());
    BlankEdge be = new BlankEdge("INIT GLOBAL VARS", 0, cfa, cur);
    addToCFA(be);

    // create a series of GlobalDeclarationEdges, one for each declaration
    for (IASTDeclaration d : globalVars) {
      assert d.isGlobal();
      
      CFANode n = new CFANode(d.getFileLocation().getStartingLineNumber(), cur.getFunctionName());
      GlobalDeclarationEdge e = new GlobalDeclarationEdge(d,
          d.getFileLocation().getStartingLineNumber(), cur, n);
      addToCFA(e);
      cur = n;
    }

    // and a blank edge connecting the declarations with the second node of CFA
    be = new BlankEdge(firstEdge.getRawStatement(), firstEdge.getLineNumber(), cur, secondNode);
    addToCFA(be);
  }
  
  private static void addToCFA(CFAEdge edge) {
    edge.getPredecessor().addLeavingEdge(edge);
    edge.getSuccessor().addEnteringEdge(edge);
  }
}
