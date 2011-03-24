package org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.heuristics;

import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.StopHeuristicsData;
import org.sosy_lab.cpachecker.util.assumptions.HeuristicToFormula.PreventingHeuristicType;

public class PathLengthHeuristicsData implements StopHeuristicsData{

  private static long threshold = -1;
  
  private final int noOfNodes;
  
  public PathLengthHeuristicsData(int noOfNodes){
    this.noOfNodes = noOfNodes;
  }
  
  public PathLengthHeuristicsData()
  {
    noOfNodes = 0;
  }
  
  public PathLengthHeuristicsData updateForEdge(StopHeuristicsData pData, int pThreshold){
    
    int newValue = (((PathLengthHeuristicsData)pData).noOfNodes);
    newValue++;
    
    if ((pThreshold > 0) && (newValue > pThreshold)){
      setThreshold(pThreshold);
      return BOTTOM;
    }
    
    else{
      return new PathLengthHeuristicsData(newValue);
    }
  }
  
  public static final PathLengthHeuristicsData TOP = new PathLengthHeuristicsData() {
    @Override
    public boolean isTop() { return true; }
    @Override
    public PathLengthHeuristicsData updateForEdge(StopHeuristicsData pData, int pThreshold) { return this; }
    @Override
    public String toString() { return "TOP"; }
  };

  public static final PathLengthHeuristicsData BOTTOM = new PathLengthHeuristicsData() {
    @Override
    public boolean isBottom() { return true; }
    @Override
    public PathLengthHeuristicsData updateForEdge(StopHeuristicsData pData, int pThreshold) { return this; }
    @Override
    public String toString() { return "BOTTOM"; }
  };
  
  public void setThreshold(long newThreshold)
  {
    threshold = newThreshold;
  }

  @Override
  public long getThreshold()
  {
    return threshold;
  }

  @Override
  public PreventingHeuristicType getHeuristicType() {
    return PreventingHeuristicType.PATHLENGTH;
  }

  @Override
  public boolean isBottom() {
    return false;
  }

  @Override
  public boolean isLessThan(StopHeuristicsData pD) {
    return false;
  }

  @Override
  public boolean isTop() {
    return false;
  }

  @Override
  public boolean shouldTerminateAnalysis() {
    return false;
  }
  
}
