#!/bin/bash

# usage: $0 <project-name> <version>

export D4J_HOME="/home/sunny/prantiks-evosuite-and-d4j/defects4j"
export PATH="$PATH:$D4J_HOME/framework/bin"
export OSA="/home/sunny/OpenStaticAnalyzer-4.0.0-x64-Linux/Java/OpenStaticAnalyzerJava"
METRIC="MIMS"

# checkout project source
mkdir -p /tmp/sources/$1/$2
defects4j checkout -p $1 -v $2 -w /tmp/sources/$1/$2

# set project base dir
base_dir=""

if [ $1 == "Chart" ]; then
 base_dir="/tmp/sources/$1/$2/source"
elif [ $1 == "Closure"]; then 
 base_dir="/tmp/sources/$1/$2/src"
elif [ $1 == "Lang"]; then 
 base_dir="/tmp/sources/$1/$2/src"
elif [ $1 == "Math"]; then 
 base_dir="/tmp/sources/$1/$2/src"
elif [ $1 == "Mockito"]; then 
 base_dir="/tmp/sources/$1/$2/src"
elif [ $1 == "Time"]; then 
 base_dir="/tmp/sources/$1/$2/src"
fi

echo "INFO: project source = $base_dir"
# metric collection

echo "INFO: collecting metric for project $1 version $2"
echo "$OSA -projectName=$1 -projectBaseDir=$base_dir -resultsDir=/tmp/MetResults -currentDate=$2 -runMetricHunter=false -runDCF=false -runFB=false -runPMD=false"
$OSA -projectName=$1 -projectBaseDir=$base_dir -resultsDir=/tmp/MetResults -currentDate=$2 -runMetricHunter=false -runDCF=false -runFB=false -runPMD=false

# metric filtering
rm -f /tmp/Metrics.csv 
csv_file="/tmp/MetResults/$1/java/$2/$1-Method.csv"
python3 metric_filtering.py $csv_file $METRIC