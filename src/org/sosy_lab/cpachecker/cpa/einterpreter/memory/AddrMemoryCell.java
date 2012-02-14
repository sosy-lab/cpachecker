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

import org.sosy_lab.cpachecker.cpa.einterpreter.InterpreterElement;

public class AddrMemoryCell implements MemoryCell {
 private HashMap<InterpreterElement, Address> addr;
 public static int AMCcnt=0;
 private AddrMemoryCell clone = null;
 private static int cnt=0;
 public static ArrayList<Integer> Adpt= new ArrayList<Integer>();

  public AddrMemoryCell(Address paddr,InterpreterElement pel){
    addr = new HashMap<InterpreterElement, Address>();
    addr.put(pel,paddr);
  }

  @SuppressWarnings("unchecked")
  private AddrMemoryCell(HashMap<InterpreterElement,Address> map){
    addr = (HashMap<InterpreterElement, Address>) map.clone();
  }

  void setAddress(Address paddr,InterpreterElement pel){
    addr.put(pel, paddr);
  }
  Address getAddress(InterpreterElement pel){
    cnt=0;
    while( pel!=null&&addr.containsKey(pel)== false ){
      AMCcnt++;
      pel = pel.getprev();
      cnt++;
    }
    Adpt.add(cnt);
    if(pel==null){
      throw new RuntimeException("No Address for given InterpreterElement");
    }
    return addr.get(pel);
  }


  @Override
  public CellType getType() {
    return CellType.AMC;
  }

 /* @Override
  public AddrMemoryCell clone(){
    if(clone != null){
      return clone;
    }else{
      clone = new AddrMemoryCell(addr.clone());
      return clone;
    }
  }*/
  @Override
  public AddrMemoryCell copy(){

   return new AddrMemoryCell(addr);

  }

}
