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
package org.sosy_lab.cpachecker.util.blocking;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

@Options(prefix="controlflow.statistics")
public class ControlFlowStatistics {

  @Option(description="write the statistics on the control-flow structure to a csv-file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File writeToFile = new File("StructureMeasures.csv");

  private final LogManager logger;

  public static class FunctionStats {
    public int nodeCount = 0;
    public int edgeCount = 0;
    public int branchCount = 0;

    public int blankEdges = 0;
    public int assumeEdges = 0;
    public int statementEdges = 0;
    public int declarationEdges = 0;
    public int returnStatementEdges = 0;
    public int functionCallEdges = 0;
    public int functionReturnEdges = 0;
    public int callToReturnEdges = 0;
    public int clusterChanges = 0;
    public int topoBackEdges = 0;
  }

  public ControlFlowStatistics(Configuration pConfig, LogManager pLogger) throws InvalidConfigurationException {
    pConfig.inject(this);
    this.logger = pLogger;
  }

  public Map<CFAFunctionDefinitionNode, FunctionStats> getFunctionFlowStats(CFA pCfa) {
    Map<CFAFunctionDefinitionNode, FunctionStats> result = new HashMap<CFAFunctionDefinitionNode, FunctionStats>();
    for (CFANode node : pCfa.getAllNodes()) {
      CFAFunctionDefinitionNode fnDefNode = pCfa.getFunctionHead(node.getFunctionName());
      assert(fnDefNode != null);

      FunctionStats fnStat = result.get(fnDefNode);
      if (fnStat == null) {
        fnStat = new FunctionStats();
        result.put(fnDefNode, fnStat);
      }

      fnStat.nodeCount++;
      int numberOfLeavingEdges = node.getNumLeavingEdges();
      if (numberOfLeavingEdges > 1) {
        fnStat.branchCount++;
      }
      for (int i=0; i<numberOfLeavingEdges; i++) {
        CFAEdge outgoingEdge = node.getLeavingEdge(i);
        switch(outgoingEdge.getEdgeType()) {
          case AssumeEdge: fnStat.assumeEdges++; break;
          case BlankEdge: fnStat.blankEdges++; break;
          case CallToReturnEdge: fnStat.callToReturnEdges++; break;
          case DeclarationEdge: fnStat.declarationEdges++; break;
          case FunctionCallEdge: fnStat.functionCallEdges++; break;
          case FunctionReturnEdge: fnStat.functionReturnEdges++; break;
          case ReturnStatementEdge: fnStat.returnStatementEdges++; break;
          case StatementEdge: fnStat.statementEdges++; break;
        }
        fnStat.edgeCount++;
        if (outgoingEdge.getSuccessor() != null) {
          if (outgoingEdge.getSuccessor().getStrictTopoSortId() < node.getStrictTopoSortId()) {
            fnStat.topoBackEdges++;
          }
        }
      }
    }

    return result;
  }

  public void writeStructureMeasures(CFA pCfa, File pTargetFile) throws IOException {
    if (pTargetFile == null) {
      return;
    }

    Map<CFAFunctionDefinitionNode, FunctionStats> functionStats = getFunctionFlowStats(pCfa);

    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(pTargetFile)));

    String[] csvFields = new String[]{
        "Functionname",
        "Nodes",
        "Edges",
        "Branches",
        "BlankEdges",
        "AssumeEdges",
        "StatementEdges",
        "DeclarationEdges",
        "ReturnStatementEdges",
        "FunctionCallEdges",
        "FunctionReturnEdges",
        "CallToReturnEdges",
        "TopoBackEdges"};

    // Write CSV header
    for (String field : csvFields) {
      out.write(String.format("%s\t", field));
    }
    out.write("\n");

    // Write CSV data
    for (CFAFunctionDefinitionNode fnDefNode : functionStats.keySet()) {
      FunctionStats fs = functionStats.get(fnDefNode);
      Object[] line = new Object[]{
          fnDefNode.getFunctionName(),
          fs.nodeCount,
          fs.edgeCount,
          fs.branchCount,
          fs.blankEdges,
          fs.assumeEdges,
          fs.statementEdges,
          fs.declarationEdges,
          fs.returnStatementEdges,
          fs.functionCallEdges,
          fs.functionReturnEdges,
          fs.callToReturnEdges,
          fs.topoBackEdges};

      for (Object value : line) {
        out.write(String.format("%s\t", value.toString()));
      }
      out.write("\n");
    }

    out.flush();
    out.close();
  }

  public void writeStatistics(CFA pCfa) {
    try {
      writeStructureMeasures(pCfa, writeToFile);
    } catch (IOException e) {
      logger.logException(Level.WARNING, e, "Error while writing the control-flow statistics!");
    }
  }
}
