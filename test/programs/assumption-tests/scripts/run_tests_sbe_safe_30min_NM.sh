BASE_DIR="`dirname \"$0\"`/.."

cd "$BASE_DIR"

CONFIG="config/sbe_safe.properties"
OUTPUTWM="output/predabst/predabst_safe_noheuristics.log"
INPUTS="testsets/predabst_safe.set"

INSTANCES="`cat \"$INPUTS\"`"

exec scripts/run_tests_sbe.safe.py --config=$CONFIG --output=$OUTPUTWM "$@" $INSTANCES
