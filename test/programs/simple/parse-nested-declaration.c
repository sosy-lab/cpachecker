// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void main() {
  int i;
  // The nested "int i;" does not collide with "int i;" from main
  // (there was a bug that lead field declarations from struct declarations
  // inside statements be handled as variable declarations).
  i = sizeof(struct s { int i; });
}
