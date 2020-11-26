package org.evosuite.coverage.aes.branch;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.evosuite.coverage.aes.AbstractAESCoverageSuiteFitness;
import org.evosuite.coverage.aes.Spectrum;
import org.evosuite.coverage.branch.BranchCoverageTestFitness;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;

public class AESBranchCoverageSuiteFitness extends AbstractAESCoverageSuiteFitness {

    private static final long serialVersionUID = 7409239464436681146L;
    /* A branch can be of 3 types: 1) true branch (stored in the "trueMap"), 2) false branch (stored in the "falseMap") and 3) a method not containing a branch
    (stored in the "branchlessMethodMap") */
    private Map<Integer, Integer> trueMap;
    private Map<Integer, Integer> falseMap;
    private Map<String, Integer> branchlessMethodsMap;

    /* This map parses the JSON object. And maps the string <classname+"."+method_name> to the suspiciousness score*/
    private Map<String, Double> suspiciousnesScores;
    /*This map stores the weights of each method (likelihood). This is done by mapping the component number of each branch to its corresponding suspiciousness score
    found in the "suspiciousnessScores" map. The idea is we first find the method to which the branch belongs to. Then we make the string in the format
    <classname+"."+method_name> and  do a look up in the "suspiciousnessScores" */
    private Map<Integer, Double> weights;
    /* mode signified whether we are using the priors or not. If set to false then all the components have equal chance of being faulty */
    private boolean mode;
    private int numberOfGoals = 0;

    public AESBranchCoverageSuiteFitness(Metric metric) {
        super(metric);
    }

    public AESBranchCoverageSuiteFitness() {
        this(Metric.AES);
    }

    private void determineCoverageGoals() {

        if (this.branchlessMethodsMap == null || this.trueMap == null || this.falseMap == null) {
            this.branchlessMethodsMap = new HashMap<String, Integer>();
            this.trueMap = new HashMap<Integer, Integer>();
            this.falseMap = new HashMap<Integer, Integer>();

            /* list "goals" contains all the branches that has been covered by the current generation of test suites */
            List<TestFitnessFunction> goals = new AESBranchCoverageFactory().getCoverageGoals();
            this.numberOfGoals = goals.size() - 1;

            /* This variable "g" is the component number */
            for (int g = 0; g < this.numberOfGoals; g++) {
                TestFitnessFunction ff = goals.get(g);

                if (ff instanceof BranchCoverageTestFitness) {
                    BranchCoverageTestFitness goal = (BranchCoverageTestFitness) ff;

                    /* For each of the goal object, which is essentially a branch covered, we must map it to the prior.
                    "branchToSuspiciousnessMap" takes care of that.*/
                    branchToSuspiciousnessMap(goal, g);
                    if (goal.getBranch() == null) { // branchless method{
                        branchlessMethodsMap.put(goal.getClassName() + "." + goal.getMethod(), g);
                    } else if (goal.getBranchExpressionValue()) { // true branch
                        trueMap.put(goal.getBranch().getActualBranchId(), g);
                    } else { // false branch
                        falseMap.put(goal.getBranch().getActualBranchId(), g);
                    }
                }
            }
        }
    }

    @Override
    protected Spectrum getSpectrum(List<ExecutionResult> results) {
        determineCoverageGoals();
        Spectrum spectrum = new Spectrum(results.size(), this.numberOfGoals);

        for (int t = 0; t < results.size(); t++) {
            ExecutionResult result = results.get(t);

            for (String method : result.getTrace().getCoveredMethods()) {
                if (branchlessMethodsMap.containsKey(method)) {
                    spectrum.setInvolved(t, branchlessMethodsMap.get(method));
                }
            }

            for (int trueBranchId : result.getTrace().getCoveredTrueBranches()) {
                if (trueMap.containsKey(trueBranchId)) {
                    spectrum.setInvolved(t, trueMap.get(trueBranchId));
                }
            }

            for (int falseBranchId : result.getTrace().getCoveredFalseBranches()) {
                if (falseMap.containsKey(falseBranchId)) {
                    spectrum.setInvolved(t, falseMap.get(falseBranchId));
                }
            }

        }

        return spectrum;
    }

    /* This function maps the prior to the branches */
    private void branchToSuspiciousnessMap(BranchCoverageTestFitness A, int component_no) {
        //uniform distribution
        if (!mode)
            return;

        //initialise weight map
        if (weights == null)
            weights = new HashMap<>();                    //mycode Integer=component_number, Double=Suspiciousness

        /* We first extract the method and class name from the goal object */
        String method_name = A.getMethod();
        String class_name = A.getClassName();

        /* Modify the string such that it follows the <classname+"."+method_name> format */
        String method_final = method_name.substring(0, method_name.indexOf('('));
        String class_final = class_name.substring(class_name.lastIndexOf('.') + 1);

        /* If the method name is <init>, it is a constructor. So we rename it to the classname */
        if (method_final.contains("<init>"))
            method_final = class_final;

        /* We make the lookup in the "suspiciousnesScores" map  to get the corresponding prior value of the branch */
        Double temp = suspiciousnesScores.get(class_final + "." + method_final);
        /* If there doesn't exist an entry then we give it a very small prior val. The weights map uses <component_no, prior_val> key-value pair. */
        if (temp == null) {
            weights.put(component_no, 0.0000001);
        } else {
            weights.put(component_no, temp);
        }

    }

    /* This method that will be called during computation of FF4, to get the weights. Remember if mode is false (not using prior) then weights map is null.
    We must put a check in the calling method to know if priors are being used or not. */
    protected Map<Integer, Double> getWeights() {
        return weights;
    }

    protected double getSumWeights() {
        //return sumWeights;
        return 0d;
    }
}
