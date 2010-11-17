BASE_DIR="`dirname \"$0\"`/.."

cd "$BASE_DIR"

CONFIG="config/explicitAnalysis_10_400MB_pathlength500.properties"
OUTPUTM="output/explicit/400MBMemLimit/explicitAnalysis_10_400MB_pathlength5005000.log"
INPUTS="testsets/explicit.set"

INSTANCES="`cat \"$INPUTS\"`"

exec scripts/run_tests_explicit.py --config=$CONFIG --output=$OUTPUTM "$@" $INSTANCES
