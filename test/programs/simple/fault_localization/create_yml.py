# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

from os import listdir
from os.path import isfile, join
mypath = "./"
onlyfiles = [f for f in listdir(mypath) if isfile(join(mypath, f)) and (".c" in f)]
cpaLicenseHeader = "# This file is part of CPAchecker,\n# a tool for configurable software verification:\n# https://cpachecker.sosy-lab.org\n#\n# SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>\n#\n# SPDX-License-Identifier: Apache-2.0\n\n"
for s in onlyfiles:
    name = s[:-2] + ".yml"
    f = open(name, "w")
    f.write(cpaLicenseHeader + "format_version: '1.0'\ninput_files: '" + s + "'\n\nproperties:\n  - property_file: ../../../../config/properties/unreach-label.prp\n    expected_verdict: false")
    f.close()
