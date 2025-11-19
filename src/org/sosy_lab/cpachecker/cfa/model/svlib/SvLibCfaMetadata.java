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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSetMultimap;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SmtLibCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibSetOptionCommand;
import org.sosy_lab.cpachecker.core.specification.svlib.ast.SvLibTagProperty;
import org.sosy_lab.cpachecker.core.specification.svlib.ast.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public record SvLibCfaMetadata(
    ImmutableList<SmtLibCommand> smtLibCommands,
    ImmutableSetMultimap<CFANode, SvLibTagProperty> tagAnnotations,
    ImmutableSetMultimap<CFANode, SvLibTagReference> tagReferences,
    boolean exportCorrectnessWitness,
    boolean exportViolationWitness) {

  public SvLibCfaMetadata {
    checkNotNull(smtLibCommands);
    checkNotNull(tagAnnotations);
    checkNotNull(tagReferences);
  }

  // TODO: Currently there is an ongoing discussion on how to handle the witness output path in
  //  CPAchecker. Once a decision has been made, this method should be adapted accordingly, i.e.,
  //  either removed or modified to fit the new design.
  @SuppressWarnings("unused")
  Optional<Path> getExportWitnessPath() {
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
