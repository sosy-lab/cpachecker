// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.model.svlib;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSetMultimap;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SmtLibCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSetOptionCommand;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTagProperty;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public record SvLibCfaMetadata(
    List<SmtLibCommand> smtLibCommands,
    ImmutableSetMultimap<CFANode, SvLibTagProperty> tagAnnotations,
    ImmutableSetMultimap<CFANode, SvLibTagReference> tagReferences,
    boolean exportCorrectnessWitness,
    boolean exportViolationWitness) {

  public SvLibCfaMetadata {
    checkNotNull(smtLibCommands);
    checkNotNull(tagAnnotations);
    checkNotNull(tagReferences);
  }

  public Optional<Path> getExportWitnessPath() {
    List<SvLibSetOptionCommand> witnessOutputChannelCommands =
        FluentIterable.from(smtLibCommands)
            .filter(SvLibSetOptionCommand.class)
            .filter(
                command ->
                    command.getOption().equals(SvLibSetOptionCommand.OPTION_WITNESS_OUTPUT_CHANNEL))
            .toList();
    if (witnessOutputChannelCommands.isEmpty()) {
      return Optional.empty();
    } else {
      return Optional.of(Path.of(witnessOutputChannelCommands.getLast().getValue()));
    }
  }
}
