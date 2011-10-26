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

import static org.sosy_lab.cpachecker.util.CFAUtils.findLoops;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
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
import org.sosy_lab.cpachecker.util.CFAUtils.Loop;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.SortedSetMultimap;

/**
 * Class that encapsulates the whole CFA creation process.
 *
 * It is not thread-safe, but it may be re-used.
 */
@Options
public class CFACreator {

  @Option(name="analysis.entryFunction", regexp="^[_a-zA-Z][_a-zA-Z0-9]*$",
      description="entry function")
  private String mainFunctionName = "main";

  @Option(name="analysis.interprocedural",
      description="run interprocedural analysis")
  private boolean interprocedural = true;

  @Option(name="analysis.useGlobalVars",
      description="add declarations for global variables before entry function")
  private boolean useGlobalVars = true;

  @Option(name="cfa.removeIrrelevantForErrorLocations",
      description="remove paths from CFA that cannot lead to a error location")
  private boolean removeIrrelevantForErrorLocations = false;

  @Option(name="cfa.export",
      description="export CFA as .dot file")
  private boolean exportCfa = true;

  @Option(name="cfa.exportPerFunction",
      description="export individual CFAs for function as .dot files")
  private boolean exportCfaPerFunction = true;

  @Option(name="cfa.file", type=Option.Type.OUTPUT_FILE,
      description="export CFA as .dot file")
  private File exportCfaFile = new File("cfa.dot");

  private final LogManager logger;
  private final CParser parser;
  private final CFAReduction cfaReduction;

  @Deprecated // use CFA#getLoopStructure() instead
  public static ImmutableMultimap<String, Loop> loops = null;

  public final Timer parserInstantiationTime = new Timer();
  public final Timer totalTime = new Timer();
  public final Timer parsingTime;
  public final Timer conversionTime;
  public final Timer checkTime = new Timer();
  public final Timer processingTime = new Timer();
  public final Timer pruningTime = new Timer();
  public final Timer exportTime = new Timer();

  public CFACreator(Configuration config, LogManager logger)
          throws InvalidConfigurationException {
    config.inject(this);

    this.logger = logger;

    parserInstantiationTime.start();
    parser = CParser.Factory.getParser(logger, CParser.Factory.getOptions(config));
    parsingTime = parser.getParseTime();
    conversionTime = parser.getCFAConstructionTime();

    if (removeIrrelevantForErrorLocations) {
      cfaReduction = new CFAReduction(config, logger);
    } else {
      cfaReduction = null;
    }

    parserInstantiationTime.stop();
  }

