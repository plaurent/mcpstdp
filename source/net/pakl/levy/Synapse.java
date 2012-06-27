package net.pakl.levy;

/** Minimal synapse object for connecting Units. */
public class Synapse
{
    
    public double weight = 0;
    public Unit targetUnit = null;
    
    public Synapse(Unit unit, double initialWeight)
    {
        targetUnit = unit;
        weight = initialWeight;
    }

    /** Add the value of the weight of this synapse to the postsynaptic unit. */
    public void feedforward()
    {
        targetUnit.addExcitation(weight);
    }
    
}
