package cpa.assumptions.collector.progressobserver;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class MemoryOutHeuristicsData implements StopHeuristicsData {

  public MemoryOutHeuristicsData() {
  }
  
  @Override
  public boolean isBottom() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean isLessThan(StopHeuristicsData pD) {
    return pD == TOP;
  }

  @Override
  public boolean isTop() {
    // TODO Auto-generated method stub
    return false;
  }

  public MemoryOutHeuristicsData updateForEdge() {

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

      // TODO this is hard-coded, should be specified in the config file
      if(memFree < 100000){
        // TODO log this
        System.out.println("MEM IS OUT");
        return BOTTOM;
      }

      dis.close();
    } catch (Exception e1) {
      e1.printStackTrace();
    }
    
    return new MemoryOutHeuristicsData();
  }
  
  public static final MemoryOutHeuristicsData TOP = new MemoryOutHeuristicsData() {
    @Override
    public boolean isTop() { return true; }
    @Override
    public MemoryOutHeuristicsData updateForEdge() { return this; }
    @Override
    public String toString() { return "TOP"; }
  };
  
  public static final MemoryOutHeuristicsData BOTTOM = new MemoryOutHeuristicsData() {
    @Override
    public boolean isBottom() { return true; }
    @Override
    public MemoryOutHeuristicsData updateForEdge() { return this; }
    @Override
    public String toString() { return "BOTTOM"; }
  };
}
