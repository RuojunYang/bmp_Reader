import javax.swing.*;
import java.awt.*;

public class Graph extends JFrame {
    private BMPProcessor bmpProcessor;
    private int width;
    private int height;
    Lossy lossy;
    long seconds;
    double ratio;

    public Graph(Lossy lossy, String name, long seconds, double ratio) {
        this.lossy = lossy;
        this.seconds = seconds;
        this.ratio = ratio;
        this.bmpProcessor = lossy.getBmpProcessor();
        setTitle("Q3 - " + name);
        setSize(bmpProcessor.getWidth() * 2 + 200, bmpProcessor.getHeight() + 200);
        setResizable(false);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //setBackground(Color.black);
        setLayout(new FlowLayout());
    }


    public void paint(Graphics graphics) {
        // convert to 2D graphic to use double value
        Graphics2D graphics2D = (Graphics2D) graphics;

        // it is 24 bits pixel size as said in project note, each pixel is 3 byte, each byte is R/G/B
        int[][] RGB = getYUVToRGB(lossy.YChannel, lossy.UChannel, lossy.VChannel);
        int[][] matrix = getYUVToRGB(lossy.tempY, lossy.tempU, lossy.tempV);
        drawRawImage(graphics2D, RGB, matrix);
        graphics2D.setColor(Color.black);
        graphics2D.drawString("Compress time: " + seconds + "ms", 100, 50);
        graphics2D.drawString("Compress ratio: " + ratio, 100, 70);
        double PSNR = getPSNR(matrix);
        graphics2D.drawString("PSNR: " + PSNR, 100, 90);
    }

    private void drawRawImage(Graphics2D graphics2D, int[][] rgb, int[][] matrix) {
        initialWidthAndHeight();
        for (int i = 0; i < lossy.tempY.length; i++) {
            Color color = new Color(rgb[0][i], rgb[1][i], rgb[2][i]);
            graphics2D.setColor(color);
            graphics2D.drawLine(width + 100, height + 100, width + 100, height + 100);
            nextHeightAndWidth();
        }
        initialWidthAndHeight();
        for (int i = 0; i < lossy.tempY.length; i++) {
            Color color = new Color(matrix[0][i], matrix[1][i], matrix[2][i]);
            graphics2D.setColor(color);
            graphics2D.drawLine(width + bmpProcessor.getWidth() + 100, height + 100, width + bmpProcessor.getWidth() + 100, height + 100);
            nextHeightAndWidth();
        }
    }

    private int[][] getYUVToRGB(double[] doubles1, double[] doubles2, double[] doubles3) {
        int[] newRChannel = new int[doubles1.length];
        int[] newGChannel = new int[doubles1.length];
        int[] newBChannel = new int[doubles1.length];
        for (int i = 0; i < doubles1.length; i++) {
            double Y = doubles1[i];
            double U = doubles2[i];
            double V = doubles3[i];
            int R = (int) (Y + 1.13983 * V);
            if (R > 255) {
                R = 255;
            } else if (R < 0) {
                R = 0;
            }
            int G = (int) (Y + -0.39465 * U + -0.58059 * V);
            if (G > 255) {
                G = 255;
            } else if (G < 0) {
                G = 0;
            }
            int B = (int) (Y + 2.03211 * U);
            if (B > 255) {
                B = 255;
            } else if (B < 0) {
                B = 0;
            }
            newRChannel[i] = R;
            newGChannel[i] = G;
            newBChannel[i] = B;
        }
        int[][] matrix = new int[3][];
        matrix[0] = newRChannel;
        matrix[1] = newGChannel;
        matrix[2] = newBChannel;
        return matrix;
    }

    private void nextHeightAndWidth() {
        if (bmpProcessor.isStartAtLeft()) {
            width = width + 1;
            if (width >= bmpProcessor.getWidth()) {
                width = 0;
                height--;
            }
        } else {
            width = width - 1;
            if (width < 0) {
                height++;
                width = bmpProcessor.getWidth() - 1;
            }
        }
    }

    private void initialWidthAndHeight() {
        if (bmpProcessor.isStartAtLeft()) {
            height = bmpProcessor.getHeight() - 1;
            width = 0;
        } else {
            height = 0;
            width = bmpProcessor.getWidth() - 1;
        }
    }

    // https://en.wikipedia.org/wiki/Peak_signal-to-noise_ratio
    private double getPSNR(int[][] matrix) {
        // get RGB
        int[] rChannel = new int[bmpProcessor.getWidth() * bmpProcessor.getHeight()];
        int[] gChannel = new int[bmpProcessor.getWidth() * bmpProcessor.getHeight()];
        int[] bChannel = new int[bmpProcessor.getWidth() * bmpProcessor.getHeight()];
        int index = 0;
        for (int i = 0; i < bmpProcessor.getPixelData().length; i = i + 3) {
            // little endian
            int R = Integer.parseInt(bmpProcessor.getPixelData()[i + 2], 16);
            int G = Integer.parseInt(bmpProcessor.getPixelData()[i + 1], 16);
            int B = Integer.parseInt(bmpProcessor.getPixelData()[i], 16);
            rChannel[index] = R;
            gChannel[index] = G;
            bChannel[index] = B;
            index++;
        }
        double MSER = 0;
        double MSEG = 0;
        double MSEB = 0;
        for (int i = 0; i < rChannel.length; i++) {
                MSER += Math.pow(rChannel[i] - matrix[0][i], 2);
                MSEG += Math.pow(gChannel[i] - matrix[1][i], 2);
                MSEB += Math.pow(bChannel[i] - matrix[2][i], 2);
        }
        MSER = MSER / (bmpProcessor.getWidth() * bmpProcessor.getHeight());
        MSEG = MSEG / (bmpProcessor.getWidth() * bmpProcessor.getHeight());
        MSEB = MSEB / (bmpProcessor.getWidth() * bmpProcessor.getHeight());
        double PSNR = 10 * Math.log10(255 * 255.0 / ((MSER + MSEB + MSEG) / 3.0));
        return PSNR;
    }
}