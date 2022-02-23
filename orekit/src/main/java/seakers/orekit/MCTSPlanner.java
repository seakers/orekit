package seakers.orekit;

import org.orekit.bodies.GeodeticPoint;
import org.orekit.frames.TopocentricFrame;
import org.orekit.time.AbsoluteDate;
import seakers.orekit.SMDP.ActionResult;
import seakers.orekit.SMDP.SatelliteAction;
import seakers.orekit.SMDP.SatelliteState;
import seakers.orekit.SMDP.StateAction;
import seakers.orekit.analysis.Record;
import seakers.orekit.coverage.access.TimeIntervalArray;
import seakers.orekit.object.Satellite;

import java.util.*;

public class MCTSPlanner {
    private ArrayList<SatelliteAction> results;
    private ArrayList<SatelliteState> V;
    private Map<Key,Integer> N;
    private Map<Key,Double> Q;
    private double c;
    private ArrayList<Observation> sortedObservations;
    private TimeIntervalArray downlinks;
    private Map<String, TimeIntervalArray> crosslinks;
    private double priorityInfo;
    private double gamma;
    private int dSolveInit;
    private Map<GeodeticPoint,Double> rewardGrid;
    private int actionSpaceSize;
    private int nMaxSim;

    public MCTSPlanner(ArrayList<Observation> sortedObservations, TimeIntervalArray downlinks, Map<String,TimeIntervalArray> crosslinks, Map<GeodeticPoint,Double> rewardGrid, SatelliteState initialState) {
        this.sortedObservations = sortedObservations;
        this.downlinks = downlinks;
        this.crosslinks = crosslinks;
        this.rewardGrid = rewardGrid;
        this.gamma = 0.999;
        this.priorityInfo = 0.0;
        this.dSolveInit = 5;
        this.actionSpaceSize = 4;
        this.nMaxSim = 50;
        this.c = 4;
        this.Q = new HashMap<>();
        this.N = new HashMap<>();
        this.V = new ArrayList<>();
        ArrayList<StateAction> stateActions = monteCarloTreeSearch(initialState);
        ArrayList<SatelliteAction> observations = new ArrayList<>();
        for (StateAction stateAction : stateActions) {
            observations.add(stateAction.getA());
        }
        results = observations;
    }

    public ArrayList<StateAction> monteCarloTreeSearch(SatelliteState initialState) {
        ArrayList<StateAction> resultList = new ArrayList<>();
        SatelliteState s = initialState;
        boolean moreActions = true;
        while(moreActions) {
            for (int n = 0; n < nMaxSim; n++) {
                simulate(s,dSolveInit);
            }
            double max = 0.0;
            SatelliteAction bestAction = null;
            for (Key sa : Q.keySet()) {
                if(sa.getS().equals(s)) {
                    double value = Q.get(sa);
                    if(value > max) {
                        max = value;
                        bestAction = sa.getA();
                    }
                }
            }
            if(bestAction==null) {
                break;
            }
            StateAction stateAction = new StateAction(s,bestAction);
            s = transitionFunction(initialState,bestAction);
            resultList.add(stateAction);
            moreActions = !getActionSpace(s).isEmpty();
        }
        return resultList;
    }



    public double simulate(SatelliteState s, int d) {
        if(d == 0) {
            return 0;
        }
        if(!V.contains(s)) {
            ArrayList<SatelliteAction> actionSpace = getActionSpace(s);
            for (SatelliteAction a : actionSpace) {
                Key sa = new Key(s,a);
                N.put(sa,1);
                Q.put(sa,0.0);
            }
            V.add(s);
            return rollout(s,actionSpace,dSolveInit);
        }
        double max = 0.0;
        SatelliteAction bestAction = null;
        int nSum = 0;
        for(Key sa1 : N.keySet()) {
            if(sa1.getS().equals(s)) {
                nSum = nSum + N.get(sa1);
            }
        }
        for (Key sa : Q.keySet()) {
            if(sa.getS().equals(s)) {
                double value = Q.get(sa) + c*Math.sqrt(Math.log10(nSum)/N.get(sa));
                if(value > max) {
                    max = value;
                    bestAction = sa.getA();
                }
            }
        }
        Key key = new Key(s,bestAction);
        if(bestAction == null) {
            return 0;
        }
        SatelliteState newSatelliteState = transitionFunction(s,bestAction);
        double r = rewardFunction(s,bestAction);
        double q = r + Math.pow(gamma,bestAction.gettStart()-s.getT()) * simulate(newSatelliteState, d-1);
        N.put(key,N.get(key)+1);
        Q.put(key,Q.get(key)+(q-Q.get(key))/N.get(key));
        return q;
    }

