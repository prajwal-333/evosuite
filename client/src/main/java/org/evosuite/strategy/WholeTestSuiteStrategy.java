/**
 * Copyright (C) 2010-2018 Gordon Fraser, Andrea Arcuri and EvoSuite
 * contributors
 *
 * This file is part of EvoSuite.
 *
 * EvoSuite is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3.0 of the License, or
 * (at your option) any later version.
 *
 * EvoSuite is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with EvoSuite. If not, see <http://www.gnu.org/licenses/>.
 */
package org.evosuite.strategy;

import org.evosuite.*;
import org.evosuite.Properties.Criterion;
import org.evosuite.contracts.FailingTestSet;
import org.evosuite.coverage.CoverageCriteriaAnalyzer;
import org.evosuite.coverage.TestFitnessFactory;
import org.evosuite.coverage.dataflow.DefUseCoverageSuiteFitness;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.metaheuristics.GeneticAlgorithm;
import org.evosuite.ga.stoppingconditions.MaxStatementsStoppingCondition;
import org.evosuite.graphs.cfg.CFGMethodAdapter;
import org.evosuite.junit.JUnitAnalyzer;
import org.evosuite.junit.writer.TestSuiteWriter;
import org.evosuite.regression.RegressionSuiteMinimizer;
import org.evosuite.result.TestGenerationResult;
import org.evosuite.result.TestGenerationResultBuilder;
import org.evosuite.rmi.ClientServices;
import org.evosuite.rmi.service.ClientState;
import org.evosuite.runtime.sandbox.PermissionStatistics;
import org.evosuite.setup.TestCluster;
import org.evosuite.statistics.RuntimeVariable;
import org.evosuite.statistics.StatisticsSender;
import org.evosuite.symbolic.DSEStats;
import org.evosuite.testcase.ConstantInliner;
import org.evosuite.testcase.TestCase;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;
import org.evosuite.testcase.execution.ExecutionTracer;
import org.evosuite.testcase.execution.TestCaseExecutor;
import org.evosuite.testsuite.*;
import org.evosuite.testsuite.similarity.DiversityObserver;
import org.evosuite.utils.ArrayUtil;
import org.evosuite.utils.LoggingUtils;
import org.evosuite.utils.Randomness;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;



/**
 * Regular whole test suite generation
 * 
 * @author gordon
 *
 */
public class WholeTestSuiteStrategy extends TestGenerationStrategy {


	//mycode_starts
	private static int checkAllTestsIfTime(List<TestCase> testCases, long delta) {
		if (TimeController.getInstance().hasTimeToExecuteATestCase()
				&& TimeController.getInstance().isThereStillTimeInThisPhase(delta)) {
			return JUnitAnalyzer.handleTestsThatAreUnstable(testCases);
		}
		return 0;
	}


