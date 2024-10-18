<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2007-2024 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

CPAchecker Contribution Guidelines
==================================

CPAchecker is an open-source project and contributions are welcome!
The following guidelines describe the workflow for contributions.
For working on CPAchecker, please also have a look at the [development documentation](doc/Developing.md)
and the documents linked there, such as our [style guide](doc/StyleGuide.md).

Code Hosting
------------
We prefer contributions (for larger contributions even from external contributors)
to be pushed into a branch in the [official CPAchecker repository](https://gitlab.com/sosy-lab/software/cpachecker/)
instead of a fork.
This is more flexible for collaboration and prevents problems with CI
(our own repository has access to more powerful GitLab runners).
Thus, please [contact us](https://cpachecker.sosy-lab.org/contact.php)
and request access to the GitLab repository providing your GitLab user name.
Smaller changes can also be contributed via an MR from a fork.

Commits
-------
- Commit early, commit often.
- Split the commits into reasonable changesets.
- Add meaningful commit messages that help other developers
  to understand what was done and (even more importantly!) *why* it was done.
  Please read [this post on how to write good commit messages](https://cbea.ms/git-commit/).
  This is especially important for revert commits -
  do not just commit with the auto-generated commit message from git!
- Try to have separate commits for bugfixes, refactorings, and new code.
- Make sure all committed code is auto-formatted with [google-java-format](https://github.com/google/google-java-format/)
  (install the respective plugin in your IDE!).

Branches
--------
Development is done in branches. Please follow these guidelines:

- Give your branch a meaningful name that describes the project / goal of the branch.
  Your name should typically not be part of the branch name.
  If an issue exists for your intended change,
  consider creating the branch directly from the issue
  or at least include the issue number in the branch name.

- Keep each branch restricted to one specific set of changes,
  do not mix unrelated changes in a single branch.

- While a branch exists, keep it close to the main branch,
  i.e., regularly merge the main branch into your branch.
  This allows you to detect merge conflicts early
  when it is often less effort to fix them.

- Aim at integrating your branch into the main branch as soon as possible.
  This benefits everyone: Other developers can see and use your code,
  and you get the benefit of much more testing
  (the integration tests done by BuildBot run only on the main branch)
  and merges are less effort for you
  (CPAchecker is actively developed and waiting too long between merges
  can lead to a large amount of changes that are difficult to apply).
  The minimum bar for integrating a branch is that CI is green,
  a core developer has approved your merge request, and
  that no breakages of existing components are expected.
  This means that while you are developing a new component (like a CPA),
  you can merge early and often even if the component is not yet finished,
  because you will not cause trouble for other users
  (who will not be using your component).

- Merging a merge request can be done by any developer
  as long as it is approved by a core developer.

- All branches are protected and force pushes are not allowed.
  We clean up merged branches and delete them from time to time.
  So please do not link to them from outside of the project;
  use permalinks (with the git hash or a tag) instead.
