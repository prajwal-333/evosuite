#!/bin/bash

projectname="Math"

for i in {1..106}
#for (( i=$1; i<=$2; i++ ))
do
bugid="$i"

echo "checking out source"
mkdir -p /tmp/sources/$projectname/"$bugid"f
defects4j checkout -p $projectname -v "$bugid"f -w /tmp/sources/$projectname/"$bugid"f

echo "collecting metrics"
echo "./metric_collection_org.sh ${projectname} ${bugid}f"
../metric_collection_org.sh ${projectname} "$bugid"f

echo "removing source: /tmp/sources/$projectname/${bugid}f"
rm -rf /tmp/sources/$projectname/"$bugid"f

done