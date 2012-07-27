/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.ast.IADeclaration;
import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.java.EclipseJavaParser;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.CFAUtils.Loop;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;

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

  @Option(name="cfa.useMultiEdges",
      description="combine sequences of simple edges into a single edge")
  private boolean useMultiEdges = false;

  @Option(name="cfa.removeIrrelevantForSpecification",
      description="remove paths from CFA that cannot lead to a specification violation")
  private boolean removeIrrelevantForSpecification = false;

  @Option(name="cfa.export",
      description="export CFA as .dot file")
  private boolean exportCfa = true;

  @Option(name="cfa.exportPerFunction",
      description="export individual CFAs for function as .dot files")
  private boolean exportCfaPerFunction = true;

  @Option(name="cfa.file",
      description="export CFA as .dot file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File exportCfaFile = new File("cfa.dot");

  private final LogManager logger;
  private final CParser parser;
  private final CFAReduction cfaReduction;

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

    if (removeIrrelevantForSpecification) {
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

      ParseResult c;

      if( filename.matches(".*.java")){
        EclipseJavaParser par = new EclipseJavaParser(logger);
        c = par.parseFile(filename);
      } else{
        c = parser.parseFile(filename);
      }


      logger.log(Level.FINE, "Parser Finished");

      if (c.isEmpty()) {
        throw new ParserException("No functions found in program");
      }

      final FunctionEntryNode mainFunction = getMainFunction(filename, c.getFunctions());

      MutableCFA cfa = new MutableCFA(c.getFunctions(), c.getCFANodes(), mainFunction);

      checkTime.start();

      // check the CFA of each function
      for (String functionName : cfa.getAllFunctionNames()) {
        assert CFACheck.check(cfa.getFunctionHead(functionName), cfa.getFunctionNodes(functionName));
      }
      checkTime.stop();

      processingTime.start();

      // annotate CFA nodes with reverse postorder information for later use
      for(FunctionEntryNode function : cfa.getAllFunctionHeads()){
        CFAReversePostorder sorter = new CFAReversePostorder();
        sorter.assignSorting(function);
      }

      // get loop information
      Optional<ImmutableMultimap<String, Loop>> loopStructure = getLoopStructure(cfa);

      // Insert call and return edges and build the supergraph
      if (interprocedural) {
        logger.log(Level.FINE, "Analysis is interprocedural, adding super edges.");

        CFASecondPassBuilder spbuilder = new CFASecondPassBuilder(cfa.getAllFunctions());
        spbuilder.insertCallEdgesRecursively();
      }

      if (useGlobalVars){
        // add global variables at the beginning of main
        insertGlobalDeclarations(cfa, c.getGlobalDeclarations());
      }

      processingTime.stop();

      // remove irrelevant locations
      if (cfaReduction != null) {
        pruningTime.start();
        cfaReduction.removeIrrelevantForSpecification(cfa);
        pruningTime.stop();

        if (cfa.isEmpty()) {
          logger.log(Level.INFO, "No states which violate the specification are syntactically reachable from the function " + mainFunction.getFunctionName()
                + ", analysis not necessary. "
                + "If you want to run the analysis anyway, set the option cfa.removeIrrelevantForSpecification to false.");

          return ImmutableCFA.empty();
        }
      }

      if (useMultiEdges) {
        MultiEdgeCreator.createMultiEdges(cfa);
      }

      final ImmutableCFA immutableCFA = cfa.makeImmutableCFA(loopStructure);

      // check the super CFA starting at the main function
      checkTime.start();
      assert CFACheck.check(mainFunction, null);
      checkTime.stop();

      if ((exportCfaFile != null) && (exportCfa || exportCfaPerFunction)) {
        exportCFA(immutableCFA);
      }

      logger.log(Level.FINE, "DONE, CFA for", immutableCFA.getNumberOfFunctions(), "functions created.");

      return immutableCFA;

    } finally {
      totalTime.stop();
    }
  }

  private FunctionEntryNode getMainFunction(String filename,
      final Map<String, FunctionEntryNode> cfas)
      throws InvalidConfigurationException {

    // try specified function
    FunctionEntryNode mainFunction = cfas.get(mainFunctionName);

    if (mainFunction != null) {
      return mainFunction;
    }

    if (!mainFunctionName.equals("main")) {
      // function explicitly given by user, but not found
      throw new InvalidConfigurationException("Function " + mainFunctionName + " not found.");
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
        throw new InvalidConfigurationException("No entry function found, please specify one.");
      }
      return mainFunction;
    }
  }

  private Optional<ImmutableMultimap<String, Loop>> getLoopStructure(MutableCFA cfa) {
    try {
      ImmutableMultimap.Builder<String, Loop> loops = ImmutableMultimap.builder();
      for (String functionName : cfa.getAllFunctionNames()) {
        SortedSet<CFANode> nodes = cfa.getFunctionNodes(functionName);
        loops.putAll(functionName, findLoops(nodes));
      }
      return Optional.of(loops.build());

    } catch (ParserException e) {
      // don't abort here, because if the analysis doesn't need the loop information, we can continue
      logger.logUserException(Level.WARNING, e, "Could not analyze loop structure of program.");

    } catch (OutOfMemoryError e) {
      logger.logUserException(Level.WARNING, e,
          "Could not analyze loop structure of program due to memory problems");
    }
    return Optional.absent();
  }

  /**
   * Insert nodes for global declarations after first node of CFA.
   */
  public static void insertGlobalDeclarations(final MutableCFA cfa, List<Pair<IADeclaration, String>> globalVars) {
    if (globalVars.isEmpty()) {
      return;
    }

    // split off first node of CFA
    FunctionEntryNode firstNode = cfa.getMainFunction();
    assert firstNode.getNumLeavingEdges() == 1;
    CFAEdge firstEdge = firstNode.getLeavingEdge(0);
    assert firstEdge instanceof BlankEdge;
    CFANode secondNode = firstEdge.getSuccessor();

    CFACreationUtils.removeEdgeFromNodes(firstEdge);

    // insert one node to start the series of declarations
    CFANode cur = new CFANode(0, firstNode.getFunctionName());
    cfa.addNode(cur);
    BlankEdge be = new BlankEdge("", 0, firstNode, cur, "INIT GLOBAL VARS");
    addToCFA(be);

    // create a series of GlobalDeclarationEdges, one for each declaration
    for (Pair< ? extends IADeclaration, String> p : globalVars) {
      IADeclaration d = p.getFirst();
      String rawSignature = p.getSecond();
      assert d.isGlobal();

      CFANode n = new CFANode(d.getFileLocation().getStartingLineNumber(), cur.getFunctionName());
      cfa.addNode(n);
      ADeclarationEdge e = new ADeclarationEdge(rawSignature,
          d.getFileLocation().getStartingLineNumber(), cur, n, d);
      addToCFA(e);
      cur = n;
    }

    // and a blank edge connecting the declarations with the second node of CFA
    be = new BlankEdge(firstEdge.getRawStatement(), firstEdge.getLineNumber(), cur, secondNode, firstEdge.getDescription());
    addToCFA(be);
  }

  private void exportCFA(final CFA cfa) {
    // We used to do this asynchronously.
    // However, FunctionPointerCPA modifies the CFA during analysis, so this is
    // no longer safe.

    exportTime.start();

    // write CFA to file
    if (exportCfa) {
      try {
        Files.writeFile(exportCfaFile,
            DOTBuilder.generateDOT(cfa.getAllFunctionHeads(), cfa.getMainFunction()));
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e,
          "Could not write CFA to dot file.");
        // continue with analysis
      }
    }

    // write the CFA to files (one file per function + some metainfo)
    if (exportCfaPerFunction) {
      try {
        File outdir = exportCfaFile.getParentFile();
        DOTBuilder2.writeReport(cfa, outdir);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e,
          "Could not write CFA to dot and json file.");
        // continue with analysis
      }
    }

    exportTime.stop();
  }

  private static void addToCFA(CFAEdge edge) {
    edge.getPredecessor().addLeavingEdge(edge);
    edge.getSuccessor().addEnteringEdge(edge);
  }
}
