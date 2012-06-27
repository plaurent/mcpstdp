
package net.pakl.levy;
import java.util.*;
import java.io.*;

// Properties which seem somewhat stable.
//         a = 0.075 Kr = 0.045 Ki = 0.01 K0 = 0.832 w0 = 0.45
//  (activity is a little high, seems to hold around 18-20%)
//
// Consider implementing Anne's program.

public class Simulation
{
    public Simulation()
    {
    }
    
    public static void main(String args[]) throws Exception
    {
        Net net = new Net();
        net.numNeurons = 2048;
        Properties p = new Properties();
        p.load(new FileInputStream("levy.prop"));
        net.K0 = new Double(p.getProperty("K0")).doubleValue();
        net.Kr = new Double(p.getProperty("Kr")).doubleValue();
        net.Ki = new Double(p.getProperty("Ki")).doubleValue();        
        net.w0 = new Double(p.getProperty("w0")).doubleValue();
        net.desiredActivity = new Double(p.getProperty("a")).doubleValue();        
        System.err.println("Initializing network");
        long beforeWiring = System.currentTimeMillis();
        net.initialize();
        long afterWiring = System.currentTimeMillis();
        System.err.println("  Wiring took " + (afterWiring-beforeWiring) + " ms");
        
        
        boolean [] Z0 = new boolean[net.numNeurons];
        System.err.println("Activating " + (net.numNeurons*net.desiredActivity) + " neurons for initial Z0 vector...");
        for (int i = 0; i < (net.numNeurons*net.desiredActivity); i++)
        {
            Z0[i] = true;
        }

        net.externallyFire(Z0);
        net.feedforward();
        net.update();
        System.out.println(net.getFirings());

        for (int i = 0; i < 100; i++)
        {
            net.feedforward();
            net.update();
            System.out.println(net.getFirings());
        }
        
    }
        
    
}
