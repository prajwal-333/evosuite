#!/bin/bash
#i=1
#j=1
projectname="Lang"
# basefolder="/home/ubuntu/abhijitc/evo_chart_causalitycoverage2"
# d4j_home="/home/ubuntu/abhijitc"
basefolder="/data/joel/ulysis/exps/${projectname}_exps_5"
d4j_home="/data/joel/ulysis/"


mkdir -p "$basefolder"
echo "folder created: $basefolder"
for i in {1..65}
#for (( i=$1; i<=$2; i++ ))
do

	bugfolder="bug$i"
	bugid="$i"
	evo_temp="temp/$bugfolder"
	fts_temp="temp1/$bugfolder"
	fl_temp="temp2/$bugfolder"

#	mkdir -p /home/abhijitc/neural_project/results/evo_itr_dump/Closure/bug"$i"
#	mkdir -p /home/abhijitc/neural_project/results/evo_itr_dump/Closure/temp/bug"$i"
#	mkdir -p /home/abhijitc/neural_project/results/evo_itr_dump/Closure/temp2/bug"$i"
	mkdir -p "$basefolder/$bugfolder"
	echo "folder created: $basefolder/$bugfolder"
	mkdir -p "$basefolder/$evo_temp"
	mkdir -p "$basefolder/$fts_temp"
	mkdir -p "$basefolder/$fl_temp"
	#cp /home/ubuntu/abhijitc/repos/d4j-j8-fl/d4j-fl-j8-testing/extracted_data/Closure/"$bugid".json /tmp/suspiciousnes_scores1.json
	j=5
	#for j in {1..5}
	#do
		iteration=$j
		export FEATURE_DUMP_LOC="$basefolder/$bugfolder/$iteration"
#		mkdir -p /home/abhijitc/neural_project/results/evo_itr_dump/Closure/bug"$i"/"$j"
#		mkdir -p /home/abhijitc/neural_project/results/evo_itr_dump/Closure/temp/bug"$i"/"$j"
#		mkdir -p /home/abhijitc/neural_project/results/evo_itr_dump/Closure/temp2/bug"$i"/"$j"
		mkdir -p "$basefolder/$bugfolder/$iteration"
		mkdir -p "$basefolder/$evo_temp/$iteration"
		mkdir -p "$basefolder/$fts_temp/$iteration"
		mkdir -p "$basefolder/$fl_temp/$iteration"
		echo "folder created: $basefolder/$bugfolder/$iteration"
		echo "folder created: $basefolder/$evo_temp/$iteration"
		echo "folder created: $basefolder/$fts_temp/$iteration"
		echo "folder created: $basefolder/$fl_temp/$iteration"

		echo "collecting metrics"
		echo "./metric_collection.sh ${projectname} ${bugid}f $basefolder/$evo_temp/$iteration"
		../metric_collection.sh ${projectname} "$bugid"f "$basefolder/$evo_temp/$iteration"
		
		"$d4j_home"/defects4j/framework/bin/run_evosuite.pl -p ${projectname} -v "$i"f -n "$bugid" -o "$basefolder/$bugfolder/$iteration" -c VCMDDU2 -b 600 -t "$basefolder/$evo_temp/$iteration"

		mkdir -p "$basefolder/$bugfolder/$iteration"/${projectname}/evosuite-VCMDDU2/wfix_test_suite
		cp -a "$basefolder/$bugfolder/$iteration"/${projectname}/evosuite-VCMDDU2/"$bugid"/${projectname}-"$bugid"f-evosuite-VCMDDU2."$bugid".tar.bz2 "$basefolder/$bugfolder/$iteration"/${projectname}/evosuite-VCMDDU2/wfix_test_suite/

			"$d4j_home"/defects4j/framework/util/fix_test_suite.pl -p ${projectname} -d "$basefolder/$bugfolder/$iteration"/${projectname}/evosuite-VCMDDU2/"$bugid" -v "$bugid"f -t "$basefolder/$fts_temp/$iteration" -A


		mv "$basefolder/$bugfolder/$iteration"/${projectname}/evosuite-VCMDDU2/"$bugid"/${projectname}-"$bugid"f-evosuite-VCMDDU2."$bugid".tar.bz2 "$basefolder/$bugfolder/$iteration"/${projectname}/evosuite-VCMDDU2/"$bugid"/${projectname}-"$bugid"b-evosuite-VCMDDU2."$bugid".tar.bz2

		mkdir -p "$basefolder/$bugfolder/$iteration"/fl

			"$d4j_home"/defects4j/framework/bin/run_fault_localization.pl -p ${projectname} -d "$basefolder/$bugfolder/$iteration"/${projectname}/evosuite-VCMDDU2/"$bugid" -o "$basefolder/$bugfolder/$iteration"/fl -t "$basefolder/$fl_temp/$iteration" -i  "$d4j_home"/defects4j/framework/projects/${projectname}/modified_classes/"$bugid".src -y sfl -e ochiai -g line

		rm -rf "$basefolder/$evo_temp/$iteration"
		rm -rf "$basefolder/$fts_temp/$iteration"
		rm -rf "$basefolder/$fl_temp/$iteration"
		#mv /tmp/feature_dump_d1_ff4.csv "$basefolder/$bugfolder/$iteration"
		echo "folder removed: $basefolder/$evo_temp/$iteration"
		echo "folder removed: $basefolder/$fts_temp/$iteration"
		echo "folder removed: $basefolder/$fl_temp/$iteration"
	#done

	
done
