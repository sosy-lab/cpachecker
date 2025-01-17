// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.types.java;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * Description of a simple Java structure's type.
 *
 * <p>These descriptions are mostly merely primitive types, but include special cases like <code>
 * null</code> either. Actually, possible concrete types are all enum constants of {@link
 * JBasicType}.
 */
public enum JSimpleType implements JType {
  BOOLEAN(JBasicType.BOOLEAN),
  BYTE(JBasicType.BYTE),
  SHORT(JBasicType.SHORT),
  CHAR(JBasicType.CHAR),
  INT(JBasicType.INT),
  LONG(JBasicType.LONG),
  FLOAT(JBasicType.FLOAT),
  DOUBLE(JBasicType.DOUBLE),
  UNSPECIFIED(JBasicType.UNSPECIFIED),
  VOID(JBasicType.VOID);

  @Serial private static final long serialVersionUID = 7153757299840260748L;

  private final JBasicType type;

  private JSimpleType(JBasicType pType) {
    type = pType;
  }

  /**
   * Returns the concrete primitive type this class represents.
   *
   * @return the concrete primitive type this class represents
   */
  public JBasicType getType() {
    return type;
  }

  @Override
  public String toASTString(String pDeclarator) {
    List<String> parts = new ArrayList<>();

    parts.add(Strings.emptyToNull(type.toASTString()));
    parts.add(Strings.emptyToNull(pDeclarator));

    return Joiner.on(' ').skipNulls().join(parts);
  }

  @Override
  public String toString() {
    return switch (type) {
      case UNSPECIFIED -> "unspecified";
      default -> type.toASTString();
    };
  }
}
