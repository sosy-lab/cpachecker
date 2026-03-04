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

This module transforms distributed block analysis worker logs into an interactive
HTML visualization with filtering capabilities and a clean, professional interface.
"""

import argparse
import json
import sys
import webbrowser
from pathlib import Path
from typing import Dict, List, Any, Optional
from dataclasses import dataclass
from enum import Enum

import networkx as nx
import pydot

ENCODING = "UTF-8"


class MessageType(Enum):
    """Enumeration of message types in the distributed block analysis."""

    POST_CONDITION = "POST_CONDITION"
    VIOLATION_CONDITION = "VIOLATION_CONDITION"
    FOUND_RESULT = "FOUND_RESULT"


@dataclass
class Message:
    """Data class representing a worker message."""

    timestamp: int
    sender_id: str
    message_type: MessageType
    content: Dict[str, Any]
    filename: str
    predecessors: List[str]
    successors: List[str]
    code: str

    @classmethod
    def from_json(
        cls, json_data: Dict[str, Any], block_logs: Dict[str, Any]
    ) -> "Message":
        """Create a Message instance from JSON data."""
        header = json_data["header"]
        sender_id = header["senderId"]
        block_info = block_logs.get(sender_id, {})

        return cls(
            timestamp=int(header["timestamp"]),
            sender_id=sender_id,
            message_type=MessageType(header["messageType"]),
            content=json_data.get("content", {}),
            filename=json_data.get("filename", ""),
            predecessors=block_info.get("predecessors", []),
            successors=block_info.get("successors", []),
            code="\n".join(x for x in block_info.get("code", []) if x),
        )


def create_arg_parser() -> argparse.ArgumentParser:
    """Create and configure the command-line argument parser."""
    parser = argparse.ArgumentParser(
        description="Transform Worker logs from distributed block analysis to interactive HTML visualization.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  %(prog)s
  %(prog)s --messages-json output/messages --block-structure-json output/blocks.json
  %(prog)s -o visualization --export-keys state predicates
        """,
    )
    parser.add_argument(
        "--messages-json",
        help="Path to directory containing JSON files sent during distributed block analysis.",
        default="output/block_analysis/messages",
    )
    parser.add_argument(
        "--block-structure-json",
        help="Path to JSON file that contains the block structure used for distributed block analysis.",
        default="output/block_analysis/blocks.json",
    )
    parser.add_argument(
        "-o",
        "--output",
        help="Output path for generated files.",
        default="output/block_analysis",
    )
    parser.add_argument(
        "--export-keys",
        help="Space separated list of keys to export from the messages. If not set, all keys are exported.",
        nargs="+",
        action="extend",
        dest="export_keys",
    )
    return parser


def parse_args(argv: List[str]) -> argparse.Namespace:
    """Parse and validate command-line arguments."""
    parser = create_arg_parser()
    args = parser.parse_args(argv)

    args.block_structure_json = Path(args.block_structure_json)
    if not args.block_structure_json.exists():
        raise ValueError(
            f"Block structure file does not exist: {args.block_structure_json}"
        )

    args.messages_json = Path(args.messages_json)
    if not args.messages_json.is_dir():
        raise ValueError(f"Messages directory does not exist: {args.messages_json}")

    args.output = Path(args.output)
    args.export_keys = args.export_keys or []
    return args


def load_json_file(json_file: Path) -> Dict[str, Any]:
    """Load and parse a JSON file with error handling."""
    try:
        with open(json_file, encoding=ENCODING) as file:
            return json.load(file)
    except json.JSONDecodeError as e:
        print(f"WARNING: JSON decoding error in {json_file}: {e}", file=sys.stderr)
        return {}
    except Exception as e:
        print(f"WARNING: Error reading {json_file}: {e}", file=sys.stderr)
        return {}


def filter_content_by_keys(
    content: Dict[str, Any], export_keys: List[str]
) -> Dict[str, Any]:
    """Filter message content based on export keys."""
    if not export_keys:
        return content

    filtered = {}
    for key, value in content.items():
        for export_key in export_keys:
            if export_key in key:
                filtered[key.replace(export_key, "")] = value
                break
    return filtered


