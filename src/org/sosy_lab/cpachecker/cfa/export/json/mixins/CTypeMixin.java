// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.mixins;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;

/**
 * This class is a mixin for {@link CType}.
 *
 * <p>It sets the names to use for all relevant subtypes.
 */
@JsonSubTypes({
  @Type(value = CArrayType.class, name = "CArray"),
  @Type(value = CBitFieldType.class, name = "CBitField"),
  @Type(value = CComplexType.class, name = "CComplex"),
  @Type(value = CFunctionType.class, name = "CFunction"),
  @Type(value = CFunctionTypeWithNames.class, name = "CFunctionWithNames"),
  @Type(value = CPointerType.class, name = "CPointer"),
  @Type(value = CProblemType.class, name = "CProblem"),
  @Type(value = CSimpleType.class, name = "CSimple"),
  @Type(value = CTypedefType.class, name = "CTypedef"),
  @Type(value = CVoidType.class, name = "CVoid"),
})
public final class CTypeMixin {}