    public double rollout(SatelliteState s, ArrayList<SatelliteAction> actionSpace, int d) {
        if(d == 0) {
            return 0;
        }
        SatelliteAction selectedAction = null;
        ArrayList<SatelliteAction> chargeActions = new ArrayList<>();
        if(s.getBatteryCharge() < 15) {
            for (SatelliteAction a : actionSpace) {
                if(a.getActionType().equals("charge")) {
                    chargeActions.add(a);
                }
            }
        }
        if(chargeActions.size()!=0) {
            Random random = new Random();
            selectedAction = chargeActions.get(random.nextInt(chargeActions.size()));
        }
        ArrayList<SatelliteAction> downlinkActions = new ArrayList<>();
        if(s.getDataStored() > 50) {
            for (SatelliteAction a : actionSpace) {
                if(a.getActionType().equals("downlink")) {
                    downlinkActions.add(a);
                }
            }
        }
        if(selectedAction==null && downlinkActions.size()!=0) {
            Random random = new Random();
            selectedAction = downlinkActions.get(random.nextInt(downlinkActions.size()));
        }
        ArrayList<SatelliteAction> crosslinkActions = new ArrayList<>();
        if(priorityInfo > 10) {
            for (SatelliteAction a : actionSpace) {
                if(a.getActionType().equals("crosslink")) {
                    crosslinkActions.add(a);
                }
            }
        }
        if(selectedAction==null && crosslinkActions.size()!=0) {
            Random random = new Random();
            selectedAction = crosslinkActions.get(random.nextInt(crosslinkActions.size()));
        }
        if(selectedAction==null) {
            ArrayList<SatelliteAction> observationActions = new ArrayList<>();
            for(SatelliteAction a : actionSpace) {
                if(a.getActionType().equals("imaging")){
                    observationActions.add(a);
                }
            }
            if(observationActions.isEmpty()) {
                return 0;
            }
            Random random = new Random();
            selectedAction = observationActions.get(random.nextInt(observationActions.size()));
        }
        double reward = rewardFunction(s,selectedAction);
        SatelliteState newSatelliteState = transitionFunction(s,selectedAction);
        return reward + Math.pow(gamma,selectedAction.gettStart()-s.getT())*rollout(newSatelliteState, getActionSpace(newSatelliteState),d-1);
    }

    public double rewardFunction(SatelliteState s, SatelliteAction a){
        double score = 0.0;
        switch(a.getActionType()) {
            case "charge":
                score = 0.0;
                break;
            case "imaging":
                score = a.getReward()*Math.pow(gamma,(a.gettStart()-s.getT()));
                break;
            case "downlink":
                double dataFracDownlinked = s.getDataStored() / ((a.gettEnd() - a.gettStart()) * 0.1);
                score = s.getStoredImageReward()*dataFracDownlinked;
                break;
            case "crosslink":
                score = priorityInfo;
                break;
        }
        if(s.getBatteryCharge() < 10) {
            score = -1000;
        }
        if(s.getDataStored() > 100) {
            score = -1000;
        }
        return score;
    }

