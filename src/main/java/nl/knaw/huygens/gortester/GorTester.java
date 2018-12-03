package nl.knaw.huygens.gortester;

import com.google.common.io.Files;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import nl.knaw.huygens.gortester.messages.GorMessage;
import nl.knaw.huygens.gortester.messages.GorOriginalResponse;
import nl.knaw.huygens.gortester.messages.GorReplayedResponse;
import nl.knaw.huygens.gortester.messages.GorRequest;
import nl.knaw.huygens.gortester.rewriterules.RewriteRule;
import java.io.FileWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.core.type.*;

public class GorTester {

  private final Map<String, GorReplayedResponse> fastReplayedResponses;
  private final Map<String, GorOriginalResponse> originalResponses;
  private final Map<String, GorRequest> requests;
  private final String dirname;
  private final RewriteRule[] rewriteRules;

  public GorTester(String dirname, RewriteRule... rewriteRules) throws FileNotFoundException,
      UnsupportedEncodingException {
    this.dirname = dirname;
    this.rewriteRules = rewriteRules;
    fastReplayedResponses = new HashMap<>();
    originalResponses = new HashMap<>();
    requests = new HashMap<>();
  }

  public void handleLine(PrintStream out, PrintWriter results, GorMessage statement) {
    if (statement instanceof GorRequest ||
      statement instanceof GorReplayedResponse ||
      statement instanceof GorOriginalResponse) {
      if (statement instanceof GorRequest) {
        requests.put(statement.getId(), (GorRequest) statement);
        handleRequest(out, results, (GorRequest) statement);
      } else {
        
        out.println(statement.asLine());
        if (statement instanceof GorOriginalResponse) {
         
          handleOrigResponse((GorOriginalResponse) statement, results, requests.get(statement.getId()));
          //the replay response arrived before the original response (your code is getting faster! yeey!)
          if (fastReplayedResponses.containsKey(statement.getId())) {
            handleReplayedResponse(
              results,
              fastReplayedResponses.get(statement.getId()),
              (GorOriginalResponse) statement,
              requests
            );
            fastReplayedResponses.remove(statement.getId());
          } else {
            originalResponses.put(statement.getId(), (GorOriginalResponse) statement);
          }
        } else if (statement instanceof GorReplayedResponse) {
           
          if (originalResponses.containsKey(statement.getId())) {
            handleReplayedResponse(
              results,
              (GorReplayedResponse) statement,
              originalResponses.get(statement.getId()),
              requests
            );
            originalResponses.remove(statement.getId());
          } else {
            fastReplayedResponses.put(statement.getId(), (GorReplayedResponse) statement);
          }
        }
      }
    } else {
      throw new IllegalStateException("Unknown statement type");
    }
  }

  private void handleRequest(PrintStream requests, PrintWriter results, GorRequest statement) {
    for (RewriteRule rewriteRule : rewriteRules) {
      if (rewriteRule.blockReplay(statement)) {
        return;
      }
    }
    for (RewriteRule rewriteRule : rewriteRules) {
      rewriteRule.modifyRequestForReplay(statement);
    }
    requests.println(statement.asLine());
  }

  private void handleOrigResponse(GorOriginalResponse statement, PrintWriter results, GorRequest gorRequest) {
    for (RewriteRule rewriteRule : rewriteRules) {
      rewriteRule.handleOriginalResponse(gorRequest, statement);
    }
  }

  private void handleReplayedResponse(PrintWriter results, GorReplayedResponse statement, GorOriginalResponse
    originalResponse, Map<String, GorRequest> requests) {
    GorRequest request = requests.get(statement.getId());
    for (RewriteRule rewriteRule : rewriteRules) {
      rewriteRule.handleReplayResponse(request, originalResponse, statement);
    }
    requests.remove(statement.getId());
    compare(request, originalResponse, statement, results);
  }

