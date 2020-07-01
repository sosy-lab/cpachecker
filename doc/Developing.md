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
The issue tracker is only accessible for CPAchecker developers,
please click on [Request Access](https://gitlab.com/sosy-lab/software/cpachecker/) after signing in to GitLab
or [contact us](Mailing.md).

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

2. IMPORTANT: Install the Eclipse plugin for [google-java-format]:
   Download the `google-java-format-eclipse-plugin-*.jar`
   from the most recent [google-java-format release](https://github.com/google/google-java-format/releases)
   and put it into the `dropins` folder of your Eclipse installation
   (where you extracted the Eclipse archive, not the workspace).

3. Install an SVN plugin for Eclipse, e.g. [SubClipse](http://subclipse.tigris.org).
   Create new project from [SVN repository](https://svn.sosy-lab.org/software/cpachecker/trunk)
   (or use GIT as described above).

4. Creat a copy of the file `.factorypath.template` and name it `.factorypath`,
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


Code-Quality Checks and Continuous Integration
----------------------------------------------

We use javac, Google Error-Prone, the Eclipse Java Compiler, and SpotBugs
for findings bugs in the source, and we keep CPAchecker
free of warnings from all these tools.
You can run them all at once (plus the unit tests) with `ant all-checks`.

Our [BuildBot](https://buildbot.sosy-lab.org/buildbot/waterfall)
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


Releasing a new Version
-----------------------

1. Preparations:
   Update [`NEWS.txt`](../NEWS.txt) with notes for all important changes since the last
   CPAchecker release (i.e., new analyses and features, important changes to
   configuration etc.),
   and ensure that [`Copyright.txt`](../Copyright.txt) and [`Authors.txt`](../Authors.txt) are up-to-date.

2. Define a new version by setting `version.base` in [`build.xml`](../build.xml) to the new value.
   Our versioning system is `decade.year.month`,
   but the first release in a year is `decade.year`.

3. Build binary versions with `ant clean dist` and test them to ensure
   that all necessary files are contained in them.
   Make sure that you do not have any local changes
   or unversioned files in your checkout.

4. Update homepage:
   - Add release archives to `/html` in the repository.
   - Put changelog of newest into `/html/NEWS-<version>.txt`.
   - Add links to `/html/download.php`.
   - Move the old download links to `/html/download-oldversions.php`.
   - Update section News on `/html/index.php`.

5. Add a tag in the repository with name `cpachecker-<version>`.

6. Send a mail with the release announcement to cpachecker-announce and
   cpachecker-users mailing lists.

7. Prepare for next development cycle by setting `version.base` in [`build.xml`](../build.xml)
   to a new development version, which is the next possible version number
   with the suffix `-svn`.
   For example, if `1.9` was just released, the next possible version
   is `1.9.1` and the new development version should be `1.9.1-svn`.
