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
package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis.lasso_ranker;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.Logics;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.logic.Theory;
import de.uni_freiburg.informatik.ultimate.modelcheckerutils.cfg.variables.IProgramVar;

@Immutable
public class RankVar implements IProgramVar {

  private final Theory THEROY = new Theory(Logics.ALL);

  private final String idenifier;
  private final boolean isGlobal;
  private final Term term;

  public RankVar(String pIdenifier, boolean pIsGlobal, Term pTerm) {
    idenifier = checkNotNull(pIdenifier);
    isGlobal = pIsGlobal;
    term = checkNotNull(pTerm);
  }

 public Term getDefinition() {
   return term;
 }

  @Override
  public String getIdentifier() {
    return getGloballyUniqueId();
  }

  @Override
  @Nullable
  public String getProcedure() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isGlobal() {
    return isGlobal;
  }

  @Override
  public boolean isOldvar() {
    return false;
  }

  @Override
  public TermVariable getTermVariable() {
    return THEROY.createTermVariable(idenifier, term.getSort());
  }

  @Override
  public ApplicationTerm getDefaultConstant() {
    throw new UnsupportedOperationException();
  }

  @Override
  public ApplicationTerm getPrimedConstant() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getGloballyUniqueId() {
    return idenifier;
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
    if (!(pObj instanceof RankVar)) {
      return false;
    }

    RankVar that = (RankVar) pObj;
    return this.getGloballyUniqueId().equals(that.getGloballyUniqueId());
  }

  @Override
  public String toString() {
    return getGloballyUniqueId();
  }
}
