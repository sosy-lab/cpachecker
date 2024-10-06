// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.serialization;

import com.fasterxml.jackson.databind.util.StdConverter;
import com.google.common.collect.ImmutableListMultimap;
import org.sosy_lab.cpachecker.cfa.export.json.CfaJsonExport;
import org.sosy_lab.cpachecker.cfa.export.json.mixins.LoopStructureMixin;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

/**
 * A converter that sorts a multimap of {@link Loop} objects with {@link String} keys.
 *
 * <p>The sorting is based on the natural order of the keys of the multimap.
 *
 * @see CfaJsonExport
 * @see LoopStructureMixin
 */
public final class LoopsMultimapSorterConverter
    extends StdConverter<ImmutableListMultimap<String, Loop>, ImmutableListMultimap<String, Loop>> {

  @Override
  public ImmutableListMultimap<String, Loop> convert(
      ImmutableListMultimap<String, Loop> pMultimap) {

    ImmutableListMultimap.Builder<String, Loop> builder = ImmutableListMultimap.builder();
    pMultimap.keySet().stream().sorted().forEach(key -> builder.putAll(key, pMultimap.get(key)));

    return builder.build();
  }
}
