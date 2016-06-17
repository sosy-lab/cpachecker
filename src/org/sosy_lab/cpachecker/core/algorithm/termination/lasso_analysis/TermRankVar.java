/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis;

import com.google.common.base.Preconditions;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nullable;

import de.uni_freiburg.informatik.ultimate.lassoranker.variables.RankVar;
import de.uni_freiburg.informatik.ultimate.logic.Term;

@SuppressFBWarnings(value = "SE_BAD_FIELD", justification = "TermRankVar is never serialized.")
public class TermRankVar extends RankVar {

  private static final long serialVersionUID = 1L;

  private final Term term;

  @Nullable private String id;

  public TermRankVar(Term pTerm) {
    term = Preconditions.checkNotNull(pTerm);
  }

  @Override
  public Term getDefinition() {
    return term;
  }

  @Override
  public String getGloballyUniqueId() {
    if (id!=null) {
      return id;
    }
    String id = term.toStringDirect();
    return id;
  }

  @Override
  public int hashCode() {
    return getGloballyUniqueId().hashCode();
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (!(pObj instanceof TermRankVar)) {
      return false;
    }

    TermRankVar that = (TermRankVar) pObj;
    return this.getGloballyUniqueId().equals(that.getGloballyUniqueId());
  }

  @Override
  public String toString() {
    return getGloballyUniqueId();
  }

}
