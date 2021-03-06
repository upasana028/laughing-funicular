package nl.knaw.huygens.gortester;

import nl.knaw.huygens.gortester.messages.GorMessage;
import nl.knaw.huygens.gortester.rewriterules.GunzipRule;
import nl.knaw.huygens.gortester.rewriterules.IgnoreDateDifferenceRule;
import nl.knaw.huygens.gortester.rewriterules.IgnoreStaticRule;
import nl.knaw.huygens.gortester.rewriterules.StoreAuthRule;
import nl.knaw.huygens.gortester.rewriterules.UnchunkRule;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;
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

public class App {

  public static void main( String[] args ) throws FileNotFoundException, UnsupportedEncodingException {
    String dirname = "output";
    if (args.length > 0) {
      dirname = args[0];
    }
    Scanner sc = new Scanner(System.in);
    PrintStream out = System.out;

    new File(dirname).mkdirs();
    try (PrintWriter results = new PrintWriter(dirname + File.separator + "log", "UTF-8")) {
      GorTester gorTester = new GorTester(
        dirname,
         new IgnoreStaticRule(),
       //  new StoreAuthRule(),
        new IgnoreDateDifferenceRule()
        // new UnchunkRule(results),
        // new GunzipRule(results)
      );
      while (sc.hasNext()) {
        try {
         
          GorMessage statement = GorMessage.make(sc.nextLine(), results);
          gorTester.handleLine(out, results, statement);
        } catch (Exception e) {
          e.printStackTrace(results);
        } finally {
          results.flush();
        }
      }
    }
  }

  private static void writeUsingFileWriter(String data) {
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
