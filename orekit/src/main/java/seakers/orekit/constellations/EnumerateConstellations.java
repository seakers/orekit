/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.constellations;

import seakers.orekit.util.Powerset;

import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 * @author paugarciabuzzi
 */
public class EnumerateConstellations {
    
    
    public static ArrayList<WalkerParameters> fullFactWalker(Double[] alts, Double[] incs, Integer[] ts){
        ArrayList<WalkerParameters> constels=new ArrayList<>();
        for (int i_a=0; i_a<alts.length; i_a++){
            for (int i_i=0; i_i<incs.length; i_i++){
                for (int i_t=0; i_t<ts.length; i_t++){
                    //find the divisors of t
                    int t = ts[i_t];
                    ArrayList<Integer> planes=new ArrayList<>();
                    if(t!=1){
                        planes.add(1);
                    }
                    for (int c = 2; c <= t / 2; c++) {
                        if (t % c == 0) {
                            planes.add(c);
                        }
                    }
                    planes.add(t);
                    for (int i_p=0; i_p<planes.size(); i_p++){
                        for (int i_f=0; i_f<=planes.get(i_p)-1; i_f++){
                            constels.add(new WalkerParameters(alts[i_a],incs[i_i],ts[i_t],planes.get(i_p),i_f));
                        }
                    }
                }
            }
        }
        return constels;
    }

    public static ArrayList<TrainParameters> fullFactTrain(Double[] alts, ArrayList<Double> LTANs) {
        ArrayList<TrainParameters> constellations = new ArrayList<>();
        ArrayList<ArrayList<Double>> combs = Powerset.powerSet(LTANs);
        for(int i_a = 0; i_a < alts.length; ++i_a) {
            Iterator<ArrayList<Double>> iterator = combs.iterator();
            while (iterator.hasNext()){
                ArrayList<Double> LTANi=iterator.next();
                if (!LTANi.isEmpty()){
                    constellations.add(new TrainParameters(alts[i_a],LTANi));
                }
            }
        }
        return constellations;
    }
    
}
