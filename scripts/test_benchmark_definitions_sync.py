# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

"""
Checks that local BenchExec test sets select the same tasks as their upstream sources.

For each run definition of an upstream benchmark definition, there must be a local run
definition that uses exactly the same set files with exactly the same property files.
Paths are compared after replacing the upstream path prefixes with the local ones.
Names of run definitions and task sets, options, limits, and order are ignored,
as is the grouping of set files into <tasks> elements.
If the test fails, its message lists the edits that make the local test set match.

The comparison needs network access and fails whenever upstream changes,
so it runs only if the environment variable CHECK_UPSTREAM_TEST_SETS is "true"
(in CI, only in the weekly scheduled pipeline). The tests of the comparison logic
always run.
"""

import os
import unittest
import urllib.error
import urllib.request
from xml.etree import ElementTree

script_path = os.path.dirname(os.path.realpath(__file__))
test_sets_dir = os.path.join(script_path, os.pardir, "test", "test-sets")
TEST_SETS_DISPLAY_DIR = "test/test-sets"

# Local test set (relative to test/test-sets/) -> URL of its upstream source
SYNCED_TEST_SETS = {
    "svcomp.xml": "https://gitlab.com/sosy-lab/sv-comp/bench-defs/-/raw/main/benchmark-defs/cpachecker.xml",
}

# Upstream path prefix -> local path prefix
PATH_PREFIX_REPLACEMENTS = {
    "../sv-benchmarks/c/": "../programs/benchmarks/",
    "../sv-benchmarks/sv-lib/": "../programs/svlib-benchmarks/",
}

DOWNLOAD_TIMEOUT_SECONDS = 60

_INCLUDE_TAGS = ("include", "includesfile")
_EXCLUDE_TAGS = ("exclude", "excludesfile")

# Indentation of the children of <tasks> in suggested snippets
_CHILD_INDENT = "  "


def _replace_prefix(path):
    for upstream_prefix, local_prefix in PATH_PREFIX_REPLACEMENTS.items():
        if path.startswith(upstream_prefix):
            return local_prefix + path[len(upstream_prefix) :]
    return path


def _unmapped_paths(xml_content):
    """Return the paths of the upstream file that no prefix replacement applies to."""
    benchmark = ElementTree.fromstring(xml_content)
    tags = (*_INCLUDE_TAGS, *_EXCLUDE_TAGS, "propertyfile")
    return sorted(
        {
            element.text.strip()
            for tag in tags
            for element in benchmark.iter(tag)
            if not element.text.strip().startswith(tuple(PATH_PREFIX_REPLACEMENTS))
        }
    )


def _property_file(*elements):
    """Return the property file of the innermost element that defines one."""
    for element in elements:
        property_file = element.find("propertyfile")
        if property_file is not None:
            return property_file.text.strip()
    return None


def _task_entries(tasks, property_file, normalize_path):
    """
    Return a dict with one entry (property file, included set, excluded sets) per
    included set of the given <tasks> element, mapped to the name of the element.
    Comparing entries instead of elements makes the grouping of sets irrelevant.
    """
    excludes = frozenset(
        (child.tag, normalize_path(child.text.strip()))
        for child in tasks
        if child.tag in _EXCLUDE_TAGS
    )
    normalized_property = normalize_path(property_file) if property_file else None
    return {
        (
            normalized_property,
            (child.tag, normalize_path(child.text.strip())),
            excludes,
        ): tasks.get("name")
        for child in tasks
        if child.tag in _INCLUDE_TAGS
    }


def parse_run_definitions(xml_content, normalize_path=lambda path: path):
    """
    Return a dict from run-definition name to a dict that maps each task entry
    selected by that run definition to the name of the <tasks> element selecting it.
    """
    benchmark = ElementTree.fromstring(xml_content)
    global_entries = {}
    for tasks in benchmark.findall("tasks"):
        global_entries.update(
            _task_entries(tasks, _property_file(tasks, benchmark), normalize_path)
        )

    run_definitions = {}
    for run_definition in benchmark.findall("rundefinition"):
        entries = dict(global_entries)
        for tasks in run_definition.findall("tasks"):
            entries.update(
                _task_entries(
                    tasks,
                    _property_file(tasks, run_definition, benchmark),
                    normalize_path,
                )
            )
        run_definitions[run_definition.get("name")] = entries
    return run_definitions


def _xml_element(tag, path):
    return f"<{tag}>{path}</{tag}>"


