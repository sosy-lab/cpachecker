BASE_DIR="`dirname \"$0\"`/.."

cd "$BASE_DIR"

CONFIG="config/explicitAnalysis-limits.properties"
OUTPUTNM="output/explicit_BUGS_NoMonitor"
INPUTS="testsets/explicit_BUG.set"
TIMELIMIT=1200 #in seconds

INSTANCES="`cat \"$INPUTS\"`"

exec scripts/run_tests_expl_BUG_NM.py -t $TIMELIMIT --config=$CONFIG --output=$OUTPUTNM "$@" $INSTANCES
