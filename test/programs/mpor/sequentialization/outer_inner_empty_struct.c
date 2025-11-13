// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdio.h>
#include <pthread.h>

// empty struct (no members)
typedef struct {} Empty;
// inner struct with member
typedef struct {
  int inner_member;
} Inner;
// outer struct with inner struct and member
typedef struct {
  Inner inner;
  int outer_member;
} Outer;
Outer outer_A;
Outer outer_B;

void field_member_parameter_test(int param) {
  param++;
}
void field_member_parameter_test_ptr(int * param) {
  *param++;
}
void field_owner_parameter_test_ptr(Inner * param_inner) {
  param_inner->inner_member = 42;
}
void * start_routine(void *arg)
{
  int *ultimate_question = malloc(sizeof(int));
   *ultimate_question = 42;
   pthread_exit((void*)ultimate_question);
}
int main(void) {
  outer_A.inner.inner_member = 42;
  outer_B.inner.inner_member = -42;
  int local_1;
  local_1 = outer_A.inner.inner_member;
  int local_2;
  local_2 = outer_B.outer_member;
  Inner * inner_ptr;
  inner_ptr = &outer_A.inner;
  *inner_ptr = outer_B.inner;
  int * outer_member_ptr;
  outer_member_ptr = &outer_A.outer_member;
  *outer_member_ptr = 42;
  Outer * outer_ptr;
  outer_ptr = &outer_A;

  int * ptr;
  int x;
  x = 42;
  ptr = ptr;
  *ptr = 7;

  field_member_parameter_test(outer_A.inner.inner_member);
  // pass the address of the inner member as parameter
  field_member_parameter_test_ptr(&outer_A.inner.inner_member);
  // pass the address of the inner struct as parameter
  field_owner_parameter_test_ptr(&outer_A.inner);

  pthread_t id1;
  pthread_create(&id, NULL, start_routine, NULL);
  void *retval;
  pthread_join(id, &retval);
}

