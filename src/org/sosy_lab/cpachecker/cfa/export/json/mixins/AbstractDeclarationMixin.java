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
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AbstractDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;

/**
 * This class is a mixin for {@link AbstractDeclaration}.
 *
 * <p>It sets the names to use for all relevant subtypes.
 */
@JsonSubTypes({
  @Type(value = AFunctionDeclaration.class, name = "AFunctionDeclaration"),
  @Type(value = AVariableDeclaration.class, name = "AVariableDeclaration"),
  @Type(value = CTypeDeclaration.class, name = "CTypeDeclaration"),
})
public final class AbstractDeclarationMixin {}
