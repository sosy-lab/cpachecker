// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int featureModelValid() {
  if (! __SELECTED_FEATURE_Verify)
	  if (__SELECTED_FEATURE_Forward)
		  if (! __SELECTED_FEATURE_Sign)
			  return 0;
		   else return 1;
	   else return 1;
   else return 1;
}

int main() {
	int tmp;
	tmp = featureModelValid();
	if (! tmp)
		ERROR: return 1;
}
