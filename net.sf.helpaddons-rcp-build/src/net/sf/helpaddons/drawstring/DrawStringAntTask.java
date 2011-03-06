/*******************************************************************************
 * Copyright (c) 2011 Holger Voormann and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Holger Voormann - initial API and implementation
 *******************************************************************************/
package net.sf.helpaddons.drawstring;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.tools.ant.Task;

public class DrawStringAntTask extends Task {

    private static final String ALIGN_LEFT   = "left";
    private static final String ALIGN_CENTER = "center";
    private static final String ALIGN_RIGHT  = "right";

    private File image;

    private File toImage;

    private String string;

    private String fontFamily = "SansSerif";

    private int fontHeight = 28;

    private int x = 0;

    private int y = fontHeight;

    private int r = 0;

    private int g = 0;

    private int b = 0;

    private double scaleX = 1d;

    private double scaleY = 1d;

    private boolean bold = false;

    private boolean italic = false;

    private String align = ALIGN_LEFT;

    public void setImage(File image) {
        this.image = image;
    }

    public void setToImage(File toImage) {
        this.toImage = toImage;
    }

    public void setString(String string) {
        this.string = string;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public void setFontHeight(int fontHeight) {
        this.fontHeight = fontHeight;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setR(int r) {
        this.r = r;
    }

    public void setG(int g) {
        this.g = g;
    }

    public void setB(int b) {
        this.b = b;
    }

    public void setScaleX(double scaleX) {
        this.scaleX = scaleX;
    }

    public void setScaleY(double scaleY) {
        this.scaleY = scaleY;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public void setItalic(boolean italic) {
        this.italic = italic;
    }

    public void setAlign(String align) {
        if (align == null) {
            this.align = ALIGN_LEFT;
        } else if (align.equalsIgnoreCase(ALIGN_CENTER)) {
            this.align = ALIGN_CENTER;
        } else if (align.equalsIgnoreCase(ALIGN_RIGHT)) {
            this.align = ALIGN_RIGHT;
        } else {
            this.align = ALIGN_LEFT;
        }
    }

    public void execute() {
        validate();
        try {
            BufferedImage imageIn = ImageIO.read(image);

            // graphics with antialiasing
            Graphics2D graphics = imageIn.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                      RenderingHints.VALUE_ANTIALIAS_ON);

            // font
            int style = Font.PLAIN;
            if (bold) {
                style |= Font.BOLD;
            }
            if (italic) {
                style |= Font.ITALIC;
            }
            Font font = new Font(fontFamily, style, fontHeight);
            graphics.setFont(font);

            // color
            Color color = new Color(r, g, b);
            graphics.setColor(color);
            graphics.setPaint(color);

            // scaling
            if (scaleX != 1d || scaleY != 1d) {
                graphics.setTransform(
                        AffineTransform.getScaleInstance(scaleX, scaleY));
            }

            // draw String
            int xAligned = (int)(x/scaleX);
            if (align.equals(ALIGN_CENTER)) {
                xAligned -= graphics.getFontMetrics().stringWidth(string) / 2;
            } else if (align.equals(ALIGN_RIGHT)) {
                xAligned -= graphics.getFontMetrics().stringWidth(string);
            }
            graphics.drawString(string, xAligned, (int)(y/scaleY));

            ImageIO.write(imageIn, "BMP", toImage);

          } catch (IOException ie) {
            ie.printStackTrace();
          }

    }

    private void validate() {
        // TODO
    }

}
