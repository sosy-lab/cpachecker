// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.k3;

import org.sosy_lab.cpachecker.cfa.types.Type;

public sealed interface K3Type extends Type permits K3CustomType, K3ProductType, K3ProcedureType {

  static K3Type getTypeForString(String pType) {
    return switch (pType) {
      default -> new K3CustomType(pType);
    };
  }
}
