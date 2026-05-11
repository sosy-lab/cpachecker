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
import com.google.common.collect.Streams;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBooleanConstantTerm;
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

class CToSvLibPropertyEncoder {

  private final Specification specification;

  CToSvLibPropertyEncoder(Specification pSpecification) {
    specification = pSpecification;
  }

  void encodeProperty(ImmutableList.Builder<SvLibCommand> pCommandsCollector) {
    // Encode properties based on specification files, since currently specification.getProperties
    // is always null
    ImmutableSet<Path> specificationFiles = specification.getFiles();
    for (Path specificationFile : specificationFiles) {
      if (specificationFile.toString().endsWith("ErrorLabel.spc")
          || specificationFile.toString().endsWith("sv-comp-errorlabel.spc")) {
        encodeReachabilityErrorLabel(pCommandsCollector);
      } else if (specificationFile.toString().endsWith("sv-comp-reachability.spc")) {
        encodeReachability_reach_error(pCommandsCollector);
        encodeReachability_VERIFIER_error(pCommandsCollector);
      } else if (specificationFile.toString().endsWith("Assertion.spc")) {
        encodeReachability_assert_fail(pCommandsCollector);
        encodeReachability_assert_func(pCommandsCollector);
      } else if (specificationFile.toString().endsWith("default.spc")) {
        encodeReachability_assert_fail(pCommandsCollector);
        encodeReachability_assert_func(pCommandsCollector);
        encodeReachabilityErrorLabel(pCommandsCollector);
      } else {
        throw new UnsupportedOperationException(
            "Encoding of the specification file "
                + specificationFile
                + " is not supported for the transformation to SV-LIB.");
      }
    }

    Set<Property> properties = specification.getProperties();
    for (Property property : properties) {
      switch (property) {
        case CommonVerificationProperty.REACHABILITY_LABEL ->
            encodeReachabilityErrorLabel(pCommandsCollector);
        case CommonVerificationProperty.REACHABILITY ->
            encodeReachabilityOfProcedure("__VERIFIER_error", pCommandsCollector);
        case CommonVerificationProperty.REACHABILITY_ERROR ->
            encodeReachabilityOfProcedure("reach_error", pCommandsCollector);
        default ->
            throw new UnsupportedOperationException(
                "Encoding for property "
                    + property
                    + " is not supported in the transformation to SV-LIB.");
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
      SvLibAnnotateTagCommand annotateTagCommand_ERROR =
          createFalseAnnotateTagCommand(tagReference);
      pCommandsCollector.add(annotateTagCommand_ERROR);
    }
  }

  private void encodeReachability_VERIFIER_error(
      ImmutableList.Builder<SvLibCommand> pCommandsCollector) {
    encodeReachabilityOfProcedure("__VERIFIER_error", pCommandsCollector);
  }

  private void encodeReachability_reach_error(
      ImmutableList.Builder<SvLibCommand> pCommandsCollector) {
    encodeReachabilityOfProcedure("reach_error", pCommandsCollector);
  }

  private void encodeReachability_assert_fail(
      ImmutableList.Builder<SvLibCommand> pCommandsCollector) {
    encodeReachabilityOfProcedure("__assert_fail", pCommandsCollector);
  }

  private void encodeReachability_assert_func(
      ImmutableList.Builder<SvLibCommand> pCommandsCollector) {
    encodeReachabilityOfProcedure("__assert_func", pCommandsCollector);
  }

  private void encodeReachabilityOfProcedure(
      String pProcedureName, ImmutableList.Builder<SvLibCommand> pCommandsCollector) {
    for (SvLibCommand command : pCommandsCollector.build()) {
      if (command instanceof SvLibProcedureDefinitionCommand procedureDefinitionCommand) {
        SvLibProcedureDeclaration procedureDeclaration =
            procedureDefinitionCommand.getProcedureDeclaration();
        if (procedureDeclaration.getProcedureName().startsWith(pProcedureName)) {
          SvLibAnnotateTagCommand annotateTagCommandProcedureCall =
              createFalseAnnotateTagCommand(
                  procedureDefinitionCommand.getBody().getTagReferences().getFirst());
          pCommandsCollector.add(annotateTagCommandProcedureCall);
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

          if (procedureDeclaration.getProcedureName().startsWith(pProcedureName)) {
            SvLibAnnotateTagCommand annotateTagCommandProcedureCall =
                createFalseAnnotateTagCommand(procedureBody.getTagReferences().getFirst());
            pCommandsCollector.add(annotateTagCommandProcedureCall);
            return;
          }
        }
      }
    }
  }

  private SvLibAnnotateTagCommand createFalseAnnotateTagCommand(
      SvLibTagReference pSvLibTagReference) {

    return new SvLibAnnotateTagCommand(
        pSvLibTagReference.getTagName(),
        ImmutableList.of(
            new SvLibCheckTrueTag(
                new SvLibBooleanConstantTerm(false, FileLocation.DUMMY), FileLocation.DUMMY)),
        FileLocation.DUMMY);
  }

  private record ProcedureDefinition(
      SvLibProcedureDeclaration procedureDeclaration, SvLibStatement body) {
    private ProcedureDefinition {
      Objects.requireNonNull(procedureDeclaration);
      Objects.requireNonNull(body);
    }
  }
}
