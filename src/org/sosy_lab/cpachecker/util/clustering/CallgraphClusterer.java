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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.ccvisu.Options.OptionsEnum;
import org.sosy_lab.ccvisu.clustering.ClustererMinDistPerc;
import org.sosy_lab.ccvisu.graph.GraphData;
import org.sosy_lab.ccvisu.graph.GraphEdge;
import org.sosy_lab.ccvisu.graph.GraphVertex;
import org.sosy_lab.ccvisu.graph.Group;
import org.sosy_lab.ccvisu.graph.Group.GroupKind;
import org.sosy_lab.ccvisu.layout.TwoPhaseMinimizer;
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
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;

import com.google.common.collect.Multimap;


@Options(prefix="clusterer.callgraph")
public class CallgraphClusterer extends AbstractGraphClusterer {

  @Option(description="Merge partitions within distance (percent of layout diagonal; e.g. 0.05).")
  private float clusterMergeDistancePercent = 0.05f;

  @Option(description="Stop clustering on min. distance (percent of layout diagonal; e.g. 0.4; 1 means one cluster for each node).")
  private float clusterMinDistancePercent = 0.3f;

  @Option(description="(max) number of clusters to create.")
  private int clusterMaxNumber = 0;

  @Option(description="Average number of nodes in a cluster. Determines the parameter clusterMaxNumber.")
  private int avgNodesInCluster = 0;

  @Option(description="Calculate a deterministic initial input-layout for the minimizer?")
  private boolean deterministicInitialLayout = false;

  @Option(name="graphLayoutFile", description="Write the layout of the clustered callgraph to this file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File graphLayoutFile = new File("CallGraph.lay");

  @Option(name="inputClusteringFile", description="Load the (precomputed) clusters from a external file.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private File inputClusteringFile = new File("InputClustering.crsf");

  @Option(name="callGraphFile", description="write the call-graph to the specified file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File callGraphFile = new File("CallGraph.rsf");

  @Option(name="clusteringResultFile", description="write the clustering of the call-graph to the specified file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File clusteringResultFile = new File("CallGraphClustering.crsf");

  private final LogManager logger;
  private final HashMap<String, Integer> clustersOfFunctions;
  private int nodeIdSeqNo = 0;

  public CallgraphClusterer(Configuration pConfig, LogManager pLogger) throws InvalidConfigurationException {
    pConfig.inject(this);
    this.logger = pLogger;
    this.clustersOfFunctions = new HashMap<String, Integer>();

    try {
      File[] filesToCheck = new File[]{graphLayoutFile, callGraphFile, clusteringResultFile};
      for (File file : filesToCheck) {
        if (file != null) {
          com.google.common.io.Files.createParentDirs(file);
        }
      }
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
  private GraphData callgraphFromCFA2 (CFA pCfa) {
    GraphData result = new GraphData();

    CFAFunctionDefinitionNode mainNode = pCfa.getMainFunction();
    GraphVertex rootNode = new GraphVertex(mainNode.getFunctionName());
    result.insertVertex(rootNode);

    Set<CFAEdge> traveredEdges = new HashSet<CFAEdge>();
    Deque<CFANode> toTraverse = new ArrayDeque<CFANode>();

    toTraverse.add(mainNode);

    while (toTraverse.size() > 0) {
      CFANode node = toTraverse.removeFirst();

      int leavingEdges = node.getNumLeavingEdges();
      for (int i=0; i<leavingEdges; i++) {
        CFAEdge edge = node.getLeavingEdge(i);
        if (traveredEdges.add(edge)) {
          toTraverse.add(edge.getSuccessor());

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

              GraphEdge callEdge = new GraphEdge("CALL", callerVertex, calledVertex, 1);
              result.insertEdge(callEdge);

              System.out.println("CALL\t" + callerFunctionName + "\t" + calledFunctionName);
            }
          } else if (edge instanceof StatementEdge) {
  //            IASTStatement statement = ((StatementEdge) edge).getStatement();
  //            if (statement instanceof IASTFunctionCall) {
  //              IASTFunctionCallExpression f = ((IASTFunctionCall)statement).getFunctionCallExpression();
  //              String functionName = f.getFunctionNameExpression().getRawSignature();
  //              System.out.println("Not added: " + functionName);
  //            }
          }
        }
      }
    }

    result.initGroups();
    assert(result.isGraphConnected("CALL"));

    return result;
  }

  /**
   * Build a call-graph out of a CFA.
   * @param pCfa  The CFA.
   * @return      GraphData that can be processed by CCVisu.
   */
  private GraphData callgraphFromCFA (CFA pCfa) {
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

              GraphEdge callEdge = new GraphEdge("CALL", callerVertex, calledVertex, 1);
              result.insertEdge(callEdge);

              System.out.println("CALL\t" + callerFunctionName + "\t" + calledFunctionName);
            }
          } else if (edge instanceof StatementEdge) {
//            IASTStatement statement = ((StatementEdge) edge).getStatement();
//            if (statement instanceof IASTFunctionCall) {
//              IASTFunctionCallExpression f = ((IASTFunctionCall)statement).getFunctionCallExpression();
//              String functionName = f.getFunctionNameExpression().getRawSignature();
//              System.out.println("Not added: " + functionName);
//            }
          }
      }
    }

    result.initGroups();
    assert(result.isGraphConnected("CALL"));

    return result;
  }

