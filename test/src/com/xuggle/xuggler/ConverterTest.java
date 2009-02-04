/*
 * Copyright (c) 2008-2009 by Xuggle Inc. All rights reserved.
 *
 * It is REQUESTED BUT NOT REQUIRED if you use this library, that you let 
 * us know by sending e-mail to info@xuggle.com telling us briefly how you're
 * using the library and what you like or don't like about it.
 *
 * This library is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 2.1 of the License, or (at your option) any later
 * version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along
 * with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.xuggle.xuggler;

import com.xuggle.xuggler.Converter;
import com.xuggle.xuggler.IVideoResampler;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import junit.framework.TestCase;

public class ConverterTest extends TestCase
{
  private final Logger log = LoggerFactory.getLogger(this.getClass());
  private Converter converter=null;

  @Before
  public void setUp()
  {
    log.debug("Executing test case: {}", this.getName());
  }
  
  @Test
  public void testDefineOptions()
  {
    converter = new Converter();
    
    Options options = converter.defineOptions();
    
    assertTrue("no options", options != null);
  }
  
  @Test
  public void testParseOptions() throws ParseException
  {
    String[] args = new String[]{
        "--containerformat",
        "mov",
        "--acodec",
        "libmp3lame",
        "--asamplerate",
        "22050",
        "--achannels",
        "2",
        "--abitrate",
        "64000",
        "--aquality",
        "2",
        "--vcodec",
        "mpeg4",
        "--vscalefactor",
        "2.5",
        "--vbitrate",
        "300000",
        "--vquality",
        "2",
        "fixtures/testfile.flv",
        "out.flv"
    };
    converter = new Converter();
    
    Options options = converter.defineOptions();

    CommandLine cmdLine = converter.parseOptions(options, args);
    assertTrue("all commandline options successful", cmdLine != null);
  }
  
  @Test
  public void testInvalidOptions() throws ParseException
  {
    String[] args = new String[]{
        "--garbageoption",
        "fixtures/testfile.flv",
        "out.flv"
    };
    converter = new Converter();
    
    Options options = converter.defineOptions();
    
    try {
      converter.parseOptions(options, args);
      // if we get here, we've failed.
      fail("all commandline options successful");
    }
    catch (UnrecognizedOptionException exception)
    {
      // ignore this.
    }
    catch (Error e)
    {
      throw e;
    }
  }

  @Test
  public void testNoInputFile() throws ParseException
  {
    String[] args = new String[]{
        "--vcodec",
        "mpeg4"
    };
    converter = new Converter();
    
    Options options = converter.defineOptions();

    try {
      converter.parseOptions(options, args);
      // if we get here, we've failed.
      fail("all commandline options successful");
    }
    catch (ParseException exception)
    {
      // ignore this.
    }
    catch (Error e)
    {
      throw e;
    }
  
  }
  
  @Test
  public void testNoOutputFile() throws ParseException
  {
    String[] args = new String[]{
        "--vcodec",
        "mpeg4",
        "fixtures/testfile.flv"
    };
    converter = new Converter();
    
    Options options = converter.defineOptions();
  
    try {
      converter.parseOptions(options, args);
      // if we get here, we've failed.
      fail("all commandline options successful");
    }
    catch (ParseException exception)
    {
      // ignore this.
    }
    catch (Error e)
    {
      throw e;
    }
      
  }

  /**
   * This test should fail, but should not crash the JVM.  It's
   * a regression test for:
   * http://code.google.com/p/xuggle/issues/detail?id=18
   * @throws ParseException
   */
  @Test
  public void testWrongPictureFormatConversionRegression18() throws ParseException
  {
    String[] args = new String[]{
        "--acodec",
        "libmp3lame",
        "fixtures/testfile.flv",
        this.getClass().getName() + "_" + this.getName() + ".mov"
    };
    converter = new ConverterWrongPictureFormat();
    
    Options options = converter.defineOptions();

    CommandLine cmdLine = converter.parseOptions(options, args);
    assertTrue("all commandline options successful", cmdLine != null);
    try {
      converter.run(cmdLine);
      fail("should fail with an error");
    } catch (RuntimeException e)
    {
      // if we get here everything was fine.
    }
  }

  class ConverterWrongPictureFormat extends Converter
  {
    @Override
    protected IVideoPicture alterVideoFrame(IVideoPicture picture1)
    {
      // get picture dimentions

      int w = picture1.getWidth();
      int h = picture1.getHeight();

      // make a resampler
      
      IVideoResampler resampleToRgb32 = IVideoResampler.make(
        w, h, IPixelFormat.Type.RGB32, w, h, picture1.getPixelType());

      // resample the picture

      final IVideoPicture picture2 = IVideoPicture.make(
        resampleToRgb32.getOutputPixelFormat(), w, h);
      if (resampleToRgb32.resample(picture2, picture1) < 0)
        throw new RuntimeException("could not resample picture.");
      
      // return picture in rgb32
      
      return picture2;
    }
  }

  
  @Test
  public void testConversion() throws ParseException
  {
    // We do this to determine if this version of Xuggler can
    // support resampling
    boolean testResampling = IVideoResampler.isSupported(IVideoResampler.Feature.FEATURE_IMAGERESCALING);
    
    String[] args = new String[]{
        "--containerformat",
        "mov",
        "--acodec",
        "libmp3lame",
        "--asamplerate",
        "22050",
        "--achannels",
        "2",
        "--abitrate",
        "64000",
        "--aquality",
        "0",
        "--vcodec",
        "mpeg4",
        "--vscalefactor",
        testResampling ? "2.0" : "1.0",
        "--vbitrate",
        "300000",
        "--vbitratetolerance",
        "12000000",
        "--vquality",
        "0",
        "fixtures/testfile.flv",
        this.getClass().getName() + "_" + this.getName() + ".mov"
    };
    converter = new Converter();
    
    Options options = converter.defineOptions();

    CommandLine cmdLine = converter.parseOptions(options, args);
    assertTrue("all commandline options successful", cmdLine != null);
    
    converter.run(cmdLine);
  }

  
  @Test
  public void testConversionH264() throws ParseException
  {
    // We do this to determine if this version of Xuggler can
    // support resampling
    boolean testResampling = IVideoResampler.isSupported(IVideoResampler.Feature.FEATURE_IMAGERESCALING);
    
    String[] args = new String[]{
        "--containerformat",
        "mov",
        "--acodec",
        "libmp3lame",
        "--asamplerate",
        "22050",
        "--achannels",
        "2",
        "--abitrate",
        "64000",
        "--aquality",
        "0",
        "--vcodec",
        "libx264",
        "--vscalefactor",
        testResampling ? "2.0" : "1.0",
        "--vbitrate",
        "300000",
        "--vbitratetolerance",
        "12000000",
        "--vquality",
        "0",
        "fixtures/testfile_videoonly_20sec.flv",
        this.getClass().getName() + "_" + this.getName() + ".mp4"
    };
    
    // check if we have the libx264 encoder
    ICodec codec = ICodec.findEncodingCodecByName("libx264");
    if (codec == null)
      // we don't have libx264 so bail
      return;
    
    converter = new Converter();
    
    Options options = converter.defineOptions();

    CommandLine cmdLine = converter.parseOptions(options, args);
    assertTrue("all commandline options successful", cmdLine != null);
    
    converter.run(cmdLine);
  }

  /**
   * This tests crashing the JVM when IVideoPicture of the wrong format
   * are compressed.
   */

  /**
   * This test attempts to convert a file using the FFMPEG
   * nellymoser encodec.
   *
   * It is currently disabled as the Nellymoser codec appears
   * to override areas of native memory, and hence can crash
   * Valgrind or a JVM.  It's here so once we fix this in FFMPEG
   * we can add this back to our regression suite.
   */
  /*
   * For some reason it appears Java 1.5 ignores the @Ignore...
  @Test
  @Ignore
  public void testConversionNellymoser() throws ParseException
  {
    // We do this to determine if this version of Xuggler can
    // support resampling
    boolean testResampling = IVideoResampler.isSupported();
    String[] args = new String[]{
        "--containerformat",
        "flv",
        "--acodec",
        "nellymoser",
        "--asamplerate",
        "22050",
        "--achannels",
        "1",
        "--abitrate",
        "64000",
        "--aquality",
        "0",
        "--vcodec",
        "flv",
        "--vscalefactor",
        !testResampling  ? "1.0" : "2.0",
        "--vbitrate",
        "300000",
        "--vquality",
        "0",
        "fixtures/testfile.flv",
        this.getClass().getName()+"_"+this.getName()+".flv"
    };
    converter = new Converter();
    
    Options options = converter.defineOptions();

    CommandLine cmdLine = converter.parseOptions(options, args);
    assertTrue("all commandline options successful", cmdLine != null);
    
    converter.run(cmdLine);
  }
  */
  

}
