import processing.core.PApplet;

import java.io.File;
import java.time.LocalDate;
import java.util.Arrays;

public class DataReader {

    public static void main(String[] args) {
        readFile("out.csv");
    }

    public static Main.Person[] readFile(String fileName) {
        String[] csv = PApplet.loadStrings(new File(fileName));
        String[] artistNames = csv[0].split(",(?=(?:[^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)");
        Main.Person[] people = new Main.Person[artistNames.length - 1];
        float[][] peopleValues = new float[artistNames.length - 1][csv.length - 1];
        for (int i = 1; i < csv.length; i++) {
            String[] data = csv[i].split(",");
            for (int j = 1; j < data.length; j++) {
                peopleValues[j - 1][i - 1] = Float.parseFloat(data[j]);
            }
        }
        for (int i = 1; i < artistNames.length; i++) {
            people[i - 1] = new Main.Person(peopleValues[i - 1], artistNames[i], csv[1].split(",")[0]);
        }

        return people;
    }

}
