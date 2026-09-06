// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.to_svlib;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibCheckTrueTag;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibProcedureDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibAnnotateTagCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibProcedureDefinitionCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands.SvLibProceduresRecDefinitionCommand;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements.SvLibStatement;
import org.sosy_lab.cpachecker.core.specification.Property;
import org.sosy_lab.cpachecker.core.specification.Property.CommonVerificationProperty;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;

class CToSvLibPropertyEncoder {

  private final Specification specification;

  /**
   * The tags that are already annotated. A specification file and the properties that are derived
   * from it describe the same property, so both would otherwise annotate the same tag, and a tag
   * that is annotated more than once cannot be reported as the violated property of a
   * counterexample.
   */
  private final Set<String> annotatedTags = new HashSet<>();

  CToSvLibPropertyEncoder(Specification pSpecification) {
    specification = pSpecification;
  }

  /** Whether the encoding of the property annotated any tag of the generated program. */
  boolean hasEncodedProperty() {
    return !annotatedTags.isEmpty();
  }

  void encodeProperty(ImmutableList.Builder<SvLibCommand> pCommandsCollector) {
    // The specification is matched by what it was parsed into, and not by the names of the files it
    // was read from, because the same property can be given in different files, and because a
    // specification file can include others (specification/default.spc for example consists of the
    // automata of specification/Assertion.spc, specification/ErrorLabel.spc and
    // specification/TerminatingFunctions.spc).
    for (Property property : specification.getProperties()) {
      switch (property) {
        case CommonVerificationProperty.REACHABILITY_LABEL ->
            encodeReachabilityErrorLabel(pCommandsCollector);
        case CommonVerificationProperty.REACHABILITY ->
            encodeReachabilityOfProcedure("__VERIFIER_error", pCommandsCollector);
        case CommonVerificationProperty.REACHABILITY_ERROR ->
            encodeReachabilityOfProcedure("reach_error", pCommandsCollector);
        case CommonVerificationProperty.CORRECT_ANNOTATIONS -> {
          // The annotations of the generated script are created by this transformation, so there is
          // nothing to encode for them.
        }
        default ->
            throw new UnsupportedOperationException(
                "Encoding for property "
                    + property
                    + " is not supported in the transformation to SV-LIB.");
      }
    }

    // A property file also contributes the automaton that checks its property, so that automaton is
    // already covered by the loop above and must not be encoded again below. This only matters for
    // the automaton of the reachability properties, whose two variants cannot be distinguished from
    // each other by their name.
    boolean reachabilityIsCoveredByProperty =
        !Sets.intersection(
                specification.getProperties(),
                ImmutableSet.of(
                    CommonVerificationProperty.REACHABILITY_LABEL,
                    CommonVerificationProperty.REACHABILITY,
                    CommonVerificationProperty.REACHABILITY_ERROR))
            .isEmpty();

    for (Automaton automaton : specification.getSpecificationAutomata()) {
      switch (automaton.getName()) {
        case "AssertionAutomaton" -> {
          encodeReachabilityOfProcedure("__assert_fail", pCommandsCollector);
          encodeReachabilityOfProcedure("__assert_func", pCommandsCollector);
        }
        case "ErrorLabelAutomaton" -> encodeReachabilityErrorLabel(pCommandsCollector);
        case "SVCOMP" -> {
          // Used by both specification/sv-comp-reachability.spc and
          // specification/sv-comp-errorlabel.spc, which have the same automaton name. If a
          // reachability property was given, it already states which of the two is checked.
          // Otherwise this automaton was given directly and the tags of both are annotated.
          if (!reachabilityIsCoveredByProperty) {
            encodeReachabilityOfProcedure("reach_error", pCommandsCollector);
            encodeReachabilityOfProcedure("__VERIFIER_error", pCommandsCollector);
            encodeReachabilityErrorLabel(pCommandsCollector);
          }
        }
        case "TerminatingFunctions" -> {
          // This automaton only stops the analysis at calls to abort() and exit(), which the
          // transformation already encodes as a procedure that assumes false.
        }
        case "CorrectAnnotations" -> {
          // The annotations of the generated script are created by this transformation, so there is
          // nothing to encode for them.
        }
        default ->
            throw new UnsupportedOperationException(
                "Encoding of the specification automaton "
                    + automaton.getName()
                    + " is not supported for the transformation to SV-LIB.");
      }
    }
  }

