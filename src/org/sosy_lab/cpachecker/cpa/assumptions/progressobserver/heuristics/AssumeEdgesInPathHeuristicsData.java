package org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.heuristics;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cpa.assumptions.progressobserver.StopHeuristicsData;
import org.sosy_lab.cpachecker.util.assumptions.HeuristicToFormula.PreventingHeuristicType;

public class AssumeEdgesInPathHeuristicsData implements StopHeuristicsData {

  private static long threshold = -1;

  private final int noOfAssumeEdges;

  public AssumeEdgesInPathHeuristicsData(int noOfAssumeEdges){
    this.noOfAssumeEdges = noOfAssumeEdges;
  }

  public AssumeEdgesInPathHeuristicsData()
  {
    noOfAssumeEdges = -1;
  }


  public AssumeEdgesInPathHeuristicsData updateForEdge(StopHeuristicsData pData, int pThreshold, CFAEdge pEdge){

    int newValue = (((AssumeEdgesInPathHeuristicsData)pData).noOfAssumeEdges);
    
    if(pEdge.getEdgeType() == CFAEdgeType.AssumeEdge){

      newValue++;
      
      if ((pThreshold > 0) && (newValue > pThreshold)){
        setThreshold(pThreshold);
        return BOTTOM;
      }
    }

    return new AssumeEdgesInPathHeuristicsData(newValue);
  }

  public static final AssumeEdgesInPathHeuristicsData TOP = new AssumeEdgesInPathHeuristicsData() {
    @Override
    public boolean isTop() { return true; }
    @Override
    public AssumeEdgesInPathHeuristicsData updateForEdge(StopHeuristicsData pData, int pThreshold, CFAEdge pEdge) { return this; }
    @Override
    public String toString() { return "TOP"; }
  };

  public static final AssumeEdgesInPathHeuristicsData BOTTOM = new AssumeEdgesInPathHeuristicsData() {
    @Override
    public boolean isBottom() { return true; }
    @Override
    public AssumeEdgesInPathHeuristicsData updateForEdge(StopHeuristicsData pData, int pThreshold, CFAEdge pEdge) { return this; }
    @Override
    public String toString() { return "BOTTOM"; }
  };
  
  @Override
  public PreventingHeuristicType getHeuristicType() {
    return PreventingHeuristicType.ASSUMEEDGESINPATH;
  }

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
