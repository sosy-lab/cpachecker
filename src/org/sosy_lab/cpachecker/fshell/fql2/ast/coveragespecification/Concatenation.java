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
package org.sosy_lab.cpachecker.fshell.fql2.ast.coveragespecification;

public class Concatenation implements CoverageSpecification {

  private CoverageSpecification mFirstSubspecification;
  private CoverageSpecification mSecondSubspecification;

  public Concatenation(CoverageSpecification pFirstSubspecification, CoverageSpecification pSecondSubspecification) {
    mFirstSubspecification = pFirstSubspecification;
    mSecondSubspecification = pSecondSubspecification;
  }

  public CoverageSpecification getFirstSubspecification() {
    return mFirstSubspecification;
  }

  public CoverageSpecification getSecondSubspecification() {
    return mSecondSubspecification;
  }

  @Override
  public <T> T accept(CoverageSpecificationVisitor<T> pVisitor) {
    return pVisitor.visit(this);
  }

  @Override
  public String toString() {
    String lFirstSubspecificationString;

    if (mFirstSubspecification instanceof Concatenation || mFirstSubspecification instanceof Union) {
      lFirstSubspecificationString = "(" + mFirstSubspecification.toString() + ")";
    }
    else {
      lFirstSubspecificationString = mFirstSubspecification.toString();
    }

    String lSecondSubspecificationString;

    if (mSecondSubspecification instanceof Concatenation || mSecondSubspecification instanceof Union) {
      lSecondSubspecificationString = "(" + mSecondSubspecification.toString() + ")";
    }
    else {
      lSecondSubspecificationString = mSecondSubspecification.toString();
    }

    return lFirstSubspecificationString + "." + lSecondSubspecificationString;
  }

}
