// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int main(){


// SHIFT LEFT

int x = 1;
if (x<<2 != 4) { goto ERROR; }
if (1<<2 != 4) { goto ERROR; }
if (5<<2 != 20) { goto ERROR; }

// SHIFT RIGHT

if (4>>2 != 1) { goto ERROR; }
if (20>>2 != 5) { goto ERROR; }
if (1>>2 != 0) { goto ERROR; }
if (5>>2 != 1) { goto ERROR; }

return 0;
ERROR: goto ERROR;
}
