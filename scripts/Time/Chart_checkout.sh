#!/bin/bash

projectname="Time"

for i in {1..27}
#for (( i=$1; i<=$2; i++ ))
do
bugid="$i"
# checkout project source
mkdir -p /tmp/sources/$projectname/"$bugid"f
defects4j checkout -p $projectname -v "$bugid"f -w /tmp/sources/$projectname/"$bugid"f

done