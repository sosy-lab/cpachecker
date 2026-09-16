<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

# State sharing inside DSS blocks

The default DSS forward analysis uses value coverage for compatible interior
`BlockState`s. This lets PredicateCPA merge path formulas at control-flow joins,
and lets stop-sep share an existing suffix when the incoming formula is already
covered. BlockCPA itself still uses merge-sep: its domain decides whether the
other components may share the block value.

The original identity domain protected information that DSS needs after forward
exploration: execution paths, callstack compatibility, block history, witnesses,
and the processing of violation conditions. Replacing identity by location
equality alone would lose that information. The value domain relies on the
following separate mechanisms.

## Values and processing records

Interior (`MID`) equality includes the block, CFA location, phase, immutable
violation-condition list, block history, witness, and witness-check restriction.
It excludes diagnostic IDs, predecessor occurrences, and processing records.
Different histories and witness restrictions are conservatively kept separate.
All other phases retain identity equality.

Partition keys contain only the location and phase. Removing an obligation from
the pending work therefore cannot change the partition of a reached state.
History and violation-condition changes produce a new state before it enters
the reached set. Reset creates fresh processing records and clears the local
transfer trace.

At a block end, each violation condition produces its own ghost successor.
The full condition list is immutable; a separate occurrence-owned record tracks
processed conditions and callstack rejections. Processing records are refreshed
from surviving ghost states, so removing a ghost during refinement makes its
condition pending again.

## Backwards callstack effects

Local DSS callstack coverage compares three things: the current structural stack,
the mode that permits unknown entry frames, and the effect of the block prefix
on backwards callstack transfer. Comparing only the current stack would accept
paths that fit different successor-block callstacks.

Effects omit edges on which backwards callstack transfer is the identity.
Unmatched calls, returns, summary statements, and unsupported-call checks remain
explicit. Matching balanced calls are normalized into guards recording the
maximum temporary frame depth for each function. Thus repeated balanced calls
need not retain a growing callsite history, while recursion limits and the
existing skip/unsupported behavior are still checked. Equality is a sufficient
test for interchangeable effects, not a complete decision procedure for all
equivalent callstack transformers.

Unnormalized effects compare CFA edges by object identity. CFA-edge `equals`
compares endpoints and cannot distinguish all statements or callsites. Effects
are local to one analysis, reset at block entries, and are not serialized.
Distributed precondition coverage keeps its separate interpretation.

## Execution paths in the ARG

With `cpa.arg.preservePaths=true`, successful merging or coverage retains the
incoming parents on the surviving ARG node. This also handles coverage without
a formula-changing merge and a merge into a state whose suffix already exists.
The change happens only after the wrapped operator accepts it. The cycle check
runs before wrapped merge because PredicateCPA merging can have side effects.

The ARG records the exact CFA-edge alternatives for each parent relation.
`BlockState` supplies the transfer trace through
`AbstractStateWithIncomingEdge`, including intermediate edges hidden by composite
edge aggregation. Parallel edges with the same endpoints remain distinct.
Replacement and removal of ARG states transfer or remove this metadata together
with the corresponding parent relation. The metadata is allocated only when used.

Block results take an immutable snapshot of the reachable predecessor graph.
Several results share that snapshot; explicit `ARGPath`s are generated lazily.
Refinement or another block run cannot change a previously captured topology.
Path counts use `BigInteger`, so counting does not require enumerating all paths
or risk integer overflow.

Violation-condition reporting still computes the backwards condition for each
explicit path and its particular successor condition. Only then does it combine
compatible results at the same program point. This preserves correlations
between predicates, callstacks, and witness paths. Cancellation is checked while
enumerating paths.

## Forward progress while violations are unresolved

An exploration group can contain several entry states at the same program point.
The engine analyzes each input separately under each group of violation conditions
with the same exit callstack. Their outcomes must remain separate until it decides
which forward summaries to publish:

- An analysis without feasible violations contributes all its reachable exit
  states. Conditions may have been refuted by predicates or rejected by the
  callstack; rejecting a condition does not make the exit unreachable.
- An analysis with a feasible violation contributes backward conditions. Its exit
  states may still be too coarse to provide useful forward refinement, so they
  are not published.
- A violation from one entry state does not suppress refined exit states obtained
  from another entry state in the same group.

Publishing coarse exits from unresolved analyses can feed them around a loop and
repeatedly weaken its entry conditions, preventing convergence. Conversely,
discarding the entire group's exits whenever any entry state has a violation can
stall the forward updates needed to discover a reachable error. Grouping by
callstack alone does not distinguish these cases: different predicate states can
have the same callstack.