	private void compileAndCheckTests(TestSuiteChromosome chromosome) {
		LoggingUtils.getEvoLogger().info("* " + ClientProcess.getPrettyPrintIdentifier() + "Compiling and checking tests");

		if (!JUnitAnalyzer.isJavaCompilerAvailable()) {
			String msg = "No Java compiler is available. Make sure to run EvoSuite with the JDK and not the JRE."
					+ "You can try to setup the JAVA_HOME system variable to point to it, as well as to make sure that the PATH "
					+ "variable points to the JDK before any JRE.";
			//logger.error(msg);
			throw new RuntimeException(msg);
		}

		ClientServices.getInstance().getClientNode().mychangeState(ClientState.JUNIT_CHECK, 0); //mycode

		// Store this value; if this option is true then the JUnit check
		// would not succeed, as the JUnit classloader wouldn't find the class
		boolean junitSeparateClassLoader = Properties.USE_SEPARATE_CLASSLOADER;
		Properties.USE_SEPARATE_CLASSLOADER = false;

		int numUnstable = 0;

		// note: compiling and running JUnit tests can be very time consuming
		if (!TimeController.getInstance().isThereStillTimeInThisPhase()) {
			Properties.USE_SEPARATE_CLASSLOADER = junitSeparateClassLoader;
			return;
		}

		List<TestCase> testCases = chromosome.getTests(); // make copy of
		// current tests

		// first, let's just get rid of all the tests that do not compile
		JUnitAnalyzer.removeTestsThatDoNotCompile(testCases);

		// compile and run each test one at a time. and keep track of total time
		long start = java.lang.System.currentTimeMillis();
		Iterator<TestCase> iter = testCases.iterator();
		while (iter.hasNext()) {
			if (!TimeController.getInstance().hasTimeToExecuteATestCase()) {
				break;
			}
			TestCase tc = iter.next();
			List<TestCase> list = new ArrayList<>();
			list.add(tc);
			numUnstable += JUnitAnalyzer.handleTestsThatAreUnstable(list);
			if (list.isEmpty()) {
				// if the test was unstable and deleted, need to remove it from
				// final testSuite
				iter.remove();
			}
		}
		/*
		 * compiling and running each single test individually will take more
		 * than compiling/running everything in on single suite. so it can be
		 * used as an upper bound
		 */
		long delta = java.lang.System.currentTimeMillis() - start;

		numUnstable += checkAllTestsIfTime(testCases, delta);

		// second passage on reverse order, this is to spot dependencies among
		// tests
		if (testCases.size() > 1) {
			Collections.reverse(testCases);
			numUnstable += checkAllTestsIfTime(testCases, delta);
		}

		chromosome.clearTests(); // remove all tests
		for (TestCase testCase : testCases) {
			chromosome.addTest(testCase); // add back the filtered tests
		}

		boolean unstable = (numUnstable > 0);

		if (!TimeController.getInstance().isThereStillTimeInThisPhase()) {
			//logger.warn("JUnit checking timed out");
		}

		//ClientServices.track(RuntimeVariable.HadUnstableTests, unstable);
		//ClientServices.track(RuntimeVariable.NumUnstableTests, numUnstable);
		Properties.USE_SEPARATE_CLASSLOADER = junitSeparateClassLoader;

	}

