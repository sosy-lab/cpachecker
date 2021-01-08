// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

const fs = require("fs");
const path = require("path");

const rawWorkerPath = path.join(__dirname, "../worker/workers");
const workerDataFile = path.join(__dirname, "../worker/workerData.js");
const externalLibsPath = path.join(__dirname, "../external_libs");

const template = "data:text/plain;base64,";
const fileHeader = `// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
`;

const workerFiles = fs.readdirSync(rawWorkerPath);
const externalLibFiles = fs.readdirSync(externalLibsPath);

let output = fileHeader;
const workerNames = [];

const externalLibContent = [];
externalLibFiles.forEach((libFile) => {
  libPath = path.join(externalLibsPath, libFile);
  externalLibContent.push(fs.readFileSync(libPath));
});

workerFiles.forEach((worker) => {
  const workerName = `${worker.split(".")[0]}Data`;
  workerNames.push(workerName);
  const workerPath = path.join(rawWorkerPath, worker);
  const workerContent = fs.readFileSync(workerPath);
  const content = Buffer.concat([
    ...externalLibContent,
    workerContent,
  ]).toString("base64");

  output += `\n const ${workerName} = "${template}${content}";\n`;
});

const allWorkers = workerNames.join(", ");
output += `\n export { ${allWorkers} };`;

fs.writeFileSync(workerDataFile, output);

console.log("Injected worker data.");
