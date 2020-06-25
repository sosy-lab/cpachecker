Logging in CPAchecker
=====================

This file contains guidelines and hints
about how to best use logging in CPAchecker.

LogManager instead of System.out
--------------------------------
In CPAchecker, it is not possible to use `System.out`/`System.err`,
because CPAchecker needs to run in environments where these are
not available or where their output is lost.
Thus it is required to always use our `LogManager` for logging.
We now have a commit hook that checks for calls to `System.out`/`err`
and rejects such commits.
The only exception is the package `cmdline`,
which contains code that is only executed when CPAchecker is run
as a command-line application.

Further advantages of using the `LogManager` are:

- Possibility to use log levels to avoid cluttering the output
  with lots of messages that are not relevant for other users/developers
  (this is important, c.f. below).
- The `LogManager` can add additional information such as a timestamp
  and the source of the logging call.
  In cases where CPAchecker runs an analysis as a sub-analysis
  or auxiliary analysis, this is also visible in the logging message
  to make it distinguishable from messages of the main analysis.
- `stdout` can be used together with a log file, and both may have
  different log levels and different amount of additional information.

These advantages (especially the second one) are the reason
why we need to pass `LogManager` instances explicitly
to all components that need them (dependency injection),
instead of for example calling a method whenever a logger is needed.
Sometimes this might be a little bit inconvenient,
but using this pattern helps us and our users
by creating more informative and helpful log output.
If a data class should not store a reference to the logger
but contains complex code that should using logging,
consider either requiring a logger as a parameter for those methods
or moving the complex code to a separate `manager` class.


Log Levels
----------
CPAchecker uses the standard logging levels in the Java API,
with the following semantics and use cases:

- `SEVERE`: This is only for cases where CPAchecker cannot continue
  any more and needs to abort. In most parts of the code,
  an exception is thrown instead, so this level is normally *not* used.
  Furthermore, problems that are severe in some circumstances
  may not be severe in under circumstances (such as when the analysis
  is used only as an auxiliary analysis),
  and thus the decision whether a problem is severe or not
  may belong to some outer layer of code.

- `WARNING`: Normal level for all cases where the user needs to be warned
  (e.g., because CPAchecker encountered a problem and may not be able
  to continue normally or behave as the user expected).

- `INFO`: Normal level for all other cases of notifications to the user,
  such as progress messages like "start of analysis X".
  Please do not log too much on this level (c.f. below).

The rest of the levels are not visible by default.

- `CONFIG`: Information about which configuration is chosen.
  This is usually only used by code of the configuration package.

- `FINE`/`FINER`/`FINEST`: Typical debug messages,
  such as fine-grained progress messages.
  The three levels should be roughly used as follows:
  - `FINE`: algorithm level
  - `FINER`: CPA level
  - `FINEST`: level of utility methods/classes/packages
  
  Exceptions to this may be appropriate, for example tight loops
  might log one level lower than other code.

- `ALL`: Level for messages with full details/much data.
  All other levels should not output large amounts of data,
  but on this level it is acceptable to for example
  log complete paths, SMT formulas, or abstract states.
  Such output can be a significant performance limitation and
  produce very large log files,
  thus we have an extra log level for this.
  Often it is appropriate to have one logging statement
  with a higher level giving a progress message
  accompanied with a second logging statement with level ALL
  giving the actual data.


Lazy Logging
------------
For performance reasons, it is important to avoid string concatenation
and calling `toString()` in logging statements.
Instead of creating the full logging message,
pass the individual pieces using the varargs of `log()` or `logf()`,
deferring the string concatenation to the logger
which will do it only if necessary.
Other expensive method calls (for example using a `Joiner`)
should also be done lazily and only if necessary.
This can be done by using the log method that takes a `Supplier<String>`,
or by wrapping the expensive code in a lambda
and a call to `MoreStrings.lazyString()`:
`logger.log(..., MoreStrings.lazyString(() -> compute()), ...);`

Do not log too much
-------------------
Especially for those logging levels that are shown to the user by default,
please make sure to not log information that is not interesting for the user,
and do not produce large amounts of output.
An important part of the usability of a tool is
that its output is readable and informative.
We do not want to overwhelm users with megabytes of output.
Plus, too much output will hide important messages
and therefore hinder yourself and other developers.

If something can be seen as a result of your analysis,
consider writing it into an output file instead of using logging to print it.

Of course, this should not stop you from adding actually helpful logging statements.


Logging of Exceptions
---------------------
There are a few additional guidelines for most effective logging of exceptions:

- Do not log and re-throw the exception.
  It is the responsibility of the code that finally catches an exception
  to log it, and logging an exception before (re-)throwing it
  will produce confusing logs which contain the same exception
  multiple times.
- When logging an exception, always add a log message that explains
  where this exception happened and what effect this has.
- `Throwable.printStackTrace()` should not be used, always use the logger.
- Always pass the exception object to the LogManager.
  Do not use `e.toString()` or `e.getMessage()` when logging the exception.
  Logging only the message of the exception would hide important details
  such as the stack trace and the cause of the exception,
  which can be very important for debugging.
  Even if the stack trace should not be shown to the user,
  we still want to log it with a low log level for debugging.

To follow the latter guideline, always use one of the `log*Exception`
methods of `LogManager` when logging an exception:

- `logUserException` is for cases where an exception that indicates
  a "normal" problem should be shown to the user,
  e.g., a configuration error. It will print only the message,
  but full details get logged on a low log level.
- `logDebugException` is for cases where an exception can be handled
  and the user does not need to be informed.
  Use this method to log the exception on a low level for debugging.
- `logException` is for cases where the full details of the exception
  should be shown, including stack trace and cause.
  This should be done only for exceptions that indicate a bug,
  and is usually not used by most of the code.


Tests
-----
Unit tests should not use logging because they are designed
to be run automatically, and any logging in a test
either clutters the output or is lost anyway.
The class `TestLogManager` should be used whenever a `LogManager` instance
is required in a unit test.
In addition to discarding the output, this class additionally checks
for correctness of all logging calls (such as correct string format,
or illegal null arguments).
The class `CPATestRunner` provides the possibility to run
a full CPAchecker analysis inside a test and retrieve the log,
such that it can be checked for specific contents.
If the same should be done for testing specific components,
a `BasicLogManager` with a `StringBuildingLogHandler` can be used.
