BASE_DIR="`dirname \"$0\"`/.."

cd "$BASE_DIR"

CONFIG="config/explicitAnalysis_02_5min_TimeLimit.properties"
OUTPUTM="output/explicit/5minTimeLimit/explicitAnalysis_02_5min_TimeLimit.log"
INPUTS="testsets/explicit.safe_5min.set"

INSTANCES="`cat \"$INPUTS\"`"

exec scripts/run_tests_explicit.py --config=$CONFIG --output=$OUTPUTM "$@" $INSTANCES
