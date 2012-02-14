/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.einterpreter.memory;

import java.util.ArrayList;
import java.util.HashMap;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cpa.einterpreter.InterpreterElement;


public class MemoryBlock {

 public enum CellType{
    DATA,
    ADDR,
    EMPTY,
    FUNC
  }
  HashMap<InterpreterElement,  MemoryCell[]> blocks;
  public static int MBCcnt=0;
  private static int cnt=0;
  public static ArrayList<Integer>Mdpt=new ArrayList<Integer>();
  MemoryBlock clone = null;
  boolean free = false;
  int size;
  protected MemoryBlock(int n,InterpreterElement pel){
    blocks = new HashMap<InterpreterElement, MemoryCell[]>();
    if(n<0){
      n+=1;
      return;
    }
    MemoryCell tmp [] = new MemoryCell[n];
    blocks.put(pel, tmp);
    size = n;
  }

  /*private MemoryBlock(HashMap<InterpreterElement, MemoryCell[]> pblock,boolean pfree){
    blocks = pblock;

    free = pfree;
  }*/

  /*MemoryCell [] getMemory(int poffset, int psize){
    MemoryCell tmp[] = new MemoryCell[psize];
    for(int i =0;i<psize;i++){
      tmp[i]=block[poffset + i];
    }
    return tmp;
  }*/



    public byte getData(int poffset,InterpreterElement el) throws RuntimeException{
      MemoryCell [] block = getMemoryBlock(el);

      if(free){
        throw new RuntimeException("can not access memory memory has been freed");
      }

      if(block[poffset] == null)
        throw new RuntimeException("uninitialized Data");
      MemoryCell c = block[poffset];
      switch(c.getType()){
        case DMC:
          return ((DataMemoryCell)c).getData(el);

        case AMC:
          throw new RuntimeException("can not access AMC for Data");

        default:
          throw new RuntimeException("error reading MemoryCell");
      }

    }


    private MemoryCell[] getMemoryBlock(InterpreterElement pEl) {
      cnt=0;
    if(blocks.isEmpty()){
      MemoryCell tmp[] = new MemoryCell[size];
      blocks.put(pEl, tmp);
      return tmp;
    }
    InterpreterElement cur = pEl;
    while(pEl !=null && blocks.containsKey(pEl)==false){
      MBCcnt++;
      cnt++;
      pEl = pEl.getprev();

    }
    Mdpt.add(cnt);
    if(pEl==null){
      MemoryCell tmp[] = new MemoryCell[size];
      blocks.put(cur, tmp);
      return tmp;
    }

    return blocks.get(pEl);
  }

    public void setData(int poffset,byte pdata, InterpreterElement el) throws RuntimeException{
      MemoryCell [] block = getMemoryBlock(el);

      if(free){
        throw new RuntimeException("can not access memory memory has been freed");
      }

      if(block[poffset] == null){
        block = block.clone();
        block[poffset] = new DataMemoryCell(el);
        blocks.put(el, block);
      }
      MemoryCell c = block[poffset];
      switch(c.getType()){
        case AMC:
        throw new RuntimeException("can not write byte in AMC");

        case DMC:
          ((DataMemoryCell)c).setData(pdata,el);
          break;

        default:
          throw new RuntimeException("error writing MemoryCell");
      }

    }

