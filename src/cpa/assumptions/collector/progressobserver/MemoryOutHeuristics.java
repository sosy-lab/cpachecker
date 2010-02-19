package cpa.assumptions.collector.progressobserver;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFANode;

import cpa.common.CPAConfiguration;
import cpa.common.LogManager;

public class MemoryOutHeuristics
  implements StopHeuristics<TrivialStopHeuristicsData>
{
  private final int threshold;
  private final LogManager logger;

  public MemoryOutHeuristics(CPAConfiguration config, LogManager pLogger) {
    threshold = Integer.parseInt(config.getProperty("threshold", "-1"));
    logger = pLogger;
  }
  
  @Override
  public TrivialStopHeuristicsData collectData(StopHeuristicsData pData, ReachedHeuristicsDataSetView pReached) {
    return (TrivialStopHeuristicsData)pData;
  }
  
  @Override
  public TrivialStopHeuristicsData processEdge(StopHeuristicsData pData, CFAEdge pEdge)
  {
    // Negative threshold => do nothing
    if (threshold <= 0)
      return TrivialStopHeuristicsData.TOP;
      
    // Bottom => nothing to do, we are already out of memory
    if (pData == TrivialStopHeuristicsData.BOTTOM)
      return TrivialStopHeuristicsData.BOTTOM;

    // try this later com.sun.management.OperatingSystemMXBean
    try {
      FileInputStream fis = new FileInputStream("/proc/meminfo");
      DataInputStream dis = new DataInputStream(fis);
      BufferedReader bfr = new BufferedReader(new InputStreamReader(dis));
      String line;

      long memFree = 0;

      while((line = bfr.readLine()) != null){
        //          MemTotal:        2060840 kB
        //          MemFree:         1732952 kB
        //          Buffers:            3164 kB
        //          Cached:            58376 kB
        if(line.contains("MemTotal:")){
          continue;
        }
        else if(line.contains("MemFree:")){
          memFree = Long.valueOf(line.split("\\s+")[1]);
          break;
        }
//        else{
//          break;
//        }
      }

//    long totalFree = memTotal - (memFree + buffers + cached);

      if(memFree < threshold) {
        logger.log(Level.WARNING, "MEMORY IS OUT");
        return TrivialStopHeuristicsData.BOTTOM;
      }

      dis.close();
    } catch (Exception e1) {
      e1.printStackTrace();
    }
    
    return TrivialStopHeuristicsData.TOP;
  }

  @Override
  public TrivialStopHeuristicsData getBottom() {
    return TrivialStopHeuristicsData.BOTTOM;
  }


  @Override
  public TrivialStopHeuristicsData getInitialData(CFANode pNode) {
    return TrivialStopHeuristicsData.TOP;
  }


  @Override
  public TrivialStopHeuristicsData getTop() {
    return TrivialStopHeuristicsData.TOP;
  }
  
}