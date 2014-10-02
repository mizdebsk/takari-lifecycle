/**
 * Copyright (c) 2014 Takari, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package io.takari.maven.plugins.jar;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Enumeration;
import java.util.Properties;

//
// The simplest override of the standard {@link Properties} to suppress the output of a timestamp.
// We want JARs built with the same content to be idempotent. Note that I took this code straight
// out of OpenJDK.
//
public class JarProperties extends Properties {

  private static final long serialVersionUID = 4112578634029874840L;

  public void store(OutputStream out) throws IOException {
    store(out, null);
  }

  @Override
  public void store(OutputStream out, String comments) throws IOException {
    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out, "8859_1"));
    if (comments != null) {
      writeComments(bw, comments);
      bw.newLine();
    }
    synchronized (this) {
      for (Enumeration e = keys(); e.hasMoreElements();) {
        String key = (String) e.nextElement();
        String val = (String) get(key);
        key = saveConvert(key, true, true);
        /*
         * No need to escape embedded and trailing spaces for value, hence pass false to flag.
         */
        val = saveConvert(val, false, true);
        bw.write(key + "=" + val);
        bw.newLine();
      }
    }
    bw.flush();
  }

  private static void writeComments(BufferedWriter bw, String comments) throws IOException {
    bw.write("#");
    int len = comments.length();
    int current = 0;
    int last = 0;
    char[] uu = new char[6];
    uu[0] = '\\';
    uu[1] = 'u';
    while (current < len) {
      char c = comments.charAt(current);
      if (c > '\u00ff' || c == '\n' || c == '\r') {
        if (last != current) bw.write(comments.substring(last, current));
        if (c > '\u00ff') {
          uu[2] = toHex((c >> 12) & 0xf);
          uu[3] = toHex((c >> 8) & 0xf);
          uu[4] = toHex((c >> 4) & 0xf);
          uu[5] = toHex(c & 0xf);
          bw.write(new String(uu));
        } else {
          bw.newLine();
          if (c == '\r' && current != len - 1 && comments.charAt(current + 1) == '\n') {
            current++;
          }
          if (current == len - 1 || (comments.charAt(current + 1) != '#' && comments.charAt(current + 1) != '!')) bw.write("#");
        }
        last = current + 1;
      }
      current++;
    }
    if (last != current) bw.write(comments.substring(last, current));
    bw.newLine();
  }

  /**
   * Convert a nibble to a hex character
   * 
   * @param nibble the nibble to convert.
   */
  private static char toHex(int nibble) {
    return hexDigit[(nibble & 0xF)];
  }

  /** A table of hex digits */
  private static final char[] hexDigit = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

  /*
   * Converts unicodes to encoded &#92;uxxxx and escapes special characters with a preceding slash
   */
  private String saveConvert(String theString, boolean escapeSpace, boolean escapeUnicode) {
    int len = theString.length();
    int bufLen = len * 2;
    if (bufLen < 0) {
      bufLen = Integer.MAX_VALUE;
    }
    StringBuffer outBuffer = new StringBuffer(bufLen);

    for (int x = 0; x < len; x++) {
      char aChar = theString.charAt(x);
      // Handle common case first, selecting largest block that
      // avoids the specials below
      if ((aChar > 61) && (aChar < 127)) {
        if (aChar == '\\') {
          outBuffer.append('\\');
          outBuffer.append('\\');
          continue;
        }
        outBuffer.append(aChar);
        continue;
      }
      switch (aChar) {
        case ' ':
          if (x == 0 || escapeSpace) outBuffer.append('\\');
          outBuffer.append(' ');
          break;
        case '\t':
          outBuffer.append('\\');
          outBuffer.append('t');
          break;
        case '\n':
          outBuffer.append('\\');
          outBuffer.append('n');
          break;
        case '\r':
          outBuffer.append('\\');
          outBuffer.append('r');
          break;
        case '\f':
          outBuffer.append('\\');
          outBuffer.append('f');
          break;
        case '=': // Fall through
        case ':': // Fall through
        case '#': // Fall through
        case '!':
          outBuffer.append('\\');
          outBuffer.append(aChar);
          break;
        default:
          if (((aChar < 0x0020) || (aChar > 0x007e)) & escapeUnicode) {
            outBuffer.append('\\');
            outBuffer.append('u');
            outBuffer.append(toHex((aChar >> 12) & 0xF));
            outBuffer.append(toHex((aChar >> 8) & 0xF));
            outBuffer.append(toHex((aChar >> 4) & 0xF));
            outBuffer.append(toHex(aChar & 0xF));
          } else {
            outBuffer.append(aChar);
          }
      }
    }
    return outBuffer.toString();
  }
}
