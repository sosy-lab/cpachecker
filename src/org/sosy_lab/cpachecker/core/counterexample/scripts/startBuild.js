// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

const fs = require("fs");

const tmpPath = "build_tmp";

if (!fs.existsSync(tmpPath)) {
  fs.mkdirSync(tmpPath);
}

require("./dependencies");
require("./injectWorkers");
