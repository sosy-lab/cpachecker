BASE_DIR="`dirname \"$0\"`/.."

cd "$BASE_DIR"

CONFIG="config/explicitAnalysis-limits.properties"
OUTPUTM="output/explicit_buggy/explicit_BUGS_timeLimit"
INPUTS="testsets/explicit_BUG.set"
TIMELIMIT=1200 #in seconds

INSTANCES="`cat \"$INPUTS\"`"

exec scripts/run_tests_expl_BUG_NM.py -t $TIMELIMIT --config=$CONFIG --output=$OUTPUTM "$@" $INSTANCES
