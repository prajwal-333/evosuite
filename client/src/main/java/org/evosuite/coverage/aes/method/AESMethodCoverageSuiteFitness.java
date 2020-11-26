package org.evosuite.coverage.aes.method;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.evosuite.coverage.aes.AbstractAESCoverageSuiteFitness;
import org.evosuite.coverage.aes.Spectrum;
import org.evosuite.coverage.method.MethodCoverageTestFitness;
import org.evosuite.testcase.execution.ExecutionResult;

import org.evosuite.utils.LoggingUtils;

public class AESMethodCoverageSuiteFitness extends AbstractAESCoverageSuiteFitness {

	private static final long serialVersionUID = 6334385024571769982L;
	final double THRESHOLD = 0.000001;
	static Map<String,Double> weights=new HashMap<String,Double>(); 
	
	private List<String> methods;
	
	public AESMethodCoverageSuiteFitness(Metric metric) {
		super(metric);
	}

	public AESMethodCoverageSuiteFitness() {
		this(Metric.AES);
	}
	
	private List<String> determineMethods() {
		if (this.methods == null) {
			this.methods = new ArrayList<String>();
			for (MethodCoverageTestFitness goal : getFactory().getCoverageGoals()) {
				if (!(goal instanceof UnreachableMethodCoverageTestFitness)) {
					this.methods.add(goal.toString());
				}
			}
		}
		return this.methods;
	}

	protected AESMethodCoverageFactory getFactory() {
		return new AESMethodCoverageFactory();
	}

	@Override
	public double number_of_1s_metric(Spectrum spectrum, Map<Integer,Double> weights_old)
    {
		List<String> methods = determineMethods();
		Readcsv rd=new Readcsv();
		
		weights=rd.map;
//		weights=null;
		LoggingUtils.getEvoLogger().info("Updated number_of_1s_metric --->>");
        double[][] ochiai = spectrum.compute_ochiai();
        if(ochiai == null)
            return 1d;
        int components = spectrum.getNumComponents();
        double[] avg_val = new double[components];

        for(int i=0;i<components;i++)
        {
            double ones = 0d;
            if(Math.abs(ochiai[i][i])<THRESHOLD)
                avg_val[i] = 1d;
            else
            {
                for (int j = 0; j < components; j++)
                {
                    if ((i != j) && (Math.abs((ochiai[i][j])-1d)<THRESHOLD))
                        ones++;
                }
                if(components < 2)
                    avg_val[i] = (ones / (components - 1));
            }
        }
        double sumWeights = 0d;
        if(weights == null)
            return compute_mean(avg_val,components);

        double sum = 0d;
        
        for(int i=0;i<components;i++) {
        	String key=methods.get(i);
        	double weight=0d;
        	if(!weights.containsKey(key))
        	{
  //      	 LoggingUtils.getEvoLogger().info("rd.min_weight-->"+rd.min_weight);
        		weight=rd.min_weight;
        	}
        	else {
        		weight=weights.get(key);
        	 LoggingUtils.getEvoLogger().info("weight = "+weight);
        	}
            sumWeights = sumWeights + weight;
            avg_val[i] = avg_val[i] * weight;
            sum = sum + avg_val[i];
        	
        	

        }
	  LoggingUtils.getEvoLogger().info("sWeighted matrix calculated-->");
	  LoggingUtils.getEvoLogger().info("metric = "+sum/(sumWeights));
        return sum/(sumWeights);

    }
	
	@Override
	protected Spectrum getSpectrum(List<ExecutionResult> results) {
		List<String> methods = determineMethods();
		//Readcsv rd=new Readcsv();
		//LoggingUtils.getEvoLogger().info("size of map-->>"+rd.map.size());
		
		//LoggingUtils.getEvoLogger().info("list size-->>"+methods.size());
		//LoggingUtils.getEvoLogger().info("-->>"+methods.toString());
		Spectrum spectrum = new Spectrum(results.size(), methods.size());
		
		for (int t = 0; t < results.size(); t++) {			
			for (String coveredMethod : results.get(t).getTrace().getCoveredMethods()) {
				coveredMethod = "[METHOD] " + coveredMethod;                                      //most important change
				spectrum.setInvolved(t, methods.indexOf(coveredMethod));
				
			}
		}
		
		return spectrum;
	}

    protected Map<Integer,Double> getWeights()
    {
        return null;
    }

    @Override
    protected double getSumWeights() {
        return -1d;
    }
}
