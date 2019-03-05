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
package org.evosuite.ga.metaheuristics;

import java.util.ArrayList;
import java.util.List;

import org.evosuite.ClientProcess;
import org.evosuite.Properties;
import org.evosuite.TimeController;
import org.evosuite.contracts.FailingTestSet;
import org.evosuite.ga.Chromosome;
import org.evosuite.ga.ChromosomeFactory;
import org.evosuite.ga.ConstructionFailedException;
import org.evosuite.ga.FitnessFunction;
import org.evosuite.ga.FitnessReplacementFunction;
import org.evosuite.ga.ReplacementFunction;
import org.evosuite.ga.localsearch.LocalSearchBudget;
import org.evosuite.junit.writer.TestSuiteWriter;
import org.evosuite.result.TestGenerationResult;
import org.evosuite.testcase.TestCase;
import org.evosuite.testsuite.TestSuiteChromosome;
import org.evosuite.utils.LoggingUtils;
import org.evosuite.utils.Randomness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.evosuite.TestSuiteGenerator.writeJUnitTestsAndCreateResult;

/**
 * Implementation of steady state GA
 * 
 * @author Gordon Fraser
 */
public class MonotonicGA<T extends Chromosome> extends GeneticAlgorithm<T> {

	private static final long serialVersionUID = 7846967347821123201L;

	protected ReplacementFunction replacementFunction;

	private final Logger logger = LoggerFactory.getLogger(MonotonicGA.class);

	private double[] fitness_values = new double[10];
	private double[] fitness_means = new double[10]; //mycode
	private double current_fitness_total = 0;//mycode
	private int current_pos = 0;
	private double current_mean_total = 0;
	private int array_size = 0;



	//mycode
	private static int starvationCounter = 0;
	private static double bestFitness = Double.MAX_VALUE;
	private static double lastBestFitness = Double.MAX_VALUE;
	//mycode
	//private double standard_deviation = -1000000000;


	/**
	 * Constructor
	 * 
	 * @param factory
	 *            a {@link org.evosuite.ga.ChromosomeFactory} object.
	 */
	public MonotonicGA(ChromosomeFactory<T> factory) {
		super(factory);

		setReplacementFunction(new FitnessReplacementFunction());
	}

	/**
	 * <p>
	 * keepOffspring
	 * </p>
	 * 
	 * @param parent1
	 *            a {@link org.evosuite.ga.Chromosome} object.
	 * @param parent2
	 *            a {@link org.evosuite.ga.Chromosome} object.
	 * @param offspring1
	 *            a {@link org.evosuite.ga.Chromosome} object.
	 * @param offspring2
	 *            a {@link org.evosuite.ga.Chromosome} object.
	 * @return a boolean.
	 */
	protected boolean keepOffspring(Chromosome parent1, Chromosome parent2, Chromosome offspring1,
			Chromosome offspring2) {
		return replacementFunction.keepOffspring(parent1, parent2, offspring1, offspring2);
	}

