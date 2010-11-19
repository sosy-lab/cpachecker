BASE_DIR="`dirname \"$0\"`/.."

cd "$BASE_DIR"

CONFIG="config/explicitAnalysis-nolimit.properties"
OUTPUTNM="output/explicit_buggy/explicit_BUGS_NoMonitor.log"
INPUTS="testsets/explicit_BUG.set"
TIMELIMIT=300 #in seconds

INSTANCES="`cat \"$INPUTS\"`"

exec scripts/run_tests_expl_BUG_NM.py -t $TIMELIMIT --config=$CONFIG --output=$OUTPUTNM "$@" $INSTANCES
