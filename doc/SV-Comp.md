<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

Steps used to generate new SV-Comp configurations (might be incomplete, update accordingly):
- go to the configs folder
- search and identify all configs for svcompXX with XX replaced by the previous competition year 
- for each folder (work folder by folder to reduce errors) follow these steps:
  - copy each configuration with svcompXX in the folder you are in to a new name svcompYY with YY = XX + 1
  - go into each config you copied, update all references to svcomp configs or names as in the step above
  - when done with the 2 steps above, cut the svcompXX configs and paste them into the same folder they are in now, but rooted at config/unmaintained
  - open each config cut and pasted and update all paths such that all paths of none svcompXX configs are updated to reflect their new location in unmaintained
    i.e. repair all paths to configs referenced outside of unmaintained except for svcompXX configs

Run the old configs, the new configs, and the unmaintained configs on the entire set of SV-COMP XX and check that they are equal.

Additional information: https://gitlab.com/sosy-lab/software/cpachecker/-/merge_requests/347
