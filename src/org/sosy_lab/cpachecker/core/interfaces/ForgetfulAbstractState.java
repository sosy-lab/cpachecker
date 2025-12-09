// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;

public interface ForgetfulAbstractState extends AbstractState {

  /**
   * This method indicates the variables which should now be treated as a nondet, i.e., anything
   * related to them should be "forgotten".
   *
   * @return set of forgettable variables
   */
  Set<ASimpleDeclaration> getForgettableVariables();
}
