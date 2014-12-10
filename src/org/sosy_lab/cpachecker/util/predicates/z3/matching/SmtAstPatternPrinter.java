/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.z3.matching;

import java.io.IOException;


public class SmtAstPatternPrinter {

  public static void print(Appendable pOut, SmtAstPattern pPattern) throws IOException {
    internalPrint(pOut, pPattern, 0);
  }

  public static void print(Appendable pOut, SmtAstPatternSelection pPattern) throws IOException {
    internalPrint(pOut, pPattern, 0);
  }


  private static void internalPrint(Appendable pOut, SmtAstPatternSelection pPattern, int pDepth) throws IOException {
    pOut.append(String.format("%s\n", pPattern.getRelationship()));
    for (SmtAstPattern elementPattern: pPattern) {
      internalPrint(pOut, elementPattern, pDepth+1);
    }
  }

  private static void internalPrint(Appendable pOut, SmtAstPattern pPattern, int pDepth) throws IOException {
    if (pPattern instanceof SmtFunctionApplicationPattern) {
      SmtFunctionApplicationPattern pApp = (SmtFunctionApplicationPattern) pPattern;

      String ident = String.format("%" + Integer.valueOf(1 + pDepth * 4) + "s", "");
      pOut.append(String.format("%s%s\n", ident, pApp.toString()));

      for (SmtAstPattern argP: pApp.getArgumentPatterns(false)) {
        internalPrint(pOut, argP, pDepth+1);
      }
    }
  }

}
