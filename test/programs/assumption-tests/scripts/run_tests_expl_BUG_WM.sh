BASE_DIR="`dirname \"$0\"`/.."

cd "$BASE_DIR"

CONFIG="config/explicitAnalysis-limits.properties"
OUTPUTM="output/explicit_BUGS_WithMonitor"
INPUTS="testsets/explicit_BUG.set"
TIMELIMIT=1200 #in seconds

INSTANCES="`cat \"$INPUTS\"`"

exec scripts/run_tests_expl_BUG_WM.py -t $TIMELIMIT --config=$CONFIG --output=$OUTPUTM "$@" $INSTANCES
