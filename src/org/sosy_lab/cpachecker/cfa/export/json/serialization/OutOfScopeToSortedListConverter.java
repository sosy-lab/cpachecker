// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.serialization;

import com.fasterxml.jackson.databind.util.StdConverter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;

/**
 * A converter that transforms a set of {@link CSimpleDeclaration} objects into a sorted list of
 * {@link CSimpleDeclaration} objects.
 *
 * <p>The sorting is based on the hash code of the {@link CSimpleDeclaration} objects.
 */
public final class OutOfScopeToSortedListConverter
    extends StdConverter<Set<CSimpleDeclaration>, List<CSimpleDeclaration>> {

  @Override
  public List<CSimpleDeclaration> convert(Set<CSimpleDeclaration> pSet) {
    List<CSimpleDeclaration> list = new ArrayList<>(pSet);
    list.sort(Comparator.comparing(CSimpleDeclaration::hashCode));

    return list;
  }
}