  private void assignClusterIdsFromMap(Map<String, Integer> pElementToClusterMap, CFA pCfa) {
    for (CFANode node: pCfa.getAllNodes()) {
      String nodeFunctionName = (node.getFunctionName());
      if (nodeFunctionName != null) {
        Integer clusterId = pElementToClusterMap.get(nodeFunctionName);
        if (clusterId == null) {
          node.setClusterId(0);
        } else {
          node.setClusterId(clusterId);
        }
      } else {
        node.setClusterId(0);
      }
    }
  }

  public void assignClusteringFromFile(CFA pCfa, File pClusteringFile) throws IOException {
    Multimap<String, String> clustering = loadClusteringFromFile(pClusteringFile);

    int clusterSeqNo = 0;
    Map<String, Integer> elementToClusterMap = new HashMap<String, Integer>();
    for (String cluster : clustering.keySet()) {
      clusterSeqNo += 1;
      for (String element : clustering.get(cluster)) {
        elementToClusterMap.put(element, clusterSeqNo);
      }
    }

    assignClusterIdsFromMap(elementToClusterMap, pCfa);
  }

  public void computeAndAssignClustering(CFA pCfa) {
    if (inputClusteringFile != null && inputClusteringFile.isFile()) {
      try {
        assignClusteringFromFile(pCfa, inputClusteringFile);
        logger.log(Level.INFO, "Loaded reference clustering for call-graph from: " + inputClusteringFile.getName());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      return;
    }

    // Extract the call-graph.
    GraphData graph = callgraphFromCFA2(pCfa);

    // Prepare the options for the minimizer.
    org.sosy_lab.ccvisu.Options options = new org.sosy_lab.ccvisu.Options();
    options.graph = graph;
    options.getOption(OptionsEnum.iter).set(1000);
    options.getOption(OptionsEnum.autoStopIterating).set(true);

    // Maybe write the call graph to a file.
    writeGraph(options, graph, callGraphFile);

    // Run the minimizer to get a layout.
    TwoPhaseMinimizer minimizer = new TwoPhaseMinimizer(options);
    minimizer.minimizeEnergy();

    // Maybe we should write the layout to a file.
    writeGraphLayout(options, graph, graphLayoutFile);

    // Do the clustering based on the layout.
    clustersOfFunctions.clear();
    try {
      int maxNumberOfClusters = this.clusterMaxNumber;
      if (avgNodesInCluster > 0) {
        maxNumberOfClusters = graph.getVertices().size() / avgNodesInCluster;
      }

      // Run the clustering algorithm.
      ClustererMinDistPerc clusterer = new ClustererMinDistPerc (
            graph,
            maxNumberOfClusters,
            this.clusterMergeDistancePercent,
            this.clusterMinDistancePercent);
      clusterer.runClusteringAndUpdateGroups(graph);

      // Store the cluster of each function.
      int uniqueClusterId = 0;
      for (int i = 0; i<graph.getNumberOfGroups(); i++) {
        uniqueClusterId++;
        Group group = graph.getGroup(i);
        if (group.getKind().equals(GroupKind.CLUSTER)) {
          for (GraphVertex vertex :  group.getNodes()) {
            System.out.println(String.format("Cluster %d: %s", uniqueClusterId, vertex.getName()));
            clustersOfFunctions.put(vertex.getName(), uniqueClusterId);
          }
        }
      }

      // Store the cluster in each CFANode.
      assignClusterIdsFromMap(clustersOfFunctions, pCfa);

    } catch (InterruptedException e) {
      logger.logDebugException(e);
    }

    // Maybe write the call graph to a file.
    writeGraphClustering(options, graph, clusteringResultFile);

  }

  public int getClusterOfNode(CFANode pCallNode) {
    return pCallNode.getClusterId();
  }

}
