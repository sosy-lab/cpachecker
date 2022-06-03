<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

CPAchecker Developing Instructions
==================================

More details can be found in the other files in this directory.

Please read and follow at least [`StyleGuide.md`](StyleGuide.md),
[`Logging.md`](Logging.md), [`Test.md`](Test.md), and [`VersionControl.md`](VersionControl.md).

For JavaScript code read [`ReportTemplateStyleGuide.md`](ReportTemplateStyleGuide.md),
and for Python code read [`PythonStyleGuide.md`](PythonStyleGuide.md).


Getting the code
----------------

There are four possibilities to retrieve the source code:

- The main [SVN repository](https://svn.sosy-lab.org/software/cpachecker)

- Our [Git mirror](https://svn.sosy-lab.org/software/cpachecker.git)

- A Git mirror at [GitLab](https://gitlab.com/sosy-lab/software/cpachecker/)

- A Git mirror at [GitHub](https://github.com/sosy-lab/cpachecker)

Only our `SVN` repository allows committing,
all mirrors are read-only.
We recommend to use our own repository hosting,
because it avoids the risk that the synchronization to a third party
fails or causes problems.

For browsing through the code online,
there are these possibilities:

- https://gitlab.com/sosy-lab/software/cpachecker/tree/trunk/
- https://github.com/sosy-lab/cpachecker/tree/trunk/

For bug tracking, we use [GitLab](https://gitlab.com/sosy-lab/software/cpachecker/issues).

For building the code on the command line, c.f. [`../INSTALL.md`](../INSTALL.md).

If you like to use Git, use the following commands
to create a working copy that allows you to transparently
commit to the SVN repository (with `git svn dcommit`)
while still using Git to fetch the commits:

```
git clone -o mirror https://svn.sosy-lab.org/software/cpachecker.git/
cd cpachecker
git svn init --prefix=mirror/ -s https://svn.sosy-lab.org/software/cpachecker
```

This also works with GitHub.


Develop CPAchecker from within Eclipse
--------------------------------------

0. Install a Java 11 compatible JDK (c.f. [`../INSTALL.md`](../INSTALL.md)).

1. Install [Eclipse](http://www.eclipse.org/) with at least version 4.6, with JDT.

2. Install the Eclipse plugin for [google-java-format](https://github.com/google/google-java-format/):
   Download the `google-java-format-eclipse-plugin-*.jar`
   from the most recent [google-java-format release](https://github.com/google/google-java-format/releases)
   and put it into the `dropins` folder of your Eclipse installation
   (where you extracted the Eclipse archive, not the workspace).

3. Install an SVN plugin for Eclipse, e.g. [SubClipse](http://subclipse.tigris.org).
   Create new project from [SVN repository](https://svn.sosy-lab.org/software/cpachecker/trunk)
   (or use GIT as described above).

4. Create a copy of the file `.factorypath.template` and name it `.factorypath`,
   and (if necessary) adjust the path to the CPAchecker directory within it.

5. If Eclipse complains about a missing JDK
   (`Unbound classpath container: 'JRE System Library [JavaSE-11]'`),
   go to Window -> Preferences -> Java -> Installed JREs,
   click the "Search" button and select the path where your Java 11 installation
   can be found (on Ubuntu `/usr/lib/jvm` will do).

6. In order to run CPAchecker, use one of the supplied launch configurations
   or create your own.
   To select the configuration, specification, and program files use the
   text box "program arguments" in the launch configuration editor.
   The text box "VM arguments" should contain "-ea" to enable assertion checking.

7. Recommended:
   If you want the sources of the libraries (like Guava or CDT),
   run `ant install-contrib` once in the CPAchecker directory.


License and Copyright
---------------------

All files in the CPAchecker repository need to have a header with a declaration
of copyright and license in the [REUSE format](https://reuse.software).

After installing the [reuse tool](https://github.com/fsfe/reuse-tool),
use the following command to add our standard header:

    reuse addheader --template=header.jinja2 --license Apache-2.0 --copyright 'Dirk Beyer <https://www.sosy-lab.org>' <FILES>

Of course, you can adjust license and copyright if necessary
(e.g., when integrating third-party code).
However, for all original contributions please consider transferring the copyright to us
and use our standard license in order to make license handling easier for us
and all users of CPAchecker.
In accordance with the Apache license, all contributions to CPAchecker
are by default under the Apache license as well unless explicitly marked otherwise.


Code-Quality Checks and Continuous Integration
----------------------------------------------

We use javac, the Eclipse Java Compiler,
[Google Error Prone](https://errorprone.info/),
[SpotBugs](https://spotbugs.github.io/),
[Checkstyle](https://checkstyle.org/), and
[Policeman's Forbidden API Checker](https://github.com/policeman-tools/forbidden-apis)
for findings bugs in the source, and we keep CPAchecker
free of warnings from all these tools.
You can run them all at once (plus the unit tests) with `ant all-checks`.

Our [BuildBot](https://buildbot.sosy-lab.org/cpachecker/)
will also execute these checks and send mails to the developer list
(cf. [`Mailing.md`](Mailing.md), please apply for membership if you commit to CPAchecker).

If any of these tools or the unit tests find a problem,
please fix them as soon as possible (ideally before committing).

The BuildBot also executes integration tests with thousands of CPAchecker runs
in various configurations on every commit and checks for regression.
All major projects and configurations within CPAchecker should be part of this test suite.
Please refer to [`Test.md`](Test.md) for more information.


Debugging
---------

For attaching a debugger to a CPAchecker process started on the command line (even remotely),
just run `scripts/cpa.sh -debug ...` and point your debugger to TCP port 5005
of the respective machine.


JavaScript Development
-----------------------
The JS files can be found in the directory `src/org/sosy_lab/cpachecker/core/counterexample`. We use Node.js as environment for our files.
All necessary third party libraries that we rely on can be installed via `npm run install`.

Our JS code is postprocessed with [webpack](https://webpack.js.org/). Webpack bundles our JS and CSS code as well as the third party libraries and puts the resulting files in the `build` directory. This can be done via the `npm run build` command, which should be executed every time changes to the JS related files are made. **Note that none of the raw files are actually used in the report generating, so any changes will not take effect unless the bundled files are updated.**

During development, we also have a small development server that can be started via `npm run start`. This server automatically opens an example report file in the browser and enables live updates for any changes to the JS code. Since a fully rendered HTML file is necessary for this server, the example file was created by CPAchecker and can be found under `development_data/index.html`. Therefore, changes to the `report.html` file will not be updated automatically. It is important to keep this file up to date by hand after any changes to the `report.html` file.


Releasing a New Version
-----------------------

1. Preparations:
   Update [`NEWS.md`](../NEWS.md) with notes for all important changes since the last
   CPAchecker release (i.e., new analyses and features, important changes to
   configuration etc.),
   and ensure that [`Authors.md`](../Authors.md) are up-to-date.

2. Define a new version by setting `version.base` in [`build.xml`](../build.xml) to the new value.
   The version tag is constructed as outlined below in Sect. "Release Tagging".

3. Build binary versions with `ant clean dist` and test them to ensure
   that all necessary files are contained in them.
   Make sure that you do not have any local changes
   or unversioned files in your checkout.

4. Update homepage:
   - Add release archives to `/html` in the repository.
   - Put changelog of newest into `/html/NEWS-<version>.txt`.
   - Publish new CPAchecker version on Zenodo under https://doi.org/10.5281/zenodo.3816620:
     - Assign new DOI and upload archive.
     - Update description with entries for new version in [`NEWS.md`](../NEWS.md).
     - Update version field and publication date.
     - Update list of contributors according to [`Authors.md`](../Authors.md).
   - Add links to `/html/download.php`.
   - Move the old download links to `/html/download-oldversions.php`.
   - Update section News on `/html/index.php`.

5. Add a tag in the repository with name `cpachecker-<version>`,
   where `<version>` is constructed as outlined below in Sect. "Release Tagging".

6. Update version number in build/Dockerfile.release and .gitlab-ci.yml
   and either build and push the Docker image manually
   or trigger the scheduled GitLab CI job after pushing
   (https://gitlab.com/sosy-lab/software/cpachecker/pipeline_schedules).

7. Send a mail with the release announcement to cpachecker-announce and
   cpachecker-users mailing lists.

8. Prepare for next development cycle by setting `version.base` in [`build.xml`](../build.xml)
   to a new development version, which is the next possible version number
   with the suffix `-svn`.
   For example, if `1.9` was just released, the next possible feature release
   is `1.9.1` and the new development version should be `1.9.1-svn`.


Version Numbering and Release Tagging
-------------------------------------

We use the following schema to construct version numbers for CPAchecker releases
(from version 1.8 onwards):

- `X.Y` is the *yearly release* in year `20XY`.
  There is exactly one such CPAchecker release every year.
- `X.Y.Z` is a *feature release*, where
  - `X.Y` is the last yearly release that already exists and that the new release builds on, and
  - `Z` is `n+1` if a release `X.Y.n` already exists, and `1` otherwise.
- `X.Y[.z]-<component-version>` is a *component release*, where
   `X.Y[.z]` is defined as above and `<component-version>` is a label that
    should give a hint on a special purpose for the release.
    Ideally, the component version ends with a date stamp.
- Examples:
  - `1.9` is the yearly release for 2019.
  - `1.8-coveritest-sttt-20190729` is a component release after yearly release `1.8`,
    which points to a commit that was made on 2019-07-29
    for the purpose of tagging the component version used for the STTT paper on CoVeriTest.

The tags in our repository are named `cpachecker-VERSION`,
e.g. `cpachecker-1.9` and `cpachecker-1.8-coveritest-sttt-20190729`.
