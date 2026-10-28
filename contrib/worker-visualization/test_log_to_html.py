# This file is part of CPAchecker,
# a tool for configurable software verification:
# https://cpachecker.sosy-lab.org
#
# SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
#
# SPDX-License-Identifier: Apache-2.0

import unittest
from pathlib import Path

import log_to_html as visualization

EXAMPLE_MESSAGES = Path(__file__).parent / "example_input" / "messages"


class WorkerVisualizationTest(unittest.TestCase):
    def test_loads_typed_example_messages(self):
        messages = visualization.load_messages(EXAMPLE_MESSAGES)

        self.assertEqual(10, len(messages))
        self.assertEqual(
            {
                "POST_CONDITION",
                "VIOLATION_CONDITION",
                "RESULT",
                "CORRECTNESS_WITNESS",
            },
            {message.message_type for message in messages},
        )
        self.assertEqual("correctness witness", messages[-1].type_label)
        self.assertEqual(1, messages[0].state_count)

    def test_repeats_ignore_volatile_headers(self):
        first = visualization.Message.from_json(
            "M0.json",
            {
                "type": "result",
                "header": {"senderId": "worker", "timestamp": "1", "identifier": 1},
                "result": "TRUE",
            },
        )
        second = visualization.Message.from_json(
            "M1.json",
            {
                "type": "result",
                "header": {"senderId": "worker", "timestamp": "2", "identifier": 1},
                "result": "TRUE",
            },
        )

        visualization.analyze_messages([first, second])

        self.assertEqual("M0", second.repeats)
        self.assertEqual([], second.changes)

    def test_reports_nested_changes_and_timestamp_fallback(self):
        first = visualization.Message.from_json(
            "M0.json",
            {
                "type": "result",
                "header": {"senderId": "worker", "identifier": 1},
                "result": "TRUE",
            },
        )
        second = visualization.Message.from_json(
            "M1.json",
            {
                "type": "result",
                "header": {"senderId": "worker", "identifier": 1},
                "result": "FALSE",
            },
        )

        visualization.analyze_messages([first, second])

        self.assertIsNone(second.timestamp_ns)
        self.assertEqual(1.0, second.relative_ms)
        self.assertEqual([("result", "TRUE", "FALSE")], second.changes)

    def test_renders_structured_block_state(self):
        messages = visualization.load_messages(EXAMPLE_MESSAGES)
        visualization.analyze_messages(messages)

        rendered = visualization.render_message_body(messages[0])

        self.assertIn('class="component-section blockState" open', rendered)
        self.assertIn("blockId", rendered)
        self.assertIn('class="component-section predicateState"', rendered)
        self.assertNotIn("components.blockState", rendered)
        self.assertIn("witnessSegments=2", rendered)
        self.assertIn("opaque blob", rendered)


if __name__ == "__main__":
    unittest.main()