For multiple condition groups, the engine first probes each group separately
and remembers only the identifiers of groups that completed with reachable exits.
It then analyzes that same input again under the selected conditions. This
produces a complete postcondition that incorporates the refinements from the
different exit contexts, instead of taking a disjunction with coarse exits from
an unrelated context. Selection is separate for each input, even when several
inputs share a callstack. With `distributedSummaries.combinePresByHash`, the
inputs at a program point are explicitly combined first.

Successful probes do not materialize summaries or retain their ARGs and
precisions. Even if only one of several groups succeeds, it is rerun to produce
the final postcondition. This trades an extra CPA run for avoiding retained probe
results. When only one condition group exists, the engine runs it directly
without a separate probe. If all earlier groups contributed no exits, the last
group also produces its output directly: no further group can cause a combined
replacement. Feasible backward obligations found during probing remain part of
the output.

Callstack rejection requests speculative exploration from an unconstrained caller.
These requests describe other possible callers and do not invalidate a completed
analysis of the known input. Thus they do not prevent combining that input's
successful refinements. They must nevertheless survive replacement of the
individual runs by the combined run. Likewise, a feasible violation on one path
must not suppress the speculative exploration needed for callstack rejections on
another path.

The engine collects these requests while refining the known inputs, then checks
each distinct group of conditions once from an unconstrained caller. This avoids
repeating the same speculative analysis for every input state. The requests live
only for the current exploration; no results are cached across incoming updates.
Speculative exploration never publishes forward summaries.

The exploration accumulator is local to one incoming update. It retains only
outgoing summaries, backward path snapshots needed for serialization, and
condition-group identifiers for pending speculative checks. The worker clears
its native reached set after producing the serialized messages, including when
analysis or serialization fails. Later updates recompute from the current
incoming conditions; they do not resume an earlier reached set. The input
handlers still retain the latest predecessor summaries and successor conditions,
which are necessary inputs to the distributed fixpoint. The CPA and its solver
also remain available for subsequent runs.

Removing reachable exits merely because their conditions were rejected by the
callstack is unsound: other contexts may still require these exits to discover an
error. A replacement must describe a complete analysis of the same input.
`AlwaysReplaceExplorationEngineTest` covers these publication and combination
rules, including multiple inputs sharing a program point and speculative callers.

## Configuration and limits

`config/distributed-summary-synthesis/dss-block-analysis.properties` enables:

```properties
cpa.block.domain=VALUE
cpa.arg.preservePaths=true
```

The DSS analysis validates that `VALUE` uses an outer ARGCPA with path
preservation, DssCallstackCPA, and CompositeCPA's `AGREE` merge operator.
ARG path preservation rejects CPA-enabled analysis and keeping covered states
in the reached set; forced covering is disabled in this mode.
Standalone BlockCPA defaults to `IDENTITY`.
To compare the forward state count with identity coverage, override
`cpa.block.domain=IDENTITY` in the DSS worker's forward configuration.

The implementation deliberately retains these limits:

* Initial, final, and abstraction occurrences remain distinct. Sharing block
  ends would require transferring ghost obligations and invalidating completed
  work when another prefix arrives after processing.
* A merge or coverage that would make the provenance graph cyclic is rejected.
  This preserves loop iterations, but does not add general local-loop
  convergence. Cyclic coverage needs a sound summary interpretation for backwards
  conditions and witnesses before it can be enabled.
* Sharing and snapshot construction avoid eagerly materializing every path.
  Reporting all backwards conditions and serializing explicit witness alternatives
  can still take exponential time and space. The graph representation alone does
  not give polynomial total reporting cost.

## Regression coverage

`BlockStateTest` checks equality boundaries, immutable semantic fields, stable
partitioning, and fresh or refreshed processing records.
`DssCallstackDomainTest` and `DssCallstackEffectTest` check local coverage, reset,
callsite distinctions, and normalization against backwards replay, including
nested calls and recursion limits.

`ARGPathPreservationTest` checks stop without merge, rejected coverage, late
merging, exact parallel edges, and cycle rejection before wrapped side effects.
`DssBlockPathGraphTest` checks lazy access to a graph with 2^60 paths, snapshot
independence, and rejection of cyclic provenance.

`DssBlockMergingTest` compares complete diamond paths and state counts under
identity and value coverage, checks composite edge aggregation, and exercises
multiple ghost obligations and repeated runs. `DistributedSummarySynthesisTest`
compares safe and unsafe end-to-end verdicts and requires reconstructed
counterexamples for the unsafe cases.
