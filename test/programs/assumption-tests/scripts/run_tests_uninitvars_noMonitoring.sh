BASE_DIR="`dirname \"$0\"`/.."

cd "$BASE_DIR"

CONFIG="config/uninitvars-limits_01_noMonitoring.properties"
OUTPUTNM="output/uninitvars"
INPUTS="testsets/uninitvars.set"
TIMELIMIT=20 #in seconds

INSTANCES="`cat \"$INPUTS\"`"

exec scripts/run_tests_uninitvars.py -t $TIMELIMIT --config=$CONFIG --output=$OUTPUTNM "$@" $INSTANCES