  private void encodeReachabilityErrorLabel(
      ImmutableList.Builder<SvLibCommand> pCommandsCollector) {
    // Encode reachability of ERROR label
    for (SvLibCommand command : pCommandsCollector.build()) {
      if (command instanceof SvLibProcedureDefinitionCommand pProcedureDefinitionCommand) {
        visitBodyAndEncodeErrorLabel(pProcedureDefinitionCommand.getBody(), pCommandsCollector);
      } else if (command
          instanceof SvLibProceduresRecDefinitionCommand pProceduresRecDefinitionCommand) {
        for (SvLibStatement body : pProceduresRecDefinitionCommand.getBodies()) {
          visitBodyAndEncodeErrorLabel(body, pCommandsCollector);
        }
      }
    }
  }

  private void visitBodyAndEncodeErrorLabel(
      SvLibStatement pBody, ImmutableList.Builder<SvLibCommand> pCommandsCollector) {
    ImmutableList.Builder<SvLibTagReference> errorLabelTagReferencesCollector =
        ImmutableList.builder();
    CToSvLibErrorLabelEncodingVisitor errorLabelEncodingVisitor =
        new CToSvLibErrorLabelEncodingVisitor(errorLabelTagReferencesCollector);
    pBody.accept(errorLabelEncodingVisitor);

    ImmutableList<SvLibTagReference> errorLabelTagReferences =
        errorLabelTagReferencesCollector.build();

    for (SvLibTagReference tagReference : errorLabelTagReferences) {
      addFalseAnnotateTagCommand(tagReference, pCommandsCollector);
    }
  }

  private void encodeReachabilityOfProcedure(
      String pProcedureName, ImmutableList.Builder<SvLibCommand> pCommandsCollector) {
    for (SvLibCommand command : pCommandsCollector.build()) {
      if (command instanceof SvLibProcedureDefinitionCommand procedureDefinitionCommand) {
        SvLibProcedureDeclaration procedureDeclaration =
            procedureDefinitionCommand.getProcedureDeclaration();
        if (procedureDeclaration.getProcedureName().equals(pProcedureName)) {
          addFalseAnnotateTagCommand(
              procedureDefinitionCommand.getBody().getTagReferences().getFirst(),
              pCommandsCollector);
          return;
        }

      } else if (command
          instanceof SvLibProceduresRecDefinitionCommand proceduresRecDefinitionCommand) {
        List<ProcedureDefinition> procedureDefinitions =
            Streams.zip(
                    proceduresRecDefinitionCommand.getProcedureDeclarations().stream(),
                    proceduresRecDefinitionCommand.getBodies().stream(),
                    ProcedureDefinition::new)
                .toList();

        for (ProcedureDefinition procedureDefinition : procedureDefinitions) {
          SvLibProcedureDeclaration procedureDeclaration = procedureDefinition.procedureDeclaration;
          SvLibStatement procedureBody = procedureDefinition.body;

          if (procedureDeclaration.getProcedureName().equals(pProcedureName)) {
            addFalseAnnotateTagCommand(
                procedureBody.getTagReferences().getFirst(), pCommandsCollector);
            return;
          }
        }
      }
    }
  }

  /** Annotate the given tag as unreachable, unless it is already annotated. */
  private void addFalseAnnotateTagCommand(
      SvLibTagReference pSvLibTagReference,
      ImmutableList.Builder<SvLibCommand> pCommandsCollector) {
    if (annotatedTags.add(pSvLibTagReference.getTagName())) {
      pCommandsCollector.add(
          new SvLibAnnotateTagCommand(
              pSvLibTagReference.getTagName(),
              ImmutableList.of(SvLibCheckTrueTag.checkFalse()),
              FileLocation.DUMMY));
    }
  }

  private record ProcedureDefinition(
      SvLibProcedureDeclaration procedureDeclaration, SvLibStatement body) {
    private ProcedureDefinition {
      Objects.requireNonNull(procedureDeclaration);
      Objects.requireNonNull(body);
    }
  }
}
