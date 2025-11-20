// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;

public class SeqParameterDeclarations {

  public static final CParameterDeclaration SIZE_PARAMETER_MALLOC =
      new CParameterDeclaration(FileLocation.DUMMY, CNumericTypes.UNSIGNED_INT, "size");
}