def generate_message_html(
    message: Optional[Dict[str, Any]],
    block_logs: Dict[str, Any],
    export_keys: List[str],
) -> str:
    """Generate HTML representation of a single message."""
    if not message:
        return "<div></div>"

    header = message["header"]
    sender_id = header["senderId"]
    message_type = header["messageType"]
    msg_id = message["filename"]

    block_info = block_logs.get(sender_id, {})
    predecessors = block_info.get("predecessors", [])
    successors = block_info.get("successors", [])
    code = "\n".join(x for x in block_info.get("code", []) if x)
    content = message.get("content", {})

    # Determine message direction and participants
    arrow, senders, receivers = "-", ["all"], ["all"]
    if message_type == "POST_CONDITION":
        arrow, senders, receivers = "&darr;", predecessors or ["Self"], successors
    elif message_type == "VIOLATION_CONDITION":
        arrow, senders, receivers = "&uarr;", successors or ["Self"], predecessors
    elif message_type == "FOUND_RESULT":
        senders = [message.get("from", "Self")]

    # Filter content based on export keys
    filtered_content = filter_content_by_keys(content, export_keys)
    content_json = json.dumps(filtered_content, indent=2)

    # Build HTML
    html = f"""
    <div class="message-card" title="{sender_id}:\n{code}" data-sender="{sender_id}" data-type="{message_type}">
        <div class="message-header">
            <span class="message-arrow">{arrow}</span>
            <span class="message-info">
                From <strong>{", ".join(senders)}</strong>
                <span class="message-id">(ID: {msg_id[1:-5]})</span>
            </span>
        </div>
        <div class="message-direction">
            To <strong>{", ".join(receivers) if receivers else "None"}</strong>
            <span class="message-type-badge">{message_type.replace("_", " ")}</span>
        </div>
        <details class="message-content">
            <summary>View Content</summary>
            <pre class="content-json">{content_json}</pre>
        </details>
    </div>
    """
    return html


def generate_html_table(
    messages: List[Dict[str, Any]], block_logs: Dict[str, Any], export_keys: List[str]
) -> str:
    """Generate HTML table from messages."""
    if not messages:
        return "<p>No messages to display.</p>"

    # Calculate relative timestamps
    first_timestamp = int(messages[0]["header"]["timestamp"])

    # Organize messages by timestamp and sender
    sorted_block_ids = sorted(block_logs.keys())
    block_id_to_index = {block_id: i for i, block_id in enumerate(sorted_block_ids)}

    timestamp_to_messages = {}
    for message in messages:
        sender_id = message["header"]["senderId"]
        if sender_id not in block_id_to_index:
            continue

        relative_time = message["header"]["timestamp"] - first_timestamp
        if relative_time not in timestamp_to_messages:
            timestamp_to_messages[relative_time] = [None] * len(sorted_block_ids)

        timestamp_to_messages[relative_time][block_id_to_index[sender_id]] = message

    # Message type to CSS class mapping
    type_to_class = {
        "POST_CONDITION": "post-condition",
        "VIOLATION_CONDITION": "violation-condition",
        "FOUND_RESULT": "found-result",
    }

    # Build table HTML
    html_parts = ['<table class="worker-table" id="messageTable">']

    # Table header
    html_parts.append('<thead><tr class="header-row">')
    html_parts.append('<th class="time-column">Time</th>')
    for block_id in sorted_block_ids:
        html_parts.append(
            f'<th class="block-column" data-block="{block_id}">{block_id}</th>'
        )
    html_parts.append("</tr></thead>")

    # Table body
    html_parts.append("<tbody>")
    for timestamp in sorted(timestamp_to_messages.keys()):
        row_messages = timestamp_to_messages[timestamp]
        html_parts.append(f'<tr data-timestamp="{timestamp}">')
        html_parts.append(f'<td class="time-cell">{timestamp}</td>')

        for msg in row_messages:
            if msg is None:
                html_parts.append('<td class="empty-cell"></td>')
            else:
                msg_type = msg["header"]["messageType"]
                css_class = type_to_class.get(msg_type, "normal")
                cell_html = generate_message_html(msg, block_logs, export_keys)
                html_parts.append(
                    f'<td class="message-cell {css_class}" data-type="{msg_type}">{cell_html}</td>'
                )

        html_parts.append("</tr>")
    html_parts.append("</tbody>")
    html_parts.append("</table>")

    return "\n".join(html_parts)


