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

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.sosy_lab.common.Files;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.exceptions.CFAGenerationRuntimeException;

import com.google.common.collect.ImmutableMap;

/**
 * Class that encapsulates the whole CFA creation process.
 */
@Options
public class CFACreator {

  @Option(name="analysis.entryFunction", regexp="^[_a-zA-Z][_a-zA-Z0-9]*$")
  private String mainFunctionName = "main";

  @Option(name="analysis.noExternalCalls")
  private boolean noExternalCalls = true;

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
  
  private Map<String, CFAFunctionDefinitionNode> functions;
  private CFAFunctionDefinitionNode mainFunction;
  
  public final Timer conversionTime = new Timer();
  public final Timer processingTime = new Timer();
  public final Timer pruningTime = new Timer();
  public final Timer exportTime = new Timer();
  
  public CFACreator(Configuration config, LogManager logger) throws InvalidConfigurationException {
    config.inject(this);
    
    this.config = config;
    this.logger = logger;
  }

  public Map<String, CFAFunctionDefinitionNode> getFunctions() {
    return functions;
  }
  
  public CFAFunctionDefinitionNode getMainFunction() {
    return mainFunction;
  }
  
  public void createCFA(IASTTranslationUnit ast) throws InvalidConfigurationException, CFAGenerationRuntimeException {
  
    // Build CFA
    conversionTime.start();
    final CFABuilder builder = new CFABuilder(logger);
    ast.accept(builder);
    conversionTime.stop();
  
    final Map<String, CFAFunctionDefinitionNode> cfas = builder.getCFAs();
    final CFAFunctionDefinitionNode mainFunction = cfas.get(mainFunctionName);
    
    if (mainFunction == null) {
      throw new InvalidConfigurationException("Function " + mainFunctionName + " not found!");
    }
    
    processingTime.start();
    
    // annotate CFA nodes with topological information for later use
    for(CFAFunctionDefinitionNode cfa : cfas.values()){
      CFATopologicalSort topSort = new CFATopologicalSort();
      topSort.topologicalSort(cfa);
    }
  
    // Insert call and return edges and build the supergraph
    if (interprocedural) {
      logger.log(Level.FINE, "Analysis is interprocedural, adding super edges");
  
      CFASecondPassBuilder spbuilder = new CFASecondPassBuilder(cfas, noExternalCalls);
      Set<String> calledFunctions = spbuilder.insertCallEdgesRecursively(mainFunctionName);
  
      // remove all functions which are never reached from cfas
      cfas.keySet().retainAll(calledFunctions);
    }
  
    if (useGlobalVars){
      // add global variables at the beginning of main
      CFABuilder.insertGlobalDeclarations(mainFunction, builder.getGlobalDeclarations(), logger);
    }
    
    processingTime.stop();

    // check the CFA of each function
    for (CFAFunctionDefinitionNode cfa : cfas.values()) {
      assert CFACheck.check(cfa);
    }
    
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
    assert CFACheck.check(mainFunction);
 
    exportTime.start();
    
    // write CFA to file
    if (exportCfa && exportCfaFile != null) {
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
    if (exportCfaPerFunction && exportCfaFile != null) {
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
    
    exportTime.stop();
    
    logger.log(Level.FINE, "DONE, CFA for", cfas.size(), "functions created");
  
    this.functions = ImmutableMap.copyOf(cfas);
    this.mainFunction = mainFunction;
  }

}