/*
 Copyright (C) 2001 - 2010 The Software Conservancy as Trustee. All rights reserved.

 Permission is hereby granted, free of charge, to any person obtaining a copy of
 this software and associated documentation files (the "Software"), to deal in the
 Software without restriction, including without limitation the rights to use, copy,
 modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 and to permit persons to whom the Software is furnished to do so, subject to the
 following conditions:

 The above copyright notice and this permission notice shall be included in all 
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 Nothing in this notice shall be deemed to grant any rights to trademarks, copyrights,
 patents, trade secrets or any other intellectual property of the licensor or any
 contributor except as expressly stated herein. No patent license is granted separate
 from the Software, for code that you delete from the Software, or for combinations
 of the Software with other software or hardware.
*/

package org.openadaptor.util.ant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;

/**
 * ant task to check the copyright header on openadaptor files.
 * 
 * @author higginse
 *
 */
public class CopyrightTask extends Task {

  private static final String START_OF_COMMENT = "\\w*\\/\\*.*";

  private static final String END_OF_COMMENT = ".*\\*\\/";

  private static final String START_OF_CODE = "package .*|import .*";

  private List filesets = new ArrayList();

  private File backupDir = new File(".copyright_backup");

  private String copyright;

  private boolean update = false;

  private boolean verbose=false;

  public void addFileset(FileSet set) {
    filesets.add(set);
  }

  /**
   * If true (default) make output more verbose
   * @param verbose
   */
  public void setVerbose (boolean verbose) {
    this.verbose=verbose;
  }

  /**
   * If true, task will update missing or incorrect copyright notices it encounters.
   * <br>
   * Default is <code>false</code>
   * @param update
   */
  public void setUpdate(final boolean update) {
    this.update = update;
  }

  public void setBackupdir(String dirname) {
    backupDir = new File(dirname);
  }

  public void setCopyright(String filename) {
    try {
      copyright = "/*\n" + readInputStreamContents(new FileInputStream(filename)) + "*/\n";
    } catch (IOException e) {
      throw new RuntimeException("IOException, " + e.getMessage(), e);
    }
  }

  public static String readInputStreamContents(InputStream is) {
    StringBuffer sb = new StringBuffer();
    char[] cbuf = new char[1024];
    int len = 0;
    InputStreamReader reader = null;
    try {
      reader = new InputStreamReader(is);
      while ((len = reader.read(cbuf, 0, cbuf.length)) != -1) {
        sb.append(cbuf, 0, len);
      }
    } catch (IOException e) {
      throw new RuntimeException("IOException, " + e.getMessage(), e);
    } finally {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e) {
        }
      }
    }
    return sb.toString();
  }

  private static String trimWhitespace(String s) {
    s = s.replaceAll("/\\*[ \t]+", "/\\*");
    s = s.replaceAll("[ \t]+\\*/", "\\*/");
    s = s.replaceAll("^[ \t]+", "");
    s = s.replaceAll("[ \t]+$", "");
    s = s.replaceAll("\n", "");
    s = s.replaceAll("\r", "");
    s = s.replaceAll("[ \t][ \t]+", "");
    return s;
  }
  public void execute() throws BuildException {

    int processedFiles=0;
    String onelineComment = trimWhitespace(copyright);

    boolean valid = true;
    for (Iterator iter = filesets.iterator(); iter.hasNext();) {
      FileSet fileSet = (FileSet) iter.next();
      DirectoryScanner ds = fileSet.getDirectoryScanner(getProject());
      String[] files = ds.getIncludedFiles();
      for (int i = 0; i < files.length; i++) {
        valid &= execute(fileSet.getDir(getProject()), files[i], onelineComment);
        processedFiles++;
      }
    }

    if (!valid) {
      throw new BuildException("Copyright notices absent or out of date");
    }
    else {
      System.out.println(processedFiles+" files processed successfully for copyright notices");
    }
  }

  protected boolean execute(File dir, String file, String onelineComment) throws BuildException {
    boolean valid=isCopyrightUptodate(dir, file, onelineComment);
    if (valid) {
      if (verbose){
        System.out.println(file+" has valid copyright notice");
      }
    } 
    else {
      System.err.println("Detected invalid copyright notice on "+file);
      if (update) {
        overwriteCopyrightComment(dir, file, copyright);
        System.out.println("updated " + file);
        valid=true;
      }
      else {
        System.out.println("Missing or invalid copyright notice on "+file);
      }
    }
     return valid;
  }

  private void overwriteCopyrightComment(File dir, String filename, String copyright) {

    createBackupDir();

    FileWriter modified = null;
    BufferedReader reader = null;

    File existingFile = new File(dir, filename);
    File newFile = new File(dir, filename + ".new");

    try {
      modified = new FileWriter(newFile);
      reader = new BufferedReader(new FileReader(existingFile));
      String line;

      // write copyright comment
      modified.write(copyright + "\n");

      // discard existing comment
      boolean inComment = false;
      while ((line = reader.readLine()) != null) {
        if (inComment) {
          inComment = !Pattern.matches(END_OF_COMMENT, line);
        } else if (Pattern.matches(START_OF_COMMENT, line)) {
          inComment = true;
        } else if (Pattern.matches(START_OF_CODE, line)) {
          modified.write(line + "\n");
          break;
        }
      }

      // write remainder
      while ((line = reader.readLine()) != null) {
        modified.write(line + "\n");
      }

      closeNoThrow(modified);
      closeNoThrow(reader);

      // swap files
      File backFile = new File(backupDir, filename);
      backFile.getParentFile().mkdirs();
      existingFile.renameTo(new File(backupDir, filename));
      (new File(dir, filename)).delete();
      newFile.renameTo(new File(dir, filename));

    } catch (IOException e) {
      throw new BuildException("IOEXception, " + e.getMessage(), e);
    } finally {
      closeNoThrow(modified);
      closeNoThrow(reader);
    }
  }

  private void createBackupDir() {
    if (!backupDir.exists()) {
      if (backupDir.mkdir()) {
        System.out.println("created backup dir " + backupDir.getAbsolutePath());
      } else {
        throw new BuildException("failed to create backup dir");
      }
    }
  }

  private void closeNoThrow(FileWriter writer) {
    if (writer != null) {
      try {
        writer.close();
      } catch (IOException e) {
      }
    }
  }

  private void closeNoThrow(BufferedReader reader) {
    if (reader != null) {
      try {
        reader.close();
      } catch (IOException e) {
      }
    }
  }

  private boolean isCopyrightUptodate(File dir, String file, String copyright) {
    StringBuffer comment = new StringBuffer();
    BufferedReader reader = null;
    boolean inComment = false;
    try {
      String line;
      reader = new BufferedReader(new FileReader(new File(dir, file)));
      while ((line = reader.readLine()) != null) {
        if (inComment) {
          comment.append(line + "\n");
          inComment = !Pattern.matches(END_OF_COMMENT, line);
        } else if (Pattern.matches(START_OF_COMMENT, line)) {
          comment.append(line + "\n");
          inComment = true;
        } else if (Pattern.matches(START_OF_CODE, line)) {
          break;
        }
      }
    } catch (IOException e) {
      throw new BuildException("IOException, " + e.getMessage(), e);
    } finally {
      closeNoThrow(reader);
    }
    String onelineComment = trimWhitespace(comment.toString());

    if (onelineComment.equals(copyright)) {
      return true;
    } else {
//    System.err.println(onelineComment);
//    System.err.println(copyright);
      return false;
    }
  }
}
