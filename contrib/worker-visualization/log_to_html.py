#!/usr/bin/env python3

# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

"""
Worker Log Visualization Tool

Transforms the messages exported by the ``DssVisualizationWorker`` of the
distributed-summary-synthesis (DSS) algorithm into an interactive HTML report.

The exported messages are flat JSON maps of the shape::

    {"header":  {"senderId": ..., "messageType": ..., "timestamp": ..., "identifier": ...},
     "content": {"states": "2",
                 "status.sound": "true",
                 "state0.<fully.qualified.CpaState>.<field>": ...}}

This script regroups that key space per contained abstract state and per CPA,
decodes the serialized ``BlockState`` (block id, block-graph history, witness),
elides machine-only blobs, and marks messages that repeat the content a block
has already sent -- the usual symptom of a fixpoint that is not reached.
"""

import argparse
import html
import json
import re
import sys
import webbrowser
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any

import networkx as nx
import pydot

ENCODING = "UTF-8"

#: All message types the Java side can emit (``DssMessage.DssMessageType``).
MESSAGE_TYPES = (
    "POST_CONDITION",
    "VIOLATION_CONDITION",
    "RESULT",
    "WITNESS",
    "EXCEPTION",
)

#: Message type -> CSS modifier class.
TYPE_TO_CSS_CLASS = {
    "POST_CONDITION": "post-condition",
    "VIOLATION_CONDITION": "violation-condition",
    "RESULT": "result",
    "WITNESS": "witness",
    "EXCEPTION": "exception",
}

#: Message type -> arrow describing the direction the message travels.
TYPE_TO_ARROW = {
    "POST_CONDITION": "&darr;",
    "VIOLATION_CONDITION": "&uarr;",
    "RESULT": "&#9679;",
    "WITNESS": "&#9679;",
    "EXCEPTION": "&#9888;",
}

#: Content fields that only a machine can read; never worth showing.
OPAQUE_FIELDS = frozenset({"pts"})

#: Values longer than this are abbreviated in the rendered content.
MAX_VALUE_LENGTH = 400

#: ``state<index>.`` prefix of a content key.
STATE_KEY_PATTERN = re.compile(r"^state(\d+)\.(.*)$")


# ---------------------------------------------------------------------------
# Data model
# ---------------------------------------------------------------------------


@dataclass(frozen=True)
class Block:
    """One node of the block graph, as exported to ``blocks.json``."""

    identifier: str
    predecessors: list[str]
    successors: list[str]
    code: str
    start_node: int | None = None
    end_node: int | None = None
    abstraction_location: int | None = None

    @classmethod
    def from_json(cls, block_id: str, data: dict[str, Any]) -> "Block":
        return cls(
            identifier=block_id,
            predecessors=list(data.get("predecessors", [])),
            successors=list(data.get("successors", [])),
            code="\n".join(line for line in data.get("code", []) if line),
            start_node=data.get("startNode"),
            end_node=data.get("endNode"),
            abstraction_location=data.get("abstractionLocation"),
        )


@dataclass
class Message:
    """One message exported by the visualization worker."""

    file_name: str
    sequence: int
    identifier: str
    sender: str
    message_type: str
    timestamp_ns: int
    content: dict[str, str]

    # Derived, filled in by ``analyze_messages``.
    relative_ms: float = 0.0
    repeats: str | None = None
    #: (content key, value in the previous message, value in this message)
    changes: list[tuple[str, str | None, str | None]] = field(default_factory=list)

    @property
    def label(self) -> str:
        """Short identifier of this message, e.g. ``M42``."""
        return Path(self.file_name).stem

    @property
    def state_count(self) -> int:
        try:
            return int(self.content.get("states", "0"))
        except ValueError:
            return 0

    @classmethod
    def from_json(cls, file_name: str, data: dict[str, Any]) -> "Message | None":
        header = data.get("header")
        content = data.get("content")
        if not isinstance(header, dict) or not isinstance(content, dict):
            print(f"WARNING: {file_name} is not a DSS message", file=sys.stderr)
            return None
        try:
            timestamp = int(header["timestamp"])
        except (KeyError, ValueError):
            print(f"WARNING: {file_name} has no usable timestamp", file=sys.stderr)
            return None
        message_type = header.get("messageType", "UNKNOWN")
        if message_type not in MESSAGE_TYPES:
            print(
                f"WARNING: {file_name} has unknown message type {message_type!r}",
                file=sys.stderr,
            )
        return cls(
            file_name=file_name,
            sequence=sequence_number(file_name),
            identifier=header.get("identifier", "UNKNOWN"),
            sender=header.get("senderId", "UNKNOWN"),
            message_type=message_type,
            timestamp_ns=timestamp,
            content=dict(content),
        )


@dataclass(frozen=True)
class ContentKey:
    """A content key of the shape ``state<index>.<package>.<Component>.<field>``."""

    index: int
    component: str
    field_name: str

    @classmethod
    def parse(cls, key: str) -> "ContentKey | None":
        match = STATE_KEY_PATTERN.match(key)
        if not match:
            return None
        segments = match.group(2).split(".")
        # The component is the first segment starting with an upper-case letter:
        # everything before it is the package, everything after it is the field.
        for position, segment in enumerate(segments[:-1]):
            if segment[:1].isupper():
                return cls(
                    index=int(match.group(1)),
                    component=segment,
                    field_name=".".join(segments[position + 1 :]),
                )
        return cls(index=int(match.group(1)), component="?", field_name=match.group(2))


