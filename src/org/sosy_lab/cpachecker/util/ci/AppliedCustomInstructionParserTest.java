/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.ci;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.globalinfo.CFAInfo;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

import com.google.common.truth.Truth;

public class AppliedCustomInstructionParserTest {

  private CFAInfo cfaInfo;
  private CFA cfa;
  private AppliedCustomInstructionParser aciParser;
  private List<CLabelNode> labelNodes;

  @Before
  public void init() throws IOException, ParserException, InterruptedException {
    String testProgram = ""
          + "extern int test3(int);"
          + "int globalVar;"
          + "int test(int p) {"
            + "return p+1;"
          + "}"
          + "int test2(int p) {"
            + "start_ci: return p+2;"
          + "}"
          + "void ci(int var) {"
            + "var = var + 39;"
            + "int u;"
            + "int x = globalVar + 5;"
            + "int y;"
            + "int z;"
            + "start_ci:"
            + "if (z>0) {"
              + "y = y + 1;"
            + "} else {"
              + "var = var + 1;"
            + "}"
            + "test(u);"
            + "z = test(globalVar);"
            + "end_ci_1: x = x + 1;"
          + "}"
          + "void main() {"
            + "int m;"
            + "int n;"
            + "int o;"
            + "start_ci:"
            + "if (m>o) {"
              + "ci(m);"
            + "}"
            + "test3(n);"
            + "n = test3(o);"
            + "end_ci_2:"
            + "test2(4);"
          + "}";
    cfa = TestDataTools.makeCFA(testProgram);
    aciParser = new AppliedCustomInstructionParser(ShutdownNotifier.create(), cfa);
    GlobalInfo.getInstance().storeCFA(cfa);
    cfaInfo = GlobalInfo.getInstance().getCFAInfo().get();
    labelNodes = getLabelNodes(cfa);
  }

  @Test
  public void testGetCFANode() throws AppliedCustomInstructionParsingFailedException, IOException, InterruptedException {
    try {
      aciParser.getCFANode("N57", cfaInfo);
    } catch (CPAException e) {
      Truth.assertThat(e).isInstanceOf(AppliedCustomInstructionParsingFailedException.class);
    }

    try {
      aciParser.getCFANode("-1", cfaInfo);
    } catch (CPAException e) {
      Truth.assertThat(e).isInstanceOf(AppliedCustomInstructionParsingFailedException.class);
    }

    Truth.assertThat(aciParser.getCFANode(cfa.getFunctionHead("main").getNodeNumber() + "", cfaInfo)).isEqualTo(cfa.getMainFunction());
  }

  @Test
  public void testReadCustomInstruction() throws AppliedCustomInstructionParsingFailedException, InterruptedException, NoSuchFieldException, SecurityException {
    try {
      aciParser.readCustomInstruction("test4");
    } catch (CPAException e) {
      Truth.assertThat(e).isInstanceOf(AppliedCustomInstructionParsingFailedException.class);
      Truth.assertThat(e.getMessage()).matches("Function unknown in program");
    }

    try {
      aciParser.readCustomInstruction("test");
    } catch (CPAException e) {
      Truth.assertThat(e).isInstanceOf(AppliedCustomInstructionParsingFailedException.class);
      Truth.assertThat(e.getMessage()).matches("Missing label for start of custom instruction");
    }

    try {
      aciParser.readCustomInstruction("test2");
    } catch (CPAException e) {
      Truth.assertThat(e).isInstanceOf(AppliedCustomInstructionParsingFailedException.class);
      Truth.assertThat(e.getMessage()).matches("Missing label for end of custom instruction");
    }

    CustomInstruction ci = aciParser.readCustomInstruction("ci");
    CFANode expectedStart = null;
    Collection<CFANode> expectedEnds = new ArrayList<>(2);
    for(CLabelNode n: labelNodes){
      if(n.getLabel().startsWith("start_ci") && n.getFunctionName().equals("ci")) {
        expectedStart = n;
      }
      if(n.getLabel().startsWith("end_ci") && n.getFunctionName().equals("ci")) {
        for(CFANode e: CFAUtils.predecessorsOf(n)) {
          expectedEnds.add(e);
        }
      }
    }
    Truth.assertThat(ci.getStartNode()).isEqualTo(expectedStart);
    Truth.assertThat(ci.getEndNodes()).containsExactlyElementsIn(expectedEnds);
    // input variables: globalVar, ci::u, ci::var, ci::y, ci::z
    List<String> list = new ArrayList<>();
    list.add("ci::u");
    list.add("ci::var");
    list.add("ci::y");
    list.add("ci::z");
    list.add("globalVar");
    Truth.assertThat(ci.getInputVariables()).containsExactlyElementsIn(list).inOrder();
     //output variables: ci::var, ci::y, ci::z, test::p
    list = new ArrayList<>();
    list.add("ci::var");
    list.add("ci::y");
    list.add("ci::z");
    list.add("test::p");
    Truth.assertThat(ci.getOutputVariables()).containsExactlyElementsIn(list).inOrder();

    ci = aciParser.readCustomInstruction("main");
    expectedStart = null;
    expectedEnds = new ArrayList<>(2);
    for(CLabelNode n: labelNodes){
      if(n.getLabel().startsWith("start_ci") && n.getFunctionName().equals("main")) {
        expectedStart = n;
      }
      if(n.getLabel().startsWith("end_ci")) {
        for(CFANode e: CFAUtils.predecessorsOf(n)) {
          expectedEnds.add(e);
        }
      }
    }
    Truth.assertThat(ci.getStartNode()).isEqualTo(expectedStart);
    Truth.assertThat(ci.getEndNodes()).containsExactlyElementsIn(expectedEnds);
    // input variables: globalVar, main::m, main::n, main::o
    list = new ArrayList<>();
    list.add("globalVar");
    list.add("main::m");
    list.add("main::n");
    list.add("main::o");
     Truth.assertThat(ci.getInputVariables()).containsExactlyElementsIn(list).inOrder();
    // output variables: ci::u, ci::var, ci::x, ci::y, ci::z, main::n, test::p
    list = new ArrayList<>();
    list.add("ci::u");
    list.add("ci::var");
    list.add("ci::x");
    list.add("ci::y");
    list.add("ci::z");
    list.add("main::n");
    list.add("test::p");
    Truth.assertThat(ci.getOutputVariables()).containsExactlyElementsIn(list).inOrder();
  }

  private List<CLabelNode> getLabelNodes(CFA cfa){
    List<CLabelNode> result = new ArrayList<>();
    for(CFANode n: cfa.getAllNodes()) {
      if(n instanceof CLabelNode){
        result.add((CLabelNode) n);
      }
    }
    return result;
  }

  @Test
  public void testParse() throws AppliedCustomInstructionParsingFailedException, IOException, InterruptedException, NoSuchFieldException, SecurityException, ParserException {
    String testProgram = ""
        + "void main() {"
          + "int x;"
          + "int y;"
          + "start_ci: x = x + y;"
          + "end_ci_1:"
          + "x = x + x;"
          + "y = y + y;"
          + "y = y + x;"
        + "}";

    CFA cfa = TestDataTools.makeCFA(testProgram);
    aciParser = new AppliedCustomInstructionParser(ShutdownNotifier.create(), cfa);
    // TODO path does not exist need to adapt content of file
    // CustomInstructionApplications cia = aciParser.parse(new FileSystemPath("src", "org", "sosy_lab", "cpachecker", "util" , "ci","testParse.c"));
    // Map<CFANode, AppliedCustomInstruction> cis = cia.getMapping();

    //Truth.assertThat(cis).hasSize(5);
  }

}
