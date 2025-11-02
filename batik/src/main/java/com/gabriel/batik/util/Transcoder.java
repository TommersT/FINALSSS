package com.gabriel.batik.util;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class Transcoder {
    public static Icon transcode(String svgFile, int width, int height) throws FileNotFoundException, TranscoderException  {
        TranscoderInput input = new TranscoderInput(new FileInputStream("icon.svg"));
        BufferedImageTranscoder transcoder = new BufferedImageTranscoder(width, height);
        transcoder.transcode(input, null);
        BufferedImage image = transcoder.getBufferedImage();
        Icon icon = new ImageIcon(image);
        return icon;
    }
}