# ---------------------------------------------------------------------------
# Loading
# ---------------------------------------------------------------------------


def sequence_number(file_name: str) -> int:
    """Numeric part of a message file name (``M42.json`` -> ``42``)."""
    digits = re.sub(r"\D", "", Path(file_name).stem)
    return int(digits) if digits else 0


def natural_key(text: str) -> tuple[Any, ...]:
    """Sort key that orders ``L2`` before ``L10`` instead of after it."""
    return tuple(
        int(part) if part.isdigit() else part for part in re.split(r"(\d+)", text)
    )


def load_json_file(json_file: Path) -> dict[str, Any]:
    """Load and parse a JSON file, reporting problems instead of raising."""
    try:
        with open(json_file, encoding=ENCODING) as file:
            return json.load(file)
    except json.JSONDecodeError as e:
        print(f"WARNING: JSON decoding error in {json_file}: {e}", file=sys.stderr)
        return {}
    except OSError as e:
        print(f"WARNING: Error reading {json_file}: {e}", file=sys.stderr)
        return {}


def load_blocks(block_structure_file: Path) -> dict[str, Block]:
    """Load ``blocks.json`` into a mapping from block id to :class:`Block`."""
    raw = load_json_file(block_structure_file)
    return {
        block_id: Block.from_json(block_id, data)
        for block_id, data in raw.items()
        if isinstance(data, dict)
    }


def load_messages(message_dir: Path) -> list[Message]:
    """Load every message file of a directory, ordered by export sequence."""
    try:
        files = [
            f for f in message_dir.iterdir() if f.is_file() and f.suffix == ".json"
        ]
    except OSError as e:
        print(f"ERROR: Failed to read message directory: {e}", file=sys.stderr)
        return []

    messages = []
    for json_file in sorted(files, key=lambda f: sequence_number(f.name)):
        data = load_json_file(json_file)
        if not data:
            continue
        message = Message.from_json(json_file.name, data)
        if message is not None:
            messages.append(message)
    messages.sort(key=lambda m: (m.timestamp_ns, m.sequence))
    return messages


def select_run(messages: list[Message], requested_run: str | None) -> list[Message]:
    """Restrict the messages to a single run of the analysis.

    Every run of DSS stamps its messages with a fresh ``identifier``; the output
    directory usually still contains the messages of earlier runs. Unless a run
    is requested explicitly, the most recent one is used.
    """
    if not messages:
        return []
    runs: dict[str, list[Message]] = defaultdict(list)
    for message in messages:
        runs[message.identifier].append(message)

    if requested_run is not None:
        if requested_run not in runs:
            known = ", ".join(sorted(runs))
            print(
                f"ERROR: No messages for run {requested_run!r}. Known runs: {known}",
                file=sys.stderr,
            )
            return []
        selected = requested_run
    else:
        selected = max(runs, key=lambda run: runs[run][-1].timestamp_ns)

    if len(runs) > 1:
        others = ", ".join(
            f"{run} ({len(runs[run])})" for run in sorted(runs) if run != selected
        )
        print(
            f"Using run {selected} with {len(runs[selected])} messages; "
            f"ignoring other runs: {others}"
        )
        warn_about_overwritten_messages(runs[selected])
    return runs[selected]


def warn_about_overwritten_messages(run: list[Message]) -> None:
    """Warn if part of a run is missing because a later run reused the file names.

    The exporter numbers its files ``M0.json``, ``M1.json``, ... and restarts at
    zero for every run, so a later run silently overwrites the beginning of an
    earlier one. Gaps in the sequence numbers of a run make that visible.
    """
    numbers = sorted(message.sequence for message in run)
    missing = set(range(numbers[0], numbers[-1] + 1)) - set(numbers)
    if numbers[0] > 0:
        missing |= set(range(numbers[0]))
    if missing:
        print(
            f"WARNING: {len(missing)} message file(s) of this run were overwritten by a "
            f"later run (missing M{min(missing)}..M{max(missing)}). Delete the message "
            "directory before a run to get a complete picture.",
            file=sys.stderr,
        )


def describe_runs(messages: list[Message]) -> str:
    """Human-readable listing of the runs contained in a message directory."""
    runs: dict[str, list[Message]] = defaultdict(list)
    for message in messages:
        runs[message.identifier].append(message)
    lines = ["identifier          messages  duration  types"]
    for identifier, run in sorted(runs.items(), key=lambda kv: kv[1][-1].timestamp_ns):
        duration_ms = (run[-1].timestamp_ns - run[0].timestamp_ns) / 1e6
        types = Counter(m.message_type for m in run)
        summary = ", ".join(f"{count}x{name}" for name, count in types.most_common())
        lines.append(
            f"{identifier:<18}  {len(run):>8}  {duration_ms:>7.1f}ms  {summary}"
        )
    return "\n".join(lines)


def analyze_messages(messages: list[Message]) -> None:
    """Fill in the derived fields of every message (in place).

    Besides the relative timestamp this detects *repeats*: a message whose
    content is byte-identical to the previous message of the same type from the
    same block. Repeats mean a block re-broadcasts a summary it has already
    broadcast, which is what a non-terminating fixpoint looks like from outside.
    """
    if not messages:
        return
    first_timestamp = messages[0].timestamp_ns
    previous: dict[tuple[str, str], Message] = {}
    for message in messages:
        message.relative_ms = (message.timestamp_ns - first_timestamp) / 1e6
        key = (message.sender, message.message_type)
        earlier = previous.get(key)
        if earlier is not None:
            if earlier.content == message.content:
                message.repeats = earlier.label
            else:
                message.changes = [
                    (key, earlier.content.get(key), message.content.get(key))
                    for key in sorted(set(earlier.content) | set(message.content))
                    if earlier.content.get(key) != message.content.get(key)
                ]
        previous[key] = message


