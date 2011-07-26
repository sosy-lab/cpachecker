/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.einterpreter;


import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.MemoryFactory;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.Scope;

public class InterpreterElement implements AbstractElement {
  private MemoryFactory factory;
  private Scope curscope;


  public InterpreterElement(){
    factory = new MemoryFactory();

    curscope = new Scope("global");

  }
  private InterpreterElement(MemoryFactory pfactory,  Scope pscope){
    factory = pfactory;
    curscope = pscope;

  }




  public MemoryFactory getFactory(){
    return factory;
  }

  public Scope getCurrentScope(){
    return curscope;
  }

  public void setCurrentScope(String name){
    Scope s = new Scope(name,curscope);
    curscope.setChild(s);
    curscope =s;
  }


  public void setCurrentScope(Scope pnew){
      curscope.setChild(pnew);

      curscope =curscope.getChildScope();
  }



  @Override
  public InterpreterElement clone(){

    return new InterpreterElement(factory,curscope.clone());
  }
}

