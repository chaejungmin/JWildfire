/*
  JWildfire - an image and animation processor written in Java 
  Copyright (C) 1995-2011 Andreas Maschke

  This is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser 
  General Public License as published by the Free Software Foundation; either version 2.1 of the 
  License, or (at your option) any later version.
 
  This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without 
  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License along with this software; 
  if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jwildfire.create.tina.swing;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import org.jwildfire.base.Prefs;
import org.jwildfire.create.tina.base.Flame;
import org.jwildfire.create.tina.io.Flam3Reader;
import org.jwildfire.create.tina.io.Flam3Writer;
import org.jwildfire.create.tina.randomflame.AllRandomFlameGenerator;
import org.jwildfire.create.tina.randomflame.RandomFlameGeneratorSampler;
import org.jwildfire.create.tina.render.FlameRenderThread;
import org.jwildfire.create.tina.render.FlameRenderer;
import org.jwildfire.create.tina.render.IterationObserver;
import org.jwildfire.create.tina.render.RenderInfo;
import org.jwildfire.image.SimpleImage;
import org.jwildfire.swing.ErrorHandler;
import org.jwildfire.swing.ImagePanel;

public class TinaInteractiveRendererController implements IterationObserver {
  private enum State {
    IDLE, RENDER
  }

  private final TinaController parentCtrl;
  private final Prefs prefs;
  private final ErrorHandler errorHandler;
  private final JButton loadFlameButton;
  private final JButton fromClipboardButton;
  private final JButton clearScreenButton;
  private final JButton nextButton;
  private final JButton stopButton;
  private final JButton toClipboardButton;
  private final JButton saveImageButton;
  private final JButton saveFlameButton;
  private final JComboBox randomStyleCmb;

  private final JPanel imageRootPanel;
  private JScrollPane imageScrollPane;
  private final JTextArea statsTextArea;
  private SimpleImage image;
  private Flame currFlame;
  List<FlameRenderThread> threads;
  private State state = State.IDLE;

  public TinaInteractiveRendererController(TinaController pParentCtrl, ErrorHandler pErrorHandler, Prefs pPrefs,
      JButton pLoadFlameButton, JButton pFromClipboardButton, JButton pClearScreenButton, JButton pNextButton,
      JButton pStopButton, JButton pToClipboardButton, JButton pSaveImageButton, JButton pSaveFlameButton,
      JComboBox pRandomStyleCmb, JPanel pImagePanel, JTextArea pStatsTextArea) {
    parentCtrl = pParentCtrl;
    prefs = pPrefs;
    errorHandler = pErrorHandler;

    loadFlameButton = pLoadFlameButton;
    fromClipboardButton = pFromClipboardButton;
    clearScreenButton = pClearScreenButton;
    nextButton = pNextButton;
    stopButton = pStopButton;
    toClipboardButton = pToClipboardButton;
    saveImageButton = pSaveImageButton;
    saveFlameButton = pSaveFlameButton;
    randomStyleCmb = pRandomStyleCmb;

    imageRootPanel = pImagePanel;
    refreshImagePanel();
    statsTextArea = pStatsTextArea;
    state = State.IDLE;
    genRandomFlame();
    enableControls();
  }

  private void refreshImagePanel() {
    if (imageScrollPane != null) {
      imageRootPanel.remove(imageScrollPane);
      imageScrollPane = null;
    }

    int width = prefs.getTinaRenderImageWidth() / 2;
    int height = prefs.getTinaRenderImageHeight() / 2;
    image = new SimpleImage(width, height);
    image.fillBackground(prefs.getTinaRandomBatchBGColorRed(), prefs.getTinaRandomBatchBGColorGreen(), prefs.getTinaRandomBatchBGColorBlue());
    ImagePanel imagePanel = new ImagePanel(image, 0, 0, image.getImageWidth());
    imagePanel.setSize(image.getImageWidth(), image.getImageHeight());

    imageScrollPane = new JScrollPane(imagePanel);
    imageScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    imageScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

    imageRootPanel.add(imageScrollPane, BorderLayout.CENTER);

    //    imageRootPanel.getParent().validate();
  }

  public void enableControls() {
    saveImageButton.setEnabled(image != null);
    stopButton.setEnabled(state == State.RENDER);
  }

  public void genRandomFlame() {
    final int IMG_WIDTH = 80;
    final int IMG_HEIGHT = 60;
    final int PALETTE_SIZE = 11;
    RandomFlameGeneratorSampler sampler = new RandomFlameGeneratorSampler(IMG_WIDTH, IMG_HEIGHT, prefs, new AllRandomFlameGenerator(), false, false, PALETTE_SIZE);
    currFlame = sampler.createSample().getFlame();
  }

  public void fromClipboardButton_clicked() {
    Flame newFlame = null;
    try {
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      Transferable clipData = clipboard.getContents(clipboard);
      if (clipData != null) {
        if (clipData.isDataFlavorSupported(DataFlavor.stringFlavor)) {
          String xml = (String) (clipData.getTransferData(
              DataFlavor.stringFlavor));
          List<Flame> flames = new Flam3Reader().readFlamesfromXML(xml);
          if (flames.size() > 0) {
            newFlame = flames.get(0);
          }
        }
      }
      if (newFlame == null) {
        throw new Exception("There is currently no valid flame in the clipboard");
      }
      else {
        currFlame = newFlame;
        cancelRender();
        renderButton_clicked();
        enableControls();
      }
    }
    catch (Throwable ex) {
      errorHandler.handleError(ex);
    }

  }

  public void loadFlameButton_clicked() {
    try {
      JFileChooser chooser = new FlameFileChooser(prefs);
      if (prefs.getInputFlamePath() != null) {
        try {
          chooser.setCurrentDirectory(new File(prefs.getInputFlamePath()));
        }
        catch (Exception ex) {
          ex.printStackTrace();
        }
      }
      if (chooser.showOpenDialog(imageRootPanel) == JFileChooser.APPROVE_OPTION) {
        File file = chooser.getSelectedFile();
        List<Flame> flames = new Flam3Reader().readFlames(file.getAbsolutePath());
        Flame newFlame = flames.get(0);
        prefs.setLastInputFlameFile(file);
        currFlame = newFlame;
        cancelRender();
        renderButton_clicked();
        enableControls();
      }
    }
    catch (Throwable ex) {
      errorHandler.handleError(ex);
    }
  }

  public void renderButton_clicked() {
    int width = prefs.getTinaRenderImageWidth() / 2;
    int height = prefs.getTinaRenderImageHeight() / 2;
    RenderInfo info = new RenderInfo(width, height);
    Flame flame = getCurrFlame();
    double wScl = (double) info.getImageWidth() / (double) flame.getWidth();
    double hScl = (double) info.getImageHeight() / (double) flame.getHeight();
    flame.setPixelsPerUnit((wScl + hScl) * 0.5 * flame.getPixelsPerUnit());
    flame.setWidth(info.getImageWidth());
    flame.setHeight(info.getImageHeight());
    boolean renderHDR = prefs.isTinaRenderNormalHDR();
    info.setRenderHDR(renderHDR);
    boolean renderHDRIntensityMap = prefs.isTinaRenderNormalHDRIntensityMap();
    info.setRenderHDRIntensityMap(renderHDRIntensityMap);
    double oldFilterRadius = flame.getSpatialFilterRadius();
    try {
      flame.setSpatialFilterRadius(prefs.getTinaRenderNormalSpatialOversample());
      FlameRenderer renderer = new FlameRenderer(flame, prefs);
      renderer.registerIterationObserver(this);
      threads = renderer.startRenderFlame(info);

      //      RenderedFlame res = renderer.renderFlame(info);
      //      new ImageWriter().saveImage(res.getImage(), file.getAbsolutePath());
      //      if (res.getHDRImage() != null) {
      //        new ImageWriter().saveImage(res.getHDRImage(), file.getAbsolutePath() + ".hdr");
      //      }
      //      if (res.getHDRIntensityMap() != null) {
      //        new ImageWriter().saveImage(res.getHDRIntensityMap(), file.getAbsolutePath() + ".intensity.hdr");
      //      }
    }
    finally {
      flame.setSpatialFilterRadius(oldFilterRadius);
    }

    state = State.RENDER;
    enableControls();
  }

  public void stopButton_clicked() {
    cancelRender();
    enableControls();
  }

  private void cancelRender() {
    if (state == State.RENDER) {
      while (true) {
        boolean done = true;
        for (FlameRenderThread thread : threads) {
          if (!thread.isFinished()) {
            done = false;
            thread.cancel();
            try {
              Thread.sleep(1);
            }
            catch (InterruptedException e) {
              e.printStackTrace();
            }
            break;
          }
        }
        if (done) {
          break;
        }
      }
      state = State.IDLE;
    }
  }

  public void saveImageButton_clicked() {
    // TODO Auto-generated method stub

  }

  public Flame getCurrFlame() {
    return currFlame;
  }

  // TODO stats
  private long smpl = 0;

  @Override
  public void notifyIterationFinished(FlameRenderThread pEventSource, int pX, int pY) {
    int x = pX;
    int y = pY;
    if (x >= 0 && x < image.getImageWidth() && y >= 0 && y < image.getImageHeight()) {
      image.setARGB(x, y, pEventSource.getTonemapper().tonemapSample(pX, pY));
      if (smpl++ % 1000 == 0) {
        imageRootPanel.repaint();
      }
    }
  }

  public void nextButton_clicked() {
    cancelRender();
    genRandomFlame();
    renderButton_clicked();
    enableControls();
  }

  public void clearScreenButton_clicked() {
    image.fillBackground(prefs.getTinaRandomBatchBGColorRed(), prefs.getTinaRandomBatchBGColorGreen(), prefs.getTinaRandomBatchBGColorBlue());
    imageRootPanel.repaint();
  }

  public void saveFlameButton_clicked() {
    try {
      Flame currFlame = getCurrFlame();
      if (currFlame != null) {
        JFileChooser chooser = new FlameFileChooser(prefs);
        if (prefs.getOutputFlamePath() != null) {
          try {
            chooser.setCurrentDirectory(new File(prefs.getOutputFlamePath()));
          }
          catch (Exception ex) {
            ex.printStackTrace();
          }
        }
        if (chooser.showSaveDialog(imageRootPanel) == JFileChooser.APPROVE_OPTION) {
          File file = chooser.getSelectedFile();
          new Flam3Writer().writeFlame(currFlame, file.getAbsolutePath());
          prefs.setLastOutputFlameFile(file);
        }
      }
    }
    catch (Throwable ex) {
      errorHandler.handleError(ex);
    }
  }

  public void toClipboardButton_clicked() {
    try {
      Flame currFlame = getCurrFlame();
      if (currFlame != null) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        String xml = new Flam3Writer().getFlameXML(currFlame);
        StringSelection data = new StringSelection(xml);
        clipboard.setContents(data, data);
      }
    }
    catch (Throwable ex) {
      errorHandler.handleError(ex);
    }
  }

}