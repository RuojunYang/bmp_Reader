import javax.swing.*;
import java.awt.*;

public class GraphDecode extends JFrame {

    private int width;
    private int height;
    Decoder decoder;
    long seconds;

    public GraphDecode(Decoder decoder, String name, long seconds) {
        this.decoder = decoder;
        this.seconds = seconds;
        setTitle("Q3 - " + name);
        setSize(decoder.getWidth() + 200, decoder.getHeight() + 200);
        setResizable(false);
        setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new FlowLayout());
    }


    public void paint(Graphics graphics) {
        // convert to 2D graphic to use double value
        Graphics2D graphics2D = (Graphics2D) graphics;
        int[][] matrix = getYUVToRGB();
        drawRawImage(graphics2D, matrix[0], matrix[1], matrix[2]);
        graphics2D.setColor(Color.black);
        graphics2D.drawString("Decompress time: " + seconds + "ms", 100, 50);
    }

    private void drawRawImage(Graphics2D graphics2D, int[] rChannel, int[] gChannel, int[] bChannel) {
        initialWidthAndHeight();
        for (int i = 0; i < decoder.getTempY().length; i++) {
            Color color = new Color(rChannel[i], gChannel[i], bChannel[i]);
            graphics2D.setColor(color);
            graphics2D.drawLine(width + 100, height + 100, width + 100, height + 100);
            nextHeightAndWidth();
        }
    }

    private int[][] getYUVToRGB() {
        int[] newRChannel = new int[decoder.getTempY().length];
        int[] newGChannel = new int[decoder.getTempY().length];
        int[] newBChannel = new int[decoder.getTempY().length];
        for (int i = 0; i < decoder.getTempY().length; i++) {
            double Y = decoder.getTempY()[i];
            double U = decoder.getTempU()[i];
            double V = decoder.getTempV()[i];
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
        width = width + 1;
        if (width >= decoder.getWidth()) {
            width = 0;
            height--;
        }
    }

    private void initialWidthAndHeight() {
        height = decoder.getHeight() - 1;
        width = 0;
    }
}