	protected void postProcessTests(TestSuiteChromosome testSuite) {

		// If overall time is short, the search might not have had enough time
		// to come up with a suite without timeouts. However, they will slow
		// down
		// the rest of the process, and may lead to invalid tests
		testSuite.getTestChromosomes()
				.removeIf(t -> t.getLastExecutionResult() != null && (t.getLastExecutionResult().hasTimeout() ||
						t.getLastExecutionResult().hasTestException()));

		if (Properties.CTG_SEEDS_FILE_OUT != null) {
			TestSuiteSerialization.saveTests(testSuite, new File(Properties.CTG_SEEDS_FILE_OUT));
		} else if (Properties.TEST_FACTORY == Properties.TestFactory.SERIALIZATION) {
			TestSuiteSerialization.saveTests(testSuite,
					new File(Properties.SEED_DIR + File.separator + Properties.TARGET_CLASS));
		}

		/*
		 * Remove covered goals that are not part of the minimization targets,
		 * as they might screw up coverage analysis when a minimization timeout
		 * occurs. This may happen e.g. when MutationSuiteFitness calls
		 * BranchCoverageSuiteFitness which adds branch goals.
		 */
		// TODO: This creates an inconsistency between
		// suite.getCoveredGoals().size() and suite.getNumCoveredGoals()
		// but it is not clear how to update numcoveredgoals
//		List<TestFitnessFunction> goals = new ArrayList<>();
//		for (TestFitnessFactory<?> ff : getFitnessFactories()) {
//			goals.addAll(ff.getCoverageGoals());
//		}
//		for (TestFitnessFunction f : testSuite.getCoveredGoals()) {
//			if (!goals.contains(f)) {
//				testSuite.removeCoveredGoal(f);
//			}
//		}

		if (Properties.INLINE) {
			ClientServices.getInstance().getClientNode().mychangeState(ClientState.INLINING, 0);
			ConstantInliner inliner = new ConstantInliner();
			// progressMonitor.setCurrentPhase("Inlining constants");

			// Map<FitnessFunction<? extends TestSuite<?>>, Double> fitnesses =
			// testSuite.getFitnesses();

			inliner.inline(testSuite);
		}

//		if (Properties.MINIMIZE) {
//			ClientServices.getInstance().getClientNode().mychangeState(ClientState.MINIMIZATION , 0);
//			// progressMonitor.setCurrentPhase("Minimizing test cases");
//			if (!TimeController.getInstance().hasTimeToExecuteATestCase()) {
//				LoggingUtils.getEvoLogger().info("* " + ClientProcess.getPrettyPrintIdentifier()
//						+ "Skipping minimization because not enough time is left");
//				//ClientServices.track(RuntimeVariable.Result_Size, testSuite.size());
//				//ClientServices.track(RuntimeVariable.Minimized_Size, testSuite.size());
//				//ClientServices.track(RuntimeVariable.Result_Length, testSuite.totalLengthOfTestCases());
//				//ClientServices.track(RuntimeVariable.Minimized_Length, testSuite.totalLengthOfTestCases());
//			} else if (Properties.isRegression()) {
//				RegressionSuiteMinimizer minimizer = new RegressionSuiteMinimizer();
//				minimizer.minimize(testSuite);
//			} else {
//
//				//double before = testSuite.getFitness();
//
//				TestSuiteMinimizer minimizer = new TestSuiteMinimizer(getFitnessFactories());
//
//				//LoggingUtils.getEvoLogger().info("* " + ClientProcess.getPrettyPrintIdentifier() + "Minimizing test suite");
//				minimizer.minimize(testSuite, true);
//
//				//double after = testSuite.getFitness();
//				//if (after > before + 0.01d) { // assume minimization
//				//	throw new Error("EvoSuite bug: minimization lead fitness from " + before + " to " + after);
//				//}
//			}
//		} else {
//			if (!TimeController.getInstance().hasTimeToExecuteATestCase()) {
//				LoggingUtils.getEvoLogger().info("* " + ClientProcess.getPrettyPrintIdentifier()
//						+ "Skipping minimization because not enough time is left");
//			}
//
//			//ClientServices.track(RuntimeVariable.Result_Size, testSuite.size());
//			//ClientServices.track(RuntimeVariable.Minimized_Size, testSuite.size());
//			//ClientServices.track(RuntimeVariable.Result_Length, testSuite.totalLengthOfTestCases());
//			//ClientServices.track(RuntimeVariable.Minimized_Length, testSuite.totalLengthOfTestCases());
//		}

		//if (Properties.COVERAGE) {
			//skipping coverage analysis
			//ClientServices.getInstance().getClientNode().changeState(ClientState.COVERAGE_ANALYSIS);
			//CoverageCriteriaAnalyzer.analyzeCoverage(testSuite);
//		}

		//double coverage = testSuite.getCoverage();

		//if (ArrayUtil.contains(Properties.CRITERION, Criterion.MUTATION)
		//		|| ArrayUtil.contains(Properties.CRITERION, Criterion.STRONGMUTATION)) {
		//	 SearchStatistics.getInstance().mutationScore(coverage);
		//}

		//StatisticsSender.executedAndThenSendIndividualToMaster(testSuite);
//		LoggingUtils.getEvoLogger().info("* " + ClientProcess.getPrettyPrintIdentifier() + "Generated " + testSuite.size()
//				+ " tests with total length " + testSuite.totalLengthOfTestCases());

		// TODO: In the end we will only need one analysis technique
		//if (!Properties.ANALYSIS_CRITERIA.isEmpty()) {
		//	 SearchStatistics.getInstance().addCoverage(Properties.CRITERION.toString(),
		//	 coverage);
		//	CoverageCriteriaAnalyzer.analyzeCriteria(testSuite, Properties.ANALYSIS_CRITERIA);
			// FIXME: can we send all bestSuites?
		//}
//		if (Properties.CRITERION.length > 1)
//			LoggingUtils.getEvoLogger().info("* " + ClientProcess.getPrettyPrintIdentifier() + "Resulting test suite's coverage: "
//					+ NumberFormat.getPercentInstance().format(coverage)
//					+ " (average coverage for all fitness functions)");
//		else
//			LoggingUtils.getEvoLogger().info("* " + ClientProcess.getPrettyPrintIdentifier() + "Resulting test suite's coverage: "
//					+ NumberFormat.getPercentInstance().format(coverage));

		// printBudget(ga); // TODO - need to move this somewhere else
//		if (ArrayUtil.contains(Properties.CRITERION, Criterion.DEFUSE) && Properties.ANALYSIS_CRITERIA.isEmpty())
//			DefUseCoverageSuiteFitness.printCoverage();

		//DSEStats.getInstance().trackConstraintTypes();

		//DSEStats.getInstance().trackSolverStatistics();

//		if (Properties.DSE_PROBABILITY > 0.0 && Properties.LOCAL_SEARCH_RATE > 0
//				&& Properties.LOCAL_SEARCH_PROBABILITY > 0.0) {
//			DSEStats.getInstance().logStatistics();
//		}

		if (Properties.FILTER_SANDBOX_TESTS) {
			for (TestChromosome test : testSuite.getTestChromosomes()) {
				// delete all statements leading to security exceptions
				ExecutionResult result = test.getLastExecutionResult();
				if (result == null) {
					result = TestCaseExecutor.runTest(test.getTestCase());
				}
				if (result.hasSecurityException()) {
					int position = result.getFirstPositionOfThrownException();
					if (position > 0) {
						test.getTestCase().chop(position);
						result = TestCaseExecutor.runTest(test.getTestCase());
						test.setLastExecutionResult(result);
					}
				}
			}
		}
//
		if (Properties.ASSERTIONS && !Properties.isRegression()) {
			LoggingUtils.getEvoLogger().info("* " + ClientProcess.getPrettyPrintIdentifier() + "Generating assertions");
			// progressMonitor.setCurrentPhase("Generating assertions");
			ClientServices.getInstance().getClientNode().mychangeState(ClientState.ASSERTION_GENERATION, 0);
			if (!TimeController.getInstance().hasTimeToExecuteATestCase()) {
				LoggingUtils.getEvoLogger().info("* " + ClientProcess.getPrettyPrintIdentifier()
						+ "Skipping assertion generation because not enough time is left");
			} else {
				TestSuiteGeneratorHelper.addAssertions(testSuite); //take care of this
			}
			//return;
			//StatisticsSender.sendIndividualToMaster(testSuite); // FIXME: can we
			// pass the list
			// of
			// testsuitechromosomes?
		}

		if(Properties.NO_RUNTIME_DEPENDENCY) {
			LoggingUtils.getEvoLogger().info("* " + ClientProcess.getPrettyPrintIdentifier()
					+ "Property NO_RUNTIME_DEPENDENCY is set to true - skipping JUnit compile check");
			LoggingUtils.getEvoLogger().info("* " +ClientProcess.getPrettyPrintIdentifier()
					+ "WARNING: Not including the runtime dependencies is likely to lead to flaky tests!");
		}
		else if (Properties.JUNIT_TESTS && Properties.JUNIT_CHECK) {
			compileAndCheckTests(testSuite);
		}

//		if (Properties.SERIALIZE_REGRESSION_TEST_SUITE) {
//			RegressionSuiteSerializer.appendToRegressionTestSuite(testSuite);
//		}
//
//		if(Properties.isRegression() && Properties.KEEP_REGRESSION_ARCHIVE){
//			RegressionSuiteSerializer.storeRegressionArchive();
//		}
	}



