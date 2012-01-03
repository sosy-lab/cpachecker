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
package org.sosy_lab.cpachecker.util.clustering;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

import org.sosy_lab.ccvisu.MinimizerBarnesHut;
import org.sosy_lab.ccvisu.Options.OptionsEnum;
import org.sosy_lab.ccvisu.clustering.ClustererMinDistPerc;
import org.sosy_lab.ccvisu.graph.GraphData;
import org.sosy_lab.ccvisu.graph.GraphEdge;
import org.sosy_lab.ccvisu.graph.GraphVertex;
import org.sosy_lab.ccvisu.graph.Group;
import org.sosy_lab.ccvisu.graph.Group.GroupKind;
import org.sosy_lab.ccvisu.writers.WriterDataLayoutLAY;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;
import org.sosy_lab.cpachecker.util.clustering.interfaces.Clusterer;

@Options(prefix="clusterer.ccvisu")
public class CCVisuClusterer implements Clusterer {

  @Option(description="Merge partitions within distance (percent of layout diagonal; e.g. 0.05).")
  private float clusterMergeDistancePercent = 0.05f;

  @Option(description="Stop clustering on min. distance (percent of layout diagonal; e.g. 0.4; 1 means one cluster for each node).")
  private float clusterMinDistancePercent = 0.4f;

  @Option(description="(max) number of clusters to create.")
  private int clusterMaxNumber = 0;

  @Option(description="Average number of nodes in a cluster. Determines the parameter clusterMaxNumber.")
  private int avgNodesInCluster = 0;

  @Option(description="Calculate a deterministic initial input-layout for the minimizer?")
  private boolean deterministicInitialLayout = false;

  @Option(description="Write the layout of the clustered callgraph to this file.")
  private String writeLayoutToFileName = "";

  @Option(description="number of iterations to do by the minimizer.")
  private int minimizerIterations = 300;

  private final ClusteringStatistics statistics;
  private final LogManager logger;
  private final HashMap<String, String> clustersOfFunctions;
  private final HashMap<CFANode,String> clustersOfNodes;
  private int nodeIdSeqNo = 0;

  public CCVisuClusterer(Configuration pConfig, LogManager pLogger, ClusteringStatistics pStatistics) throws InvalidConfigurationException {
    pConfig.inject(this);
    this.logger = pLogger;
    this.statistics = pStatistics;
    this.clustersOfFunctions = new HashMap<String, String>();
    this.clustersOfNodes = new HashMap<CFANode, String>();
  }

  private GraphVertex createCallGraphVertex (String pFunctionName) {
    GraphVertex result = new GraphVertex(pFunctionName);
    result.setId(nodeIdSeqNo++);
    if (deterministicInitialLayout) {
      result.getPosition().x = (pFunctionName.hashCode() % 3019) * 10000 + nodeIdSeqNo;
      result.getPosition().y = (pFunctionName.hashCode() + 1 % 2287) * 10000 + nodeIdSeqNo;
    } else {
      result.getPosition().x = 2 * (float) Math.random() - 1;
      result.getPosition().y = 2 * (float) Math.random() - 1;
    }

    return result;
  }

  /**
   * Build a call-graph out of a CFA.
   * @param pCfa  The CFA.
   * @return      GraphData that can be processed by CCVisu.
   */
  private GraphData extractCallGraph (CFA pCfa) {
    GraphData result = new GraphData();

    CFAFunctionDefinitionNode mainNode = pCfa.getMainFunction();
    GraphVertex rootNode = new GraphVertex(mainNode.getFunctionName());
    result.insertVertex(rootNode);

    for (CFANode node : pCfa.getAllNodes()) {
        int leavingEdges = node.getNumLeavingEdges();
        for (int i=0; i<leavingEdges; i++) {
          CFAEdge edge = node.getLeavingEdge(i);

          if (edge instanceof FunctionCallEdge) {
            String callerFunctionName = node.getFunctionName();
            String calledFunctionName = ((FunctionCallEdge) edge).getSuccessor().getFunctionName();
            if (pCfa.getFunctionHead(calledFunctionName) != null) {

              GraphVertex callerVertex = result.getVertexByName(callerFunctionName);
              if (callerVertex == null) {
                callerVertex = createCallGraphVertex(callerFunctionName);
                result.insertVertex(callerVertex);
              }

              GraphVertex calledVertex = result.getVertexByName(calledFunctionName);
              if (calledVertex == null) {
                calledVertex = createCallGraphVertex(calledFunctionName);
                result.insertVertex(calledVertex);
              }

              GraphEdge callEdge = new GraphEdge(calledFunctionName, callerVertex, calledVertex, 1);
              result.insertEdge(callEdge);

              System.out.println("CALL\t" + callerFunctionName + "\t" + calledFunctionName);
            }
          } else if (edge instanceof StatementEdge) {
//            IASTStatement statement = ((StatementEdge) edge).getStatement();
//            if (statement instanceof IASTFunctionCall) {
//              IASTFunctionCallExpression f = ((IASTFunctionCall)statement).getFunctionCallExpression();
//              String functionName = f.getFunctionNameExpression().getRawSignature();
//            }
          }
      }
    }

    result.initGroups();
    return result;
  }

  @Override
  public void buildClustering(CFA pCfa) {
    // Extract the call-graph.
    GraphData graph = extractCallGraph(pCfa);

    // Run the minimizer to get a layout.
    org.sosy_lab.ccvisu.Options options = new org.sosy_lab.ccvisu.Options();
    options.graph = graph;
    options.getOption(OptionsEnum.iter).set(minimizerIterations);

    MinimizerBarnesHut minimizer = new MinimizerBarnesHut(options);
    minimizer.minimizeEnergy();

    // Maybe we should write the layout to a file.
    if (!writeLayoutToFileName.trim().isEmpty()) {
      try {
        PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(writeLayoutToFileName)));
        WriterDataLayoutLAY writerLay = new WriterDataLayoutLAY(out, graph, options);
        writerLay.write();
        out.flush();
        out.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    // Do the clustering based on the layout.
    clustersOfFunctions.clear();
    clustersOfNodes.clear();
    try {
      int maxNumberOfClusters = this.clusterMaxNumber;
      if (avgNodesInCluster > 0) {
        maxNumberOfClusters = graph.getVertices().size() / avgNodesInCluster;
      }

      ClustererMinDistPerc clusterer = new ClustererMinDistPerc (
            graph,
            maxNumberOfClusters,
            this.clusterMergeDistancePercent,
            this.clusterMinDistancePercent);

      clusterer.runClusteringAndUpdateGroups(graph);

      for (int i = 0; i<graph.getNumberOfGroups(); i++) {
        Group group = graph.getGroup(i);
        if (group.getKind().equals(GroupKind.CLUSTER)) {
          for (GraphVertex vertex :  group.getNodes()) {
            System.out.println(group.getName() + " : " + vertex.getName());
            clustersOfFunctions.put(vertex.getName(), group.getName());
          }
        }
      }

    } catch (InterruptedException e) {
      logger.logDebugException(e);
    }

    // Initialize clustering statistics.
    statistics.setClusterer(this);
    statistics.setClusteredGraph(graph);
    statistics.printStatistics(System.out, null, null);
  }

  @Override
  public String getClusterOfNode(CFANode pCallNode) {
    return clustersOfFunctions.get( pCallNode.getFunctionName());
  }

}
