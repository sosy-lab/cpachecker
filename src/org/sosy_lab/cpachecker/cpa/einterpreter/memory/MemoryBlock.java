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

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;

public class MemoryBlock {

 public enum CellType{
    DATA,
    ADDR,
    EMPTY,
    FUNC
  }
  MemoryCell block[];
  MemoryBlock clone = null;
  boolean free = false;
  protected MemoryBlock(int n){
    block = new MemoryCell[n];
  }

  private MemoryBlock(MemoryCell[] pblock,boolean pfree){
    block = pblock;
    free = pfree;
  }

  MemoryCell [] getMemory(int poffset, int psize){
    MemoryCell tmp[] = new MemoryCell[psize];
    for(int i =0;i<psize;i++){
      tmp[i]=block[poffset + i];
    }
    return tmp;
  }



    public byte getData(int poffset) throws MemoryException{
      if(free){
        throw new MemoryException("can not access memory memory has been freed");
      }

      if(block[poffset] == null)
        throw new MemoryException("uninitialized Data");
      MemoryCell c = block[poffset];
      switch(c.getType()){
        case DMC:
          return ((DataMemoryCell)c).getData();

        case AMC:
          throw new MemoryException("can not access AMC for Data");

        default:
          throw new MemoryException("error reading MemoryCell");
      }

    }


    public void setData(int poffset,byte pdata) throws MemoryException{
      if(free){
        throw new MemoryException("can not access memory memory has been freed");
      }

      if(block[poffset] == null){
        block[poffset] = new DataMemoryCell();
      }
      MemoryCell c = block[poffset];
      switch(c.getType()){
        case DMC:
          ((DataMemoryCell)c).setData(pdata);
          break;
        case AMC:
          throw new MemoryException("can not write byte in AMC");

        default:
          throw new MemoryException("error writing MemoryCell");
      }

    }

    public Address getAddress(int poffset)throws MemoryException{
      if(free){
        throw new MemoryException("can not access memory memory has been freed");
      }
      if(block[poffset] == null)
        throw new MemoryException("uninitialized Data");
      MemoryCell c = block[poffset];
      switch(c.getType()){
        case DMC:
          throw new MemoryException("can not access DMC for Address");

        case AMC:
          return ((AddrMemoryCell)c).getAddress();

        default:
          throw new MemoryException("error reading MemoryCell");
      }
    }
    public void setAddress(int poffset,Address paddr)throws MemoryException{
      if(free){
        throw new MemoryException("can not access memory memory has been freed");
      }
      if(block[poffset] == null)
        block[poffset] = new AddrMemoryCell(paddr);
      MemoryCell c = block[poffset];
      switch(c.getType()){
        case DMC:
          throw new MemoryException("can not access DMC for Address");

        case AMC:
          ((AddrMemoryCell)c).setAddress(paddr);
          break;
        default:
          throw new MemoryException("error reading MemoryCell");
      }
    }



    public  CFAFunctionDefinitionNode getFunctionPointer(int poffset)throws MemoryException{
      if(free){
        throw new MemoryException("can not access memory memory has been freed");
      }
      if(block[poffset] == null)
        throw new MemoryException("uninitialized Data");
      MemoryCell c = block[poffset];
      switch(c.getType()){
        case DMC:
          throw new MemoryException("can not access DMC for function pointer");

        case AMC:
          throw new MemoryException("can not access AMC for function pointer");
        case FMC:
           return ((FuncMemoryCell)c).getFunctionPoint();

        default:
          throw new MemoryException("error reading MemoryCell");
      }
    }
    public void setFunctionPointer(int poffset,CFAFunctionDefinitionNode func)throws MemoryException{
      if(free){
        throw new MemoryException("can not access memory memory has been freed");
      }
      if(block[poffset] == null)
        block[poffset] = new FuncMemoryCell(func);
      MemoryCell c = block[poffset];
      switch(c.getType()){
        case DMC:
          throw new MemoryException("can not access DMC for FuncPnt");

        case AMC:
          throw new MemoryException("can not access DMC for FuncPnt");
        case FMC:
          ((FuncMemoryCell)c).setFunctionPoint(func);
          return;
        default:
          throw new MemoryException("error reading MemoryCell");
      }
    }












    public MemoryCell getMemoryCell(int poffset){
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

    public CellType getCellType(int offset){
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


    @Override
    public MemoryBlock clone(){
      if(clone != null){
        return clone;
      }else{

        MemoryCell v[] = new MemoryCell[block.length];
          for(int x=0;x <block.length;x++){
            if(block[x] instanceof AddrMemoryCell){
              v[x]=((AddrMemoryCell)block[x]).clone();

            }else if(block[x] instanceof DataMemoryCell){
              v[x]=((DataMemoryCell)block[x]).clone();
            }else if (block[x] instanceof FuncMemoryCell){
              v[x] = ((FuncMemoryCell)block[x]).clone();
            }
          }


        clone = new MemoryBlock(v,free);
        return clone;
      }
    }

    public int getBlockSize(){
      return block.length;
    }

    public void setMemoryCell(MemoryCell pClone, int pX) throws Exception {
      if(free){
        throw new MemoryException("can not access memory; memory has been freed");
      }
      block[pX]= pClone;

    }
    public void free(){
      free = true;
      for(int x=0; x< block.length;x++){
        block[x]=null;
      }
    }


}
