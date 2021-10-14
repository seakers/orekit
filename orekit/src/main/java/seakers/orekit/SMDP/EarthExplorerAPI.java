package seakers.orekit.SMDP;

import org.orekit.bodies.GeodeticPoint;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class EarthExplorerAPI {
    public static String login() throws IOException {
        String urlParameters  = "username=bgorr&password=SpectralAnalysis8&param3=c";
        byte[] postData       = urlParameters.getBytes( StandardCharsets.UTF_8 );
        int    postDataLength = postData.length;
        URL url = new URL ("https://earthexplorer.usgs.gov/inventory/json/v/1.4.0/login");
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        con.setDoOutput( true );
        con.setInstanceFollowRedirects( false );
        con.setRequestMethod( "POST" );
        con.setRequestProperty( "Content-Type", "application/json");
        con.setRequestProperty( "charset", "utf-8");
        con.setRequestProperty( "Content-Length", Integer.toString( postDataLength ));
        con.setUseCaches( false );
        try( DataOutputStream wr = new DataOutputStream( con.getOutputStream())) {
            wr.write( postData );
        }
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);
        int status = con.getResponseCode();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();
        return null;
    }
    public static String sceneSearch(String apiKey, GeodeticPoint pointOfInterest) throws IOException {
        String urlString = "https://earthexplorer.usgs.gov/inventory/json/v/1.4.0/search?jsonRequest={\"apiKey\":\""+apiKey+"\",\"datasetName\":\"LANDSAT_8_C1\",\"spatialFilter\":{\"filterType\":\"mbr\",\"lowerLeft\":{\"latitude\":\""+(int)Math.toDegrees(pointOfInterest.getLatitude())+"\",\"longitude\":\""+(int)Math.toDegrees(pointOfInterest.getLongitude())+"\"},\"upperRight\":{\"latitude\":\""+(int)Math.toDegrees(pointOfInterest.getLatitude())+"\",\"longitude\":\""+(int)Math.toDegrees(pointOfInterest.getLongitude())+"\"}},\"temporalFilter\":{\"startDate\":\"2020-06-01\",\"endDate\":\"2020-06-17\"},\"months\":[],\"includeUnknownCloudCover\":false,\"sortOrder\":\"ASC\"}";
//        urlString = urlString+"{\"apiKey\":\""+apiKey+"\",";
//        urlString = urlString+"\"datasetName\":\"LANDSAT_8_C1\",";
//        urlString = urlString+"\"spatialFilter\":{\"filterType\":\"mbr","lowerLeft":{"latitude":"49","longitude":"49"},"upperRight":{"latitude":"49","longitude":"49"}},"
        URL url = new URL (urlString);
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(60000);
        con.setReadTimeout(60000);
        int status = con.getResponseCode();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();
        String output = content.toString();
        int entityIndex = output.indexOf("entityId")+11;
        String result = output.substring(entityIndex,entityIndex+21);


        return result;
    }
    public static double sceneMetadata_CC(String apiKey, String sceneName) throws IOException {
        String urlString = "https://earthexplorer.usgs.gov/inventory/json/v/1.4.0/metadata?jsonRequest={\"apiKey\":\""+apiKey+"\",\"datasetName\":\"LANDSAT_8_C1\",\"entityIds\":\""+sceneName+"\"}";
//        urlString = urlString+"{\"apiKey\":\""+apiKey+"\",";
//        urlString = urlString+"\"datasetName\":\"LANDSAT_8_C1\",";
//        urlString = urlString+"\"spatialFilter\":{\"filterType\":\"mbr","lowerLeft":{"latitude":"49","longitude":"49"},"upperRight":{"latitude":"49","longitude":"49"}},"
        URL url = new URL (urlString);
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(20000);
        con.setReadTimeout(20000);
        int status = con.getResponseCode();
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer content = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        con.disconnect();
        String output = content.toString();
        int entityIndex = output.indexOf("Land Cloud Cover")+119;
        String result = output.substring(entityIndex,entityIndex+6);
        result = result.replaceAll("[^\\d.]", "");
        if(result.equals("")) {
            System.out.println("sadge");
            result="50.0";
        }
        return Double.parseDouble(result);
    }
}
