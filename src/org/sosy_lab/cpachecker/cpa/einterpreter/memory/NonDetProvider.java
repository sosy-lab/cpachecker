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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.sosy_lab.cpachecker.cpa.einterpreter.InterpreterElement;
public class NonDetProvider {
  private ArrayList<BigInteger> values;
  private HashMap<InterpreterElement, Integer>indexs=null;
  private Random rnd;
  private long t1;
  public NonDetProvider(BigInteger pvalues[],InterpreterElement el){
    indexs = new HashMap<InterpreterElement, Integer>();
    indexs.put(el, 0);
    values = new ArrayList<BigInteger>(pvalues.length);
    for(int x=0;x<pvalues.length;x++){
      values.add(pvalues[x]);
    }
    rnd = new Random();
  }

  public BigInteger getValue(InterpreterElement el) throws Exception{

    BigInteger numb;
    InterpreterElement pel = el;
    Integer index = null;
    while(index == null && pel != null){
      index = indexs.get(pel);
      pel = pel.getprev();
    }

    if(index < values.size()){
       numb = values.get(index);
       index++;
       indexs.put(el, index);
    }else{
      throw new Exception("Rnd number currently not supported");
     /* numb = BigInteger.valueOf(rnd.nextLong());
      values.add(index, numb);
      index++;
      indexs.put(el,index);*/
    }

   // System.out.println("__BLAST_NONDET returns " + numb);
    return numb;
  }


}
