// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// predicate analysis needs cpa.predicate.handleStringLiteralInitializers=true
int main(void) {

  char a[] = "abc";
//  char b[] = { "abc" };
  char *c = { "abc" };
  const char * d[2] = {"a","b"};

  if (a[0] != 'a') goto ERROR;
  if (a[1] != 'b') goto ERROR;
  if (a[2] != 'c') goto ERROR;
  if (a[3] != 0) goto ERROR;

  /*
  if (b[0] != 'a') goto ERROR;
  if (b[1] != 'b') goto ERROR;
  if (b[2] != 'c') goto ERROR;
  if (b[3] != 0) goto ERROR;
  */

  if (c[0] != 'a') goto ERROR;
  if (c[1] != 'b') goto ERROR;
  if (c[2] != 'c') goto ERROR;
  if (c[3] != 0) goto ERROR;

  if (d[0][0] != 'a') goto ERROR;
  if (d[0][1] != 0) goto ERROR;
  if (d[1][0] != 'b') goto ERROR;
  if (d[1][1] != 0) goto ERROR;

  return 0;
ERROR:
  return 1;
}
