Version Control
===============

Commits
-------
- Commit early, commit often.
- Split the commits into reasonable changesets.
- Add meaningful commit messages that help other developers
  to understand what was done and (even more importantly!) *why* it was done.
- Do not mix semantic changes with code-formatting changes.
- Try to have separate commits for bugfixes, refactorings, and new code.
- When renaming files, ensure that SVN recognizes this as a renaming
  in order to not disconnect the history (use SVN plugin for Eclipse,
  or "svn mv" on comand line). This is especially important for git-svn users.
- In order to avoid EOL problems caused by Unix/Windows/Mac, let's run
  `find ./ -name "*.java" -exec svn propset svn:eol-style native {} \;`
  from time to time.
- Set the `svn:executable` property only on files that need it (executables).
  On Linux this is done automatically, but Windows users should take care
  that this property is not added for all newly created files.

Branches
--------
As much branching as necessary, but as little branching as possible.

Too little branching complicates development,
when changes interact and break things too often.
Too much branching produces unnecessary merging effort,
and complicates refactorings.

New code that does not influence existing code
can and should be added directly to the trunk.
This certainly includes new CPAs, new algorithms
but also other new packages or features
that are not used unless specifically enabled.

Development that may influence existing code on which other developers or users rely
should be done in a branch.
Branches should be used for one feature and merged back as soon as possible,
such that the origin of the code can be understood.
It is recommended to synchronize branches regularly with the trunk,
because CPAchecker is actively developed and waiting too long between merges
can lead to a large amount of changes that are difficult to apply.
