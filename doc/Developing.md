<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2007-2024 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

CPAchecker Developing Instructions
==================================

More details can be found in the other files in this directory.

Please read and follow at least our [contribution guidelines](../CONTRIBUTING.md),
the [style guide](StyleGuide.md),
[`Logging.md`](Logging.md), and [`Test.md`](Test.md).

For JavaScript code read [`ReportTemplateStyleGuide.md`](ReportTemplateStyleGuide.md),
and for Python code read [`PythonStyleGuide.md`](PythonStyleGuide.md).


Getting the code
----------------

There are two possibilities to retrieve the source code:

- The [main repository on GitLab](https://gitlab.com/sosy-lab/software/cpachecker/)

- A read-only mirror at [GitHub](https://github.com/sosy-lab/cpachecker)

For bug tracking, we use [GitLab](https://gitlab.com/sosy-lab/software/cpachecker/issues).

For building the code on the command line, c.f. [`../INSTALL.md`](../INSTALL.md).


Develop CPAchecker from within Eclipse
--------------------------------------

0. Install a Java 17 compatible JDK (c.f. [`../INSTALL.md`](../INSTALL.md)).

1. Install [Eclipse](http://www.eclipse.org/) with at least version 4.22, with JDT.
   If you have more than one Java version installed,
   make sure to start Eclipse with Java 17 or newer.

2. Install the Eclipse plugin for [google-java-format](https://github.com/google/google-java-format/):
   Download the `google-java-format-eclipse-plugin-*.jar`
   from the most recent [google-java-format release](https://github.com/google/google-java-format/releases)
   and put it into the `dropins` directory of your Eclipse installation
   (where you extracted the Eclipse archive, not the workspace).
   Open the `eclipse.ini` file in your Eclipse installation and append the following lines:

```
--add-opens=java.base/java.lang=ALL-UNNAMED
--add-opens=java.base/java.util=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED
--add-exports=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED
```

3. Create a copy of the file `.factorypath.template` and name it `.factorypath`,
   and (if necessary) adjust the path to the CPAchecker directory within it.

4. If Eclipse complains about a missing JDK
   (`Unbound classpath container: 'JRE System Library [JavaSE-17]'`),
   go to Window -> Preferences -> Java -> Installed JREs,
   click the "Search" button and select the path where your Java 17 installation
   can be found (on Ubuntu `/usr/lib/jvm` will do).

5. In order to run CPAchecker, use one of the supplied launch configurations
   or create your own.
   To select the configuration, specification, and program files use the
   text box "program arguments" in the launch configuration editor.
   The text box "VM arguments" should contain "-ea" to enable assertion checking.

6. Recommended:
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
There is one additional check (Refaster) that is not included
because it needs additional setup, cf. below.

Our [CI](https://gitlab.com/sosy-lab/software/cpachecker/-/pipelines)
will also execute these checks and send mails to the developer list
(cf. [`Mailing.md`](Mailing.md), please apply for membership if you commit to CPAchecker).

If any of these tools or the unit tests find a problem,
please fix them as soon as possible (ideally before committing).

Additionally, our [BuildBot](https://buildbot.sosy-lab.org/cpachecker/)
executes integration tests with thousands of CPAchecker runs
in various configurations on every commit to the main branch and checks for regressions.
All major projects and configurations within CPAchecker should be part of this test suite.
Please refer to [`Test.md`](Test.md) for more information.


Refaster Setup
--------------

[Refaster](https://errorprone.info/docs/refaster) is a way to extend Google Error Prone
with custom rules, and we have a [collection of such rules](https://gitlab.com/sosy-lab/software/refaster).
To apply them to CPAchecker, the following setup is required:
- Checkout rule repository with `git clone https://gitlab.com/sosy-lab/software/refaster.git` to some directory.
- Add the following line to `build.properties` in the CPAchecker project directory
  (create the file if you do not have it already):
  ```
  refaster.rule.file=/PATH_TO_YOUR_REFASTER_RULES_CHECKOUT/rule.refaster
  ```
- Then from time to time update the rules checkout
  (the desired revision can be seen with `grep REFASTER_REPO_REVISION .gitlab-ci.yml` in the CPAchecker directory)
  and compile the rules with the following command in the rules directory:
  ```
  ant build-refaster-rule -Drefaster.source.pattern=**/*.java -Derrorprone.version=ERROR_PRONE_VERSION
  ```
  (The desired Error Prone version can be seen with `grep REFASTER_VERSION .gitlab-ci.yml` in the CPAchecker directory.)

Now you can execute the rules with `ant refaster`
and an `error-prone.patch` file with the result will be created if there are recommendations.


Debugging
---------

For attaching a debugger to a CPAchecker process started on the command line (even remotely),
just run `bin/cpachecker --jvm-debug ...` and point your debugger to TCP port 5005
of the respective machine.


JavaScript Development
-----------------------
The JS files can be found in the directory `src/org/sosy_lab/cpachecker/core/counterexample`. We use Node.js as environment for our files.
All necessary third party libraries that we rely on can be installed via `npm run install`.

Our JS code is postprocessed with [webpack](https://webpack.js.org/). Webpack bundles our JS and CSS code as well as the third party libraries and puts the resulting files in the `build` directory. This can be done via the `npm run build` command, which should be executed every time changes to the JS related files are made. **Note that none of the raw files are actually used in the report generating, so any changes will not take effect unless the bundled files are updated.**

During development, we also have a small development server that can be started via `npm run start`. This server automatically opens an example report file in the browser and enables live updates for any changes to the JS code. Since a fully rendered HTML file is necessary for this server, the example file was created by CPAchecker and can be found under `development_data/index.html`. Therefore, changes to the `report.html` file will not be updated automatically. It is important to keep this file up to date by hand after any changes to the `report.html` file.


Offline Development
-------------------

Dependencies for CPAchecker are downloaded from an Ivy repository
hosted at sosy-lab.org during build.
Once all dependencies are cached locally, builds work offline.

It is also possible to override the path to the Ivy repository
and to use a fully local copy of the Ivy repository.
Instructions for doing so can be found in our cross-project
[wiki](https://gitlab.com/sosy-lab/doc/-/wikis/Using-a-Different-Ivy-Repository-for-Development).

As a last resort for offline builds even if not all dependencies are available,
one can disable Ivy with `ant -Divy.disable=true`.
But of course, as soon as a missing dependency is required,
this may not work.

Releasing a New Version
-----------------------

1. Preparations:
   Update [`NEWS.md`](../NEWS.md) with notes for all important changes since the last
   CPAchecker release (i.e., new analyses and features, important changes to
   configuration etc.),
   and ensure that [`Authors.md`](../Authors.md) are up-to-date.

1. Make sure to work in a fresh checkout with no uncommitted files and modifications.

1. Define a new version by setting `version.base` in [`build.xml`](../build.xml) to the new value.
   The version number is constructed as outlined below in Sect. "Version Numbering".

1. Update the heading in [`NEWS.md`](../NEWS.md) and
   update the version number in all places in the following files:
   - [`.gitlab-ci.yml`](../.gitlab-ci.yml)
   - [`build/Dockerfile.release`](../build/Dockerfile.release)
   - [`build/debian/rules`](../build/debian/rules)

1. Build binary archives with `ant clean dist`.

1. Build `.deb` package with
  `build/deb-package.sh <version> CPAchecker-<version>-unix.zip dist-<version>/`.

1. Test binary archives and the `.deb` package.

1. Commit the changes with commit message `Release <version>`.

1. Add a tag in the repository with name `cpachecker-<version>`.

1. Prepare for next development cycle by adding the suffix `-git`
   to `version.base` in [`build.xml`](../build.xml).

1. Publish the `.deb` package created in `dist-<version>/`
   in our [APT repository](https://apt.sosy-lab.org)
   using the [instructions](https://svn.sosy-lab.org/software/apt/README.md) there.

1. Publish new CPAchecker version on Zenodo under https://doi.org/10.5281/zenodo.3816620:
   - Assign new DOI and upload `CPAchecker-<version>-unix.zip` archive.
   - Update title to `CPAchecker Release <version> (image)`.
   - Set publication date.
   - Update description with entries for new version in [`NEWS.md`](../NEWS.md).
   - Update list of contributors according to [`Authors.md`](../Authors.md).
   - Set version field to `<version> (unix)`.

1. Update homepage:
   - Add release ZIP archives to `/html` in the repository.
   - Put changelog of newest version into `/html/NEWS-<version>.txt`.
   - Add links to `/html/download.php`.
   - Move the old download links to `/html/download-oldversions.php`.
   - Update section News on `/html/index.php`.

1. Publish the Docker image by either building and pushing the image manually
   as described in [`build/Dockerfile.release`](../build/Dockerfile.release)
   or triggering the scheduled GitLab CI job
   (https://gitlab.com/sosy-lab/software/cpachecker/pipeline_schedules).
   This needs to be done after updating the homepage.

1. Send a mail with the release announcement to cpachecker-announce and
   cpachecker-users mailing lists.


Version Numbering and Release Tagging
-------------------------------------

We use the following schema to construct version numbers for CPAchecker releases
(from version 3.0 onwards):

- `X.Y` is a release that should be done at least *yearly*,
  where an increase of `X` to version `X.0` indicates a major change
  and an increase of `Y` indicates a minor change (e.g., added functionality).
- `X.Y.Z` indicates a bug-fix release,
  where `Z` is increased (starting from `0`).
- Development versions have versions as produced by `git describe`,
  i.e. `4.0-2-gabcdef` for commit `abcdef` if it is the second commit after version 4.0.
  Note that this differs from semantic versioning and for CPAchecker versions before 4.0,
  where development version `X.Y-svn-suffix` was ordered before `X.Y`.

The tags in our repository are named `cpachecker-VERSION`, e.g., `cpachecker-3.0`.
