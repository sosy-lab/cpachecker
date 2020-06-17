// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
// 
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
// 
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.timedautomata;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;

public class TaDeclaration extends AFunctionDeclaration {

  private static final long serialVersionUID = 1L;
  private final Set<String> clocks;
  private final Set<String> actions;

  public TaDeclaration(
      FileLocation pFileLocation, String pName, Set<String> pClocks, Set<String> pActions) {
    super(pFileLocation, CFunctionType.NO_ARGS_VOID_FUNCTION, pName, pName, new ArrayList<>());
    clocks = pClocks;
    actions = pActions;
  }

  public Set<String> getClocks() {
    return ImmutableSet.copyOf(clocks);
  }

  public Set<String> getActions() {
    return ImmutableSet.copyOf(actions);
  }

  @Override
  public <
          R,
          R1 extends R,
          R2 extends R,
          X1 extends Exception,
          X2 extends Exception,
          V extends CAstNodeVisitor<R1, X1> & JAstNodeVisitor<R2, X2>>
      R accept_(V pV) throws X1, X2 {
    return null;
  }
}
