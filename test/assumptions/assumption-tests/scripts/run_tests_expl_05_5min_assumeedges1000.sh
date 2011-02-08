BASE_DIR="`dirname \"$0\"`/.."

cd "$BASE_DIR"

CONFIG="config/explicitAnalysis_05_5min_assumeedges1000.properties"
OUTPUTM="output/explicit/5minTimeLimit/explicitAnalysis_05_5min_assumeedges1000.log"
INPUTS="testsets/explicit.safe_5min.set"

INSTANCES="`cat \"$INPUTS\"`"

exec scripts/run_tests_explicit.py --config=$CONFIG --output=$OUTPUTM "$@" $INSTANCES