	/** {@inheritDoc} */
	@SuppressWarnings("unchecked")
	@Override
	protected void evolve() {
		List<T> newGeneration = new ArrayList<T>();

		// Elitism
		logger.debug("Elitism");
		newGeneration.addAll(elitism());

		// Add random elements
		// new_generation.addAll(randomism());

		while (!isNextPopulationFull(newGeneration) && !isFinished()) {
			logger.debug("Generating offspring");

			T parent1 = selectionFunction.select(population);
			T parent2;
			if (Properties.HEADLESS_CHICKEN_TEST)
				parent2 = newRandomIndividual(); // crossover with new random
													// individual
			else
				parent2 = selectionFunction.select(population); // crossover
																// with existing
																// individual

			T offspring1 = (T) parent1.clone();
			T offspring2 = (T) parent2.clone();

			try {
				// Crossover
				if (Randomness.nextDouble() <= Properties.CROSSOVER_RATE) {
					crossoverFunction.crossOver(offspring1, offspring2);
				}

			} catch (ConstructionFailedException e) {
				logger.info("CrossOver failed");
				continue;
			}

			// Mutation
			notifyMutation(offspring1);
			offspring1.mutate();
			notifyMutation(offspring2);
			offspring2.mutate();

			if (offspring1.isChanged()) {
				offspring1.updateAge(currentIteration);
			}
			if (offspring2.isChanged()) {
				offspring2.updateAge(currentIteration);
			}

			// The two offspring replace the parents if and only if one of
			// the offspring is not worse than the best parent.
			for (FitnessFunction<T> fitnessFunction : fitnessFunctions) {
				fitnessFunction.getFitness(offspring1);
				notifyEvaluation(offspring1);
				fitnessFunction.getFitness(offspring2);
				notifyEvaluation(offspring2);
			}

			if (keepOffspring(parent1, parent2, offspring1, offspring2)) {
				logger.debug("Keeping offspring");

				// Reject offspring straight away if it's too long
				int rejected = 0;
				if (isTooLong(offspring1) || offspring1.size() == 0) {
					rejected++;
				} else {
					// if(Properties.ADAPTIVE_LOCAL_SEARCH ==
					// AdaptiveLocalSearchTarget.ALL)
					// applyAdaptiveLocalSearch(offspring1);
					newGeneration.add(offspring1);
				}

				if (isTooLong(offspring2) || offspring2.size() == 0) {
					rejected++;
				} else {
					// if(Properties.ADAPTIVE_LOCAL_SEARCH ==
					// AdaptiveLocalSearchTarget.ALL)
					// applyAdaptiveLocalSearch(offspring2);
					newGeneration.add(offspring2);
				}

				if (rejected == 1)
					newGeneration.add(Randomness.choice(parent1, parent2));
				else if (rejected == 2) {
					newGeneration.add(parent1);
					newGeneration.add(parent2);
				}
			} else {
				logger.debug("Keeping parents");
				newGeneration.add(parent1);
				newGeneration.add(parent2);
			}

		}

		population = newGeneration;
		// archive
		updateFitnessFunctionsAndValues();

		currentIteration++;
	}

	private T newRandomIndividual() {
		T randomChromosome = chromosomeFactory.getChromosome();
		for (FitnessFunction<?> fitnessFunction : this.fitnessFunctions) {
			randomChromosome.addFitness(fitnessFunction);
		}
		return randomChromosome;
	}

	/** {@inheritDoc} */
	@Override
	public void initializePopulation() {
		notifySearchStarted();
		currentIteration = 0;

		// Set up initial population
		generateInitialPopulation(Properties.POPULATION);
		logger.debug("Calculating fitness of initial population");
		calculateFitnessAndSortPopulation();

		this.notifyIteration();
	}

	private static final double DELTA = 0.000000001; // it seems there is some
														// rounding error in LS,
														// but hard to debug :(


//	public void writeJUnitFailingTests() {
//		if (!Properties.CHECK_CONTRACTS)
//			return;
//
//		FailingTestSet.sendStatistics();
//
//		if (Properties.JUNIT_TESTS) {
//
//			TestSuiteWriter suiteWriter = new TestSuiteWriter();
//			//suiteWriter.insertTests(FailingTestSet.getFailingTests());
//
//			TestSuiteChromosome suite = new TestSuiteChromosome();
//			for(TestCase test : FailingTestSet.getFailingTests()) {
//				test.setFailing();
//				suite.addTest(test);
//			}
//
//			String name = Properties.TARGET_CLASS.substring(Properties.TARGET_CLASS.lastIndexOf(".") + 1);
//			String testDir = Properties.TEST_DIR;
//			LoggingUtils.getEvoLogger().info("* " + ClientProcess.getPrettyPrintIdentifier() + "Writing failing test cases '"
//					+ (name + Properties.JUNIT_SUFFIX) + "' to " + testDir);
//			suiteWriter.insertAllTests(suite.getTests());
//			FailingTestSet.writeJUnitTestSuite(suiteWriter);
//
//			suiteWriter.writeTestSuite(name + Properties.JUNIT_FAILED_SUFFIX, testDir, suite.getLastExecutionResults());
//		}
//	}