def _tasks_snippet(tasks_name, property_file, includes, excludes):
    """Return a <tasks> element that selects the given sets, ready to be pasted."""
    lines = [f'<tasks name="{tasks_name}">']
    lines += [_CHILD_INDENT + _xml_element(*include) for include in sorted(includes)]
    lines += [_CHILD_INDENT + _xml_element(*exclude) for exclude in sorted(excludes)]
    if property_file:
        lines.append(_CHILD_INDENT + _xml_element("propertyfile", property_file))
    lines.append("</tasks>")
    return "\n".join(lines)


def _group_by_tasks(entries):
    """Group entries by (tasks name, property file, excluded sets) of their element."""
    groups = {}
    for (property_file, include, excludes), tasks_name in entries.items():
        groups.setdefault((tasks_name, property_file, excludes), []).append(include)
    return groups


def _indent(text, prefix):
    return "\n".join(prefix + line for line in text.splitlines())


def _addition_steps(missing_entries, local_entries):
    """
    Return the edits that add the missing entries to a local run definition,
    reusing a local <tasks> element if one with the same name and property exists.
    """
    local_groups = _group_by_tasks(local_entries)
    steps = []
    for (tasks_name, property_file, excludes), includes in sorted(
        _group_by_tasks(missing_entries).items(), key=str
    ):
        if (tasks_name, property_file, excludes) in local_groups:
            for include in sorted(includes):
                steps.append(
                    f"Add {_xml_element(*include)} "
                    f'to the existing <tasks name="{tasks_name}">.'
                )
        else:
            snippet = _tasks_snippet(tasks_name, property_file, includes, excludes)
            steps.append("Add this <tasks> element:\n" + _indent(snippet, "  "))
    return steps


def _removal_steps(extra_entries, local_entries):
    """Return the edits that remove the extra entries from a local run definition."""
    local_groups = _group_by_tasks(local_entries)
    steps = []
    for (tasks_name, property_file, excludes), includes in sorted(
        _group_by_tasks(extra_entries).items(), key=str
    ):
        if len(includes) == len(local_groups[(tasks_name, property_file, excludes)]):
            steps.append(f'Remove the whole <tasks name="{tasks_name}"> element.')
        else:
            for include in sorted(includes):
                steps.append(
                    f'Remove {_xml_element(*include)} from <tasks name="{tasks_name}">.'
                )
    return steps


def _pair_run_definitions(upstream_run_definitions, local_run_definitions):
    """
    Pair each upstream run definition with a distinct local one.
    Exactly equal run definitions are paired first, then the most similar ones
    (run definitions with a common set file or the same name).
    Return the list of pairs (upstream name, local name or None)
    and the list of unpaired local names.
    """
    upstream_sets = {n: frozenset(e) for n, e in upstream_run_definitions.items()}
    local_sets = {n: frozenset(e) for n, e in local_run_definitions.items()}
    pairs = {}
    unpaired_local = list(local_sets)

    def pair(upstream_name, local_name):
        pairs[upstream_name] = local_name
        unpaired_local.remove(local_name)

    # Exact matches, preferring the same name
    for upstream_name, entries in upstream_sets.items():
        candidates = [n for n in unpaired_local if local_sets[n] == entries]
        if candidates:
            pair(
                upstream_name,
                upstream_name if upstream_name in candidates else candidates[0],
            )

    # Greedy pairing of the remaining run definitions by similarity
    candidates = sorted(
        (
            (
                len(upstream_sets[u] & local_sets[n]),
                u == n,
                -list(upstream_sets).index(u),
                -list(local_sets).index(n),
            ),
            u,
            n,
        )
        for u in upstream_sets
        if u not in pairs
        for n in unpaired_local
        if upstream_sets[u] & local_sets[n] or u == n
    )
    for _, upstream_name, local_name in reversed(candidates):
        if upstream_name not in pairs and local_name in unpaired_local:
            pair(upstream_name, local_name)

    return [(u, pairs.get(u)) for u in upstream_sets], unpaired_local


