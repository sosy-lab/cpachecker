// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity;

public class Variable implements SeqDataEntity {

  public final String name;

  public Variable(String pName) {
    name = pName;
  }

  @Override
  public String toString() {
    return name;
  }
}