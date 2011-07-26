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
package org.sosy_lab.cpachecker.cpa.einterpreter;

import org.junit.Test;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cpa.einterpreter.memory.MemoryException;



public class ParseTest {
  CParser c;
  @Test
  public void test() throws InvalidConfigurationException, MemoryException{

    /*InterpreterElement element = new InterpreterElement();



   c = CParser.Factory.getParser(null, CParser.Factory.getDefaultOptions());
   try {
    /*IASTNode test= c.parseSingleStatement("void test(int a, int b){ v=d*c;}");
    System.out.println(test.getRawSignature());
    CFA cfa= c.parseString(" const long x;const long y;char v; short w; int u;");
    Iterator<IASTDeclaration> k = cfa.getGlobalDeclarations().iterator();
    while (k.hasNext()){
    IASTDeclaration v = k.next();
    System.out.println(v.getName());
    InterpreterTransferRelation.handleSimpleDecl((IASTSimpleDeclSpecifier)v.getDeclSpecifier(), v.getName(),element);
    element.getCurrentScope().getVariable("x").getAddress().getMemoryBlock().setData(0, (byte) 34);
    }
  } catch (ParserException e) {
    // TODO Auto-generated catch block
    e.printStackTrace();
  } catch (MemoryException e) {
    // TODO Auto-generated catch block
    e.printStackTrace();
  } catch (Exception e) {
    // TODO Auto-generated catch block
    e.printStackTrace();
  }
  System.out.println(element.getCurrentScope().getVariable("x").getAddress().getMemoryBlock().getData(0));
  if(element.getCurrentScope().getVariable("x").getAddress().getMemoryBlock() == element.getCurrentScope().getVariable("y").getAddress().getMemoryBlock())
    System.out.println("negative");
  System.out.println(element.getCurrentScope().getVariable("x").isConst());

    Assert.assertTrue(true);*/
  }
}