    public SatelliteState transitionFunction(SatelliteState s, SatelliteAction a) {
        double t = a.gettEnd();
        double tPrevious = s.getT();
        ArrayList<SatelliteAction> history = s.getHistory();
        history.add(a);
        double storedImageReward = s.getStoredImageReward();
        double batteryCharge = s.getBatteryCharge();
        double dataStored = s.getDataStored();
        double currentAngle = s.getCurrentAngle();
        switch (a.getActionType()) {
            case "charge":
                batteryCharge = batteryCharge + (a.gettEnd() - s.getT()) * 5 / 3600; // Wh
                break;
            case "imaging":
                batteryCharge = batteryCharge - (a.gettEnd() - a.gettStart()) * 10 / 3600;
                dataStored = dataStored + 1.0;
                currentAngle = a.getAngle();
                storedImageReward = storedImageReward + a.getReward();
                // insert reward grid update here
                break;
            case "downlink":
                batteryCharge = batteryCharge - (a.gettEnd() - a.gettStart()) * 10 / 3600;
                double dataFracDownlinked = dataStored / ((a.gettEnd() - a.gettStart()) * 0.1);
                dataStored = dataStored - (a.gettEnd() - a.gettStart()) * 0.1;
                storedImageReward = storedImageReward - storedImageReward * dataFracDownlinked;
                break;
            case "crosslink":
                batteryCharge = batteryCharge - (a.gettEnd() - a.gettStart()) * 10 / 3600;
                break;
        }
        return new SatelliteState(t,tPrevious,history,batteryCharge,dataStored,currentAngle,storedImageReward);
    }

    public ArrayList<SatelliteAction> getActionSpace(SatelliteState s) {
        double currentTime = s.getT();
        ArrayList<SatelliteAction> possibleActions = new ArrayList<>();
        ArrayList<Observation> currentObservations = new ArrayList<>();
        for (Observation obs : sortedObservations) {
            if(obs.getObservationStart() > currentTime && currentObservations.size() < actionSpaceSize) {
                SatelliteAction obsAction = new SatelliteAction(obs.getObservationStart(),obs.getObservationEnd(),obs.getObservationPoint(),"imaging",rewardGrid.get(obs.getObservationPoint()),obs.getObservationAngle());
                SatelliteAction chargeAction = new SatelliteAction(obs.getObservationStart(),obs.getObservationEnd(),null,"charge");
                if(canSlew(s.getCurrentAngle(),obs.getObservationAngle(),currentTime,obs.getObservationStart())) {
                    possibleActions.add(obsAction);
                    possibleActions.add(chargeAction);
                    currentObservations.add(obs);
                }
            }
        }
        for (int i = 0; i < downlinks.getRiseAndSetTimesList().length; i=i+2) {
            if(downlinks.getRiseAndSetTimesList()[i] > currentTime) {
                SatelliteAction downlinkAction = new SatelliteAction(downlinks.getRiseAndSetTimesList()[i],downlinks.getRiseAndSetTimesList()[i+1],null,"downlink");
                SatelliteAction chargeAction = new SatelliteAction(downlinks.getRiseAndSetTimesList()[i],downlinks.getRiseAndSetTimesList()[i+1],null,"charge");
                possibleActions.add(downlinkAction);
                possibleActions.add(chargeAction);
            } else if (downlinks.getRiseAndSetTimesList()[i] < currentTime && downlinks.getRiseAndSetTimesList()[i+1] > currentTime) {
                SatelliteAction downlinkAction = new SatelliteAction(currentTime,downlinks.getRiseAndSetTimesList()[i+1],null,"downlink");
                SatelliteAction chargeAction = new SatelliteAction(currentTime,downlinks.getRiseAndSetTimesList()[i+1],null,"charge");
                possibleActions.add(downlinkAction);
                possibleActions.add(chargeAction);
            }
        }
        for (String sat : crosslinks.keySet()) {
            double[] crosslinkTimes = crosslinks.get(sat).getRiseAndSetTimesList();
            for (int i = 0; i < crosslinkTimes.length; i=i+2) {
                if(crosslinkTimes[i] >= currentTime) {
                    SatelliteAction crosslinkAction = new SatelliteAction(crosslinkTimes[i],crosslinkTimes[i+1],null,"crosslink",sat);
                    SatelliteAction chargeAction = new SatelliteAction(crosslinkTimes[i],crosslinkTimes[i+1],null,"charge");
                    possibleActions.add(crosslinkAction);
                    possibleActions.add(chargeAction);
                }
            }
        }

        return possibleActions;
    }

    public boolean canSlew(double angle1, double angle2, double time1, double time2){
        double slewTorque = 4*Math.abs(angle2-angle1)*0.05/Math.pow(Math.abs(time2-time1),2);
        double maxTorque = 4e-3;
        if (slewTorque > maxTorque) {
            return false;
        } else {
            return true;
        }
    }
    
    public ArrayList<SatelliteAction> getResults() {
        return results;
    }
}