	private double compute_std_dev(double[] A, double total)
	{
		double mymean = total / 10;
		double sum = 0;
		for(int i=0;i<10;i++)
		{
			sum = sum + ((A[i] - mymean) * (A[i] - mymean));
		}
		return Math.sqrt(sum / 10);
	}

	/** {@inheritDoc} */
	@Override
	public void generateSolution() {
		if (Properties.ENABLE_SECONDARY_OBJECTIVE_AFTER > 0 || Properties.ENABLE_SECONDARY_OBJECTIVE_STARVATION) {
			disableFirstSecondaryCriterion();
		}

		if (population.isEmpty()) {
			initializePopulation();
			assert!population.isEmpty() : "Could not create any test";
		}

		logger.debug("Starting evolution");

		if (getFitnessFunction().isMaximizationFunction()) {
			bestFitness = 0.0;
			lastBestFitness = 0.0;
		}

		//int num_iteration = 1;
		logger.info("The size of the population is: " + population.size());
		//LoggingUtils.getEvoLogger().info("The population object belongs to:", population.getClass() ); //mycode
		//System.out.println("hello world"); //mycode
		while (!isFinished()) {

            Properties.isCompleted = 1;
		//	logger.info("Iteration: " + num_iteration ); //mycode
		//	num_iteration++; //mycode

			logger.info("Population size before: " + population.size());
			// related to Properties.ENABLE_SECONDARY_OBJECTIVE_AFTER;
			// check the budget progress and activate a secondary criterion
			// according to the property value.

			{
				double bestFitnessBeforeEvolution = getBestFitness();
				evolve();
				sortPopulation();

				double bestFitnessAfterEvolution = getBestFitness();

				if (getFitnessFunction().isMaximizationFunction())
					assert(bestFitnessAfterEvolution >= (bestFitnessBeforeEvolution
							- DELTA)) : "best fitness before evolve()/sortPopulation() was: " + bestFitnessBeforeEvolution
									+ ", now best fitness is " + bestFitnessAfterEvolution;
				else
					assert(bestFitnessAfterEvolution <= (bestFitnessBeforeEvolution
							+ DELTA)) : "best fitness before evolve()/sortPopulation() was: " + bestFitnessBeforeEvolution
									+ ", now best fitness is " + bestFitnessAfterEvolution;
			}

			{
				double bestFitnessBeforeLocalSearch = getBestFitness();
				applyLocalSearch();
				double bestFitnessAfterLocalSearch = getBestFitness();

				if (getFitnessFunction().isMaximizationFunction())
					assert(bestFitnessAfterLocalSearch >= (bestFitnessBeforeLocalSearch
							- DELTA)) : "best fitness before applyLocalSearch() was: " + bestFitnessBeforeLocalSearch
									+ ", now best fitness is " + bestFitnessAfterLocalSearch;
				else
					assert(bestFitnessAfterLocalSearch <= (bestFitnessBeforeLocalSearch
							+ DELTA)) : "best fitness before applyLocalSearch() was: " + bestFitnessBeforeLocalSearch
									+ ", now best fitness is " + bestFitnessAfterLocalSearch;
			}

			/*
			 * TODO: before explanation: due to static state handling, LS can
			 * worse individuals. so, need to re-sort.
			 * 
			 * now: the system tests that were failing have no static state...
			 * so re-sorting does just hide the problem away, and reduce
			 * performance (likely significantly). it is definitively a bug
			 * somewhere...
			 */
			// sortPopulation();

			double newFitness = getBestFitness();

			if (getFitnessFunction().isMaximizationFunction())
				assert(newFitness >= (bestFitness - DELTA)) : "best fitness was: " + bestFitness
						+ ", now best fitness is " + newFitness;
			else
				assert(newFitness <= (bestFitness + DELTA)) : "best fitness was: " + bestFitness
						+ ", now best fitness is " + newFitness;
			bestFitness = newFitness;

			if (Double.compare(bestFitness, lastBestFitness) == 0) {
				starvationCounter++;
			} else {
				logger.info("reset starvationCounter after " + starvationCounter + " iterations");
				starvationCounter = 0;
				lastBestFitness = bestFitness;

			}

			updateSecondaryCriterion(starvationCounter);
			//mycode_starts
			if(array_size < 10)
			{

				fitness_values[current_pos] = bestFitness;
				array_size++;
				current_fitness_total = current_fitness_total + bestFitness;
				//fitness_means[current_pos] = current_fitness_total / array_size ;
				//current_mean_total = current_fitness_total + (current_fitness_total / array_size) ;
				current_pos = (current_pos + 1) % 10;


			}
			else
			{
				double temp = fitness_values[current_pos];
				fitness_values[current_pos] = bestFitness;
				current_fitness_total = (current_fitness_total - temp) + bestFitness;
				temp = fitness_means[current_pos];
				fitness_means[current_pos] = current_fitness_total / 10;
				current_mean_total = (current_mean_total - temp) + fitness_means[current_pos];
				current_pos = (current_pos + 1) % 10;
				temp = compute_std_dev(fitness_means, current_mean_total);
				if(temp > 0.7) {
					logger.info("Standard Deviation is: " + temp);
					//return 1;
					//standard_deviation = temp;
					Properties.isCompleted = 0;
					//may have to archive it
					logger.info("Archiving the test suite");
					TimeController.execute(this::updateBestIndividualFromArchive, "update from archive", 5_000);
					return;
//					TestSuiteChromosome bestIndividual = (TestSuiteChromosome)getBestIndividual();
//					TestGenerationResult result = writeJUnitTestsAndCreateResult(bestIndividual);
//					writeJUnitFailingTests();
//					logger.info("Completed writing Test Suite");

				}


			}
			//mycode_ends
			logger.info("Current iteration: " + currentIteration);
			this.notifyIteration();

			logger.info("Population size: " + population.size());
			logger.info("Best individual has fitness: " + population.get(0).getFitness());
			logger.info("Worst individual has fitness2: " + population.get(population.size() - 1).getFitness());
			logger.info("Hello world");
			//standard_deviation = 1000000000;
			Properties.isCompleted = 1;

		}


		// archive
		TimeController.execute(this::updateBestIndividualFromArchive, "update from archive", 5_000);
		//standard_deviation = 1000000000;
		notifySearchFinished();
	}

	//mycode_starts
//	public int mygenerateSolution() {
//
//		generateSolution();
//		if(standard_deviation != 1000000000)
//			return 1;
//		return 0;
//	}

	//mycode_ends

	private double getBestFitness() {
		T bestIndividual = getBestIndividual();
		for (FitnessFunction<T> ff : fitnessFunctions) {
			ff.getFitness(bestIndividual);
		}
		return bestIndividual.getFitness();
	}

	/**
	 * <p>
	 * setReplacementFunction
	 * </p>
	 * 
	 * @param replacement_function
	 *            a {@link org.evosuite.ga.ReplacementFunction} object.
	 */
	public void setReplacementFunction(ReplacementFunction replacement_function) {
		this.replacementFunction = replacement_function;
	}

	/**
	 * <p>
	 * getReplacementFunction
	 * </p>
	 * 
	 * @return a {@link org.evosuite.ga.ReplacementFunction} object.
	 */
	public ReplacementFunction getReplacementFunction() {
		return replacementFunction;
	}

}
