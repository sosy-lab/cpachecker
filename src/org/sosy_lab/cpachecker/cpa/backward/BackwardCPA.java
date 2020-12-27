// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// Copyright (C) 2007-2016  Dirk Beyer
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.backward;

import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

public class BackwardCPA extends AbstractSingleWrapperCPA {

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(BackwardCPA.class);
  }

  public BackwardCPA(ConfigurableProgramAnalysis pCpa) {
    super(pCpa);
  }

  @Override
  public TransferRelation getTransferRelation() {
    return new BackwardTransferRelation(getWrappedCpa().getTransferRelation());
  }
}
