#!/bin/bash

sed -e 's%test/programs/[^/]*/%%g' -e 's/ *\t */\t/g' -e 's/[0-9.-]\+\([[:space:]]OUT OF MEMORY\)/MO\1/' -e 's/-1\([[:space:]]KILLED\)/> 1800\1/' -e 's/\([0-9][0-9]\)s/\1/' | tail -n +3