    public Address getAddress(int poffset, InterpreterElement el)throws RuntimeException{
      MemoryCell [] block = getMemoryBlock(el);
      if(free){
        throw new RuntimeException("can not access memory memory has been freed");
      }
      if(block[poffset] == null)
        throw new RuntimeException("uninitialized Data");
      MemoryCell c = block[poffset];
      switch(c.getType()){
        case DMC:
          throw new RuntimeException("can not access DMC for Address");

        case AMC:
          return ((AddrMemoryCell)c).getAddress(el);

        default:
          throw new RuntimeException("error reading MemoryCell");
      }
    }
    public void setAddress(int poffset,Address paddr, InterpreterElement el)throws RuntimeException{
      MemoryCell [] block = getMemoryBlock(el);
      if(free){
        throw new RuntimeException("can not access memory memory has been freed");
      }
      if(block[poffset] == null){
        block = block.clone();
        block[poffset] = new AddrMemoryCell(paddr,el);
        blocks.put(el, block);
      }
      MemoryCell c = block[poffset];
      switch(c.getType()){
        case DMC:
         // throw new MemoryException("can not access DMC for Address");
          //TODO: eventuell ueberschreiben von memcells in allen schreibzugriffen zulassen
          c = new AddrMemoryCell(paddr,el);
          block  = block.clone();
          block[poffset]= c;
          blocks.put(el, block);
          return;
        case AMC:
          ((AddrMemoryCell)c).setAddress(paddr,el);
          break;
        default:
          throw new RuntimeException("error reading MemoryCell");
      }
    }



    public  CFAFunctionDefinitionNode getFunctionPointer(int poffset, InterpreterElement el)throws RuntimeException{
      MemoryCell [] block = getMemoryBlock(el);
      if(free){
        throw new RuntimeException("can not access memory memory has been freed");
      }
      if(block[poffset] == null)
        throw new RuntimeException("uninitialized Data");
      MemoryCell c = block[poffset];
      switch(c.getType()){
        case DMC:
          throw new RuntimeException("can not access DMC for function pointer");

        case AMC:
          throw new RuntimeException("can not access AMC for function pointer");
        case FMC:
           return ((FuncMemoryCell)c).getFunctionPoint(el);

        default:
          throw new RuntimeException("error reading MemoryCell");
      }
    }
    public void setFunctionPointer(int poffset,CFAFunctionDefinitionNode func, InterpreterElement el)throws RuntimeException{
      MemoryCell [] block = getMemoryBlock(el);
      if(free){
        throw new RuntimeException("can not access memory memory has been freed");
      }
      if(block[poffset] == null){
        block = block.clone();
        block[poffset] = new FuncMemoryCell(func,el);
        blocks.put(el, block);
      }
      MemoryCell c = block[poffset];
      switch(c.getType()){
        case DMC:
          throw new RuntimeException("can not access DMC for FuncPnt");

        case AMC:
          throw new RuntimeException("can not access AMC for FuncPnt");
        case FMC:
          ((FuncMemoryCell)c).setFunctionPoint(func,el);
          return;
        default:
          throw new RuntimeException("error reading MemoryCell");
      }
    }












    public MemoryCell getMemoryCell(int poffset, InterpreterElement el){
      MemoryCell [] block = getMemoryBlock(el);
      return block[poffset];
    }

/*  MemoryCell  getMemory(int poffset){
    return block[poffset];
  }

  void setMemory(int poffset,MemoryCell pdata){
    if(block[poffset]==null){
      block[poffset]=pdata;
    }
  }*/

    public CellType getCellType(int offset, InterpreterElement el){
      MemoryCell [] block = getMemoryBlock(el);
      if(block[offset]!=null && block[offset] instanceof DataMemoryCell){
          return CellType.DATA;
      }
      if(block[offset]!=null &&block[offset] instanceof AddrMemoryCell){
        return CellType.ADDR;
      }

      if(block[offset]!=null &&block[offset] instanceof FuncMemoryCell){
        return CellType.FUNC;
      }


      return CellType.EMPTY;
    }




    public int getBlockSize(){
       return size;
    }

    public void setMemoryCell(MemoryCell pClone, int pX, InterpreterElement el) throws Exception {
      MemoryCell [] block = getMemoryBlock(el);
      block = block.clone();
      blocks.put(el, block);
      if(free){
        throw new MemoryException("can not access memory; memory has been freed");
      }
      block[pX]= pClone;

    }
    public void free(InterpreterElement el){ //
      MemoryCell [] block = getMemoryBlock(el);
      free = true;
      for(int x=0; x< block.length;x++){
        block[x]=null;
      }
    }


}
