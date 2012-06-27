package net.pakl.levy;

import java.util.*;
import java.io.*;


public class SimulationLearning
{

    public static int trainingTrials = 120;
    public static int testingTrials = 10;
    public static int earlyTrialToSave = 10;

    public static void main(String args[]) throws Exception
    {
        long startTime = System.currentTimeMillis();
        Net net = new Net();

        int sequenceLength = 20;
        int stutter = 1;
        int numOnPerTimestep = 20;
        int spacing = 5;
        double pExternalOffNoise = 0;
        double connectionProbability = 0.1;

        double desiredActivityOnLastTrain = 0;

        try
        {
            Properties p = new Properties();
            p.load(new FileInputStream("levy.prop"));
            if (p.getProperty("K0")!=null) net.K0 = new Double(p.getProperty("K0")).doubleValue();
            if (p.getProperty("Kr")!=null) net.Kr = new Double(p.getProperty("Kr")).doubleValue();
            if (p.getProperty("Ki")!=null) net.Ki = new Double(p.getProperty("Ki")).doubleValue();
            net.w0 = new Double(p.getProperty("w0")).doubleValue();
            net.numNeurons = new Integer(p.getProperty("n")).intValue();
            net.desiredActivity = new Double(p.getProperty("a")).doubleValue();
            net.preserveParameter = new Double(p.getProperty("preserveParameter")).doubleValue();   
            net.learningRate = new Double(p.getProperty("synmodrate")).doubleValue();
            if (p.getProperty("trainingTrials")!=null) trainingTrials = new Integer(p.getProperty("trainingTrials"));

            desiredActivityOnLastTrain = net.desiredActivity;
            if (p.getProperty("a_last_train")!=null) desiredActivityOnLastTrain = new Double(p.getProperty("pExternalOffNoise"));

            if (p.getProperty("pExternalOffNoise")!=null) pExternalOffNoise = new Double(p.getProperty("pExternalOffNoise"));
            if (p.getProperty("connectionProbability")!=null) connectionProbability = new Double(p.getProperty("connectionProbability"));
            if (p.getProperty("isCompetitive")!=null) if (p.getProperty("isCompetitive").equalsIgnoreCase("true")) net.isCompetitive = true;
            if (p.getProperty("earlyTrialToSave")!=null) earlyTrialToSave = new Integer(p.getProperty("earlyTrialToSave"));

            net.connectionProbability = connectionProbability;

            spacing = new Integer(p.getProperty("spacing")).intValue();
            stutter = new Integer(p.getProperty("stutter")).intValue();            
            sequenceLength = new Integer(p.getProperty("sequenceLength")).intValue(); 
            numOnPerTimestep = new Integer(p.getProperty("patternSize")).intValue();
        }
        catch (Exception e)
        {
            System.err.println(e.getClass() + " " + e.getMessage());
            System.err.println("Problem loading properties from levy.prop.");
            throw e;
        }
        
        
        net.initialize();

        
        boolean [][] input = new boolean[sequenceLength*stutter][net.numNeurons];

        int t = 0;
        for (int pattern = 0; pattern < sequenceLength; pattern++)
        {
            for (int j = (pattern*spacing); j < (pattern*spacing)+numOnPerTimestep; j++)
            {
                for (int s = 0; s < stutter; s++)
                {
                    input[t+s][j] = true;
                }
            }
            t = t + stutter;
        }

        PrintStream myOutput = new PrintStream(new FileOutputStream("levyneurons.txt"));
        PrintStream finalTrain = new PrintStream(new FileOutputStream("finaltrain.txt"));
        PrintStream earlyTrain = new PrintStream(new FileOutputStream("earlytrain.txt"));
        PrintStream finalTest = new PrintStream(new FileOutputStream("finaltest.txt"));

        int timestepsPerTrial = sequenceLength * stutter;
        for (int i = 0; i < trainingTrials; i++)
        {
            System.err.print("\n Train "+i+ " ");

            if (i == trainingTrials-1) { net.desiredActivity = desiredActivityOnLastTrain; }

            applyInitialFiring(net);
            
            for (t = 0; t < timestepsPerTrial; t++)
            {
                if (t < sequenceLength*stutter) 
                {
                    net.externallyFire(offNoise(input[t], pExternalOffNoise));
                }
                    
                net.feedforward();
                net.update();
                myOutput.println(net.getFirings());
                if (i == earlyTrialToSave) earlyTrain.println(net.getFirings());
                if (i == trainingTrials-1) finalTrain.println(net.getFirings());
            }
        }

        timestepsPerTrial = sequenceLength * stutter;
        net.learningRate = 0;
        for (int i = 0; i < testingTrials; i++)
        {
            applyInitialFiring(net);


            for (t = 0; t < timestepsPerTrial; t++)
            {
                if (t < stutter) net.externallyFire(offNoise(input[0], pExternalOffNoise)); // give initial pattern of sequence as recall cue.
                net.feedforward();
                net.update();
                myOutput.println(net.getFirings());

                if (i == testingTrials-1) finalTest.println(net.getFirings());
            }
        }
        System.err.println("");
        myOutput.close();
        finalTrain.close();
        finalTest.close();
        earlyTrain.close();
        System.out.println("Total run time: " + ((System.currentTimeMillis() - startTime) /1000.0d) + " seconds.");
    }

    public static boolean[] offNoise(boolean[] pattern, double probabilityOfTurnOff)
    {
        boolean [] result = new boolean[pattern.length];
        
        for (int i = 0; i < pattern.length; i++)
        {
            if (pattern[i] == true)
            {
                if (Math.random() < probabilityOfTurnOff)
                {
                    result[i] = false;
                }
                else
                {
                    result[i] = true;
                }
            }
        }
        return result;
    }
        
    public static void applyInitialFiring(Net net)
    {
            boolean [] initialFiring = new boolean[net.numNeurons];
            int numNeeded = (int) (net.numNeurons*net.desiredActivity);
            while (numNeeded > 0)
            {
                for (int i = 0; i < net.numNeurons; i++)
                {
                    if (numNeeded == 0) break;
                    if (Math.random() < net.desiredActivity)
                    {
                        if (initialFiring[i] == false)
                        {
                            initialFiring[i] = true;
                            numNeeded--;
                        }

                    }
                }
            }
            net.divineInterventionFire(initialFiring);
            net.feedforward();
            net.update();
            
    }
    
}

// Example levy.prop files

// ------------------------------------------------------------------
// EXAMPLE 1
// SIMPLE SEQUENCE, 1 PATTERN PER TIMESTEP, OVERLAPPING
// ------------------------------------------------------------------
//n = 1024
//a = 0.10
//Kr = 0.050
//Ki = 0.046
//K0 = 0.0
//w0 = 0.40
//synmodrate = 0.005
//preserveParameter = 0
//spacing = 3
//patternSize = 20
//stutter = 1
//sequenceLength = 20
// ------------------------------------------------------------------

// ------------------------------------------------------------------
// EXAMPLE 2
// STUTTERED PATTERNS with NO OVERLAP
// ------------------------------------------------------------------
//n = 1024
//a = 0.10
//Kr = 0.050
//Ki = 0.046
//K0 = 0.0
//w0 = 0.40
//synmodrate = 0.005
//preserveParameter = 0.5
//spacing = 20
//patternSize = 20
//stutter = 2
//sequenceLength = 10
// ------------------------------------------------------------------



