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

import java.util.HashMap;

import org.sosy_lab.cpachecker.cpa.einterpreter.InterpreterElement;


public class PersMemory {
  private MemoryFactory factory;

  private HashMap<InterpreterElement,Scope> curscopes;

  public PersMemory(InterpreterElement el){
    factory = new MemoryFactory();
    curscopes = new HashMap<InterpreterElement,Scope>();
    curscopes.put(el, new Scope("global",el));

  }





  public MemoryFactory getFactory(){
    return factory;
  }

  public Scope getCurrentScope(InterpreterElement el){
    Scope s;
    s = curscopes.get(el);
    while(s == null && el.getprev() != null){
      el = el.getprev();
      s = curscopes.get(el);
    }

    if(s==null){
      //TODO: not possible
    }

    return s;
  }

  public void setCurrentScope(String name, InterpreterElement el){
    Scope s = new Scope(name,getCurrentScope(el),el);

    curscopes.put(el, s);
  }

  public void redScope(InterpreterElement el){
    Scope s  = getCurrentScope(el);
    curscopes.put(el,s.getParentScope());
  }


 /* public void setCurrentScope(Scope pnew){

      curscope =pnew;
  }*/




}
