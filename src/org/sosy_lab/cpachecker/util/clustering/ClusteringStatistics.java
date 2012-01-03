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
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.ccvisu.graph.GraphData;
import org.sosy_lab.ccvisu.graph.Group;
import org.sosy_lab.ccvisu.graph.Group.GroupKind;
import org.sosy_lab.ccvisu.measuring.ClusterQuality;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ContinuousStatistics;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.clustering.interfaces.Clusterer;


@Options(prefix="clusteringstatistics")
public class ClusteringStatistics implements Statistics, ContinuousStatistics {

  private GraphData clusteredGraph = null;
  private Clusterer clusterer;

  private int waitlistClusterStatSeqNo = 0;

  @Option(name="file", description="write statistics about how many elements of the waitlist belong to which cluster.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File waitlistClusteringStatisticsFile = new File("WaitlistClusteringStatistics.txt");

  private PrintStream waitlistStatPrintWriter;
  private boolean waitlistStatHeaderPrinted = false;

  public ClusteringStatistics(Configuration pConfig) throws InvalidConfigurationException {
     pConfig.inject(this);

     this.waitlistStatPrintWriter = null;
     if (waitlistClusteringStatisticsFile != null) {
       try {
          com.google.common.io.Files.createParentDirs(waitlistClusteringStatisticsFile);
          this.waitlistStatPrintWriter = new PrintStream(waitlistClusteringStatisticsFile);
       } catch (Exception e) {
          e.printStackTrace();
       }
     }
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
    int numberOfClusters = 0;
    for (int i=0; i<clusteredGraph.getNumberOfGroups(); i++) {
      Group g = clusteredGraph.getGroup(i);
      if (g.getKind() == GroupKind.CLUSTER) {
        numberOfClusters++;
      }
    }

    pOut.println("Number of clusters: " + numberOfClusters);
    if (numberOfClusters > 0) {
      try {
        pOut.println("Cut of clustering: " + ClusterQuality.cutOfClustering(clusteredGraph));
        pOut.println("Modularization quality of clustering: " + ClusterQuality.modularizationQualityOfGraph(clusteredGraph));
        pOut.println("Edge-normalized cut of clustering: " + ClusterQuality.edgeNormalizedCutOfClustering(clusteredGraph));
        //System.out.println("Edge-normalized cut (v2) of clustering: " + org.sosy_lab.ccvisu.Statistics.edgeNormalizedCutOfClusteringV2(options.graph));
        pOut.println("Modularity of clustering: " + ClusterQuality.modularityOfClustering(clusteredGraph));
      } catch (Exception e) {
      }
    }
  }

  public void setClusteredGraph (GraphData pGraph) {
    this.clusteredGraph  = pGraph;
  }

  @Override
  public String getName() {
    return "Callgraph-Clustering";
  }

  @Override
  public String[] announceStatisticColumns() {
    // TODO Auto-generated method stub
    return null;
  }

  private static class WaitlistStatEntry {
    public int numberOfAbstractStates = 0;
  }

  @Override
  public Object[] provideStatisticValues(ReachedSet pReached) {

    if (waitlistStatPrintWriter != null) {

      if (!waitlistStatHeaderPrinted) {
        waitlistStatPrintWriter.print("Statistic\tIteration\t");
      }

      Map<String, WaitlistStatEntry> numOfStatesPerCluster = new HashMap<String, WaitlistStatEntry>();
      for (int i=0; i<clusteredGraph.getNumberOfGroups(); i++) {
        Group g = clusteredGraph.getGroup(i);
        if (g.getKind() == GroupKind.CLUSTER) {
          numOfStatesPerCluster.put(g.getName(), new WaitlistStatEntry());

          if (!waitlistStatHeaderPrinted) {
            waitlistStatPrintWriter.print(String.format("%s\t", g.getName()));
          }
        }
      }

      if (!waitlistStatHeaderPrinted) {
        waitlistStatPrintWriter.print("\n");
        waitlistStatHeaderPrinted = true;
      }

      for (AbstractElement ae: pReached.getWaitlist()) {
        CFANode loc = AbstractElements.extractLocation(ae);
        String clusterOfAbstractState = this.clusterer.getClusterOfNode(loc);
        numOfStatesPerCluster.get(clusterOfAbstractState).numberOfAbstractStates++;
      }


      waitlistStatPrintWriter.print(String.format("WAITLIST-CLUSTER-STAT\t%d\t", waitlistClusterStatSeqNo++));
      for (int i=0; i<clusteredGraph.getNumberOfGroups(); i++) {
        Group g = clusteredGraph.getGroup(i);
        if (g.getKind() == GroupKind.CLUSTER) {
          WaitlistStatEntry entry = numOfStatesPerCluster.get(g.getName());
          waitlistStatPrintWriter.print(String.format("%s\t", entry.numberOfAbstractStates));
        }
      }
      waitlistStatPrintWriter.print("\n");
    }

    return new Object[]{};
  }

  public void setClusterer(Clusterer pCcVisuClusterer) {
    this.clusterer = pCcVisuClusterer;
  }

}