	public static TestGenerationResult writeJUnitTestsAndCreateResult(TestSuiteChromosome testSuite, String suffix) {
		List<TestCase> tests = testSuite.getTests();
		if (Properties.JUNIT_TESTS) {
			ClientServices.getInstance().getClientNode().mychangeState(ClientState.WRITING_TESTS, 0);

			TestSuiteWriter suiteWriter = new TestSuiteWriter();
			suiteWriter.insertTests(tests);

			String name = Properties.TARGET_CLASS.substring(Properties.TARGET_CLASS.lastIndexOf(".") + 1);
			String testDir = Properties.TEST_DIR;

			LoggingUtils.getEvoLogger().info("* " + ClientProcess.getPrettyPrintIdentifier() + "Writing JUnit test case '"
					+ (name + suffix) + "' to " + testDir);
			suiteWriter.writeTestSuite(name + suffix, testDir, testSuite.getLastExecutionResults());
		}
		return TestGenerationResultBuilder.buildSuccessResult();
	}


	public void writeJUnitFailingTests() {
		if (!Properties.CHECK_CONTRACTS)
			return;

		//FailingTestSet.sendStatistics();

		if (Properties.JUNIT_TESTS) {

			TestSuiteWriter suiteWriter = new TestSuiteWriter();
			//suiteWriter.insertTests(FailingTestSet.getFailingTests());

			TestSuiteChromosome suite = new TestSuiteChromosome();
			for(TestCase test : FailingTestSet.getFailingTests()) {
				test.setFailing();
				suite.addTest(test);
			}

			String name = Properties.TARGET_CLASS.substring(Properties.TARGET_CLASS.lastIndexOf(".") + 1);
			String testDir = Properties.TEST_DIR;
			LoggingUtils.getEvoLogger().info("* " + ClientProcess.getPrettyPrintIdentifier() + "Writing failing test cases '"
					+ (name + Properties.JUNIT_SUFFIX) + "' to " + testDir);
			suiteWriter.insertAllTests(suite.getTests());
			FailingTestSet.writeJUnitTestSuite(suiteWriter);

			suiteWriter.writeTestSuite(name + Properties.JUNIT_FAILED_SUFFIX, testDir, suite.getLastExecutionResults());
		}
	}


