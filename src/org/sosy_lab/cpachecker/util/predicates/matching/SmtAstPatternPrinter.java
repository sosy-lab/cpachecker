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
package org.sosy_lab.cpachecker.util.predicates.matching;

import java.io.IOException;


public class SmtAstPatternPrinter {

  public static void print(Appendable pOut, SmtAstPattern pPattern) throws IOException {
    internalPrint(pOut, pPattern, 0);
  }

  public static void print(Appendable pOut, SmtAstPatternSelection pPattern) throws IOException {
    internalPrint(pOut, pPattern, 0);
  }


  private static void internalPrint(Appendable pOut, SmtAstPatternSelection pPattern, int pDepth) throws IOException {
    String ident = String.format("%" + Integer.valueOf(1 + pDepth * 4) + "s", "");
    pOut.append(String.format("%s%s%n", ident, pPattern.getRelationship()));
    for (SmtAstPatternSelectionElement elementPattern: pPattern) {
      if (elementPattern instanceof SmtAstPatternSelection) {
        internalPrint(pOut, (SmtAstPatternSelection) elementPattern, pDepth+1);
      } else {
        internalPrint(pOut, (SmtAstPattern) elementPattern, pDepth+1);
      }
    }
  }

  private static void internalPrint(Appendable pOut, SmtAstPattern pPattern, int pDepth) throws IOException {
    if (pPattern instanceof SmtFunctionApplicationPattern) {
      SmtFunctionApplicationPattern pApp = (SmtFunctionApplicationPattern) pPattern;

      String ident = String.format("%" + Integer.valueOf(1 + pDepth * 4) + "s", "");
      pOut.append(String.format("%s%s%n", ident, pApp.toString()));

      for (SmtAstPatternSelectionElement argP: pApp.getArgumentPatterns(false)) {
        if (argP instanceof SmtAstPatternSelection) {
          internalPrint(pOut, (SmtAstPatternSelection) argP, pDepth+1);
        } else {
          internalPrint(pOut, (SmtAstPattern) argP, pDepth+1);
        }
      }
    }
  }

}
