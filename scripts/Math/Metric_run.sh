#!/bin/bash

projectname="Math"

for i in {1..106}
#for (( i=$1; i<=$2; i++ ))
do
bugid="$i"
echo "collecting metrics"
echo "./metric_collection_org.sh ${projectname} ${bugid}f"
../metric_collection_org.sh ${projectname} "$bugid"f"

done