	//mycode_ends


	@Override
	public TestSuiteChromosome generateTests() {
		// Set up search algorithm
		LoggingUtils.getEvoLogger().info("* Setting up search algorithm for whole suite generation");
		PropertiesSuiteGAFactory algorithmFactory = new PropertiesSuiteGAFactory();
		GeneticAlgorithm<TestSuiteChromosome> algorithm = algorithmFactory.getSearchAlgorithm();
		
		if(Properties.SERIALIZE_GA || Properties.CLIENT_ON_THREAD)
			TestGenerationResultBuilder.getInstance().setGeneticAlgorithm(algorithm);

		long startTime = System.currentTimeMillis() / 1000;

		// What's the search target
		List<TestSuiteFitnessFunction> fitnessFunctions = getFitnessFunctions();

		// TODO: Argh, generics.
		algorithm.addFitnessFunctions((List)fitnessFunctions);
//		for(TestSuiteFitnessFunction f : fitnessFunctions) 
//			algorithm.addFitnessFunction(f);

		// if (Properties.SHOW_PROGRESS && !logger.isInfoEnabled())
		algorithm.addListener(progressMonitor); // FIXME progressMonitor may cause
		// client hang if EvoSuite is
		// executed with -prefix!

		if(Properties.TRACK_DIVERSITY)
			algorithm.addListener(new DiversityObserver());

		if (ArrayUtil.contains(Properties.CRITERION, Criterion.DEFUSE)
				|| ArrayUtil.contains(Properties.CRITERION, Criterion.ALLDEFS)
				|| ArrayUtil.contains(Properties.CRITERION, Criterion.STATEMENT)
				|| ArrayUtil.contains(Properties.CRITERION, Criterion.RHO)
				|| ArrayUtil.contains(Properties.CRITERION, Criterion.AMBIGUITY))
			ExecutionTracer.enableTraceCalls();

		// TODO: why it was only if "analyzing"???
		// if (analyzing)
		algorithm.resetStoppingConditions();

		List<TestFitnessFunction> goals = getGoals(true);
		if(!canGenerateTestsForSUT()) {
			LoggingUtils.getEvoLogger().info("* Found no testable methods in the target class "
					+ Properties.TARGET_CLASS);
			ClientServices.getInstance().getClientNode().trackOutputVariable(RuntimeVariable.Total_Goals, goals.size());

			return new TestSuiteChromosome();
		}

		/*
		 * Proceed with search if CRITERION=EXCEPTION, even if goals is empty
		 */
		TestSuiteChromosome testSuite = null;
		if (!(Properties.STOP_ZERO && goals.isEmpty()) || ArrayUtil.contains(Properties.CRITERION, Criterion.EXCEPTION)) {
			// Perform search
			LoggingUtils.getEvoLogger().info("* Using seed {}", Randomness.getSeed() );
			LoggingUtils.getEvoLogger().info("* Starting evolution");
			ClientServices.getInstance().getClientNode().changeState(ClientState.SEARCH);
			//LoggingUtils.getEvoLogger().info("The default search algo is:", algorithm.getClass() ); //mycode
			//mycode_starts

			while(true)
			{
				algorithm.generateSolution();
				TestSuiteChromosome testCases = (TestSuiteChromosome)algorithm.getBestIndividual();
				TestSuiteChromosome testCases_clone =  testCases.clone();	//clone it
				if(Properties.isCompleted == 1)
					break;

				Properties.TEST_ARCHIVE = false;
				TestGenerationResult result = null;
				if (ClientProcess.DEFAULT_CLIENT_NAME.equals(ClientProcess.getIdentifier())) {
					postProcessTests(testCases_clone);
					//ClientServices.getInstance().getClientNode().publishPermissionStatistics();
					//PermissionStatistics.getInstance().printStatistics(LoggingUtils.getEvoLogger());

					// progressMonitor.setCurrentPhase("Writing JUnit test cases");
					LoggingUtils.getEvoLogger().info("* " + ClientProcess.getPrettyPrintIdentifier() + "Writing tests to file");
					result = writeJUnitTestsAndCreateResult(testCases_clone, Properties.JUNIT_SUFFIX);;
					writeJUnitFailingTests();
					ClientServices.getInstance().getClientNode().mychangeState(ClientState.SEARCH , 0);

				}

			}

			//mycode_ends

			// TODO: Refactor MOO!
			// bestSuites = (List<TestSuiteChromosome>) ga.getBestIndividuals();
			testSuite = (TestSuiteChromosome) algorithm.getBestIndividual();
		} else {
			zeroFitness.setFinished();
			testSuite = new TestSuiteChromosome();
			for (FitnessFunction<?> ff : fitnessFunctions) {
				testSuite.setCoverage(ff, 1.0);
			}
		}

		long endTime = System.currentTimeMillis() / 1000;

		goals = getGoals(false); //recalculated now after the search, eg to handle exception fitness
        ClientServices.getInstance().getClientNode().trackOutputVariable(RuntimeVariable.Total_Goals, goals.size());
        
		// Newline after progress bar
		if (Properties.SHOW_PROGRESS)
			LoggingUtils.getEvoLogger().info("");
		
		if(!Properties.IS_RUNNING_A_SYSTEM_TEST) { //avoid printing time related info in system tests due to lack of determinism
			LoggingUtils.getEvoLogger().info("* Search finished after "
					+ (endTime - startTime)
					+ "s and "
					+ algorithm.getAge()
					+ " generations, "
					+ MaxStatementsStoppingCondition.getNumExecutedStatements()
					+ " statements, best individual has fitness: "
					+ testSuite.getFitness());
		}

		// Search is finished, send statistics
		sendExecutionStatistics();

		return testSuite;
	}
	
