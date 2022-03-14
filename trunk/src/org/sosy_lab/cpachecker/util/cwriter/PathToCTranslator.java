// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter;

import java.util.Set;
import org.sosy_lab.common.Appender;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;

public class PathToCTranslator extends PathTranslator {

  PathToCTranslator() {}

  /**
   * Transform a single linear path into C code. The path needs to be loop free.
   *
   * <p>TODO: Detect loops in the paths and signal an error. Currently when there are loops, the
   * generated C code is invalid because there is a goto to a missing label.
   *
   * @param pPath The path.
   * @return An appender that generates C code.
   */
  public static Appender translateSinglePath(ARGPath pPath) {
    PathToCTranslator translator = new PathToCTranslator();

    translator.translateSinglePath0(pPath, new DefaultEdgeVisitor(translator));

    return translator.generateCCode();
  }

  /**
   * Transform a set of paths into C code. All paths need to have a single root, and all paths need
   * to be loop free.
   *
   * <p>TODO: Detect loops in the paths and signal an error. Currently when there are loops, the
   * generated C code is invalid because there is a goto to a missing label.
   *
   * @param argRoot The root of all given paths.
   * @param elementsOnErrorPath The set of states that are on all paths.
   * @return An appender that generates C code.
   */
  public static Appender translatePaths(ARGState argRoot, Set<ARGState> elementsOnErrorPath) {
    PathToCTranslator translator = new PathToCTranslator();

    translator.translatePaths0(argRoot, elementsOnErrorPath, new DefaultEdgeVisitor(translator));

    return translator.generateCCode();
  }

  @Override
  protected Appender generateCCode() {
    mGlobalDefinitionsList.add("extern void __VERIFIER_error(void);");
    return super.generateCCode();
  }

  @Override
  protected String getTargetState() {
    return "__VERIFIER_error(); // target state";
  }
}
