package org.sosy_lab.cpachecker.cpa.einterpreter.memory;

import org.junit.Test;

public class MemoryTest {

  @Test
  public void test002() throws Exception {
    MemoryFactory fact = new MemoryFactory();
    MemoryBlock test = fact.allocateMemoryBlock(4);
    Address addr = new Address(test,0);

    int v = -5525543;
    int vtmp=v;
    byte mtmp;
    for(int x = addr.getOffset();x< addr.getOffset()+4;x++){
      mtmp = (byte) (vtmp& 0xFF);

      vtmp >>=8;
      addr.getMemoryBlock().setData(x, mtmp);

    }

   vtmp =0;
   for(int x = addr.getOffset()+3;x>= addr.getOffset();x--){

     vtmp |= addr.getMemoryBlock().getData(x) &0xFF;

     if(x!= addr.getOffset()){
       vtmp <<=8;
     }
   }
   System.out.println(vtmp);


  }


}


