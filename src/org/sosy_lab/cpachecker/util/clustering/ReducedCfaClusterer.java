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

import org.sosy_lab.ccvisu.Options.OptionsEnum;
import org.sosy_lab.ccvisu.clustering.ClustererMinDistPerc;
import org.sosy_lab.ccvisu.graph.GraphData;
import org.sosy_lab.ccvisu.graph.GraphEdge;
import org.sosy_lab.ccvisu.graph.GraphVertex;
import org.sosy_lab.ccvisu.graph.Group;
import org.sosy_lab.ccvisu.graph.Group.GroupKind;
import org.sosy_lab.ccvisu.layout.Minimizer;
import org.sosy_lab.ccvisu.layout.TwoPhaseMinimizer;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.util.blocking.BlockedCFAReducer;
import org.sosy_lab.cpachecker.util.blocking.container.ItemTree;
import org.sosy_lab.cpachecker.util.blocking.container.ReducedEdge;
import org.sosy_lab.cpachecker.util.blocking.container.ReducedFunction;
import org.sosy_lab.cpachecker.util.blocking.container.ReducedNode;
import org.sosy_lab.cpachecker.util.blocking.interfaces.BlockComputer;


@Options(prefix="clusterer.reducedcfa")
public class ReducedCfaClusterer extends AbstractGraphClusterer implements BlockComputer {

  @Option(description="Merge partitions within distance (percent of layout diagonal; e.g. 0.03).")
  private float clusterMergeDistancePercent = 0.03f;

  @Option(description="Stop clustering on min. distance (percent of layout diagonal; e.g. 0.4; 1 means one cluster for each node).")
  private float clusterMinDistancePercent = 0.3f;

  @Option(description="(max) number of clusters to create.")
  private int clusterMaxNumber = 0;

  @Option(description="Average number of nodes in a cluster. Determines the parameter clusterMaxNumber.")
  private int avgNodesInCluster = 0;

  @Option(description="Calculate a deterministic initial input-layout for the minimizer?")
  private boolean deterministicInitialLayout = true;

