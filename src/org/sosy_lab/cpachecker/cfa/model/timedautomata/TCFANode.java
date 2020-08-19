// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
// 
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// 
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.timedautomata;

import com.google.common.base.Optional;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableCondition;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class TCFANode extends CFANode {

  private final String name;
  private final Optional<TaVariableCondition> invariant;
  private final boolean isInitialState;
  private final boolean isErrorLocation;

  private static final long serialVersionUID = -7796108813615096804L;

  public TCFANode(
      String pName,
      Optional<TaVariableCondition> pInvariant,
      TaDeclaration pDeclaration,
      boolean pIsInitialState,
      boolean pIsErrorLocation) {
    super(pDeclaration);
    name = pName;
    invariant = pInvariant;
    isInitialState = pIsInitialState;
    isErrorLocation = pIsErrorLocation;
  }

  public String getName() {
    return getAutomatonDeclaration().getName() + "#" + name;
  }

  public String getShortName() {
    return name;
  }

  public boolean isInitialState() {
    return isInitialState;
  }

  public Optional<TaVariableCondition> getInvariant() {
    return invariant;
  }

  public TaDeclaration getAutomatonDeclaration() {
    return (TaDeclaration) getFunction();
  }

  public boolean isErrorLocation() {
    return isErrorLocation;
  }
}
