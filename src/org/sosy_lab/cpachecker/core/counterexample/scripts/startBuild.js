// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

const fs = require("fs");
const tmp_path = "build_tmp";

if (!fs.existsSync(tmp_path)) {
  fs.mkdirSync(tmp_path);
}

require("./dependencies.js")
require("./injectWorkers.js")
