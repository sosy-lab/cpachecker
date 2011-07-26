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

public class DataMemoryCell implements MemoryCell{
  private byte data;
  private DataMemoryCell clone = null;


  public DataMemoryCell( ){
    data =0;


  }
  private DataMemoryCell(byte pvalue){
    data=pvalue;


  }

  public byte getData(){
    return data;
  }
  public void setData(byte pdata){
    data = pdata;

  }



  @Override
  public CellType getType() {
    // TODO Auto-generated method stub
    return CellType.DMC;
  }

  @Override
  public DataMemoryCell clone(){
    if(clone!=null){
      return clone;
    }else{
      clone = new DataMemoryCell(data);
      return clone;
    }
  }
  @Override
  public MemoryCell copy() {
    // TODO Auto-generated method stub
    return  new DataMemoryCell(data);
  }


}