  @Option(name="graphLayoutFile", description="Write the layout of the clustered callgraph to this file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File graphLayoutFile = new File("CallGraph.lay");

  @Option(name="reducedCfaFile", description="write the call-graph to the specified file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File reducedCfaFile = new File("ReducedCfa.rsf");

  @Option(name="resultClusteringFile", description="write the clustering of the call-graph to the specified file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File resultClusteringFile = new File("CallGraphClustering.crsf");

  private final Configuration config;
  private final LogManager logger;

  public ReducedCfaClusterer(Configuration pConfig, LogManager pLogger) throws InvalidConfigurationException {
    pConfig.inject(this);
    this.config = pConfig;
    this.logger = pLogger;

    try {
      File[] filesToCheck = new File[]{graphLayoutFile, reducedCfaFile, resultClusteringFile};
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

  private int nodePosSeqNo = 0;

  private GraphVertex createOrGetCallGraphVertex (GraphData pG, Map<Integer, GraphVertex> pVertexMap, int pNodeId) {
    GraphVertex result = pVertexMap.get(pNodeId);
    if (result != null) {
      return result;
    }

    this.nodePosSeqNo++;
    result = new GraphVertex(Integer.toString(pNodeId));
    pVertexMap.put(pNodeId, result);
    pG.insertVertex(result);

    if (deterministicInitialLayout) {
      float x = (nodePosSeqNo % 100) * 10.0f + (float) Math.random();
      float y = (float)Math.floor (nodePosSeqNo / 100) * 10.0f + (float) Math.random();
      float z = (float)Math.floor (nodePosSeqNo / 100) * 10.0f + (float) Math.random();

      result.getPosition().x = x;
      result.getPosition().y = y;
      result.getPosition().z = z;
    } else {
      result.getPosition().x = 2 * (float) Math.random() - 1;
      result.getPosition().y = 2 * (float) Math.random() - 1;
      result.getPosition().z = 2 * (float) Math.random() - 1;
    }

    return result;
  }


  private GraphData graphFrom (ReducedFunction pRootFunction) {
    GraphData graph = new GraphData();
    Map<Integer, GraphVertex> vertexMap = new HashMap<Integer, GraphVertex>();

    Set<ReducedEdge> processed = new HashSet<ReducedEdge>();
    Deque<ReducedNode> toProcess = new ArrayDeque<ReducedNode>();

    ReducedNode mainNode = pRootFunction.getEntryNode();
    toProcess.add(mainNode);

    assert(graph.getVertices().size() == 0);

    while (toProcess.size() > 0) {
      ReducedNode u = toProcess.removeFirst();
      GraphVertex vertexU = createOrGetCallGraphVertex(graph, vertexMap, u.getUniqueNodeId());

      for (ReducedEdge e: pRootFunction.getLeavingEdges(u)) {
        if (processed.add(e)) {
          ReducedNode v = e.getPointsTo();
          GraphVertex vertexV = createOrGetCallGraphVertex(graph, vertexMap, v.getUniqueNodeId());

          GraphEdge callEdge = new GraphEdge("R", vertexU, vertexV, 1);
          graph.insertEdge(callEdge);

          toProcess.add(v);
        }
      }
    }

    graph.setNodesToRandomPositions(graph.getVertices(), 2);
    graph.initGroups();

    assert(pRootFunction.getNumOfActiveNodes() <= graph.getVertices().size());
    assert(graph.isGraphConnected());

    return graph;
  }

  @Override
  public ItemTree<String, CFANode> computeAbstractionNodes(CFA pCfa) throws InvalidConfigurationException {
    ItemTree<String, CFANode> result = new ItemTree<String, CFANode>();
    BlockedCFAReducer reducer = new BlockedCFAReducer(this.config, this.logger);
    Map<Integer, Integer> clustersOfNodes = new HashMap<Integer, Integer>();

    ReducedFunction reducedProgram = reducer.inlineAndSummarize(pCfa.getMainFunction());
    GraphData graph = graphFrom(reducedProgram);

    // Run the minimizer to get a layout.
    org.sosy_lab.ccvisu.Options options = new org.sosy_lab.ccvisu.Options();
    options.graph = graph;
    options.getOption(OptionsEnum.iter).set(1000);
    options.getOption(OptionsEnum.autoStopIterating).set(true);

    // Maybe we should write the graph data to a file.
    writeGraph(options, graph, reducedCfaFile);

    // Run the minimizer/laouter.
    int tries = 5;
    Minimizer minimizer = new TwoPhaseMinimizer(options);
    do {
      try {
        minimizer.minimizeEnergy();
      } catch (java.lang.StackOverflowError e) {
        // Interruption should not occur in this case.
        // Maybe a stack overflow occurs because of invalid positions.
        graph.setNodesToRandomPositions(graph.getVertices(), 2);
      } catch (Exception e) {
        graph.setNodesToRandomPositions(graph.getVertices(), 2);
      }
      tries = 0;
    } while (tries-- > 0);

    // Maybe we should write the layout to a file.
    writeGraphLayout(options, graph, graphLayoutFile);

    // Do the clustering based on the layout.
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

      int uniqueClusterId = 0;
      for (int i = 0; i<graph.getNumberOfGroups(); i++) {
        Group group = graph.getGroup(i);
        if (group.getKind().equals(GroupKind.CLUSTER)) {
          uniqueClusterId++;
          for (GraphVertex vertex :  group.getNodes()) {
            clustersOfNodes.put(Integer.parseInt(vertex.getName()), uniqueClusterId);
          }
        }
      }
      System.out.println(String.format("Number of clusters: %d", uniqueClusterId));
    } catch (InterruptedException e) {
      logger.logDebugException(e);
    }

    // Maybe write the call graph to a file.
    writeGraphClustering(options, graph, resultClusteringFile);

    // Determine the abstraction nodes.
    for (ReducedNode u: reducedProgram.getAllActiveNodes()) {
      for (ReducedEdge e: reducedProgram.getLeavingEdges(u)) {
        ReducedNode v = e.getPointsTo();

        int clusterOfU = clustersOfNodes.get(u.getUniqueNodeId());
        int clusterOfV = clustersOfNodes.get(v.getUniqueNodeId());

        if (clusterOfU != clusterOfV) {
          result.put(u.getCallstack()).addLeaf(u.getWrapped());
        }
      }
    }

    return result;
  }

}