  private void compare(GorRequest request, GorOriginalResponse orig, GorReplayedResponse replay, PrintWriter results) {
    boolean differs = false;
    results.println(replay.getId());
    writeUsingFileWriter(replay.getId());
    int original_status = orig.getStatus();
    int replay_status = replay.getStatus();
    if( original_status == 200 && replay_status == 904)
      {
        results.println("  ignorance is bliss");
         writeUsingFileWriter(" ignorance is bliss");
         return;
      }
      
    if (orig.getStatus() != replay.getStatus()) {
      results.println("  had differing status");
       writeUsingFileWriter("  had differing status" + orig.getStatus() + "--" + replay.getStatus());
      differs = true;
    }
  try{
    if (!Arrays.equals(orig.getBody(), replay.getBody())) {
      results.println("  had differing response bodies");
      writeUsingFileWriter("  had differing response bodies");
      differs = true;
      String leftJson = new String(orig.getBody(),"UTF8");
      String rightJson = new String(replay.getBody(),"UTF8");
       
    ObjectMapper mapper = new ObjectMapper();
    TypeReference<HashMap<String, Object>> type = new TypeReference<HashMap<String, Object>>() {};

    Map<String, Object> leftMap = mapper.readValue(leftJson, type);
    Map<String, Object> rightMap = mapper.readValue(rightJson, type);

      Map<String, Object> leftFlatMap = FlatMapUtil.flatten(leftMap);
      Map<String, Object> rightFlatMap = FlatMapUtil.flatten(rightMap);

      MapDifference<String, Object> difference = Maps.difference(leftFlatMap, rightFlatMap);

      writeUsingFileWriter("Entries only in Original Response\n--------------------------");
      difference.entriesOnlyOnLeft().forEach((key, value) -> writeUsingFileWriter(key + ": " + value));

      writeUsingFileWriter("\n\nEntries only in Replayed Response\n--------------------------");
      difference.entriesOnlyOnRight().forEach((key, value) -> writeUsingFileWriter(key + ": " + value));

      writeUsingFileWriter("\n\nEntries differing\n--------------------------");
      difference.entriesDiffering().forEach((key, value) -> writeUsingFileWriter(key + ": " + value));

    }
  }
  catch(Exception e){}



    // for (Map.Entry<String, String> entry : orig.getHeaderList()) {
    //   Optional<String> replayedHeader = replay.getHeader(entry.getKey());
    //   if (replayedHeader.isPresent()) {
    //     if (!replayedHeader.get().equals(entry.getValue())) {
    //       results.println("  header differs. " + entry.getKey() + ": " + entry.getValue() + " - " +
    //         replayedHeader.get());
    //         writeUsingFileWriter("  header differs. " + entry.getKey() + ": " + entry.getValue() + " - " +
    //         replayedHeader.get());
    //       differs = true;
    //     }
    //   } else {
    //     results.println("  replayed does not contain " + entry.getKey() + ": " + entry.getValue());
    //     writeUsingFileWriter("  replayed does not contain " + entry.getKey() + ": " + entry.getValue());
    //       differs = true;
    //   }
    //   //replayed is allowed to have more headers.
    // }
    if (differs)
     {
      writeToFile(request, orig, replay, results);
    }
  }

  private void writeToFile(GorRequest request, GorOriginalResponse orig, GorReplayedResponse replayed, PrintWriter
    results) {
    try {
      String slash = File.separator;
      Files.write(request.getHttpBlock(results), new File(dirname + slash + request.getId() + "_req.txt"));
      Files.write(orig.getHttpBlock(results), new File(dirname + slash + request.getId() + "_orig.txt"));
      Files.write(replayed.getHttpBlock(results), new File(dirname + slash + request.getId() + "_repl.txt"));
    } catch (IOException e) {
      e.printStackTrace(results);
    }
  }
	
  private void writeUsingFileWriter(String data) {
      File file = new File(this.dirname + "/FileWriter.txt");
      FileWriter fr = null;
      try {
          fr = new FileWriter(file,true);
          fr.write(data + "\n");
      } catch (IOException e) {
          e.printStackTrace();
      }finally{
          //close resources
          try {
              fr.close();
          } catch (IOException e) {
              e.printStackTrace();
          }
      }
  }
  
}
