package org.sosy_lab.cpachecker.cfa.export;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import org.sosy_lab.cpachecker.cfa.export.json.CfaJsonExport;
import org.sosy_lab.cpachecker.cfa.export.json.CfaJsonImport;

public class Test {
  public static void main(String[] args) {
    try {
      System.out.println("\nTesting Soundness of JSON CFA:");
    
      Path exportCfaJsonFile = Path.of("output/cfa.json");
      Path childPath = exportCfaJsonFile.getParent().resolve("childCfa.json");
    
      new CfaJsonExport(new CfaJsonImport().read(exportCfaJsonFile).immutableCopy()).write(childPath);
    
      JsonNode mainNode = new CfaJsonImport().readJsonNode(exportCfaJsonFile);
      JsonNode childNode = new CfaJsonImport().readJsonNode(childPath);
    
      boolean soundness = mainNode.equals(childNode);
      boolean equal = areFilesEqual(exportCfaJsonFile, childPath);
      boolean identityInfo =
          propertyOnlyInNode(childNode, "nodes", ".*nodeId.*")
              && propertyOnlyInNode(childNode, "edges", ".*edgeId.*")
              && propertyOnlyInNode(
                  childNode,
                  "partitions",
                  ".*index.*vars.*");
    
      System.out.println(
          statusColor("Soundness\n", soundness)
              + statusColor("File equality\n", equal)
              + statusColor("JsonIdentityInfos\n", identityInfo));
    
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static String statusColor(String message, boolean status) {
    return status ? "\u001B[32m" + message + "\u001B[0m" : "\u001B[31m" + message + "\u001B[0m";
  }

  public static boolean areFilesEqual(Path file1, Path file2) throws IOException {
    try (InputStream is1 = Files.newInputStream(file1);
         InputStream is2 = Files.newInputStream(file2)) {

        byte[] buffer1 = new byte[1024];
        byte[] buffer2 = new byte[1024];

        int bytesRead1, bytesRead2;
        while ((bytesRead1 = is1.read(buffer1)) != -1) {
            bytesRead2 = is2.read(buffer2);
            if (bytesRead1 != bytesRead2 || !Arrays.equals(buffer1, buffer2)) {
                return false;
            }
        }
        return is2.read() == -1; // Ensure file2 has no extra bytes
    }
  }

  public static boolean propertyOnlyInNode(JsonNode node, String expectedInNode, String regex) {
    Iterator<String> fields = node.fieldNames();

    while (fields.hasNext()) {
      String fieldName = fields.next();

      if (!fieldName.equals(expectedInNode) && node.get(fieldName).toString().matches(regex)) {
        System.err.println(
            "Regex " + regex + " found in " + fieldName + ", but expected in " + expectedInNode);
        return false;
      }
    }

    return true;
  }
}
