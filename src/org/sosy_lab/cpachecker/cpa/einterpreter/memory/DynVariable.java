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

import org.sosy_lab.cpachecker.cpa.einterpreter.InterpreterElement;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Type.CompositeType;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Type.TypeClass;

public class DynVariable implements Variable{
  String name;
  Address addr;
  CompositeType type;
  boolean isConst;
  DynVariable clone=null;
  InterpreterElement tmp;
  public enum dyntypes{
    STRUCT,
    UNION
  }
  dyntypes dyn;

  public DynVariable(String pname, Address paddr, CompositeType ptype,boolean pisConst,dyntypes pdyn){
    name =pname;
    addr = paddr;
    type = ptype;
    isConst = pisConst;
    dyn = pdyn;
  }
  @Override
  public TypeClass getTypeClass() {
    // TODO Auto-generated method stub
    if(dyn == dyntypes.STRUCT)
      return TypeClass.STRUCT;
    else
      return TypeClass.UNION;
  }

  @Override
  public Type getType() {
    // TODO Auto-generated method stub
    return type;
  }

  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return name;
  }

  @Override
  public Address getAddress() {
    // TODO Auto-generated method stub
    return addr;
  }

  @Override
  public int getSize() {
    // TODO Auto-generated method stub
    return type.sizeOf();
  }

  @Override
  public void copyVar(String pPname, InterpreterElement pEl) throws Exception {

    MemoryBlock b = pEl.getFactory().allocateMemoryBlock(type.sizeOf());
    Address naddr = new Address(b,0);
    MemoryBlock o=addr.getMemoryBlock();
    int offset = addr.getOffset();
    for(int x=0;x<type.sizeOf();x++){
      MemoryCell tmp = o.getMemoryCell(offset +x);
      if(tmp!=null){
        tmp.copy();
      }
      b.setMemoryCell(tmp, x);
    }

    DynVariable tmp = new DynVariable(pPname,naddr,type,isConst,dyn );
    pEl.getCurrentScope().addVariable(tmp,pEl);


  }

  @Override
  public boolean isConst() {
    // TODO Auto-generated method stub
    return isConst;
  }

  @Override
  public DynVariable clone(){
    if(clone ==null){
      clone = new DynVariable(name,addr.clone(),type,isConst,dyn);

    }

    return clone;
  }



}
