// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.mixins;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;

/**
 * This class is a mixin for {@link AAstNode}.
 *
 * <p>Type information is being serialized to account for subtype polymorphism.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.CLASS,
    include = JsonTypeInfo.As.PROPERTY,
    property = "typeOfAAstNode")
public final class AAstNodeMixin {}
