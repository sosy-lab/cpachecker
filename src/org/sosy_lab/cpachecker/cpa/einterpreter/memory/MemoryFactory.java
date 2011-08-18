package org.sosy_lab.cpachecker.cpa.einterpreter.memory;

import org.sosy_lab.cpachecker.cpa.einterpreter.InterpreterElement;




public class MemoryFactory{
  int addr;

  public MemoryFactory(){
    addr =0;
  }
  public MemoryBlock allocateMemoryBlock(int psize, InterpreterElement pel){
    addr+=psize;
    return new MemoryBlock(psize,pel);
  }


}