def describe_required_changes(upstream_run_definitions, local_run_definitions):
    """
    Return the list of edits that make the local run definitions select the same
    tasks as the upstream ones, or an empty list if they already do.
    """
    pairs, unpaired_local = _pair_run_definitions(
        upstream_run_definitions, local_run_definitions
    )
    changes = []
    for upstream_name, local_name in pairs:
        upstream_entries = upstream_run_definitions[upstream_name]
        if local_name is None:
            snippets = [
                _tasks_snippet(tasks_name, property_file, includes, excludes)
                for (tasks_name, property_file, excludes), includes in sorted(
                    _group_by_tasks(upstream_entries).items(), key=str
                )
            ]
            changes.append(
                "Add this run definition "
                "(it corresponds to the upstream run definition "
                f'"{upstream_name}"):\n'
                f'  <rundefinition name="{upstream_name}">\n'
                + "\n".join(_indent(snippet, "    ") for snippet in snippets)
                + "\n  </rundefinition>"
            )
            continue

        local_entries = local_run_definitions[local_name]
        missing = {
            entry: name
            for entry, name in upstream_entries.items()
            if entry not in local_entries
        }
        extra = {
            entry: name
            for entry, name in local_entries.items()
            if entry not in upstream_entries
        }
        steps = _addition_steps(missing, local_entries) + _removal_steps(
            extra, local_entries
        )
        if steps:
            changes.append(
                f'In <rundefinition name="{local_name}"> '
                f'(corresponds to the upstream run definition "{upstream_name}"):\n'
                + "\n".join(_indent(f"- {step}", "  ") for step in steps)
            )

    for local_name in unpaired_local:
        changes.append(
            f'Remove <rundefinition name="{local_name}">: '
            "no upstream run definition selects the same tasks."
        )
    return changes


def _format_failure(local_name, upstream_url, changes):
    local_path = f"{TEST_SETS_DISPLAY_DIR}/{local_name}"
    numbered = [
        f"{i}. " + change.replace("\n", "\n   ")
        for i, change in enumerate(changes, start=1)
    ]
    return (
        f"{local_path} does not select the same tasks as {upstream_url}.\n"
        f"Make the following changes in {local_path} and run this test again.\n"
        "Paths are already converted to the local layout; "
        "names of run definitions and <tasks> elements may be chosen freely.\n\n"
        + "\n\n".join(numbered)
    )


def _download(url):
    # The content is kept in memory only, so nothing needs to be cleaned up.
    with urllib.request.urlopen(url, timeout=DOWNLOAD_TIMEOUT_SECONDS) as response:
        return response.read()


@unittest.skipUnless(
    os.environ.get("CHECK_UPSTREAM_TEST_SETS") == "true",
    "Set the environment variable CHECK_UPSTREAM_TEST_SETS=true "
    "to compare the test sets with their upstream sources",
)
class TestBenchmarkDefinitionsSync(unittest.TestCase):
    def test_run_definitions_match_upstream(self):
        for local_name, upstream_url in SYNCED_TEST_SETS.items():
            with self.subTest(test_set=local_name):
                try:
                    upstream_content = _download(upstream_url)
                except (urllib.error.URLError, TimeoutError) as e:
                    self.fail(
                        f"Cannot download {upstream_url}: {e}\n"
                        "Check that the URL in SYNCED_TEST_SETS in "
                        f"scripts/{os.path.basename(__file__)} is still valid "
                        "and that the network is reachable, "
                        "then run this test again."
                    )

                unmapped = _unmapped_paths(upstream_content)
                if unmapped:
                    self.fail(
                        f"{upstream_url} uses paths without a known local "
                        "location:\n"
                        + "\n".join(f"  {path}" for path in unmapped)
                        + "\nAdd an entry for their upstream prefix "
                        "to PATH_PREFIX_REPLACEMENTS in "
                        f"scripts/{os.path.basename(__file__)} "
                        "that maps it to the corresponding directory "
                        "below test/programs/, and run this test again."
                    )

                with open(os.path.join(test_sets_dir, local_name), "rb") as f:
                    local_content = f.read()

                changes = describe_required_changes(
                    parse_run_definitions(upstream_content, _replace_prefix),
                    parse_run_definitions(local_content),
                )
                if changes:
                    self.fail(_format_failure(local_name, upstream_url, changes))


