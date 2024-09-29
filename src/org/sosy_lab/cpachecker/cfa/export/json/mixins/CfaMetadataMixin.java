// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.mixins;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;

/**
 * This class is a mixin for {@link CfaMetadata}.
 *
 * <p>It ensures that the {@link AstCfaRelation} is not being serialized.
 */
public final class CfaMetadataMixin {

  @SuppressWarnings("unused")
  @JsonIgnore
  private AstCfaRelation astCFARelation;
}
