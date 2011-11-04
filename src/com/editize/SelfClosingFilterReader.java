package com.editize;

import java.io.FilterReader;
import java.io.Reader;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.StringReader;

/**
 * Filters a stream of HTML, replacing XHTML-compliant self-closing tags
 * with HTML-compliant simple tags.
 */

public class SelfClosingFilterReader extends FilterReader
{
  boolean marked, inTag, markInTag;
  int peekedChar, markPeekedChar;

  public SelfClosingFilterReader(Reader in)
  {
    super(in);

    // Because there is a lot of character-by-character reading in this class,
    // make sure the Reader is buffered to improve efficiency.
    in = in instanceof BufferedReader || in instanceof StringReader ? in :
        new BufferedReader(in);

    init();
  }

  private void init()
  {
    peekedChar = markPeekedChar = -1;
    marked = inTag = markInTag = false;
  }

  public void close() throws IOException
  {
    synchronized (lock)
    {
      super.close();
      init();
    }
  }

  public void mark(int readAheadLimit) throws IOException
  {
    synchronized (lock)
    {
      super.mark(readAheadLimit);
      marked = true;
      markPeekedChar = peekedChar;
      markInTag = inTag;
    }
  }

  public int read() throws IOException
  {
    synchronized (lock)
    {
      // Get character, either from underlying Reader or from previously
      // peeked character cache.
      int ch = peekedChar < 0 ? super.read() : peekedChar;
      peekedChar = -1;

      // Spot self-closing tag markers (/>) and replace with normal tag end (>)
      if (inTag && ch == '/')
      {
        peekedChar = super.read();
        if (peekedChar == '>')
        {
          ch = peekedChar;
          peekedChar = -1;
        }
      }

      // Spot tag ends
      if (inTag && ch == '>')
      {
        inTag = false;
      }

      // Spot tag starts
      if (!inTag && ch == '<')
      {
        inTag = true;
      }

      return ch;
    }
  }

  public int read(char[] cbuf, int off, int len)
      throws IOException
  {
    synchronized (lock)
    {
      if ((off < 0) || (off > cbuf.length) || (len < 0) ||
          ((off + len) > cbuf.length) || ((off + len) < 0)) {
          throw new IndexOutOfBoundsException();
      } else if (len == 0) {
          return 0;
      }

      int i;
      for (i=0; i<len; i++)
      {
        int ch = read();
        if (i==0 && ch < 0) return -1; // No characters read and end of stream!
        if (ch < 0) break; // End of stream
        cbuf[off+i] = (char)ch;
      }

      return i;
    }
  }

  public boolean ready()
      throws IOException
  {
    if (peekedChar >= 0) return true;
    else return super.ready();
  }

  public void reset()
      throws IOException
  {
    synchronized (lock)
    {
      super.reset();
      if (marked)
      {
        marked = false;
        inTag = markInTag;
        peekedChar = markPeekedChar;
      }
    }
  }

  public long skip(long n)
      throws IOException
  {
    synchronized (lock)
    {
      long i;
      for (i=0; i<n; i++)
      {
        if (read() < 0) break;
      }
      return i;
    }
  }
}