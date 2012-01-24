/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.fshell.fql2.ast.pathpattern;

public class Concatenation implements PathPattern {

  private PathPattern mFirstSubpattern;
  private PathPattern mSecondSubpattern;

  public Concatenation(PathPattern pFirstSubpattern, PathPattern pSecondSubpattern) {
    mFirstSubpattern = pFirstSubpattern;
    mSecondSubpattern = pSecondSubpattern;
  }

  public PathPattern getFirstSubpattern() {
    return mFirstSubpattern;
  }

  public PathPattern getSecondSubpattern() {
    return mSecondSubpattern;
  }

  @Override
  public <T> T accept(PathPatternVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public String toString() {
    String lFirstSubpatternString;

    if (mFirstSubpattern instanceof Concatenation || mFirstSubpattern instanceof Union) {
      lFirstSubpatternString = "(" + mFirstSubpattern.toString() + ")";
    }
    else {
      lFirstSubpatternString = mFirstSubpattern.toString();
    }

    String lSecondSubpatternString;

    if (mSecondSubpattern instanceof Concatenation || mSecondSubpattern instanceof Union) {
      lSecondSubpatternString = "(" + mSecondSubpattern.toString() + ")";
    }
    else {
      lSecondSubpatternString = mSecondSubpattern.toString();
    }

    return lFirstSubpatternString + "." + lSecondSubpatternString;
  }

}
