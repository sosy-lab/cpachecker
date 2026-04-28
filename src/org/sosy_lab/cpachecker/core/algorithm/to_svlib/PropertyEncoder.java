// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.to_svlib;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
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

public class PropertyEncoder {

  private final Specification specification;

  public PropertyEncoder(Specification pSpecification) {
    specification = pSpecification;
  }

  void encodeProperty(ImmutableList.Builder<SvLibCommand> pCommandsCollector) {
    Set<Property> properties = specification.getProperties();
    for (Property property : properties) {
      if (property.equals(Property.CommonVerificationProperty.REACHABILITY_LABEL)) {
        encodeReachabilityLabel(pCommandsCollector);
      }

      // TODO SV-Comp reachability has 2 properties: REACHABILITY (__VERIFIER_error) &
      //  REACHABILITY_ERROR (reach_error)
      //  => need to split encoding accordingly
      if (property.equals(Property.CommonVerificationProperty.REACHABILITY)) {
        encodeReachability(pCommandsCollector);
      }
      if (property.equals(CommonVerificationProperty.REACHABILITY_ERROR)) {
        encodeReachabilityError(pCommandsCollector);
      }

      // TODO throw for other specs
    }
  }

  private void encodeReachabilityLabel(ImmutableList.Builder<SvLibCommand> pCommandsCollector) {
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
    ErrorLabelEncodingVisitor errorLabelEncodingVisitor =
        new ErrorLabelEncodingVisitor(errorLabelTagReferencesCollector);
    pBody.accept(errorLabelEncodingVisitor);

    ImmutableList<SvLibTagReference> errorLabelTagReferences =
        errorLabelTagReferencesCollector.build();

    for (SvLibTagReference tagReference : errorLabelTagReferences) {
      SvLibAnnotateTagCommand annotateTagCommand_ERROR =
          createFalseAnnotateTagCommand(tagReference);
      pCommandsCollector.add(annotateTagCommand_ERROR);
    }
  }

  private void encodeReachability(ImmutableList.Builder<SvLibCommand> pCommandsCollector) {
    // Encode reachability of call to __VERIFIER_error
    for (SvLibCommand command : pCommandsCollector.build()) {
      if (command instanceof SvLibProcedureDefinitionCommand procedureDefinitionCommand) {
        SvLibProcedureDeclaration procedureDeclaration =
            procedureDefinitionCommand.getProcedureDeclaration();
        if (procedureDeclaration.getProcedureName().startsWith("__VERIFIER_error")) {
          SvLibAnnotateTagCommand annnotateTagCommand_reach_error =
              createFalseAnnotateTagCommand(
                  procedureDefinitionCommand.getBody().getTagReferences().getFirst());
          pCommandsCollector.add(annnotateTagCommand_reach_error);
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

          if (procedureDeclaration.getProcedureName().startsWith("__VERIFIER_error")) {
            SvLibAnnotateTagCommand annnotateTagCommand_reach_error =
                createFalseAnnotateTagCommand(procedureBody.getTagReferences().getFirst());
            pCommandsCollector.add(annnotateTagCommand_reach_error);
            return;
          }
        }
      }
    }
  }

  private void encodeReachabilityError(ImmutableList.Builder<SvLibCommand> pCommandsCollector) {
    // Encode reachability of call to reach_error
    for (SvLibCommand command : pCommandsCollector.build()) {
      if (command instanceof SvLibProcedureDefinitionCommand procedureDefinitionCommand) {
        SvLibProcedureDeclaration procedureDeclaration =
            procedureDefinitionCommand.getProcedureDeclaration();
        if (procedureDeclaration.getProcedureName().startsWith("reach_error")) {

          SvLibAnnotateTagCommand annotateTagCommand_reach_error =
              createFalseAnnotateTagCommand(
                  procedureDefinitionCommand.getBody().getTagReferences().getFirst());
          pCommandsCollector.add(annotateTagCommand_reach_error);
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

          if (procedureDeclaration.getProcedureName().startsWith("reach_error")) {
            SvLibAnnotateTagCommand annotateTagCommand_reach_error =
                createFalseAnnotateTagCommand(procedureBody.getTagReferences().getFirst());
            pCommandsCollector.add(annotateTagCommand_reach_error);
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
