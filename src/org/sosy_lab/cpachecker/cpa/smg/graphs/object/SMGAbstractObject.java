// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object;

public interface SMGAbstractObject {

  boolean isAbstract();

  boolean matchGenericShape(SMGAbstractObject pOther);

  boolean matchSpecificShape(SMGAbstractObject pOther);

  SMGObjectKind getKind();
}
