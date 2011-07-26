package org.sosy_lab.cpachecker.cpa.einterpreter.memory;




public class MemoryFactory{
  int addr;

  public MemoryFactory(){
    addr =0;
  }
  public MemoryBlock allocateMemoryBlock(int psize){
    addr+=psize;
    return new MemoryBlock(psize);
  }


}