    private List<TestFitnessFunction> getGoals(boolean verbose) {
        List<TestFitnessFactory<? extends TestFitnessFunction>> goalFactories = getFitnessFactories();
        List<TestFitnessFunction> goals = new ArrayList<>();

        if(goalFactories.size() == 1) {
                TestFitnessFactory<? extends TestFitnessFunction> factory = goalFactories.iterator().next();
                goals.addAll(factory.getCoverageGoals());

                if(verbose) {
                    LoggingUtils.getEvoLogger().info("* Total number of test goals: {}", factory.getCoverageGoals().size());
					if (Properties.PRINT_GOALS) {
						for (TestFitnessFunction goal : factory.getCoverageGoals())
							LoggingUtils.getEvoLogger().info("" + goal.toString());
					}
				}
        } else {
                if(verbose) {
                        LoggingUtils.getEvoLogger().info("* Total number of test goals: ");
                }

                for (TestFitnessFactory<? extends TestFitnessFunction> goalFactory : goalFactories) {
                        goals.addAll(goalFactory.getCoverageGoals());

                        if(verbose) {
                            LoggingUtils.getEvoLogger().info("  - " + goalFactory.getClass().getSimpleName().replace("CoverageFactory", "")
                                                + " " + goalFactory.getCoverageGoals().size());
							if (Properties.PRINT_GOALS) {
								for (TestFitnessFunction goal : goalFactory.getCoverageGoals())
									LoggingUtils.getEvoLogger().info("" + goal.toString());
							}
                        }
                }
        }
        return goals;
}
}