def visualize_block_graph(
    block_structure_file: Path,
    output_path: Path,
    output_dot_name: str = "graph.dot",
    output_png_name: str = "graph.png",
) -> None:
    """Generate block structure graph visualization."""
    block_logs = load_json_file(block_structure_file)
    if not block_logs:
        print("WARNING: No block structure data found", file=sys.stderr)
        return

    graph = nx.DiGraph()

    # Add nodes with code labels
    for block_id, block_info in block_logs.items():
        code_parts = [c.replace('"', "'") for c in block_info.get("code", [])]
        code = "\n".join(c for c in code_parts if c)

        # Truncate long code snippets
        if len(code) > 1000:
            code = code[:1000] + "..."

        label = f"{block_id}:\n{code}" if code else block_id
        graph.add_node(block_id, shape="box", label=f'"{label}"')

    # Add edges
    for block_id, block_info in block_logs.items():
        for successor in block_info.get("successors", []):
            graph.add_edge(block_id, successor)

    # Create output directory
    output_path.mkdir(parents=True, exist_ok=True)

    # Write DOT and PNG files
    graph_dot_path = output_path / output_dot_name
    nx.drawing.nx_pydot.write_dot(graph, str(graph_dot_path))

    try:
        (pydot_graph,) = pydot.graph_from_dot_file(str(graph_dot_path))
        pydot_graph.write_png(str(output_path / output_png_name))
        print(f"Block graph visualization saved to {output_path / output_png_name}")
    except Exception as e:
        print(f"WARNING: Failed to generate PNG from DOT file: {e}", file=sys.stderr)


def generate_timeline_view(
    messages: List[Dict[str, Any]], block_logs: Dict[str, Any], export_keys: List[str]
) -> str:
    """Generate timeline view HTML where messages are shown chronologically."""
    if not messages:
        return "<p>No messages to display.</p>"

    # Group messages by timestamp
    first_timestamp = int(messages[0]["header"]["timestamp"])
    timestamp_groups = {}

    for message in messages:
        relative_time = message["header"]["timestamp"] - first_timestamp
        if relative_time not in timestamp_groups:
            timestamp_groups[relative_time] = []
        timestamp_groups[relative_time].append(message)

    # Message type to CSS class mapping
    type_to_class = {
        "POST_CONDITION": "post-condition",
        "VIOLATION_CONDITION": "violation-condition",
        "FOUND_RESULT": "found-result",
    }

    html_parts = ['<div class="timeline-container">']

    for timestamp in sorted(timestamp_groups.keys()):
        messages_at_time = timestamp_groups[timestamp]

        html_parts.append(f'<div class="timeline-row" data-timestamp="{timestamp}">')
        html_parts.append(f'<div class="timeline-time">{timestamp}</div>')
        html_parts.append('<div class="timeline-messages">')

        for msg in messages_at_time:
            header = msg["header"]
            sender_id = header["senderId"]
            message_type = header["messageType"]
            msg_id = msg["filename"]

            block_info = block_logs.get(sender_id, {})
            predecessors = block_info.get("predecessors", [])
            successors = block_info.get("successors", [])
            code = "\n".join(x for x in block_info.get("code", []) if x)
            content = msg.get("content", {})

            # Determine message direction and participants
            arrow, senders, receivers = "-", ["all"], ["all"]
            if message_type == "POST_CONDITION":
                arrow, senders, receivers = (
                    "&darr;",
                    predecessors or ["Self"],
                    successors,
                )
            elif message_type == "VIOLATION_CONDITION":
                arrow, senders, receivers = (
                    "&uarr;",
                    successors or ["Self"],
                    predecessors,
                )
            elif message_type == "FOUND_RESULT":
                senders = [msg.get("from", "Self")]

            # Filter content based on export keys
            filtered_content = filter_content_by_keys(content, export_keys)
            content_json = json.dumps(filtered_content, indent=2)

            css_class = type_to_class.get(message_type, "normal")

            # Build compact message card
            html_parts.append(f'''
            <div class="timeline-message-card {css_class}" data-sender="{sender_id}" data-type="{message_type}" title="{sender_id}:\n{code}">
                <div class="timeline-card-header">
                    <span class="timeline-arrow">{arrow}</span>
                    <span class="timeline-sender"><strong>{sender_id}</strong></span>
                    <span class="timeline-type-badge">{message_type.replace("_", " ")}</span>
                </div>
                <div class="timeline-card-body">
                    <div class="timeline-participants">
                        <div class="timeline-participant-group">
                            <span class="label">From:</span>
                            <span class="value">{", ".join(senders)}</span>
                        </div>
                        <div class="timeline-participant-group">
                            <span class="label">To:</span>
                            <span class="value">{", ".join(receivers) if receivers else "None"}</span>
                        </div>
                    </div>
                    <div class="timeline-msg-id">ID: {msg_id[1:-5]}</div>
                    <details class="timeline-content">
                        <summary>View Content</summary>
                        <pre class="timeline-content-json">{content_json}</pre>
                    </details>
                </div>
            </div>
            ''')

        html_parts.append("</div>")  # Close timeline-messages
        html_parts.append("</div>")  # Close timeline-row

    html_parts.append("</div>")  # Close timeline-container
    return "\n".join(html_parts)


