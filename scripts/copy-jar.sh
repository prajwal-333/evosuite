defects4j_path="/home/sunny/prantiks-evosuite-and-d4j"
evosuite_path="/home/sunny/prantiks-evosuite-and-d4j"

cp $evosuite_path/evosuite/master/target/evosuite-master-1.0.7-SNAPSHOT.jar $defects4j_path/defects4j/framework/lib/test_generation/generation/evosuite-1.0.5.jar
cp $evosuite_path/evosuite/standalone_runtime/target/evosuite-standalone-runtime-1.0.7-SNAPSHOT.jar $defects4j_path/defects4j/framework/lib/test_generation/runtime/evosuite-standalone-runtime-1.0.5.jar

md5sum $evosuite_path/evosuite/master/target/evosuite-master-1.0.7-SNAPSHOT.jar 
md5sum $defects4j_path/defects4j/framework/lib/test_generation/generation/evosuite-1.0.5.jar
md5sum $evosuite_path/evosuite/standalone_runtime/target/evosuite-standalone-runtime-1.0.7-SNAPSHOT.jar 
md5sum $defects4j_path/defects4j/framework/lib/test_generation/runtime/evosuite-standalone-runtime-1.0.5.jar