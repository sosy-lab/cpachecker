// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.serialization;

import com.fasterxml.jackson.databind.util.StdConverter;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.export.json.CfaJsonExport;
import org.sosy_lab.cpachecker.cfa.export.json.mixins.VariableClassificationMixin;

/**
 * A converter that transforms a set of {@link String} objects into a sorted list of {@link String}
 * objects.
 *
 * <p>The sorting is based on the natural ordering of the {@link String} objects.
 *
 * @see CfaJsonExport
 * @see VariableClassificationMixin
 */
public final class StringSetToSortedListConverter extends StdConverter<Set<String>, List<String>> {

  @Override
  public List<String> convert(Set<String> pSet) {
    return pSet.stream().sorted().toList();
  }
}
