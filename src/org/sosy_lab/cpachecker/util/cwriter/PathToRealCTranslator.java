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
package org.sosy_lab.cpachecker.util.cwriter;

import java.util.Iterator;
import java.util.Set;

import org.sosy_lab.common.Appender;
import org.sosy_lab.cpachecker.core.counterexample.RichModel;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

public class PathToRealCTranslator extends PathTranslator {

  private PathToRealCTranslator() {}

  /**
   * Transform a single linear path into C code.
   * The path needs to be loop free.
   *
   * TODO: Detect loops in the paths and signal an error.
   * Currently when there are loops, the generated C code is invalid
   * because there is a goto to a missing label.
   *
   * @param pPath The path.
   * @param model The model
   * @return An appender that generates C code.
   */
  public static Appender translateSinglePath(ARGPath pPath, RichModel model) {
    PathToRealCTranslator translator = new PathToRealCTranslator();

    translator.translateSinglePath0(pPath,
        new RealCEdgeVisitor(translator, model.getExactVariableValuePath(pPath.getInnerEdges())));

    return translator.generateCCode();
  }

  /**
   * Transform a set of paths into C code.
   * All paths need to have a single root,
   * and all paths need to be loop free.
   *
   * TODO: Detect loops in the paths and signal an error.
   * Currently when there are loops, the generated C code is invalid
   * because there is a goto to a missing label.
   *
   * @param argRoot The root of all given paths.
   * @param elementsOnErrorPath The set of states that are on all paths.
   * @param model The model
   * @return An appender that generates C code.
   */
  public static Appender translatePaths(ARGState argRoot, Set<ARGState> elementsOnErrorPath, RichModel model) {
    PathToRealCTranslator translator = new PathToRealCTranslator();

    translator.translatePaths0(argRoot, elementsOnErrorPath,
        new RealCEdgeVisitor(translator, model.getCFAPathWithAssignments()));

    return translator.generateCCode();
  }

  @Override
  protected Appender generateCCode() {
    mGlobalDefinitionsList.add("#define __CPROVER_assume(x) if(!(x)){exit(0);} // safe because assumption didn't hold");

    for (Iterator<String> it = mGlobalDefinitionsList.iterator(); it.hasNext();) {
      String s = it.next();
      s = s.toLowerCase().trim();
      if (s.startsWith("void main()") || s.startsWith("int main()")) {
        it.remove();
      }
    }
    mGlobalDefinitionsList.add("void main() {main_0();}");

    return super.generateCCode();
  }

  @Override
  protected String getTargetState() {
    return "exit(1); // target state; all assumptions were true";
  }

}