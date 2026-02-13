<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

Steps used to generate new SV-Comp configurations (might be incomplete, update accordingly):
- go to the `config` folder
- search and identify all configurations for svcompXX with XX replaced by the previous competition year 
- for each folder (work folder by folder to reduce errors) follow these steps:
  - copy each configuration with svcompXX in the folder you are in to a new name svcompYY with YY = XX + 1
  - edit each configuration just copied, update all references to svcomp configurations or names as in the step above
  - when done with the 2 steps above, cut the svcompXX configurations and paste them into the same folder they are in now, but rooted at `config/unmaintained`
  - open each configuration cut and pasted and update all paths such that all paths of none svcompXX configurations are updated to reflect their new location in `config/unmaintained` and its subfolders
    i.e. repair all paths to configurations referenced outside `config/unmaintained` except for svcompXX configurations
- check file differences to make sure no additional edits are done, e.g. using `diff -u2 -r --ignore-matching-lines=SPDX-FileCopyrightText` on all edited files
- important: run the old, new, and unmaintained competition configuration on the entire set of SV-COMP XX and check that their results match
- change the configuration names in all benchmark definitions in `test/test-sets` from svcompXX to svcompYY 
- change the configuration ran in `scripts/smoketest.sh` to be executed with the latest svcomp config

Example commands to copy, rename and sanity check from svcomp25 to svcomp26:
(Add more folders with svcomp configurations when necessary!)
```bash
cp -a config/svcomp25* config/unmaintained/
cp -a config/components/svcomp25* config/unmaintained/components/
cp -a config/includes/svcomp25* config/unmaintained/includes/
rename 's/svcomp25/svcomp26/' config/svcomp25* config/components/svcomp25* config/includes/svcomp25*
sed -e "s/svcomp25/svcomp26/g" -e "s/SV-COMP'25/SV-COMP'26/g" -i config/**/svcomp26*
sed -e "s/svcomp25/svcomp26/g" test/test-sets/*.xml
```

Additional information: https://gitlab.com/sosy-lab/software/cpachecker/-/merge_requests/347