# ---------------------------------------------------------------------------
# Content decoding
# ---------------------------------------------------------------------------


def decode_block_state(value: str) -> str:
    """Decode the wire format of a ``BlockState`` (``<id> W:<witness> H:<history>``)."""
    block_and_rest = value.split(" W:", 1)
    if len(block_and_rest) != 2:
        return "<unparsable>"
    block_id, rest = block_and_rest
    witness_and_history = rest.split(" H:", 1)
    witness = witness_and_history[0]
    history = witness_and_history[1] if len(witness_and_history) == 2 else ""
    segments = [s for s in witness.split(";") if s]
    parts = [f"block={block_id}"]
    parts.append("history=[" + " -> ".join(h for h in history.split(",") if h) + "]")
    parts.append(f"witnessSegments={len(segments)}")
    return "  ".join(parts)


def abbreviate(value: str, max_length: int | None = None) -> str:
    """Shorten a value, noting how much was cut off."""
    limit = MAX_VALUE_LENGTH if max_length is None else max_length
    if len(value) <= limit:
        return value
    return f"{value[:limit]}... <{len(value)} chars total>"


def render_value(field_name: str, value: str, has_readable: bool) -> str:
    """Render one content value, eliding what cannot be read anyway."""
    if field_name in OPAQUE_FIELDS:
        return f"<opaque blob, {len(value)} chars, elided>"
    if has_readable and field_name == "state":
        return f"<smt2, {len(value)} chars; see 'readable'>"
    if not value:
        return "<empty>"
    return abbreviate(value)


def split_content(
    content: dict[str, str],
) -> tuple[dict[str, str], dict[int, dict[str, dict[str, str]]]]:
    """Split a flat content map into message-level meta data and per-state data.

    Returns ``(meta, states)`` where ``states`` maps a state index to a mapping
    from CPA component name to its fields.
    """
    meta: dict[str, str] = {}
    states: dict[int, dict[str, dict[str, str]]] = defaultdict(
        lambda: defaultdict(dict)
    )
    for key, value in content.items():
        parsed = ContentKey.parse(key)
        if parsed is None:
            meta[key] = value
        else:
            states[parsed.index][parsed.component][parsed.field_name] = value
    return meta, dict(states)


# ---------------------------------------------------------------------------
# HTML rendering
# ---------------------------------------------------------------------------


def esc(value: Any) -> str:
    """Escape a value for use in HTML text or in a quoted attribute."""
    return html.escape(str(value), quote=True)


def render_meta_table(meta: dict[str, str]) -> str:
    if not meta:
        return ""
    rows = "".join(
        f"<tr><th>{esc(key)}</th><td>{esc(abbreviate(value))}</td></tr>"
        for key, value in sorted(meta.items())
    )
    return f'<table class="kv-table">{rows}</table>'


def render_states(states: dict[int, dict[str, dict[str, str]]]) -> str:
    if not states:
        return ""
    blocks = []
    for index in sorted(states):
        rows = []
        for component, fields in states[index].items():
            has_readable = "readable" in fields
            for field_name, value in fields.items():
                rows.append(
                    f'<tr><td class="cpa">{esc(component)}</td>'
                    f'<td class="field">{esc(field_name)}</td>'
                    f'<td class="value">{esc(render_value(field_name, value, has_readable))}</td></tr>'
                )
            if component == "BlockState" and "state" in fields:
                rows.append(
                    '<tr class="decoded"><td class="cpa"></td>'
                    '<td class="field">&rarr; decoded</td>'
                    f'<td class="value">{esc(decode_block_state(fields["state"]))}</td></tr>'
                )
        blocks.append(
            f'<div class="state-block"><div class="state-title">state {index}</div>'
            f'<table class="state-table">{"".join(rows)}</table></div>'
        )
    return "".join(blocks)


def render_message_body(message: Message) -> str:
    """The expandable body of a message card: meta data plus decoded states."""
    meta, states = split_content(message.content)
    parts = [render_meta_table(meta), render_states(states), render_changes(message)]
    return "".join(part for part in parts if part)


def render_changes(message: Message) -> str:
    """Show what this message changed with respect to the block's previous one.

    A block that keeps sending messages is only making progress if something in
    them actually changes; this names the keys and shows the old and the new
    value side by side.
    """
    if not message.changes:
        return ""
    rows = []
    for key, old, new in message.changes:
        parsed = ContentKey.parse(key)
        field_name = parsed.field_name if parsed else key
        rows.append(
            f'<tr><td class="cpa">{esc(shorten_key(key))}</td>'
            f'<td class="value old">{esc(change_value(field_name, old))}</td>'
            f'<td class="value new">{esc(change_value(field_name, new))}</td></tr>'
        )
        if parsed is not None and parsed.component == "BlockState":
            rows.append(
                '<tr class="decoded"><td class="cpa">&rarr; decoded</td>'
                f'<td class="value old">{esc(decode_block_state(old or ""))}</td>'
                f'<td class="value new">{esc(decode_block_state(new or ""))}</td></tr>'
            )
    return (
        '<div class="changed-keys"><div class="state-title">changed since previous '
        f"{esc(message.message_type)} of {esc(message.sender)}</div>"
        f'<table class="state-table"><thead><tr><td class="cpa">key</td>'
        '<td class="value">before</td><td class="value">now</td></tr></thead>'
        f"<tbody>{''.join(rows)}</tbody></table></div>"
    )


