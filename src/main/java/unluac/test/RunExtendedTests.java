package unluac.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RunExtendedTests {

  private static void gatherTests(Path base, Path folder, List<String> files) throws IOException {
    for(Path file : Files.newDirectoryStream(folder, "*.lua")) {
      String relative = base.relativize(file).toString();
      files.add(relative.substring(0, relative.length() - 4));
    }
    for(Path dir : Files.newDirectoryStream(folder)) {
      if(Files.isDirectory(dir)) {
        gatherTests(base, dir, files);
      }
    }
  }
  
  public static void main(String[] args) throws IOException {
    FileSystem fs = FileSystems.getDefault();
    Path luatest = fs.getPath(args[0]);
    TestReport report = new TestReport();
    for(int version = 0x50; version <= 0x54; version++) {
      LuaSpec spec = new LuaSpec(version);
      UnluacSpec uspec = new UnluacSpec();
      System.out.println(spec.id());
      for(Path subfolder : Files.newDirectoryStream(luatest)) {
        if(Files.isDirectory(subfolder) && spec.compatible(subfolder.getFileName().toString())) {
          List<String> files = new ArrayList<String>();
          gatherTests(subfolder, subfolder, files);
          TestSuite suite = new TestSuite(subfolder.getFileName().toString(), subfolder.toString() + File.separator, files.toArray(new String[files.size()]));
          System.out.print("\t" + subfolder.getFileName().toString());
          suite.run(spec, uspec, report);
          System.out.println();
        }
      }
    }
    report.report(System.out);
  }
  
}
