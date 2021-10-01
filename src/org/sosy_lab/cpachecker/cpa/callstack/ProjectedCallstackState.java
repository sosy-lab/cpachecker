// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.callstack;

public class ProjectedCallstackState extends CallstackState {
  private static final long serialVersionUID = 3250857147115751058L;

  private final static ProjectedCallstackState instance = new ProjectedCallstackState();

  private ProjectedCallstackState() {
    super(null, null, null);
  }

  @Override
  public Object getPartitionKey() {
    return null;
  }

  public static ProjectedCallstackState getInstance() {
    return instance;
  }
}
