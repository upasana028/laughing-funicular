package nl.knaw.huygens.gortester.rewriterules;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import nl.knaw.huygens.gortester.messages.GorOriginalResponse;
import nl.knaw.huygens.gortester.messages.GorReplayedResponse;
import nl.knaw.huygens.gortester.messages.GorRequest;
import javax.json.JsonObject;
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
import java.util.concurrent.ExecutionException;
import org.json.JSONObject;

public class StoreAuthRule implements RewriteRule {

  private final Cache<String, String> auths;

  public StoreAuthRule() {
    auths = CacheBuilder.newBuilder().maximumSize(1000).build();
  }

  @Override
  public void modifyRequestForReplay(GorRequest statement) {
    byte[] body = statement.getBody();
    // String bodyString = new String(body);
    // writeUsingFileWriter(bodyString);
    // JSONObject jsonObj = new JSONObject(bodyString);
    // jsonObj.put("is_checkout","false");
    // String newBody = jsonObj.toString();
    // writeUsingFileWriter(newBody);
    statement.setBody(body);
    
    
  }

  @Override
  public boolean blockReplay(GorRequest statement) {
    return false;
  }

  @Override
  public void handleOriginalResponse(GorRequest request, GorOriginalResponse response) {
  }

  @Override
  public void handleReplayResponse(GorRequest request, GorOriginalResponse originalResponse,
                                   GorReplayedResponse response) {
    if (request.getPath().equals("/v2.1/authenticate")) {
      response.getHeader("Authorization").ifPresent(authHeader ->
        originalResponse.getHeader("Authorization").ifPresent(origHeader ->
          auths.put(origHeader, authHeader)
        )
      );
    }
  }

    private void writeUsingFileWriter(String data) {
      File file = new File("/tmp/FileWriter.txt");
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