  /**
   * Parse a file and create a CFA, including all post-processing etc.
   *
   * @param filename  The file to parse.
   * @return A representation of the CFA.
   * @throws InvalidConfigurationException If the main function that was specified in the configuration is not found.
   * @throws IOException If an I/O error occurs.
   * @throws ParserException If the parser or the CFA builder cannot handle the C code.
   * @throws InterruptedException
   */
  public CFA parseFileAndCreateCFA(String filename)
          throws InvalidConfigurationException, IOException, ParserException, InterruptedException {

    totalTime.start();
    try {

      logger.log(Level.FINE, "Starting parsing of file");
      ParseResult c = parser.parseFile(filename);
      logger.log(Level.FINE, "Parser Finished");

      final Map<String, CFAFunctionDefinitionNode> cfas = c.getFunctions();
      final SortedSetMultimap<String, CFANode> cfaNodes = c.getCFANodes();

      if (cfas.isEmpty()) {
        throw new ParserException("No functions found in program");
      }

      final CFAFunctionDefinitionNode mainFunction = getMainFunction(filename, cfas);

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
      Optional<ImmutableMultimap<String, Loop>> loopStructure;
      try {
        ImmutableMultimap.Builder<String, Loop> loops = ImmutableMultimap.builder();
        for (String functionName : cfaNodes.keySet()) {
          SortedSet<CFANode> nodes = cfaNodes.get(functionName);
          loops.putAll(functionName, findLoops(nodes));
        }
        loopStructure = Optional.of(loops.build());
      } catch (ParserException e) {
        // don't abort here, because if the analysis doesn't need the loop information, we can continue
        logger.logUserException(Level.WARNING, e, "Could not analyze loop structure of program");
        loopStructure = Optional.absent();
      }
      CFACreator.loops = loopStructure.orNull();

      // Insert call and return edges and build the supergraph
      if (interprocedural) {
        logger.log(Level.FINE, "Analysis is interprocedural, adding super edges");

        CFASecondPassBuilder spbuilder = new CFASecondPassBuilder(cfas);
        spbuilder.insertCallEdgesRecursively();
      }

      if (useGlobalVars){
        // add global variables at the beginning of main
        insertGlobalDeclarations(mainFunction, c.getGlobalDeclarations(), logger);
      }

      processingTime.stop();

      // remove irrelevant locations
      if (cfaReduction != null) {
        pruningTime.start();
        cfaReduction.removeIrrelevantForErrorLocations(new CFA(cfas, mainFunction, loopStructure));
        pruningTime.stop();

        if (mainFunction.getNumLeavingEdges() == 0) {
          logger.log(Level.INFO, "No error locations reachable from " + mainFunction.getFunctionName()
                + ", analysis not necessary. "
                + "If the code contains no error location named ERROR, set the option cfa.removeIrrelevantForErrorLocations to false.");

          return CFA.empty();
        }
      }

      // check the super CFA starting at the main function
      checkTime.start();
      assert CFACheck.check(mainFunction, null);
      checkTime.stop();

      if ((exportCfaFile != null) && (exportCfa || exportCfaPerFunction)) {

        // execute asynchronously, this may take several seconds for large programs on slow disks
        new Thread(new Runnable() {
          @Override
          public void run() {
            exportTime.start();

            // running the following in parallel is thread-safe
            // because we don't modify the CFA from this point on

            // write CFA to file
            if (exportCfa) {
              try {
                Files.writeFile(exportCfaFile,
                    DOTBuilder.generateDOT(cfas.values(), mainFunction));
              } catch (IOException e) {
                logger.logUserException(Level.WARNING, e,
                  "Could not write CFA to dot file");
                // continue with analysis
              }
            }

            // write the CFA to files (one file per function + some metainfo)
            if (exportCfaPerFunction) {
              try {
                File outdir = exportCfaFile.getParentFile();
                DOTBuilder2.writeReport(mainFunction, outdir);
              } catch (IOException e) {
                logger.logUserException(Level.WARNING, e,
                  "Could not write CFA to dot and json file");
                // continue with analysis
              }
            }

            exportTime.stop();
          }
        }, "CFA export thread").start();
      }

      logger.log(Level.FINE, "DONE, CFA for", cfas.size(), "functions created");

      return new CFA(cfas, mainFunction, loopStructure);

    } finally {
      totalTime.stop();
    }
  }

  private CFAFunctionDefinitionNode getMainFunction(String filename,
      final Map<String, CFAFunctionDefinitionNode> cfas)
      throws InvalidConfigurationException {

    // try specified function
    CFAFunctionDefinitionNode mainFunction = cfas.get(mainFunctionName);

    if (mainFunction != null) {
      return mainFunction;
    }

    if (!mainFunctionName.equals("main")) {
      // function explicitly given by user, but not found
      throw new InvalidConfigurationException("Function " + mainFunctionName + " not found!");
    }

    if (cfas.size() == 1) {
      // only one function available, take this one
      return Iterables.getOnlyElement(cfas.values());

    } else {
      // get the AAA part out of a filename like test/program/AAA.cil.c
      filename = (new File(filename)).getName(); // remove directory

      int indexOfDot = filename.indexOf('.');
      String baseFilename = indexOfDot >= 1 ? filename.substring(0, indexOfDot) : filename;

      // try function with same name as file
      mainFunction = cfas.get(baseFilename);

      if (mainFunction == null) {
        throw new InvalidConfigurationException("No entry function found, please specify one!");
      }
      return mainFunction;
    }
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

    CFACreationUtils.removeEdgeFromNodes(firstEdge);

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
