package com.planimrt.services;

import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;

@Service
public class CollimatorPlotterService {
    // TODO: diagramacion real
    public BufferedImage generatePlot(List<List<Double>> positions, List<Double> times) {
        int size = 300;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, size, size);
        g.setColor(Color.BLUE);
        g.drawLine(0, 0, size, size);
        g.dispose();
        return img;
    }
}
