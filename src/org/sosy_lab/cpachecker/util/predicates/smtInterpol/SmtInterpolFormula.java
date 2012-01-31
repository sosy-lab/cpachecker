/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.smtInterpol;

import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;

import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Term;

/** A Formula represented as a term. */
public class SmtInterpolFormula implements Formula {

  private final Term term;
  private final Script script;

  public SmtInterpolFormula(Term term, Script script) {
    this.term = term;
    this.script = script;
  }

  @Override
  public boolean isFalse() {
    return SmtInterpolUtil.isFalse(script, term);
  }

  @Override
  public boolean isTrue() {
    return SmtInterpolUtil.isTrue(script, term);

  }

  @Override
  public String toString() {
    return term.toString();
  }

  public Term getTerm() {
    return term;
  }

  @Override
  public int hashCode() {
    // return toString().hashCode();
    return term.hashCode(); // TODO working??
  }

  @Override
  public boolean equals(Object other) {
    return (other instanceof SmtInterpolFormula) &&
        this.hashCode() == other.hashCode(); // TODO working??
  }
}
