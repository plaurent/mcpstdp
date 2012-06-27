package net.pakl.levy;
import java.util.*;
/** A clean-room implementation of a Levy CA1-CA3 RUNIT network
 * (based purely on published work), containing all parameters,
 * and synaptic modification ("learning") code. */
public class Net
{
    /** Number of neurons in this network. */
    public int numNeurons = 2048;
    
    ArrayList <Unit> units = new ArrayList<Unit>();

    /** Probability of one neuron connecting to another, excluding self-connections. */
    public double connectionProbability = 0.1;

    /** Shunting inhibition parameter */
    public double K0 = 0.832;
   
    /** Feedforward inhibition parameter (for external inputs; new name is K_FF) */
    public double Ki = 0.01;

    /** Recurrent inhibition parameter (new name is K_FB) */
    public double Kr = 0.05;

    /** Default initial weight */
    public double w0 = 0.45;

    /** Desired activity used for initial stimulation pulse (how many neurons to fire) */
    public double desiredActivity = 0.075;


    /** Makes the network competitive by effectively ignoring K parameters and
     * instead choosing the top numNeurons * desiredActivity neurons to be active. */
    public boolean isCompetitive = false;

    /**
     * How much subthreshold activation to preserve from the previous time step.
     * This is alpha in the Levy network implementation.
     */
    public double preserveParameter = 0;

    /** Rate of synaptic modification. */
    public double learningRate = 0;
    
    /** Number of neurons firing on last timestep to be scaled by recurrent inhibition. */
    public double numPreviouslyFired = 0;

    /** Number of neurons forced to fire (from external input), to be scaled by feedforward inhibition. */
    public double numForcedToFire = 0;

    int numCurrentlyFired = 0;

    /** Create neurons and connect them. */
    public void initialize()
    {
        for (int i = 0; i < numNeurons; i++)
            units.add(new Unit(this));
        
        for (Unit i : units)
        {
            int numNeeded = (int) (connectionProbability * numNeurons);

            while (numNeeded > 0)
            {
                Unit j = units.get( (int) (Math.random() * units.size()));
                {
                    //if (Math.random() < connectionProbability)
                    //{
                        if ((i != j) && !i.projectsTo(j))
                        {
                            i.projectTo(j, w0);
                            numNeeded--;
                        }
                        if (numNeeded == 0) break;
                    //}
                }
            }
                        
            
        }
    }
    
    /** Calls update on all units, and performs synaptic modification if learningRate > 0 */
    public void update()
    {
        if (isCompetitive)
        {
            updateNetworkAsCompetitive();
        }
        else
        {
            updateNetworkUsingInhibitoryConstants();
        }
    }

    /** Calls feedforward on all the units. */
    public void feedforward()
    {
        feedforwardSingleThread();
    }

    private void updateNetworkAsCompetitive()
    {
        numCurrentlyFired = 0;
        final int numDesiredToFire = (int) (numNeurons * desiredActivity);

        for (Unit u : units)
        {
            if (u.externallyFired)
            {
                u.excitation = Double.MAX_VALUE;
                u.externallyFired = false;
            }
        }

        final ArrayList<Unit> sortedUnits = new ArrayList<Unit>();
        sortedUnits.addAll(units);

        Collections.shuffle(sortedUnits); // This should help with tie-breaking stuff.
        Collections.sort(sortedUnits);    // This will sort them by their excitation.

        for (int rank = 0; rank < sortedUnits.size(); rank++)
        {
            Unit unit = sortedUnits.get(rank);
            unit.previousActivation = unit.activation;
            //System.out.println("Unit with rank " + rank + " has excitation " + unit.excitation);
            if (rank <= numDesiredToFire)
            {
                unit.fired = true;
                unit.activation = 1.0;
                numCurrentlyFired++;
            }
            else
            {
                unit.fired = false;
                unit.activation = unit.activation * preserveParameter;
            }
            unit.excitation = 0;
        }


        numPreviouslyFired = numCurrentlyFired;
        //System.err.println("activity = " + 100.0d*(numPreviouslyFired/numNeurons)+"%");

        if (learningRate > 0)
        {
            for (Unit presynaptic : units)
            {
                for (Synapse synapse : presynaptic.synapses)
                {
                    if (synapse.targetUnit.fired)
                    {
                        synapse.weight = synapse.weight + learningRate * (presynaptic.previousActivation - synapse.weight);
                    }
                }
            }
        }
    }

    private void updateNetworkUsingInhibitoryConstants()
    {
        numCurrentlyFired = 0;
        for (Unit i : units)
        {
            i.update();
            if (i.fired) numCurrentlyFired++;
        }
        numPreviouslyFired = numCurrentlyFired;
        System.err.println("activity = " + 100.0d*(numPreviouslyFired/numNeurons)+"%");

        if (learningRate > 0)
        {
            for (Unit presynaptic : units)
            {
                for (Synapse synapse : presynaptic.synapses)
                {
                    if (synapse.targetUnit.fired)
                    {
                        synapse.weight = synapse.weight + learningRate * (presynaptic.previousActivation - synapse.weight);
                    }
                }
            }
        }
    }


    public void feedforwardSingleThread()
    {
        for (Unit i : units) i.feedforward();
    }

    
    /** Activates the specified neurons by clamping them on (in addition to any existing activity in the network) */
    public void externallyFire(boolean [] firings)
    {
        int numForced = 0;
        for (int i = 0; i < units.size(); i++)
        {
            if (firings[i]) 
            {
                units.get(i).externallyFired = true; 
                numForced++;
                //System.out.println("externallyFire() requested to fire unit " + i);
            }
            else
            {
                units.get(i).externallyFired = false;
            }
        }
        numForcedToFire = numForced;
    }
    
    /** Activates (and de-activates) neurons without engaging the inhibition that would be due to external input. */
    public void divineInterventionFire(boolean [] firings)
    {
        for (int i = 0; i < units.size(); i++)
        {
            if (firings[i]) 
            {
                units.get(i).externallyFired = true; 
            }
            else
            {
                units.get(i).externallyFired = false;
                units.get(i).fired = false;
            }
        }
        numForcedToFire = 0;
    }

//    /** Implements Z0 random initial firing pattern */
//    public void randomlyFireAtDesiredActivityLevel()
//    {
//        int numDesiredToFire = (int) (numNeurons * desiredActivity);
//        Collections.shuffle(units);
//        for (int i = 0; i < units.size(); i++)
//        {
//            Unit u = units.get(i);
//            if (i < numDesiredToFire) { u.externallyFired = true; }
//        }
//    }
    
    public String getFirings()
    {
        String result = "";
        for (Unit u : units)
        {
            if (u.fired) { result += "1 "; } else { result += "0 "; }
        }
        return result;
    }






}
