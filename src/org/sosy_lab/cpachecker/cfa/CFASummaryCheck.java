// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa;

import com.google.common.base.VerifyException;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;

public class CFASummaryCheck extends CFACheck {

  /**
   * Traverse the CFA and run a series of checks at each node
   *
   * @param cfa Node to start traversal from
   * @param nodes Optional set of all nodes in the CFA (may be null)
   * @param machineModel model to get the size of types
   * @return true if all checks succeed
   * @throws VerifyException if not all checks succeed
   */
  public static boolean check(
      FunctionEntryNode cfa, @Nullable Set<CFANode> nodes, MachineModel machineModel)
      throws VerifyException {
    return CFACheck.check(cfa, nodes, machineModel);
  }
}
