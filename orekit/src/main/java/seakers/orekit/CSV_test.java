package seakers.orekit;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Double.parseDouble;

public class CSV_test {
    public static void main(String[] args) {
        List<List<String>> records = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("D:/Documents/VASSAR/orekit/orekit/src/main/java/seakers/orekit/repeat_orbits.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                records.add(Arrays.asList(values));
            }
        }
        catch (Exception e) {
            System.out.println(e);
        }
        for(int i = 0; i<records.size(); i++) {
            double alt = parseDouble(records.get(i).get(1));
            double inc = parseDouble(records.get(i).get(4));
            System.out.println(alt+" "+inc);
        }
    }
}
