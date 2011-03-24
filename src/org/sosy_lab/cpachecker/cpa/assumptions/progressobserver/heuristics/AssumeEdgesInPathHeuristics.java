package org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.heuristics;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.ReachedHeuristicsDataSetView;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.StopHeuristics;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.StopHeuristicsData;

/**
 * This heuristic keeps track of the number of assume edges on a path.
 * If the given threshold is exceed, it returns bottom and the assumption
 * collector algorithm is notified.
 */
public class AssumeEdgesInPathHeuristics implements StopHeuristics<AssumeEdgesInPathHeuristicsData>{

  private final int threshold;

  public AssumeEdgesInPathHeuristics(Configuration config, LogManager logger){
    threshold = Integer.parseInt(config.getProperty("threshold", "-1").trim());
  }

  @Override
  public AssumeEdgesInPathHeuristicsData collectData(StopHeuristicsData pData,
      ReachedHeuristicsDataSetView pReached) {
    return (AssumeEdgesInPathHeuristicsData)pData;
  }

  @Override
  public AssumeEdgesInPathHeuristicsData getInitialData(CFANode pNode) {
    return new AssumeEdgesInPathHeuristicsData(0);
  }

  @Override
  public AssumeEdgesInPathHeuristicsData processEdge(StopHeuristicsData pData,
      CFAEdge pEdge) {
    return ((AssumeEdgesInPathHeuristicsData)pData).updateForEdge(pData, threshold, pEdge);
  }

}
