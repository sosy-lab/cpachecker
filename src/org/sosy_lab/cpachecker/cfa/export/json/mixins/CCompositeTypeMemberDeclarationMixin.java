// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.mixins;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/**
 * This class is a mixin for {@link CCompositeTypeMemberDeclaration}.
 *
 * <p>It specifies the constructor to use during deserialization.
 */
public final class CCompositeTypeMemberDeclarationMixin {

  @SuppressWarnings("unused")
  @JsonCreator
  public CCompositeTypeMemberDeclarationMixin(
      @JsonProperty("type") CType pType, @JsonProperty("name") String pName) {}
}
