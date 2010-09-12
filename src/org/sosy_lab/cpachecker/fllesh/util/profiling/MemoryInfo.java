package org.sosy_lab.cpachecker.fllesh.util.profiling;

public class MemoryInfo {
  
  public static long getUsedMemory() { // in bytes
    Runtime lRuntime = Runtime.getRuntime();
    return (lRuntime.totalMemory() - lRuntime.freeMemory());
  }
  
}
