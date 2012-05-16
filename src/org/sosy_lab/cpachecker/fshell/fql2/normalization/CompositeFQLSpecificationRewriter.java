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
package org.sosy_lab.cpachecker.fshell.fql2.normalization;

import org.sosy_lab.cpachecker.fshell.fql2.ast.FQLSpecification;
import org.sosy_lab.cpachecker.fshell.fql2.ast.coveragespecification.CoverageSpecification;
import org.sosy_lab.cpachecker.fshell.fql2.ast.pathpattern.PathPattern;
import org.sosy_lab.cpachecker.fshell.fql2.normalization.coveragespecification.CoverageSpecificationRewriter;
import org.sosy_lab.cpachecker.fshell.fql2.normalization.pathpattern.PathPatternRewriter;

public class CompositeFQLSpecificationRewriter implements FQLSpecificationRewriter {

  private CoverageSpecificationRewriter mCoverRewriter;
  private PathPatternRewriter mPassingRewriter;

  public CompositeFQLSpecificationRewriter(CoverageSpecificationRewriter pCoverRewriter, PathPatternRewriter pPassingRewriter) {
    mCoverRewriter = pCoverRewriter;
    mPassingRewriter = pPassingRewriter;
  }

  @Override
  public FQLSpecification rewrite(FQLSpecification pSpecification) {
    CoverageSpecification lCover = pSpecification.getCoverageSpecification();
    PathPattern lPassing = pSpecification.getPathPattern();

    CoverageSpecification lNewCover = mCoverRewriter.rewrite(lCover);
    PathPattern lNewPassing = mPassingRewriter.rewrite(lPassing);

    if (lNewCover.equals(lCover) && lNewPassing.equals(lPassing)) {
      return pSpecification;
    }
    else {
      return new FQLSpecification(lNewCover, lNewPassing);
    }
  }

}
