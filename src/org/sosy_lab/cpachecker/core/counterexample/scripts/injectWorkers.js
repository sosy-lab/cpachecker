// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

const fs = require("fs");
const path = require("path");

const rawWorkerPath = path.join(__dirname, "../worker");
const workerDataFile = path.join(__dirname, "../build_tmp/workerData.js");
const vendorPath = path.join(__dirname, "../vendor");

const template = "data:text/plain;base64,";

const workerFiles = ["argWorker.js", "cfaWorker.js"];
const vendorFiles = fs.readdirSync(vendorPath).filter(file => !file.includes("license"));

let output = "";
const workerNames = [];

const vendorFileContents = [];
vendorFiles.forEach((file) => {
  const filePath = path.join(vendorPath, file);
  vendorFileContents.push(fs.readFileSync(filePath));
});

workerFiles.forEach((worker) => {
  const workerName = `${worker.split(".")[0]}Data`;
  workerNames.push(workerName);
  const workerPath = path.join(rawWorkerPath, worker);
  const workerContent = fs.readFileSync(workerPath);
  const content = Buffer.concat([
    ...vendorFileContents,
    workerContent,
  ]).toString("base64");

  output += `\n const ${workerName} = "${template}${content}";\n`;
});

const allWorkers = workerNames.join(", ");
output += `\n export { ${allWorkers} };`;

fs.writeFileSync(workerDataFile, output);

console.log("Injected worker data.");
