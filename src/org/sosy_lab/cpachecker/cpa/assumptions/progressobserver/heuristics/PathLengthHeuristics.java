package org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.heuristics;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.ReachedHeuristicsDataSetView;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.StopHeuristics;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.StopHeuristicsData;

public class PathLengthHeuristics implements StopHeuristics<PathLengthHeuristicsData>{

  private final int threshold;
  
  public PathLengthHeuristics(Configuration config, LogManager logger){
    threshold = Integer.parseInt(config.getProperty("threshold", "-1").trim());
  }
  
  @Override
  public PathLengthHeuristicsData collectData(StopHeuristicsData pData,
      ReachedHeuristicsDataSetView pReached) {
    return (PathLengthHeuristicsData)pData;
  }

  @Override
  public PathLengthHeuristicsData getInitialData(CFANode pNode) {
    return new PathLengthHeuristicsData(1);
  }

  @Override
  public PathLengthHeuristicsData processEdge(StopHeuristicsData pData,
      CFAEdge pEdge) {
    return ((PathLengthHeuristicsData)pData).updateForEdge(pData, threshold);
  }

}
