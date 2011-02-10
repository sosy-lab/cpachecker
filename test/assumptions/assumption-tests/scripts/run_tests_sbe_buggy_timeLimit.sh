BASE_DIR="`dirname \"$0\"`/.."

cd "$BASE_DIR"

CONFIG="config/sbe_buggy_30min_timelimit.properties"
OUTPUTWM="output/predabst/buggy/sbe_buggy_30min_timelimit.log"
INPUTS="testsets/predasbt_sbe_buggy.set"

INSTANCES="`cat \"$INPUTS\"`"

exec scripts/run_tests_sbe_bug.py --config=$CONFIG --output=$OUTPUTWM "$@" $INSTANCES