class TestDescribeRequiredChanges(unittest.TestCase):
    """Checks the comparison logic without network access."""

    upstream = """<benchmark tool="t">
      <rundefinition name="A">
        <tasks name="X">
          <includesfile>../sv-benchmarks/c/One.set</includesfile>
          <includesfile>../sv-benchmarks/c/Two.set</includesfile>
          <propertyfile>../sv-benchmarks/c/properties/p.prp</propertyfile>
        </tasks>
      </rundefinition>
    </benchmark>"""

    def _changes(self, local):
        return describe_required_changes(
            parse_run_definitions(self.upstream, _replace_prefix),
            parse_run_definitions(local),
        )

    def test_ignores_names_options_order_and_grouping(self):
        local = """<benchmark tool="t">
          <option name="--foo"/>
          <rundefinition name="B">
            <tasks name="Y">
              <includesfile>../programs/benchmarks/Two.set</includesfile>
              <propertyfile>../programs/benchmarks/properties/p.prp</propertyfile>
            </tasks>
            <tasks name="Z">
              <includesfile>../programs/benchmarks/One.set</includesfile>
              <propertyfile>../programs/benchmarks/properties/p.prp</propertyfile>
            </tasks>
          </rundefinition>
        </benchmark>"""
        self.assertEqual(self._changes(local), [])

    def test_missing_set_is_added_to_existing_tasks(self):
        local = """<benchmark tool="t">
          <rundefinition name="A">
            <tasks name="X">
              <includesfile>../programs/benchmarks/One.set</includesfile>
              <propertyfile>../programs/benchmarks/properties/p.prp</propertyfile>
            </tasks>
          </rundefinition>
        </benchmark>"""
        changes = self._changes(local)
        self.assertEqual(len(changes), 1)
        self.assertIn('In <rundefinition name="A">', changes[0])
        self.assertIn(
            "Add <includesfile>../programs/benchmarks/Two.set</includesfile> "
            'to the existing <tasks name="X">.',
            changes[0],
        )

    def test_different_property_is_replaced(self):
        local = """<benchmark tool="t">
          <rundefinition name="A">
            <tasks name="X">
              <includesfile>../programs/benchmarks/One.set</includesfile>
              <includesfile>../programs/benchmarks/Two.set</includesfile>
              <propertyfile>../programs/benchmarks/properties/q.prp</propertyfile>
            </tasks>
          </rundefinition>
        </benchmark>"""
        changes = self._changes(local)
        self.assertEqual(len(changes), 1)
        self.assertIn("Add this <tasks> element:", changes[0])
        self.assertIn(
            "<propertyfile>../programs/benchmarks/properties/p.prp</propertyfile>",
            changes[0],
        )
        self.assertIn('Remove the whole <tasks name="X"> element.', changes[0])

    def test_sets_split_across_run_definitions(self):
        local = """<benchmark tool="t">
          <rundefinition name="A1">
            <tasks name="X">
              <includesfile>../programs/benchmarks/One.set</includesfile>
              <propertyfile>../programs/benchmarks/properties/p.prp</propertyfile>
            </tasks>
          </rundefinition>
          <rundefinition name="A2">
            <tasks name="X">
              <includesfile>../programs/benchmarks/Two.set</includesfile>
              <propertyfile>../programs/benchmarks/properties/p.prp</propertyfile>
            </tasks>
          </rundefinition>
        </benchmark>"""
        changes = self._changes(local)
        self.assertEqual(len(changes), 2)
        self.assertIn('In <rundefinition name="A1">', changes[0])
        self.assertIn("Two.set</includesfile> to the existing", changes[0])
        self.assertIn('Remove <rundefinition name="A2">', changes[1])

    def test_missing_run_definition_is_added(self):
        local = """<benchmark tool="t">
          <rundefinition name="B">
            <tasks name="Y">
              <includesfile>../programs/benchmarks/Three.set</includesfile>
              <propertyfile>../programs/benchmarks/properties/p.prp</propertyfile>
            </tasks>
          </rundefinition>
        </benchmark>"""
        changes = self._changes(local)
        self.assertEqual(len(changes), 2)
        self.assertIn('<rundefinition name="A">', changes[0])
        self.assertIn(
            "<includesfile>../programs/benchmarks/One.set</includesfile>", changes[0]
        )
        self.assertIn('Remove <rundefinition name="B">', changes[1])

    def test_unmapped_paths(self):
        upstream = """<benchmark tool="t">
          <rundefinition name="A">
            <tasks name="X">
              <includesfile>../sv-benchmarks/java/One.set</includesfile>
              <propertyfile>../sv-benchmarks/c/properties/p.prp</propertyfile>
            </tasks>
          </rundefinition>
        </benchmark>"""
        self.assertEqual(_unmapped_paths(upstream), ["../sv-benchmarks/java/One.set"])


if __name__ == "__main__":
    unittest.main()
