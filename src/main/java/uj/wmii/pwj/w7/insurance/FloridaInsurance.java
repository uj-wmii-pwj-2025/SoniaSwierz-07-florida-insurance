package uj.wmii.pwj.w7.insurance;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

record InsuranceEntry(String county, double tiv2011, double tiv2012) {
    public double getGrowth() {
        return tiv2012 - tiv2011;
    }
}

public class FloridaInsurance {

    private static final String ZIP_FILE_PATH = "FL_insurance.csv.zip";
    private static final String CSV_FILE_NAME = "FL_insurance.csv";

    public static void main(String[] args) {
        try {
            List<InsuranceEntry> data = loadDataFromZip();

            generateCountFile(data);
            generateTiv2012File(data);
            generateMostValuableFile(data);

        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static List<InsuranceEntry> loadDataFromZip() throws IOException {
        try (ZipFile zipFile = new ZipFile(FloridaInsurance.ZIP_FILE_PATH)) {
            ZipEntry entry = zipFile.getEntry(FloridaInsurance.CSV_FILE_NAME);
            if (entry == null)
                throw new FileNotFoundException("no CSV file in ZIP.");

            try (InputStream is = zipFile.getInputStream(entry);
                 BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

                return br.lines()
                        .skip(1)
                        .map(line -> {
                            String[] parts = line.split(",");
                            String county = parts[2];
                            double tiv11 = Double.parseDouble(parts[7]);
                            double tiv12 = Double.parseDouble(parts[8]);

                            return new InsuranceEntry(county, tiv11, tiv12);
                        })
                        .collect(Collectors.toList());
            }
        }
    }

    private static void generateCountFile(List<InsuranceEntry> data) throws IOException {
        long count = data.stream()
                .map(InsuranceEntry::county)
                .distinct()
                .count();

        Files.writeString(Paths.get("count.txt"), String.valueOf(count));
    }

    private static void generateTiv2012File(List<InsuranceEntry> data) throws IOException {
        double total = data.stream()
                .mapToDouble(InsuranceEntry::tiv2012)
                .sum();

        Files.writeString(Paths.get("tiv2012.txt"), String.format(Locale.US, "%.2f", total));
    }

    private static void generateMostValuableFile(List<InsuranceEntry> data) throws IOException {
        String content = data.stream()
                .collect(Collectors.groupingBy(
                        InsuranceEntry::county,
                        Collectors.summingDouble(InsuranceEntry::getGrowth)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .map(entry -> String.format(Locale.US, "%s,%.2f", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("\n"));

        Files.writeString(Paths.get("most_valuable.txt"), "country,value\n" + content);
    }
}