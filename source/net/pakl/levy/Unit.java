package net.pakl.levy;

import java.util.*;

/** Simple McCulloch-Pitts like unit for use in Levy network.  It can 
 * compare its excitation against other Unit objects for competitive activity control. */
public class Unit implements Comparable
{
    public ArrayList <Synapse> synapses = new ArrayList<Synapse>();
    double activation = 0;
    double excitation = 0;
    double previousActivation = 0;
    
    public boolean externallyFired = false;
    
    public boolean fired = false;
    private Net net = null;
    
    public Unit(Net net)
    {
        this.net = net;
    }
    
    public void projectTo(Unit u, double weight)
    {
        synapses.add(new Synapse(u, weight));
    }
    
    public boolean projectsTo(Unit u)
    {
        for (Synapse s : synapses)
        {
            if (s.targetUnit == u) return true;
        }
        return false;
    }
    
    /** Called by a Synapse that is adding its excitation into this unit. */
    public void addExcitation(double toAdd)
    {
        this.excitation += toAdd;
    }
    
    /** Calculate thresholding and effects of inhibition on this unit, 
     * determine whether to fire, and decay activation appropriately. */
    public void update()
    {
        previousActivation = activation;
        if (externallyFired)
        {
            fired = true;
            activation = 1.0;
        }
        else
        {
            excitation = excitation / 
                    (excitation 
                    + net.Kr * net.numPreviouslyFired
                    + net.Ki * net.numForcedToFire
                    + net.K0);

            if (excitation > 0.5) 
            {
                fired = true; 
                activation = 1;
            }
            else
            {
                fired = false;
                activation = activation * net.preserveParameter;
            }
        }
        excitation = 0;
        externallyFired = false;
    }
    
    /** If this unit fired due to update(), pass excitation on to neurons to which this unit is connected. */
    public void feedforward()
    {
        if (fired)
        {
            for (Synapse s : synapses)
            {
                s.feedforward();
            }
        }
    }

    public int compareTo(Object o)
    {
        if (!(o instanceof Unit))
        {
            throw new RuntimeException("Could not compare a Unit's excitation to another object of type "+o.getClass());
        }
        Unit other = (Unit) o;

        if ((this.excitation - other.excitation) < 0) return +1;
        else return -1;
    }



    public static void main(String args[])
    {
        ArrayList<Unit> list = new ArrayList<Unit>();
        Unit u1 = new Unit(null); u1.excitation = 20; list.add(u1);
        Unit u2 = new Unit(null); u2.excitation = 10; list.add(u2);
        Unit u3 = new Unit(null); u3.excitation = 30; list.add(u3);

        for (int i = 0; i < list.size(); i++)
        {
            Unit u = list.get(i);
            System.out.println(u.excitation);
        }
        Collections.sort(list);
        Collections.reverse(list);
        for (int i = 0; i < list.size(); i++)
        {
            Unit u = list.get(i);
            System.out.println(u.excitation);
        }

    }
}