def generate_html_report(
    messages: List[Dict[str, Any]],
    block_logs: Dict[str, Any],
    output_path: Path,
    export_keys: Optional[List[str]] = None,
    report_filename: str = "report.html",
) -> Path:
    """Generate the complete HTML report with embedded styles and scripts."""
    export_keys = export_keys or []

    # Sort messages by timestamp and sender
    for message in messages:
        message["header"]["timestamp"] = int(message["header"]["timestamp"])

    messages.sort(key=lambda m: (m["header"]["timestamp"], m["header"]["senderId"][1:]))

    # Generate table HTML
    table_html = generate_html_table(messages, block_logs, export_keys)

    # Generate timeline HTML
    timeline_html = generate_timeline_view(messages, block_logs, export_keys)

    # Get unique block IDs and message types for filters
    block_ids = sorted(block_logs.keys())
    message_types = ["POST_CONDITION", "VIOLATION_CONDITION", "FOUND_RESULT"]

    # Build complete HTML document
    html_content = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Worker Events - Block Analysis Visualization</title>
    <style>
        {get_embedded_css()}
    </style>
</head>
<body>
    <div class="container">
        <header class="header">
            <h1>Worker Events Visualization</h1>
            <p class="subtitle">Distributed Block Analysis Messages</p>
        </header>

        <div class="controls">
            <div class="control-group">
                <div class="view-toggle">
                    <button id="tableViewBtn" class="btn btn-view active">
                        <span>📊</span> Table View
                    </button>
                    <button id="timelineViewBtn" class="btn btn-view">
                        <span>📅</span> Timeline View
                    </button>
                </div>
                <button id="resetFiltersBtn" class="btn btn-secondary">
                    <span>🔄</span> Reset Filters
                </button>
            </div>

            <div class="filter-panel">
                <div class="filter-section">
                    <label>Filter by Message Type:</label>
                    <div class="filter-buttons">
                        {" ".join(f'<button class="filter-btn active" data-filter-type="{mt}">{mt.replace("_", " ")}</button>' for mt in message_types)}
                    </div>
                </div>

                <div class="filter-section">
                    <label>Filter by Block ID:</label>
                    <div class="filter-input-group">
                        <input type="text" id="blockFilter" placeholder="Enter block ID..." />
                        <button id="clearBlockFilter" class="btn-clear">✕</button>
                    </div>
                </div>

                <div class="filter-section">
                    <label>Search Message Content:</label>
                    <div class="filter-input-group">
                        <input type="text" id="contentSearch" placeholder="Search in message content..." />
                        <button id="clearContentSearch" class="btn-clear">✕</button>
                    </div>
                </div>
            </div>
        </div>

        <div class="stats-panel">
            <div class="stat-card">
                <span class="stat-label">Total Messages:</span>
                <span class="stat-value" id="totalMessages">{len(messages)}</span>
            </div>
            <div class="stat-card">
                <span class="stat-label">Visible Messages:</span>
                <span class="stat-value" id="visibleMessages">{len(messages)}</span>
            </div>
            <div class="stat-card">
                <span class="stat-label">Block Count:</span>
                <span class="stat-value">{len(block_ids)}</span>
            </div>
        </div>

        <div class="view-container" id="tableViewContainer">
            <div class="table-container">
                {table_html}
            </div>
        </div>

        <div class="view-container hidden" id="timelineViewContainer">
            <div class="timeline-wrapper">
                {timeline_html}
            </div>
        </div>
    </div>

    <!-- Fixed floating button for block graph -->
    <button class="floating-graph-btn" id="floatingGraphBtn" title="View Block Graph">
        📊
    </button>

    <div class="modal" id="graphModal">
        <div class="modal-overlay" id="modalOverlay"></div>
        <div class="modal-content">
            <button class="modal-close" id="modalClose">&times;</button>
            <h2>Block Structure Graph</h2>
            <div class="modal-body">
                <img src="graph.png" alt="Block Structure Graph" id="graphImage" />
            </div>
        </div>
    </div>

    <script>
        {get_embedded_javascript()}
    </script>
