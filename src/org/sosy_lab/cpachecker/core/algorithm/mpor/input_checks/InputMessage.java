// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.input_checks;

/**
 * Enumeration of warnings and errors to show to the user when a problem occurs while
 * sequentializing a program.
 */
public enum InputMessage {
  FUNCTION_POINTER_ASSIGNMENT("MPOR does not support function pointers in assignments: ", false),
  LANGUAGE_NOT_C("MPOR only supports language C.", false),
  DUPLICATE_STRUCT_MEMBER_NAMES(
      "MPOR does not support non unique nested struct member names in line ", true),
  FIELD_MEMBER_NOT_FOUND(
      "MPOR cannot determine the memory location of the following struct member because it is"
          + " not part of the CType of the accessed variable: ",
      false),
  MULTIPLE_DECLARATIONS_IN_FIELD_REFERENCE_OWNER(
      "MPOR does not support multiple declarations in the owner expression of field references in"
          + " line ",
      true),
  NOT_CONCURRENT(
      "MPOR expects a concurrent C program with at least one pthread_create call."
          + " The program is sequentialized regardless.",
      false),
  POINTER_ASSIGNMENT_TO_NON_POINTER(
      "MPOR does not support the assignment to the following non-pointer: ", false),
  POINTER_DEREFERENCE_OF_NON_POINTER(
      "MPOR does not support the dereference of the following non-pointer: ", false),
  POINTER_DEREFERENCE_TYPE_MISMATCH(
      "MPOR does not support the following pointer dereference, its type is not compatible with"
          + " the type of the memory location it points to: ",
      false),
  POINTER_WRITE_BINARY_EXPRESSION(
      "MPOR does not support binary expressions as assignments to pointers in line ", true),
  POINTER_WRITE(
      "allowPointerWrites is disabled, but the input program contains a pointer write in line ",
      true),
  PTHREAD_CREATE_LOOP(
      "MPOR does not support pthread_create calls in loops (or recursive functions).", false),
  PTHREAD_FUNCTION_RETURN_VALUE(
      "MPOR does not support pthread method return value assignments in line ", true),
  PTHREAD_OBJECT_ARRAY(
      "MPOR does not support the following array of pthread objects or array of structs with"
          + " inner pthread objects in line ",
      true),
  RECURSIVE_FUNCTION("MPOR does not support the (in)direct recursive function in line ", true),
  UNHANDLED_NONDET_TYPE(
      "MPOR could not find __VERIFIER_nondet function for the following CType: ", true),
  UNSUPPORTED_FUNCTION("MPOR does not support the function in line ", true);

  final String message;

  final boolean containsLineAndCode;

  InputMessage(String pMessage, boolean pContainsLineAndCode) {
    message = pMessage;
    containsLineAndCode = pContainsLineAndCode;
  }

  public String formatMessage() {
    if (containsLineAndCode) {
      return message + "%s: %s.";
    } else {
      return message;
    }
  }
}
