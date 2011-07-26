package org.sosy_lab.cpachecker.cpa.einterpreter.memory;



public interface MemoryCell {
  enum CellType{
    AMC, DMC, FMC
  }
  CellType getType();
  MemoryCell clone();
  MemoryCell copy();
}