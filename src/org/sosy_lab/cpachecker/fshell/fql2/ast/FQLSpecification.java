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
package org.sosy_lab.cpachecker.fshell.fql2.ast;

import org.sosy_lab.cpachecker.fshell.fql2.ast.coveragespecification.CoverageSpecification;
import org.sosy_lab.cpachecker.fshell.fql2.ast.filter.Identity;
import org.sosy_lab.cpachecker.fshell.fql2.ast.pathpattern.PathPattern;
import org.sosy_lab.cpachecker.fshell.fql2.ast.pathpattern.Repetition;
import org.sosy_lab.cpachecker.fshell.fql2.parser.FQLParser;

public class FQLSpecification {
  private CoverageSpecification mCoverageSpecification;
  private PathPattern mPathPattern;

  private static PathPattern mDefaultPassingClause = new Repetition(new Edges(Identity.getInstance()));

  public static PathPattern getDefaultPassingClause() {
    return mDefaultPassingClause;
  }

  public FQLSpecification(CoverageSpecification pCoverageSpecification, PathPattern pPathPattern) {
    mCoverageSpecification = pCoverageSpecification;
    mPathPattern = pPathPattern;
  }

  public FQLSpecification(CoverageSpecification pCoverageSpecification) {
    mCoverageSpecification = pCoverageSpecification;
    mPathPattern = null;
  }

  public CoverageSpecification getCoverageSpecification() {
    return mCoverageSpecification;
  }

  public boolean hasPassingClause() {
    return (mPathPattern != null);
  }

  public PathPattern getPathPattern() {
    if (!hasPassingClause()) {
      throw new UnsupportedOperationException();
    }

    return mPathPattern;
  }

  @Override
  public String toString() {
    if (hasPassingClause()) {
      return "COVER " + mCoverageSpecification.toString() + " PASSING " + mPathPattern.toString();
    }
    else {
      return "COVER " + mCoverageSpecification.toString();
    }
  }

  public static FQLSpecification parse(String pFQLSpecificationString) throws Exception {
    FQLParser lParser = new FQLParser(pFQLSpecificationString);

    Object pParseResult;

    try {
      pParseResult = lParser.parse().value;
    }
    catch (Exception e) {
      System.out.println(pFQLSpecificationString);

      throw e;
    }

    assert(pParseResult instanceof FQLSpecification);

    return (FQLSpecification)pParseResult;
  }

}
