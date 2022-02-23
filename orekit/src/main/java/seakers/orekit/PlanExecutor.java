package seakers.orekit;

import org.apache.commons.math3.util.FastMath;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.orekit.bodies.GeodeticPoint;
import seakers.orekit.SMDP.SatelliteAction;
import seakers.orekit.SMDP.SatelliteState;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Double.NaN;

public class PlanExecutor {
    private double stopTime;
    private String replanFlag;
    private ArrayList<SatelliteAction> actionsTaken;
    private boolean doneFlag;
    private SatelliteState returnState;
    private Map<GeodeticPoint, Double> rewardGridUpdates;

    public PlanExecutor(SatelliteState s, double startTime, double endTime, ArrayList<SatelliteAction> actionsToTake) {
        doneFlag = false;
        rewardGridUpdates = new HashMap<>();
        actionsTaken = new ArrayList<>();
        replanFlag = "";
        double currentTime = startTime;
        while(!doneFlag) {
            SatelliteAction actionToTake = null;
            for(SatelliteAction a : actionsToTake) {
                if(a.gettStart() > currentTime && a.gettStart() < endTime) {
                    actionToTake = a;
                    break;
                }
            }
            actionsTaken.add(actionToTake);
            if(actionToTake == null) {
                stopTime = endTime;
                returnState = s;
                break;
            }
            s = transitionFunction(s,actionToTake);
            returnState = s;
            currentTime = s.getT();
            if(currentTime > endTime) {
                doneFlag = true;
                stopTime = currentTime;
            }
        }
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
                boolean interestingImage = processImage(a.gettStart(), a.getLocation());
                if(interestingImage) {
                    stopTime = a.gettEnd();
                    replanFlag = "image";
                    doneFlag = true;
                }
                break;
            case "downlink":
                batteryCharge = batteryCharge - (a.gettEnd() - a.gettStart()) * 10 / 3600;
                double dataFracDownlinked = dataStored / ((a.gettEnd() - a.gettStart()) * 0.1);
                dataStored = dataStored - (a.gettEnd() - a.gettStart()) * 0.1;
                storedImageReward = storedImageReward - storedImageReward * dataFracDownlinked;
                stopTime = a.gettEnd();
                replanFlag = "downlink";
                doneFlag = true;
                break;
            case "crosslink":
                batteryCharge = batteryCharge - (a.gettEnd() - a.gettStart()) * 10 / 3600;
                stopTime = a.gettEnd();
                replanFlag = a.getCrosslinkSat();
                doneFlag = true;
                break;
        }
        return new SatelliteState(t,tPrevious,history,batteryCharge,dataStored,currentAngle,storedImageReward);
    }

    public Map<GeodeticPoint, Double> getRewardGridUpdates() {
        return rewardGridUpdates;
    }

    public boolean processImage(double time, GeodeticPoint location) {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost("http://localhost:9020");

        List<NameValuePair> locationParams = new ArrayList<NameValuePair>();
        locationParams.add(new BasicNameValuePair("lat", Double.toString(FastMath.toDegrees(location.getLatitude()))));
        locationParams.add(new BasicNameValuePair("lon", Double.toString(FastMath.toDegrees(location.getLongitude()))));
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(locationParams));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        JSONObject radarResult = new JSONObject();
        CloseableHttpResponse response = null;
        String answer = null;
        double bda = 0.0;
        try {
            response = client.execute(httpPost);
            HttpEntity entity = response.getEntity();
            String jsonString = EntityUtils.toString(entity, StandardCharsets.UTF_8);
            JSONParser parser = new JSONParser();
            radarResult = (JSONObject) parser.parse(jsonString);
            bda = (double) radarResult.get("bda");
            answer = (String) radarResult.get("flag");
            client.close();
        } catch (IOException | ParseException | org.json.simple.parser.ParseException e) {
            e.printStackTrace();
        }
        if(answer.equals("outlier")) {
            rewardGridUpdates.put(location,100.0);
            return true;
        } else {
            rewardGridUpdates.put(location,bda);
            return false;
        }
    }

    public double getStopTime() {
        return stopTime;
    }

    public String getReplanFlag() {
        return replanFlag;
    }

    public ArrayList<SatelliteAction> getActionsTaken() {
        return actionsTaken;
    }

    public SatelliteState getReturnState() {
        return returnState;
    }
}