</body>
</html>
"""

    # Write output file
    output_path.mkdir(parents=True, exist_ok=True)
    output_file = output_path / report_filename
    with open(output_file, "w", encoding=ENCODING) as f:
        f.write(html_content)

    print(f"HTML report generated: {output_file}")
    return output_file


def load_and_process_messages(
    message_dir: Path,
    block_structure_json: Path,
    output_path: Path,
    export_keys: Optional[List[str]] = None,
) -> Optional[Path]:
    """Load messages from directory and generate visualization."""
    all_messages = []
    hash_code = None

    # Get sorted list of JSON message files
    try:
        json_files = sorted(
            [
                f.name
                for f in message_dir.iterdir()
                if f.is_file() and f.suffix == ".json"
            ],
            key=lambda name: name[1:-5] if name[0].isdigit() else name,
        )
    except Exception as e:
        print(f"ERROR: Failed to read message directory: {e}", file=sys.stderr)
        return None

    # Load block structure
    block_logs = load_json_file(block_structure_json)
    if not block_logs:
        print("ERROR: Failed to load block structure", file=sys.stderr)
        return None

    # Load all message files
    for json_filename in json_files:
        message_data = load_json_file(message_dir / json_filename)
        if not message_data:
            continue

        header = message_data.get("header", {})

        # Filter by identifier hash
        current_hash = header.get("identifier", "UNKNOWN")
        if hash_code is None:
            hash_code = current_hash

        if current_hash == hash_code:
            message_data["filename"] = json_filename
            all_messages.append(message_data)

        if "identifier" not in header:
            print(f"WARNING: Missing identifier in {json_filename}", file=sys.stderr)

    if not all_messages:
        print("WARNING: No messages found to visualize", file=sys.stderr)
        return None

    print(f"Loaded {len(all_messages)} messages from {len(json_files)} files")

    # Generate HTML report
    return generate_html_report(
        messages=all_messages,
        block_logs=block_logs,
        output_path=output_path,
        export_keys=export_keys,
    )


def get_embedded_css() -> str:
    """Return the embedded CSS for the HTML report."""
    return (Path(__file__).parent / "table.css").read_text(encoding=ENCODING)


def get_embedded_javascript() -> str:
    """Return the embedded JavaScript for the HTML report."""
    return (Path(__file__).parent / "table.js").read_text(encoding=ENCODING)


def main(argv=None):
    """Main entry point for the visualization tool."""
    if argv is None:
        argv = sys.argv[1:]

    try:
        args = parse_args(argv)
    except ValueError as e:
        print(f"ERROR: {e}", file=sys.stderr)
        return 1

    output_path: Path = args.output

    # Generate block graph visualization
    print("Generating block structure visualization...")
    visualize_block_graph(
        block_structure_file=args.block_structure_json, output_path=output_path
    )

    # Generate message visualization
    print("Processing messages...")
    output_file = load_and_process_messages(
        message_dir=args.messages_json,
        block_structure_json=args.block_structure_json,
        output_path=output_path,
        export_keys=args.export_keys,
    )

    if output_file:
        print("\n✓ Visualization complete!")
        print(f"Opening {output_file} in browser...")
        webbrowser.open(str(output_file))
        return 0
    else:
        print("ERROR: Failed to generate visualization", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
