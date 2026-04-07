// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformation;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import org.sosy_lab.cpachecker.cfa.ImmutableCFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class TailRecursionEliminationTest {

  private ImmutableCFA tailRecursiveCFA;
  private ImmutableCFA nonTailRecursiveCFA;
  private FunctionEntryNode N1;
  private String mainProgram;
  private String tailRecursiveAdd;
  private String nonTailRecursiveAdd;

  @Before
  public void init(){
    mainProgram = """
    int main(int argc, char** argv){
      return add(2,3);
    }
    """;
    tailRecursiveAdd = """
    unsigned int add(unsigned int a, unsigned int b){
      if(b == 0){
        return a;
      } else {
        return add(a+1, b-1);
      }
    }
    """;
    nonTailRecursiveAdd = """
    unsigned int add(unsigned int a, unsigned int b){
      unsigned int c;
      c = a + b;
      return c;
    }
    """;
    try{
      String test = tailRecursiveAdd + mainProgram;
      tailRecursiveCFA = TestDataTools.makeCFA(tailRecursiveAdd, mainProgram);
    } catch (InterruptedException | ParserException pE) {
      throw new RuntimeException(pE);
    }
    try{
      nonTailRecursiveCFA = TestDataTools.makeCFA(nonTailRecursiveAdd, mainProgram);
    } catch (InterruptedException | ParserException pE) {
      throw new RuntimeException(pE);
    }


  }

  @Test
  public void testSuccessfulTransformation(){
    Optional<SubCFA> successfulTransformation = new TailRecursionEliminationProgramTransformation().transform(tailRecursiveCFA, tailRecursiveCFA.getAllFunctions().get("add"));
    Assert.assertFalse(successfulTransformation.isEmpty());
  }

  @Test
  public void testUnsuccessfulTransformation(){
    Optional<SubCFA> successfulTransformation = new TailRecursionEliminationProgramTransformation().transform(nonTailRecursiveCFA, nonTailRecursiveCFA.getAllFunctions().get("add"));
    Assert.assertTrue(successfulTransformation.isEmpty());
  }
}
