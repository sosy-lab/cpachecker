// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Arrays with the same string initializer are distinct objects. If they get the same address,
// the error looks unreachable (pattern from the Cir2c tasks of SV-COMP).

extern void abort(void);
void reach_error(void) { abort(); }

static const char tag[] = "_ZTIi";
static const char name[] = "_ZTIi";
static const void *typeinfo[2] = {(const void *)0, (const void *)name};

int main(void) {
  const void *type = (const void *)tag;
  if (type == (const void *)tag && (const void *)tag != (const void *)name) {
    reach_error();
  }
  return 0;
}
