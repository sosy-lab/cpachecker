Style & Coding Guide
====================

The style guide of this project is the [Google Java Style](https://google.github.io/styleguide/javaguide.html).

Further guidelines that are worth reading:
- Bloch: [Effective Java, 2nd Edition](http://www.amazon.com/Effective-Java-Edition-Joshua-Bloch/dp/0321356683/)
- [Guava User Guide](https://github.com/google/guava/wiki)

Some additional information can be found in other files
in this directory, e.g. [`Logging.md`](Logging.md) and [`Test.md`](Test.md).

Please read all these documents, they will let you write better code
with considerably less effort!

We use an automatic code formatter to format our new code
(existing code still may violate the style guide).
Before committing, run `ant format-diff` to reformat your changes
according to the style guide.
For git users: You need to stage your changes first (with `git add`),
then run `ant format-diff` and then stage the changes again.
This allows you to view exactly what the reformatter changed after running it.
Please do not use the Eclipse code formatter,
it destroys a lot of meaningful manual formatting.

Additional rules and hints:

Spelling:

- Try to avoid typos.
- Interfaces are not named with a leading `I`.
  In general, client code should not need to know whether
  it is using an interface or a concrete class,
  thus there should not be a naming difference.
  Furthermore, a good API should always make sure that
  the best way to do something is also the easiest way.
  When both an interface and a similarly-named class exist,
  the interface is the one that should primarily be used,
  and thus the interface gets the normal/clean/beautiful name,
  and the class the internal/ugly name.
  It is better to have one place using `FooImpl` and hundreds of places using `Foo`
  instead of one place using `Foo` and hundreds of places using `IFoo`.
- Parameters should start with `p` to avoid confusion with
  local variables or fields.
- For a set of names of concepts (e.g., of files, classes),
  the prefix order induced by the names should
  represent the semantic relation and structure of the concepts.

Compilation:

- Never check in with compile errors.
- Avoid warnings:
  - If there is a way to fix the code, fix it (before committing).
  - Otherwise use `@SuppressWarnings`.
- After adding/changing an `@Option` configuration,
  run `ant` to update documentation (before committing).

Design:

- Prefer immutable objects, for own classes and for collections
  (https://github.com/google/guava/wiki/ImmutableCollectionsExplained).
  Do not add setters unless really required!
- Avoid null, replace it with real objects, or (at last resort) Optional:
  https://github.com/google/guava/wiki/UsingAndAvoidingNullExplained
- Avoid boolean parameters. They carry no semantics
  and make it hard to understand for the reader what effect they have
  (cf. http://martinfowler.com/bliki/FlagArgument.html)

Configuration options:

- Only use the `@Option` scheme so your options are automatically type-checked
  and documented.
- Only introduce an option when its meaningful
  (do not add options that nobody would ever want to change).
- Preferably use one of the existing prefixes
  (`cpa.YOURCPA.`, `analysis.`, `parser.`, ...)
  instead of adding a new one.
- Do not use negated predicates as option name
  (use `something.enable` instead `something.disable`,
  `something.foo` instead of `something.noFoo` etc.).
- Do not forget to update the file `doc/ConfigurationOptions.txt`
  (done automatically by ant) and commit it together with your changes.

Documentation / Comments:

- The following ranks several places by their importance of having comments:
  * packages (in `package-info.java`, EVERY package should have one!)
  * interfaces and public classes (at least a short note at the top of their responsibility)
  * public methods in interfaces and classes
  * non-public classes, methods and fields
- Please add comments wherever sensible,
  but make sure to add comments for the top three items!
- All command-line options need to be explained in doc/Configuration.txt.
- All `@Option` fields need to have a non-empty description
  that explains (to a user) what the option does.

Coding:

- Never have public fields,
  never have non-private non-final fields,
  and try to keep all other non-private fields to a minimum.
- If you use null in method parameters or return values, annotate them with @Nullable.
- Mark fields as final, if they are never modified,
  and try to make them final, if they are modified (-> immutability).
- Prefer enhanced for-loop over `List.get(int)`.
- Use try-with-resources instead of manually calling `close()`.
- Use arrays only with primitive types (`int`, `long`, etc.)
  or when existing APIs require them.
  Otherwise never use arrays of object types, use lists instead.
  They have a much nicer API, are equally fast,
  and allow you to use `ImmutableList` and `Collections.unmodifiableList()`
  to avoid the need for defensive copying while still guaranteeing immutability.
- Never use classes from the `sun.*` packages, they are not part of the Java API.
  Specifically, use `UnsupportedOperationException` instead of `NotImplementedException`.
- Do not use the following classes from the JDK, they all have better replacements:
  `Vector` (replaced by `ArrayList`),
  `Stack` (replaced by `Deque` interface with implementations `ArrayDeque` and `LinkedList`),
  and `Hashtable` (replaced by `HashMap`).
- The default list implementation is `ArrayList` (fast and memory-efficient),
  use `LinkedList` only if you need (i.e., when adding/removing elements in the middle of the list).
  When adding/removing elements at the start of the list, use `ArrayDeque`.
- When declaring variables of collection types,
  use the interface as type instead of the implementation (e.g., `List` instead of `ArrayList`).
  This is especially true for fields, parameters, and return types.
  Do use the `Immutable*` types from Guava, though, to show that your collection is immutable.
- Avoid Collectors.toList() and Collectors.toSet().
  These guarantee neither mutability nor immutability.
  Use a solution that returns an immutable collection instead,
  or one that is guaranteed to return a mutable one if you need it.
  We have utility methods for this in MoreCollectors and Collections3.
- Do not over-use functional idioms!
  When an imperative version of the code is shorter and easier to read,
  it should be preferred.
- Use `Integer`, `Double`, `Long`, `Boolean` etc. only when necessary (this is, inside generics like in `List<Integer>`).
  In fields, method parameters and return values, and local parameters,
  it is better to use the corresponding primitive types like int.
- Never call the constructor of `Integer`, `Double`, `Long`, `Boolean`, etc.!
  Use the `valueOf()` method, it may do caching.
- Never call the `String` constructor.
  Strings do not need copying, and for other uses there is the `valueOf()` method.
- Be very careful when implementing `Comparator` or `Comparable`!
  The logic for comparison is typically very error prone.
  For example, one must not use integer subtraction or integer negation,
  because both can overflow and cause a wrong result.
  Try to avoid implementing Comparators and instead use Guava's Ordering
  or the Comparator utilities that exist since Java 8
  (static methods on `Comparator` interface).
  If you implement `Comparator` or `Comparable`, use Guava's `ComparisonChain`
  or at least methods like `Integer.compare` for comparison.
- Avoid `Cloneable` and `clone()`, use copy constructors instead if you need them
  (you don't if you use immutable classes).
- Never swallow an exception.
  If throwing another exception, add the original one as the cause.
  If logging, use the appropriate logger methods (c.f. [`Logging.md`](Logging.md)).
- Do not catch `Exception`, catch only the specific exception types
  that need to be caught!
- Always put `@Override` when implementing an overriding method
  (Eclipse does this automatically).
