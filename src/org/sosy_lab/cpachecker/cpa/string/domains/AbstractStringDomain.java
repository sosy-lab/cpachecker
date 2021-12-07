// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string.domains;

import org.sosy_lab.cpachecker.cpa.string.utils.Aspect;

/*
 * Interface for a generic abstract string domain.
 */
public interface AbstractStringDomain<T> {

  public Aspect<T> addNewAspect(String pVariable);

  public DomainType getType();

  public boolean isLessOrEqual(Aspect<?> first, Aspect<?> second);

  public Aspect<?> combineAspectsForStringConcat(Aspect<?> first, Aspect<?> second);


}
