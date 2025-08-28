// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.substitution;

import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;

public class MPORSubstitutionTrackerUtil {

  public static void copyContents(MPORSubstitutionTracker pFrom, MPORSubstitutionTracker pTo) {
    for (CParameterDeclaration mainFunctionArg : pFrom.getAccessedMainFunctionArgs()) {
      pTo.addAccessedMainFunctionArg(mainFunctionArg);
    }
    // pointer assignments
    for (var entry : pFrom.getPointerAssignments().entrySet()) {
      pTo.addPointerAssignment(entry.getKey(), entry.getValue());
    }
    for (var cell : pFrom.getPointerFieldMemberAssignments().cellSet()) {
      pTo.addPointerFieldMemberAssignment(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
    }
    // pointer dereferences
    for (CSimpleDeclaration accessedPointerDereference : pFrom.getAccessedPointerDereferences()) {
      pTo.addAccessedPointerDereference(accessedPointerDereference);
    }
    for (CSimpleDeclaration writtenPointerDereference : pFrom.getWrittenPointerDereferences()) {
      pTo.addWrittenPointerDereference(writtenPointerDereference);
    }
    // pointer dereferences from field members
    for (CSimpleDeclaration fieldOwner :
        pFrom.getAccessedFieldReferencePointerDereferences().keySet()) {
      for (CCompositeTypeMemberDeclaration fieldMember :
          pFrom.getAccessedFieldReferencePointerDereferences().get(fieldOwner)) {
        pTo.addAccessedFieldReferencePointerDereference(fieldOwner, fieldMember);
      }
    }
    for (CSimpleDeclaration fieldOwner :
        pFrom.getWrittenFieldReferencePointerDereferences().keySet()) {
      for (CCompositeTypeMemberDeclaration fieldMember :
          pFrom.getWrittenFieldReferencePointerDereferences().get(fieldOwner)) {
        pTo.addWrittenFieldReferencePointerDereference(fieldOwner, fieldMember);
      }
    }
    // declarations accessed
    for (CSimpleDeclaration accessedDeclaration : pFrom.getAccessedDeclarations()) {
      pTo.addAccessedDeclaration(accessedDeclaration);
    }
    for (CSimpleDeclaration writtenDeclaration : pFrom.getWrittenDeclarations()) {
      pTo.addWrittenDeclaration(writtenDeclaration);
    }
    // field members accessed
    for (CSimpleDeclaration fieldOwner : pFrom.getAccessedFieldMembers().keySet()) {
      for (CCompositeTypeMemberDeclaration fieldMember :
          pFrom.getAccessedFieldMembers().get(fieldOwner)) {
        pTo.addAccessedFieldMember(fieldOwner, fieldMember);
      }
    }
    for (CSimpleDeclaration fieldOwner : pFrom.getWrittenFieldMembers().keySet()) {
      for (CCompositeTypeMemberDeclaration fieldMember :
          pFrom.getWrittenFieldMembers().get(fieldOwner)) {
        pTo.addWrittenFieldMember(fieldOwner, fieldMember);
      }
    }
    // function pointers
    for (CFunctionDeclaration functionDeclaration : pFrom.getAccessedFunctionPointers()) {
      pTo.addAccessedFunctionPointer(functionDeclaration);
    }
  }
}