def change_value(field_name: str, value: str | None) -> str:
    if value is None:
        return "<absent>"
    if field_name in OPAQUE_FIELDS:
        return f"<opaque blob, {len(value)} chars, elided>"
    if not value:
        return "<empty>"
    return abbreviate(value, MAX_VALUE_LENGTH // 2)


def shorten_key(key: str) -> str:
    """``state0.org.....BlockState.state`` -> ``state0.BlockState.state``."""
    parsed = ContentKey.parse(key)
    if parsed is None:
        return key
    return f"state{parsed.index}.{parsed.component}.{parsed.field_name}"


def participants(
    message: Message, blocks: dict[str, Block]
) -> tuple[list[str], list[str]]:
    """The blocks a message comes from and the blocks it is delivered to."""
    block = blocks.get(message.sender)
    if block is None:
        return [message.sender], ["all"]
    if message.message_type == "POST_CONDITION":
        return block.predecessors or ["entry"], block.successors or ["none"]
    if message.message_type == "VIOLATION_CONDITION":
        return block.successors or ["violation in block"], block.predecessors or [
            "none"
        ]
    return [message.sender], ["all"]


def render_message_card(
    message: Message, blocks: dict[str, Block], card_class: str
) -> str:
    """Render one message as a card usable in both views."""
    senders, receivers = participants(message, blocks)
    block = blocks.get(message.sender)
    tooltip = f"{message.sender}:\n{block.code}" if block else message.sender
    css_class = TYPE_TO_CSS_CLASS.get(message.message_type, "normal")
    arrow = TYPE_TO_ARROW.get(message.message_type, "-")

    badges = [
        f'<span class="type-badge {css_class}">{esc(message.message_type)}</span>'
    ]
    if message.repeats:
        badges.append(
            f'<span class="repeat-badge" title="identical content to {esc(message.repeats)}">'
            f"repeat of {esc(message.repeats)}</span>"
        )
    if message.state_count:
        plural = "" if message.state_count == 1 else "s"
        badges.append(
            f'<span class="count-badge">{message.state_count} state{plural}</span>'
        )

    body = render_message_body(message)
    details = (
        f'<details class="message-content"><summary>View content</summary>'
        f'<div class="content-body">{body}</div></details>'
        if body
        else ""
    )
    # The columns of the table view are far too narrow for a decoded state, so
    # every card can also open its content in a full-width viewer.
    expand = (
        '<button class="card-expand" type="button" '
        'title="Open content in full width (or double-click the header)">&#10530;</button>'
        if body
        else ""
    )

    return f"""
    <div class="{card_class} {css_class}" title="{esc(tooltip)}"
         data-sender="{esc(message.sender)}" data-type="{esc(message.message_type)}"
         data-repeat="{"yes" if message.repeats else "no"}">
        <div class="card-header">
            <span class="card-arrow">{arrow}</span>
            <span class="card-sender"><strong>{esc(message.sender)}</strong></span>
            <span class="card-id">{esc(message.label)}</span>
            {expand}
        </div>
        <div class="card-badges">{"".join(badges)}</div>
        <div class="card-flow">
            <span class="label">from</span> <span class="value">{esc(", ".join(senders))}</span>
            <span class="label">to</span> <span class="value">{esc(", ".join(receivers))}</span>
        </div>
        {details}
    </div>
    """


def column_order(messages: list[Message], blocks: dict[str, Block]) -> list[str]:
    """Column order of the table view: the blocks, then any other sender."""
    senders = {message.sender for message in messages}
    known = sorted(blocks, key=natural_key)
    extra = sorted(senders - set(blocks), key=natural_key)
    return known + extra


def bucket_of(message: Message, bucket_ms: float) -> float:
    if bucket_ms <= 0:
        return message.relative_ms
    return (message.relative_ms // bucket_ms) * bucket_ms


def generate_table_view(
    messages: list[Message], blocks: dict[str, Block], bucket_ms: float
) -> str:
    """One column per block, one row per point in time."""
    if not messages:
        return "<p>No messages to display.</p>"

    columns = column_order(messages, blocks)
    column_index = {block_id: i for i, block_id in enumerate(columns)}

    rows: dict[float, list[list[Message]]] = {}
    for message in messages:
        bucket = bucket_of(message, bucket_ms)
        row = rows.setdefault(bucket, [[] for _ in columns])
        row[column_index[message.sender]].append(message)

    parts = ['<table class="worker-table" id="messageTable">']
    parts.append('<thead><tr class="header-row"><th class="time-column">Time [ms]</th>')
    for block_id in columns:
        block = blocks.get(block_id)
        title = f"{block_id}:\n{block.code}" if block else block_id
        parts.append(
            f'<th class="block-column" data-block="{esc(block_id)}" title="{esc(title)}">'
            f"{esc(block_id)}</th>"
        )
    parts.append("</tr></thead><tbody>")

    for bucket in sorted(rows):
        parts.append(f'<tr data-timestamp="{bucket:.3f}">')
        parts.append(f'<td class="time-cell">{bucket:.3f}</td>')
        for cell in rows[bucket]:
            if not cell:
                parts.append('<td class="empty-cell"></td>')
                continue
            cards = "".join(
                render_message_card(message, blocks, "message-card") for message in cell
            )
            types = " ".join(sorted({m.message_type for m in cell}))
            parts.append(
                f'<td class="message-cell" data-types="{esc(types)}">{cards}</td>'
            )
        parts.append("</tr>")
    parts.append("</tbody></table>")
    return "\n".join(parts)


def generate_timeline_view(
    messages: list[Message], blocks: dict[str, Block], bucket_ms: float
) -> str:
    """All messages in chronological order, independent of their sender."""
    if not messages:
        return "<p>No messages to display.</p>"

    groups: dict[float, list[Message]] = defaultdict(list)
    for message in messages:
        groups[bucket_of(message, bucket_ms)].append(message)

    parts = ['<div class="timeline-container">']
    for bucket in sorted(groups):
        parts.append(f'<div class="timeline-row" data-timestamp="{bucket:.3f}">')
        parts.append(f'<div class="timeline-time">{bucket:.3f} ms</div>')
        parts.append('<div class="timeline-messages">')
        for message in groups[bucket]:
            parts.append(render_message_card(message, blocks, "timeline-message-card"))
        parts.append("</div></div>")
    parts.append("</div>")
    return "\n".join(parts)


# ---------------------------------------------------------------------------
# Graph view
# ---------------------------------------------------------------------------

#: Light-to-strong tints used to shade a block by how much traffic it produced.
ACTIVITY_TINTS = ("#ffffff", "#eaf3fb", "#d4e8f7", "#bcdcf2", "#9fcbea", "#7fb8e0")


def activity_color(count: int, busiest: int) -> str:
    """Tint for a block that sent ``count`` of at most ``busiest`` messages."""
    if count == 0 or busiest == 0:
        return ACTIVITY_TINTS[0]
    step = (count - 1) * (len(ACTIVITY_TINTS) - 2) // max(busiest, 1)
    return ACTIVITY_TINTS[min(step + 1, len(ACTIVITY_TINTS) - 1)]


def build_graph_dot(blocks: dict[str, Block], messages: list[Message]) -> str:
    """DOT source for the interactive graph.

    Deliberately compact: only the block id and its message counts, because the
    code and the messages themselves are shown in the detail panel next to it.
    """
    per_block: dict[str, Counter] = defaultdict(Counter)
    for message in messages:
        per_block[message.sender][message.message_type] += 1
    busiest = max((sum(c.values()) for c in per_block.values()), default=0)

    lines = [
        "digraph BlockGraph {",
        '  rankdir=TB; bgcolor="transparent";',
        (
            '  node [shape=box, style="filled,rounded", fontname="Helvetica",'
            ' fontsize=11, color="#7f8c8d", penwidth=1];'
        ),
        '  edge [color="#95a5a6", arrowsize=0.7];',
    ]
    for block_id in sorted(blocks, key=natural_key):
        counts = per_block.get(block_id, Counter())
        total = sum(counts.values())
        label = block_id
        if total:
            label += (
                f"\\nPC {counts.get('POST_CONDITION', 0)}"
                f"  VC {counts.get('VIOLATION_CONDITION', 0)}"
            )
        if counts.get("EXCEPTION"):
            label += f"\\n! {counts['EXCEPTION']} exception(s)"
        attributes = [
            f'label="{label}"',
            f'fillcolor="{activity_color(total, busiest)}"',
        ]
        if counts.get("EXCEPTION"):
            attributes.append('color="#c0392b"')
            attributes.append("penwidth=2")
        elif not blocks[block_id].predecessors:
            attributes.append('color="#2c3e50"')
            attributes.append("penwidth=2")
        lines.append(f'  "{block_id}" [{", ".join(attributes)}];')
    for block_id, block in blocks.items():
        for successor in block.successors:
            if successor in blocks:
                lines.append(f'  "{block_id}" -> "{successor}";')
    lines.append("}")
    return "\n".join(lines)


#: ``<g id="node1" class="node"><title>L18</title>`` produced by graphviz.
SVG_GROUP_PATTERN = re.compile(
    r'<g id="(?P<id>[^"]+)" class="(?P<kind>node|edge)">\s*<title>(?P<title>[^<]*)</title>'
)


def annotate_graph_svg(svg: str) -> str:
    """Make a graphviz SVG addressable from JavaScript.

    Graphviz only records the identity of a node or an edge in its ``<title>``,
    which is invisible to a selector. This lifts that information into
    ``data-block`` / ``data-from`` / ``data-to`` attributes and drops the fixed
    pixel size so that the drawing scales with its container.
    """

    def annotate(match: re.Match[str]) -> str:
        title = html.unescape(match.group("title"))
        if match.group("kind") == "node":
            extra = f'data-block="{esc(title)}"'
        else:
            source, _, target = title.partition("->")
            extra = f'data-from="{esc(source)}" data-to="{esc(target)}"'
        return (
            f'<g id="{match.group("id")}" class="{match.group("kind")}" {extra}>'
            f"<title>{match.group('title')}</title>"
        )

    svg = SVG_GROUP_PATTERN.sub(annotate, svg)
    # Strip the XML prologue and the absolute size; keep the viewBox for scaling.
    svg = svg[svg.index("<svg") :]
    svg = re.sub(r'\s(width|height)="[^"]*pt"', "", svg, count=2)
    return svg.replace("<svg", '<svg class="block-graph" id="blockGraph"', 1)


def render_graph_svg(blocks: dict[str, Block], messages: list[Message]) -> str:
    """Render the interactive block graph, or an explanation why it is missing."""
    try:
        (graph,) = pydot.graph_from_dot_data(build_graph_dot(blocks, messages))
        svg = graph.create_svg().decode(ENCODING)
    except Exception as e:  # noqa: BLE001 TODO more specific type
        print(f"WARNING: Failed to render the interactive graph: {e}", file=sys.stderr)
        return (
            '<p class="graph-missing">The block graph could not be rendered. '
            "Is Graphviz installed?</p>"
        )
    return annotate_graph_svg(svg)


def unknown_senders(messages: list[Message], blocks: dict[str, Block]) -> list[str]:
    """Senders that are not nodes of the block graph.

    Infrastructure actors such as the observer are expected here. A *block* in
    this list means that ``blocks.json`` does not belong to the visualized run:
    it is overwritten by every run, just like the message files are.
    """
    return sorted({m.sender for m in messages} - set(blocks), key=natural_key)


def generate_graph_view(blocks: dict[str, Block], messages: list[Message]) -> str:
    """Block graph on the left, details of the selected block on the right."""
    strangers = unknown_senders(messages, blocks)
    stranger_note = (
        f'<p class="graph-note">Not shown in the graph: '
        f"{esc(', '.join(strangers))} &mdash; these senders are not blocks in "
        f"blocks.json.</p>"
        if strangers
        else ""
    )
    block_data = {
        block_id: {
            "predecessors": block.predecessors,
            "successors": block.successors,
            "code": block.code,
            "entry": block.start_node,
            "exit": block.end_node,
            "violationConditionLocation": block.abstraction_location,
            "isRoot": not block.predecessors,
        }
        for block_id, block in blocks.items()
    }
    # The block code is arbitrary program text; escaping "<" keeps a stray
    # "</script>" inside it from ending the element early.
    block_json = json.dumps(block_data).replace("<", "\\u003c")
    return f"""
    <div class="graph-layout">
        <div class="graph-pane">
            <div class="graph-toolbar">
                <label class="toggle">Zoom
                    <input type="range" id="graphZoom" min="40" max="400" value="100" step="10">
                </label>
                <span class="graph-legend">
                    <span class="legend-swatch root"></span> root
                    <span class="legend-swatch busy"></span> traffic
                    <span class="legend-swatch failed"></span> exception
                    <span class="legend-note">PC = post conditions, VC = violation conditions</span>
                </span>
            </div>
            {stranger_note}
            <div class="graph-canvas" id="graphCanvas">{
        render_graph_svg(blocks, messages)
    }</div>
            <div class="graph-scrubber">
                <button class="btn btn-step" id="graphPlay" title="Replay the message flow">
                    &#9654;</button>
                <button class="btn btn-step" id="graphPrev" title="Previous message">
                    &#9664;</button>
                <button class="btn btn-step" id="graphNext" title="Next message">
                    &#9654;&#9654;</button>
                <input type="range" id="graphTime" min="0" max="0" value="0" step="1">
                <span class="graph-position" id="graphPosition">no message selected</span>
            </div>
        </div>
        <div class="graph-detail" id="graphDetail">
            <p class="graph-hint">Click a block to inspect its messages, or drag the
               slider to replay the message flow. Arrow keys step through messages.</p>
        </div>
    </div>
    <script id="blockGraphData" type="application/json">{block_json}</script>
    """


def generate_exception_panel(messages: list[Message]) -> str:
    """A prominent panel for the exceptions of a run; empty if there are none."""
    exceptions = [m for m in messages if m.message_type == "EXCEPTION"]
    if not exceptions:
        return ""
    items = []
    for message in exceptions:
        text = message.content.get("exception", "<no exception text>")
        first_line = text.splitlines()[0] if text else ""
        items.append(
            f'<details class="exception-item"><summary>{esc(message.sender)} '
            f"({esc(message.label)}, {message.relative_ms:.3f} ms): {esc(first_line)}</summary>"
            f"<pre>{esc(text)}</pre></details>"
        )
    return (
        f'<div class="exception-panel"><h2>&#9888; {len(exceptions)} exception(s)'
        f"</h2>{''.join(items)}</div>"
    )


def generate_stats_panel(messages: list[Message], blocks: dict[str, Block]) -> str:
    """Overall counters plus a per-block, per-type breakdown."""
    per_type = Counter(m.message_type for m in messages)
    repeats = sum(1 for m in messages if m.repeats)
    duration = messages[-1].relative_ms if messages else 0.0

    cards = [
        ("Messages", len(messages)),
        ("Visible", f'<span id="visibleMessages">{len(messages)}</span>'),
        ("Blocks", len(blocks)),
        ("Repeats", repeats),
        ("Duration", f"{duration:.1f} ms"),
    ]
    stat_cards = "".join(
        f'<div class="stat-card"><span class="stat-label">{esc(label)}</span>'
        f'<span class="stat-value">{value}</span></div>'
        for label, value in cards
    )

    per_block: dict[str, Counter] = defaultdict(Counter)
    for message in messages:
        per_block[message.sender][message.message_type] += 1

    header = "".join(f"<th>{esc(t.replace('_', ' '))}</th>" for t in MESSAGE_TYPES)
    body_rows = []
    for sender in sorted(per_block, key=natural_key):
        counts = per_block[sender]
        cells = "".join(f"<td>{counts.get(t, 0) or ''}</td>" for t in MESSAGE_TYPES)
        body_rows.append(f"<tr><th>{esc(sender)}</th>{cells}</tr>")
    total_cells = "".join(f"<td>{per_type.get(t, 0) or ''}</td>" for t in MESSAGE_TYPES)
    body_rows.append(f'<tr class="total-row"><th>total</th>{total_cells}</tr>')

    breakdown = (
        '<details class="breakdown"><summary>Messages per block</summary>'
        f'<table class="breakdown-table"><thead><tr><th>block</th>{header}</tr></thead>'
        f"<tbody>{''.join(body_rows)}</tbody></table></details>"
    )
    return f'<div class="stats-panel">{stat_cards}</div>{breakdown}'


def generate_controls() -> str:
    filter_buttons = " ".join(
        f'<button class="filter-btn active" data-filter-type="{t}">'
        f"{t.replace('_', ' ')}</button>"
        for t in MESSAGE_TYPES
    )
    return f"""
    <div class="controls">
        <div class="control-group">
            <div class="view-toggle">
                <button id="tableViewBtn" class="btn btn-view active">Table view</button>
                <button id="timelineViewBtn" class="btn btn-view">Timeline view</button>
                <button id="graphViewBtn" class="btn btn-view">Graph view</button>
            </div>
            <label class="toggle"><input type="checkbox" id="hideRepeats"> Hide repeats</label>
            <label class="toggle"><input type="checkbox" id="expandAll"> Expand all content</label>
            <button id="resetFiltersBtn" class="btn btn-secondary">Reset filters</button>
        </div>
        <div class="filter-panel">
            <div class="filter-section">
                <label>Filter by message type</label>
                <div class="filter-buttons">{filter_buttons}</div>
            </div>
            <div class="filter-section">
                <label>Filter by block id</label>
                <div class="filter-input-group">
                    <input type="text" id="blockFilter" placeholder="e.g. L20" />
                    <button id="clearBlockFilter" class="btn-clear">&times;</button>
                </div>
            </div>
            <div class="filter-section">
                <label>Search message content</label>
                <div class="filter-input-group">
                    <input type="text" id="contentSearch" placeholder="e.g. main::i" />
                    <button id="clearContentSearch" class="btn-clear">&times;</button>
                </div>
            </div>
        </div>
    </div>
    """


def generate_html_report(
    messages: list[Message],
    blocks: dict[str, Block],
    output_path: Path,
    bucket_ms: float,
    report_filename: str = "report.html",
) -> Path:
    """Write the complete self-contained HTML report."""
    table_html = generate_table_view(messages, blocks, bucket_ms)
    timeline_html = generate_timeline_view(messages, blocks, bucket_ms)
    graph_html = generate_graph_view(blocks, messages)
    run_identifier = messages[0].identifier if messages else "n/a"

    html_content = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>DSS Worker Messages</title>
    <style>{get_embedded_css()}</style>
</head>
<body>
    <div class="container">
        <header class="header">
            <h1>Distributed Summary Synthesis &ndash; Message Flow</h1>
            <p class="subtitle">run {esc(run_identifier)} &middot; {len(messages)} messages
               &middot; {len(blocks)} blocks</p>
        </header>
        {generate_exception_panel(messages)}
        {generate_controls()}
        {generate_stats_panel(messages, blocks)}
        <div class="view-container" id="tableViewContainer">
            <div class="table-container">{table_html}</div>
        </div>
        <div class="view-container hidden" id="timelineViewContainer">
            <div class="timeline-wrapper">{timeline_html}</div>
        </div>
        <div class="view-container hidden" id="graphViewContainer">{graph_html}</div>
    </div>

    <button class="floating-graph-btn" id="floatingGraphBtn" title="View block graph">&#9783;</button>

    <div class="modal" id="graphModal">
        <div class="modal-overlay" data-close="graphModal"></div>
        <div class="modal-content">
            <button class="modal-close" data-close="graphModal">&times;</button>
            <h2>Block structure</h2>
            <div class="modal-body">
                <img src="graph.png" alt="Block structure graph (graph.png not generated)"
                     id="graphImage" />
            </div>
        </div>
    </div>

    <div class="modal" id="detailModal">
        <div class="modal-overlay" data-close="detailModal"></div>
        <div class="modal-content wide">
            <button class="modal-close" data-close="detailModal">&times;</button>
            <h2 id="detailTitle">Message</h2>
            <div class="modal-body" id="detailBody"></div>
        </div>
    </div>

    <script>{get_embedded_javascript()}</script>
</body>
</html>
"""

    output_path.mkdir(parents=True, exist_ok=True)
    output_file = output_path / report_filename
    with open(output_file, "w", encoding=ENCODING) as f:
        f.write(html_content)
    print(f"HTML report generated: {output_file}")
    return output_file


# ---------------------------------------------------------------------------
# Block graph
# ---------------------------------------------------------------------------


def visualize_block_graph(
    blocks: dict[str, Block],
    output_path: Path,
    output_dot_name: str = "graph.dot",
    output_png_name: str = "graph.png",
) -> None:
    """Render the block graph to DOT and, if possible, to PNG."""
    if not blocks:
        print("WARNING: No block structure data found", file=sys.stderr)
        return

    graph = nx.DiGraph()
    for block_id, block in blocks.items():
        code = abbreviate(block.code.replace('"', "'"), 1000)
        location = ""
        if block.start_node is not None and block.end_node is not None:
            location = f" [{block.start_node} -> {block.end_node}]"
        label = f"{block_id}{location}:\n{code}" if code else f"{block_id}{location}"
        graph.add_node(
            block_id,
            shape="box",
            label=f'"{label}"',
            style='"bold"' if not block.predecessors else '""',
        )
    for block_id, block in blocks.items():
        for successor in block.successors:
            graph.add_edge(block_id, successor)

    output_path.mkdir(parents=True, exist_ok=True)
    graph_dot_path = output_path / output_dot_name
    nx.drawing.nx_pydot.write_dot(graph, str(graph_dot_path))

    try:
        (pydot_graph,) = pydot.graph_from_dot_file(str(graph_dot_path))
        pydot_graph.write_png(str(output_path / output_png_name))
        print(f"Block graph visualization saved to {output_path / output_png_name}")
    except Exception as e:  # noqa: BLE001 TODO more specific type
        print(f"WARNING: Failed to generate PNG from DOT file: {e}", file=sys.stderr)


# ---------------------------------------------------------------------------
# Embedded assets and command line
# ---------------------------------------------------------------------------


def get_embedded_css() -> str:
    return (Path(__file__).parent / "table.css").read_text(encoding=ENCODING)


def get_embedded_javascript() -> str:
    return (Path(__file__).parent / "table.js").read_text(encoding=ENCODING)


def create_arg_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description=(
            "Transform the messages of a distributed-summary-synthesis run "
            "into an interactive HTML report."
        ),
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s
  %(prog)s --messages-json output/block_analysis/messages --list-runs
  %(prog)s --run -1522568348 --bucket-ms 1 --no-browser
        """,
    )
    parser.add_argument(
        "--messages-json",
        help="Directory with the JSON files exported during the analysis.",
        default="output/block_analysis/messages",
    )
    parser.add_argument(
        "--block-structure-json",
        help="JSON file with the block structure used for the analysis.",
        default="output/block_analysis/blocks.json",
    )
    parser.add_argument(
        "-o",
        "--output",
        help="Output path for the generated files.",
        default="output/block_analysis",
    )
    parser.add_argument(
        "--run",
        help=(
            "Identifier of the run to visualize. Defaults to the most recent run "
            "found in the message directory."
        ),
        default=None,
    )
    parser.add_argument(
        "--list-runs",
        help="List the runs found in the message directory and exit.",
        action="store_true",
    )
    parser.add_argument(
        "--bucket-ms",
        help=(
            "Group messages that were sent within the same time bucket of this many "
            "milliseconds into one row. 0 (default) keeps the exact timestamps."
        ),
        type=float,
        default=0.0,
    )
    parser.add_argument(
        "--limit",
        help=(
            "Render only the last N messages of the run. Long runs produce very large "
            "reports; 0 (default) renders everything."
        ),
        type=int,
        default=0,
    )
    parser.add_argument(
        "--max-value-length",
        help=(
            f"Abbreviate content values longer than this many characters "
            f"(default {MAX_VALUE_LENGTH}). Raise it to see long formulas in full."
        ),
        type=int,
        default=MAX_VALUE_LENGTH,
    )
    parser.add_argument(
        "--no-browser",
        help="Do not open the generated report in a browser.",
        action="store_true",
    )
    return parser


def parse_args(argv: list[str]) -> argparse.Namespace:
    args = create_arg_parser().parse_args(argv)

    args.block_structure_json = Path(args.block_structure_json)
    if not args.block_structure_json.exists():
        raise ValueError(
            f"Block structure file does not exist: {args.block_structure_json}"
        )

    args.messages_json = Path(args.messages_json)
    if not args.messages_json.is_dir():
        raise ValueError(f"Messages directory does not exist: {args.messages_json}")

    args.output = Path(args.output)
    return args


def main(argv=None):
    global MAX_VALUE_LENGTH

    if argv is None:
        argv = sys.argv[1:]

    try:
        args = parse_args(argv)
    except ValueError as e:
        print(f"ERROR: {e}", file=sys.stderr)
        return 1

    MAX_VALUE_LENGTH = args.max_value_length

    all_messages = load_messages(args.messages_json)
    if not all_messages:
        print("ERROR: No messages found to visualize", file=sys.stderr)
        return 1

    if args.list_runs:
        print(describe_runs(all_messages))
        return 0

    messages = select_run(all_messages, args.run)
    if not messages:
        return 1
    # Repeats are detected on the complete run, then the rendering is truncated.
    analyze_messages(messages)
    if args.limit > 0 and len(messages) > args.limit:
        print(f"Rendering only the last {args.limit} of {len(messages)} messages.")
        messages = messages[-args.limit :]
    elif len(messages) > 1500:
        print(
            f"WARNING: rendering {len(messages)} messages produces a large report; "
            "use --limit to render only the most recent ones.",
            file=sys.stderr,
        )

    blocks = load_blocks(args.block_structure_json)
    if not blocks:
        print("ERROR: Failed to load block structure", file=sys.stderr)
        return 1

    strangers = unknown_senders(messages, blocks)
    if strangers:
        print(
            f"WARNING: {len(strangers)} sender(s) of this run are not blocks in "
            f"{args.block_structure_json}: {', '.join(strangers)}. Apart from "
            "infrastructure actors this means the block structure belongs to a "
            "different run; it is overwritten by every run, just like the messages.",
            file=sys.stderr,
        )

    print("Generating block structure visualization...")
    visualize_block_graph(blocks, args.output)

    print(f"Rendering {len(messages)} messages of run {messages[0].identifier}...")
    output_file = generate_html_report(messages, blocks, args.output, args.bucket_ms)

    print("\nVisualization complete.")
    if not args.no_browser:
        print(f"Opening {output_file} in browser...")
        webbrowser.open(output_file.resolve().as_uri())
    return 0


if __name__ == "__main__":
    sys.exit(main())
