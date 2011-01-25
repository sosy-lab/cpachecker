BASE_DIR="`dirname \"$0\"`/.."

cd "$BASE_DIR"

CONFIG="config/sbe_buggy_30min_NoMonitoring.properties"
OUTPUTWM="output/predabst/buggy/sbe_buggy_30min_NoMonitoring.log"
INPUTS="testsets/predasbt_sbe_buggy.set"
TIMELIMIT=1800 #seconds

INSTANCES="`cat \"$INPUTS\"`"

exec scripts/run_tests_sbe.safe.py -t $TIMELIMIT --config=$CONFIG --output=$OUTPUTWM "$@" $INSTANCES
