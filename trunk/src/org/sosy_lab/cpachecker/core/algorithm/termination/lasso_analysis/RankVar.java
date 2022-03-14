// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis;

import static com.google.common.base.Preconditions.checkNotNull;

import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.variables.IProgramVar;
import de.uni_freiburg.informatik.ultimate.logic.ApplicationTerm;
import de.uni_freiburg.informatik.ultimate.logic.Logics;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import de.uni_freiburg.informatik.ultimate.logic.TermVariable;
import de.uni_freiburg.informatik.ultimate.logic.Theory;

@SuppressWarnings("serial")
@javax.annotation.concurrent.Immutable // cannot prove deep immutability
public class RankVar implements IProgramVar {

  private static final Theory THEORY = new Theory(Logics.ALL);

  private final String identifier;
  private final boolean isGlobal;
  private final Term term;

  public RankVar(String pIdenifier, boolean pIsGlobal, Term pTerm) {
    identifier = checkNotNull(pIdenifier);
    isGlobal = pIsGlobal;
    term = checkNotNull(pTerm);
  }

  @Override
  public Term getTerm() {
    return term;
  }

  @Override
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
    return THEORY.createTermVariable(identifier, term.getSort());
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
    return identifier;
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
    return getGloballyUniqueId().equals(that.getGloballyUniqueId());
  }

  @Override
  public String toString() {
    return getGloballyUniqueId();
  }
}
