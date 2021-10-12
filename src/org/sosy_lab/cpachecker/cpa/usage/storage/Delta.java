// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage.storage;

public interface Delta<T> {
  public T apply(T pState);

  public boolean covers(Delta<T> pDelta);

  @Override
  public boolean equals(Object pDelta);

  @Override
  public int hashCode();

  public Delta<T> add(Delta<T> pDelta);

}
