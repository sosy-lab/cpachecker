// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

const checker = require("license-checker");
const fs = require("fs");

const stripPrefix = (str, prefix) =>
  prefix && str.startsWith(prefix)
    ? str.substring(prefix.length).trimStart()
    : str;
const stripUpTo = (str, token, removeToken = true) => {
  const start = str.indexOf(token);
  if (start >= 0) {
    str = str.substring(start);
    if (removeToken) {
      str = str.substring(token.length);
    }
    return str.trimStart();
  }
  return str;
};

console.log("Checking licenses of dependencies...");
checker.init(
  {
    start: ".",
    production: true,
    excludePrivatePackages: true,
    // The following is the list of currently allowed licenses for bundled code.
    // We can add other free licenses, but must adjust the licences for
    // build/vendors.* in ~/reuse/dep5 accordingly and run "reuse download --all".
    onlyAllow:
      "0BSD; Apache-2.0; BSD*; BSD-2-Clause; BSD-3-Clause; CC0-1.0; CC-BY-3.0; CC-BY-4.0; ISC; MIT; OFL-1.1",
    customFormat: {
      name: "",
      version: "",
      licenses: "",
      copyright: "",
      repository: "",
      licenseText: "",
    },
  },
  (err, packages) => {
    if (err) {
      console.log(err);
      process.exit(1);
    } else {
      // We store some metadata for each package, including its license.
      // Because licenses are large, we deduplicate them:
      // We store each occurring license in licensesTexts and refer to it via its index.
      const licenseTextMapping = {};
      const licenseTexts = [];
      const licenseCounts = {};
      const dependencies = [];
      Object.keys(packages).forEach((key) => {
        const dependency = packages[key];
        let license = dependency.licenseText;

        // Replace windows specific line endings with unix based ones so the bundled
        // files stay the same on any OS
        license = license.replace(/\r\n/g, "\n");

        if (dependency.licenses === "(MIT OR GPL-3.0)") {
          // Trim long GPL from dual-licenses dependency, we choose MIT anyway
          license = stripUpTo(license, "GPL version 3", false);
        }

        // fix some other metadata issues
        if (dependency.licenses === "MIT*") {
          dependency.licenses = "MIT";
        } else if (dependency.licenses === "BSD*") {
          dependency.licenses = "BSD-2-Clause";
        }
        if (dependency.copyright === "") {
          const copyright = license.match(/\n(Copyright[^\n]*)\n/);
          if (copyright !== null) {
            dependency.copyright = copyright[1];
          }
        }

        // For dependencies that have no license file but only a readme,
        // we remove the readme part until the start of the license section.
        ["\n## License\n", "\n## **License**\n"].forEach((prefix) => {
          license = stripUpTo(license, prefix);
        });

        // Many license texts differ only in a small header.
        // Because we show the copyright and the license name separately anyway,
        // we can remove such prefixes and increase the chance of deduplication.
        // This list is a heuristic of currently occuring prefixes.
        [
          "The ISC License",
          "MIT License",
          "The MIT License",
          "The MIT License (MIT)",
          "(The MIT License)",
          "MIT",
          "(MIT)",
          "This software is released under the MIT license:",
          "Software License Agreement (BSD License)",
          "===========",
          "========================================",
          "BSD License",
          "For React software",
          `# ${dependency.name}`,
          // plus each sentence of copyright
          ...dependency.copyright.split(/(\.)\.?[ *]/),
        ].forEach((prefix) => {
          license = stripPrefix(license, prefix);
        });

        // Furthermore, some license texts differ only in whitespace and
        // punctuation, so for deduplication, we normalize this.
        const normalizedLicense = license
          .replace(/[\s*]+/g, " ")
          .replace(/['"](Software|AS IS)['"]/g, "'$1'");

        let licenseId;
        if (normalizedLicense in licenseTextMapping) {
          licenseId = licenseTextMapping[normalizedLicense];
        } else if (license) {
          licenseId = licenseTexts.push(license) - 1;
          licenseTextMapping[normalizedLicense] = licenseId;

          // count variants per license
          if (dependency.licenses in licenseCounts) {
            licenseCounts[dependency.licenses] += 1;
          } else {
            licenseCounts[dependency.licenses] = 1;
          }
        }

        dependencies.push({
          name: dependency.name,
          version: dependency.version,
          repository: dependency.repository,
          copyright: dependency.copyright,
          licenses: dependency.licenses,
          licenseId,
        });
      });
      const dependencyData = JSON.stringify({
        dependencies,
        licenses: licenseTexts,
      });

      const prettyPrintLicense = (d) =>
        `${d.licenses} (${licenseCounts[d.licenses]} variants)`;
      console.info(
        "Found %d dependencies under %s, adding %d bytes of metadata.",
        dependencies.length,
        [...new Set(dependencies.map(prettyPrintLicense))].join(", "),
        dependencyData.length
      );
      fs.mkdir(
        "./build_tmp",
        () => {
          fs.writeFile(
            "./build_tmp/dependencies.json",
            dependencyData,
            (error) => {
              if (error) {
                console.log(error);
                process.exit(1);
              }
            }
          );
        },
        (error) => {
          if (error) {
            console.log(error);
            process.exit(1);
          }
        }
      );
    }
  }
);
