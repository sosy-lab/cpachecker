// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.data_entity;

public class Value implements SeqDataEntity {

  private final String value;

  public Value(String pValue) {
    value = pValue;
  }

  @Override
  public String createString() {
    return value;
  }
}
