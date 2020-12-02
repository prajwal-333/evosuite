#!/bin/bash

projectname="Chart"

for i in {1..25}
#for (( i=$1; i<=$2; i++ ))
do
bugid="$i"
echo "collecting metrics"
echo "./metric_collection_org.sh ${projectname} ${bugid}f"
./metric_collection_org.sh ${projectname} "$bugid"f"

done