import TreeModule.HuffmanTree;
import TreeModule.Tree;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class Lossy {
    private BMPProcessor bmpProcessor;
    private int[] rChannel;
    private int[] gChannel;
    private int[] bChannel;
    double[] YChannel;
    double[] UChannel;
    double[] VChannel;
    private int width;
    private int height;
    private int[] RGBData;
    private long rawSize;
    private HuffmanTree huffmanTree;
    private Tree tree;
    private String output;
    private double oneWordHuffRate;
    private double[][] YMatrix;
    private double[][] UMatrix;
    private double[][] VMatrix;
    private int[] ZigZagTable;
    private int[] UnZigZagTable;
    double[] tempY;
    double[] tempU;
    double[] tempV;
    int size;

    public Lossy(BMPProcessor bmpProcessor, long rawSize) {
        this.bmpProcessor = bmpProcessor;
        getRGB();
        getYUV();
        this.rawSize = rawSize;
        buildYUVMatrix();
        //https://my.oschina.net/tigerBin/blog/1083549
        ZigZagTable = new int[]
                {
                        0, 1, 8, 16, 9, 2, 3, 10,
                        17, 24, 32, 25, 18, 11, 4, 5,
                        12, 19, 26, 33, 40, 48, 41, 34,
                        27, 20, 13, 6, 7, 14, 21, 28,
                        35, 42, 49, 56, 57, 50, 43, 36,
                        29, 22, 15, 23, 30, 37, 44, 51,
                        58, 59, 52, 45, 38, 31, 39, 46,
                        53, 60, 61, 54, 47, 55, 62, 63
                };
        UnZigZagTable = new int[]
                {
                        0, 1, 5, 6, 14, 15, 27, 28,
                        2, 4, 7, 13, 16, 26, 29, 42,
                        3, 8, 12, 17, 25, 30, 41, 43,
                        9, 11, 18, 24, 31, 40, 44, 53,
                        10, 19, 23, 32, 39, 45, 52, 54,
                        20, 22, 33, 38, 46, 41, 55, 60,
                        21, 34, 37, 47, 50, 56, 59, 61,
                        35, 36, 48, 49, 57, 58, 62, 63
                };
    }

    private void buildYUVMatrix() {
        //4:4:4 YUV sampling rate
        YMatrix = new double[bmpProcessor.getHeight()][bmpProcessor.getWidth()];
        UMatrix = new double[bmpProcessor.getHeight()][bmpProcessor.getWidth()];
        VMatrix = new double[bmpProcessor.getHeight()][bmpProcessor.getWidth()];
        for (int i = 0; i < bmpProcessor.getHeight(); i++) {
            for (int j = 0; j < bmpProcessor.getWidth(); j++) {
                YMatrix[i][j] = YChannel[i * bmpProcessor.getWidth() + j];
                UMatrix[i][j] = UChannel[i * bmpProcessor.getWidth() + j];
                VMatrix[i][j] = VChannel[i * bmpProcessor.getWidth() + j];
            }
        }
    }

    public void compress(String name) throws IOException {
        // DCT
        double[][] DCT = new double[8][8];
        for (int x = 0; x < 8; x++) {
            double a;
            if (x == 0) {
                a = Math.sqrt(1.0 / 8.0);
            } else {
                a = Math.sqrt(2.0 / 8.0);
            }
            for (int y = 0; y < 8; y++) {
                // ppt 6 page 31
                DCT[x][y] = a * Math.cos((2 * y + 1) * x * Math.PI / 16);
            }
        }
        double[][] newYMatrix = new double[bmpProcessor.getHeight()][bmpProcessor.getWidth()];
        double[][] newUMatrix = new double[bmpProcessor.getHeight()][bmpProcessor.getWidth()];
        double[][] newVMatrix = new double[bmpProcessor.getHeight()][bmpProcessor.getWidth()];
        double[][] transpose = new double[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                transpose[i][j] = DCT[j][i];
            }
        }
        for (int x = 0; x < YMatrix.length / 8; x++) {
            for (int y = 0; y < YMatrix[0].length / 8; y++) {
                double[][] inputY = new double[8][8];
                double[][] inputU = new double[8][8];
                double[][] inputV = new double[8][8];
                for (int i = 0; i < 8; i++) {
                    for (int j = 0; j < 8; j++) {
                        inputY[i][j] = YMatrix[x * 8 + i][y * 8 + j];
                        inputU[i][j] = UMatrix[x * 8 + i][y * 8 + j];
                        inputV[i][j] = VMatrix[x * 8 + i][y * 8 + j];
                    }
                }
                double[][] resultY = multiplyMatrices(multiplyMatrices(DCT, inputY), transpose);
                double[][] resultU = multiplyMatrices(multiplyMatrices(DCT, inputU), transpose);
                double[][] resultV = multiplyMatrices(multiplyMatrices(DCT, inputV), transpose);
                for (int i = 0; i < 8; i++) {
                    for (int j = 0; j < 8; j++) {
                        newYMatrix[x * 8 + i][y * 8 + j] = resultY[i][j];
                        newUMatrix[x * 8 + i][y * 8 + j] = resultU[i][j];
                        newVMatrix[x * 8 + i][y * 8 + j] = resultV[i][j];
                    }
                }
            }
        }
        for (int x = 0; x < YMatrix.length; x++) {
            System.arraycopy(newYMatrix[x], 0, YMatrix[x], 0, YMatrix[0].length);
            System.arraycopy(newUMatrix[x], 0, UMatrix[x], 0, UMatrix[0].length);
            System.arraycopy(newVMatrix[x], 0, VMatrix[x], 0, VMatrix[0].length);
        }

        // end of DCT

        // Quantization
        // ppt 8 page 11
        int[][] Q = new int[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (i < 2 && j < 2) {
                    Q[i][j] = 1;
                } else if (i < 3 && j < 3) {
                    Q[i][j] = 2;
                } else if (i < 4 && j < 4) {
                    Q[i][j] = 4;
                } else if (i < 5 && j < 5) {
                    Q[i][j] = 8;
                } else if (i < 6 && j < 6) {
                    Q[i][j] = 16;
                } else if (i < 7 && j < 7) {
                    Q[i][j] = 32;
                } else {
                    Q[i][j] = 64;
                }
            }
        }
        for (int i = 0; i < YMatrix.length / 8; i++) {
            for (int j = 0; j < YMatrix[0].length / 8; j++) {
                for (int x = 0; x < 8; x++) {
                    for (int y = 0; y < 8; y++) {
                        YMatrix[i * 8 + x][j * 8 + y] = Math.round(YMatrix[i * 8 + x][j * 8 + y] / Q[x][y]);
                        UMatrix[i * 8 + x][j * 8 + y] = Math.round(UMatrix[i * 8 + x][j * 8 + y] / Q[x][y]);
                        VMatrix[i * 8 + x][j * 8 + y] = Math.round(VMatrix[i * 8 + x][j * 8 + y] / Q[x][y]);
                    }
                }
            }
        }
        double[][] oneDYMatrix = new double[rChannel.length / 64][64];
        double[][] oneDUMatrix = new double[rChannel.length / 64][64];
        double[][] oneDVMatrix = new double[rChannel.length / 64][64];
        int index = 0;
        for (int i = 0; i < YMatrix.length / 8; i++) {
            for (int j = 0; j < YMatrix[0].length / 8; j++) {
                for (int x = 0; x < 8; x++) {
                    for (int y = 0; y < 8; y++) {
                        oneDYMatrix[index][x * 8 + y] = YMatrix[i * 8 + x][j * 8 + y];
                        oneDUMatrix[index][x * 8 + y] = UMatrix[i * 8 + x][j * 8 + y];
                        oneDVMatrix[index][x * 8 + y] = VMatrix[i * 8 + x][j * 8 + y];
                    }
                }
                index++;
            }
        }
        // Quantization end
        // zigzag
        // https://my.oschina.net/tigerBin/blog/1083549
        // in zigzag order
        for (int i = 0; i < oneDYMatrix.length; i++) {
            oneDYMatrix[i] = ZigZag(oneDYMatrix[i]);
            oneDUMatrix[i] = ZigZag(oneDUMatrix[i]);
            oneDVMatrix[i] = ZigZag(oneDVMatrix[i]);
        }

        // Runlength coding
        int count = 0;
        List<Integer> YList = new ArrayList<>();
        for (int i = 0; i < oneDYMatrix.length; i++) {
            for (int j = 0; j < 64; j++) {
                if (oneDYMatrix[i][j] == 0.0) {
                    count++;
                } else {
                    YList.add(count);
                    YList.add((int) Math.round(oneDYMatrix[i][j]));
                    count = 0;
                }
            }
        }
        count = 0;
        List<Integer> UList = new ArrayList<>();
        for (int i = 0; i < oneDYMatrix.length; i++) {
            for (int j = 0; j < 64; j++) {
                if (oneDUMatrix[i][j] == 0) {
                    count++;
                } else {
                    UList.add(count);
                    UList.add((int) Math.round(oneDUMatrix[i][j]));
                    count = 0;
                }
            }
        }
        count = 0;
        List<Integer> VList = new ArrayList<>();
        for (int i = 0; i < oneDYMatrix.length; i++) {
            for (int j = 0; j < 64; j++) {
                if (oneDVMatrix[i][j] == 0) {
                    count++;
                } else {
                    VList.add(count);
                    VList.add((int) Math.round(oneDVMatrix[i][j]));
                    count = 0;
                }
            }
        }

        int[] YInt = new int[YList.size()];
        int[] UInt = new int[UList.size()];
        int[] VInt = new int[VList.size()];
        for (int i = 0; i < YList.size(); i++) {
            YInt[i] = YList.get(i);
        }
        for (int i = 0; i < UList.size(); i++) {
            UInt[i] = UList.get(i);
        }
        for (int i = 0; i < VList.size(); i++) {
            VInt[i] = VList.get(i);
        }

        HuffmanTree YHuffmanTree = Huffman(YInt);
        HuffmanTree UHuffmanTree = Huffman(UInt);
        HuffmanTree VHuffmanTree = Huffman(VInt);
        writeTo(name, YHuffmanTree, UHuffmanTree, VHuffmanTree);

        // inverse transform
        for (int i = 0; i < oneDYMatrix.length; i++) {
            oneDYMatrix[i] = UnZigZag(oneDYMatrix[i]);
            oneDUMatrix[i] = UnZigZag(oneDUMatrix[i]);
            oneDVMatrix[i] = UnZigZag(oneDVMatrix[i]);
        }
        index = 0;
        for (int i = 0; i < YMatrix.length / 8; i++) {
            for (int j = 0; j < YMatrix[0].length / 8; j++) {
                for (int x = 0; x < 8; x++) {
                    for (int y = 0; y < 8; y++) {
                        YMatrix[i * 8 + x][j * 8 + y] = oneDYMatrix[index][x * 8 + y];
                        UMatrix[i * 8 + x][j * 8 + y] = oneDUMatrix[index][x * 8 + y];
                        VMatrix[i * 8 + x][j * 8 + y] = oneDVMatrix[index][x * 8 + y];
                    }
                }
                index++;
            }
        }
        newYMatrix = new double[bmpProcessor.getHeight()][bmpProcessor.getWidth()];
        newUMatrix = new double[bmpProcessor.getHeight()][bmpProcessor.getWidth()];
        newVMatrix = new double[bmpProcessor.getHeight()][bmpProcessor.getWidth()];
        for (int x = 0; x < YMatrix.length / 8; x++) {
            for (int y = 0; y < YMatrix[0].length / 8; y++) {
                double[][] inputY = new double[8][8];
                double[][] inputU = new double[8][8];
                double[][] inputV = new double[8][8];
                for (int i = 0; i < 8; i++) {
                    for (int j = 0; j < 8; j++) {
                        inputY[i][j] = YMatrix[x * 8 + i][y * 8 + j];
                        inputU[i][j] = UMatrix[x * 8 + i][y * 8 + j];
                        inputV[i][j] = VMatrix[x * 8 + i][y * 8 + j];
                    }
                }
                double[][] resultY = multiplyMatrices(multiplyMatrices(transpose, inputY), DCT);
                double[][] resultU = multiplyMatrices(multiplyMatrices(transpose, inputU), DCT);
                double[][] resultV = multiplyMatrices(multiplyMatrices(transpose, inputV), DCT);
                for (int i = 0; i < 8; i++) {
                    for (int j = 0; j < 8; j++) {
                        newYMatrix[x * 8 + i][y * 8 + j] = resultY[i][j];
                        newUMatrix[x * 8 + i][y * 8 + j] = resultU[i][j];
                        newVMatrix[x * 8 + i][y * 8 + j] = resultV[i][j];
                    }
                }
            }
        }
        for (int x = 0; x < YMatrix.length; x++) {
            System.arraycopy(newYMatrix[x], 0, YMatrix[x], 0, YMatrix[0].length);
            System.arraycopy(newUMatrix[x], 0, UMatrix[x], 0, UMatrix[0].length);
            System.arraycopy(newVMatrix[x], 0, VMatrix[x], 0, VMatrix[0].length);
        }
        tempY = new double[YMatrix.length * YMatrix[0].length];
        tempU = new double[YMatrix.length * YMatrix[0].length];
        tempV = new double[YMatrix.length * YMatrix[0].length];
        index = 0;
        for (int x = 0; x < YMatrix.length; x++) {
            for (int y = 0; y < YMatrix[0].length; y++) {
                tempY[index] = YMatrix[x][y];
                tempU[index] = UMatrix[x][y];
                tempV[index] = VMatrix[x][y];
                index++;
            }
        }
        // inverse end
    }

    public void writeTo(String name, HuffmanTree YHuffmanTree, HuffmanTree UHuffmanTree, HuffmanTree VHuffmanTree) {
        // find length of data
        // Y header
        int YDataLen = YHuffmanTree.getOutput().length() / 8;
        int YDataFillZero = 8 - YHuffmanTree.getOutput().length() + YDataLen * 8;
        if (YDataFillZero == 8) {
            YDataFillZero = 0;
        }
        if (YHuffmanTree.getOutput().length() % 8 > 0) {
            YDataLen++;
        }
        int YTreeLen = YHuffmanTree.getTree().getTreeString().length() / 8;
        int YTreeFillZero = 8 - YHuffmanTree.getTree().getTreeString().length() + YTreeLen * 8;
        if (YTreeFillZero == 8) {
            YTreeFillZero = 0;
        }
        if (YHuffmanTree.getTree().getTreeString().length() % 8 > 0) {
            YTreeLen++;
        }
        int YKeyLen = YHuffmanTree.getTree().dataList().size() * 2;

        // initial byte array as header to tell the data size
        byte[] YDataByte = new byte[3];
        int temp = YDataLen / 256 / 256;
        YDataByte[0] = Integer.valueOf(temp - 128).byteValue();
        YDataByte[1] = Integer.valueOf(YDataLen / 256 - temp * 256 - 128).byteValue();
        temp = YDataLen / 256;
        YDataByte[2] = Integer.valueOf(YDataLen - temp * 256 - 128).byteValue();
        byte[] YDataFillZeroByte = new byte[1];
        YDataFillZeroByte[0] = Integer.valueOf(YDataFillZero - 128).byteValue();
        byte[] YTreeByte = new byte[2];
        YTreeByte[0] = Integer.valueOf(YTreeLen / 256 - 128).byteValue();
        YTreeByte[1] = Integer.valueOf(YTreeLen - YTreeLen / 256 * 256 - 128).byteValue();
        byte[] YTreeFillZeroByte = new byte[1];
        YTreeFillZeroByte[0] = Integer.valueOf(YTreeFillZero - 128).byteValue();
        byte[] YKeyByte = new byte[2];
        YKeyByte[0] = Integer.valueOf(YKeyLen / 256 - 128).byteValue();
        YKeyByte[1] = Integer.valueOf(YKeyLen - YKeyLen / 256 * 256 - 128).byteValue();

        // Y data
        String YData = YHuffmanTree.getOutput();
        YData = YData + "0".repeat(YDataFillZero);
        String YTree = YHuffmanTree.getTree().getTreeString();
        YTree = YTree + "0".repeat(YTreeFillZero);
        byte[] YDataBytes = new byte[YData.length() / 8];
        byte[] YTreeBytes = new byte[YTree.length() / 8];
        byte[] YKeyBytes = new byte[YKeyLen];
        for (int i = 0; i < YData.length() / 8; i++) {
            String s = YData.substring(i * 8, i * 8 + 8);
            YDataBytes[i] = Integer.valueOf(Integer.parseInt(s, 2) - 128).byteValue();
        }
        for (int i = 0; i < YTree.length() / 8; i++) {
            String s = YTree.substring(i * 8, i * 8 + 8);
            YTreeBytes[i] = Integer.valueOf(Integer.parseInt(s, 2) - 128).byteValue();
        }
        for (int i = 0; i < YKeyLen - 1; i = i + 2) {
            int data = Integer.parseInt(YHuffmanTree.getTree().dataList().get(i / 2));
            if (data < 0) {
                data = -data;
                YKeyBytes[i] = Integer.valueOf(-(data / 128)).byteValue();
                YKeyBytes[i + 1] = Integer.valueOf(-(data - data / 128 * 128)).byteValue();
            } else {
                YKeyBytes[i] = Integer.valueOf(data / 128).byteValue();
                YKeyBytes[i + 1] = Integer.valueOf(data - data / 128 * 128).byteValue();
            }
        }

        // U header
        int UDataLen = UHuffmanTree.getOutput().length() / 8;
        int UDataFillZero = 8 - UHuffmanTree.getOutput().length() + UDataLen * 8;
        if (UDataFillZero == 8) {
            UDataFillZero = 0;
        }
        if (UHuffmanTree.getOutput().length() % 8 > 0) {
            UDataLen++;
        }
        int UTreeLen = UHuffmanTree.getTree().getTreeString().length() / 8;
        int UTreeFillZero = 8 - UHuffmanTree.getTree().getTreeString().length() + UTreeLen * 8;
        if (UTreeFillZero == 8) {
            UTreeFillZero = 0;
        }
        if (UHuffmanTree.getTree().getTreeString().length() % 8 > 0) {
            UTreeLen++;
        }
        int UKeyLen = UHuffmanTree.getTree().dataList().size() * 2;

        // initial byte array as header to tell the data size
        byte[] UDataByte = new byte[3];
        temp = UDataLen / 256 / 256;
        UDataByte[0] = Integer.valueOf(temp - 128).byteValue();
        UDataByte[1] = Integer.valueOf(UDataLen / 256 - temp * 256 - 128).byteValue();
        temp = UDataLen / 256;
        UDataByte[2] = Integer.valueOf(UDataLen - temp * 256 - 128).byteValue();
        byte[] UDataFillZeroByte = new byte[1];
        UDataFillZeroByte[0] = Integer.valueOf(UDataFillZero - 128).byteValue();
        byte[] UTreeByte = new byte[2];
        UTreeByte[0] = Integer.valueOf(UTreeLen / 256 - 128).byteValue();
        UTreeByte[1] = Integer.valueOf(UTreeLen - UTreeLen / 256 * 256 - 128).byteValue();
        byte[] UTreeFillZeroByte = new byte[1];
        UTreeFillZeroByte[0] = Integer.valueOf(UTreeFillZero - 128).byteValue();
        byte[] UKeyByte = new byte[2];
        UKeyByte[0] = Integer.valueOf(UKeyLen / 256 - 128).byteValue();
        UKeyByte[1] = Integer.valueOf(UKeyLen - UKeyLen / 256 * 256 - 128).byteValue();

        // U data
        String UData = UHuffmanTree.getOutput();
        UData = UData + "0".repeat(UDataFillZero);
        String UTree = UHuffmanTree.getTree().getTreeString();
        UTree = UTree + "0".repeat(UTreeFillZero);
        byte[] UDataBytes = new byte[UData.length() / 8];
        byte[] UTreeBytes = new byte[UTree.length() / 8];
        byte[] UKeyBytes = new byte[UKeyLen];
        for (int i = 0; i < UData.length() / 8; i++) {
            String s = UData.substring(i * 8, i * 8 + 8);
            UDataBytes[i] = Integer.valueOf(Integer.parseInt(s, 2) - 128).byteValue();
        }
        for (int i = 0; i < UTree.length() / 8; i++) {
            String s = UTree.substring(i * 8, i * 8 + 8);
            UTreeBytes[i] = Integer.valueOf(Integer.parseInt(s, 2) - 128).byteValue();
        }
        for (int i = 0; i < UKeyLen; i = i + 2) {
            int data = Integer.parseInt(UHuffmanTree.getTree().dataList().get(i / 2));
            if (data < 0) {
                data = -data;
                UKeyBytes[i] = Integer.valueOf(-(data / 128)).byteValue();
                UKeyBytes[i + 1] = Integer.valueOf(-(data - data / 128 * 128)).byteValue();
            } else {
                UKeyBytes[i] = Integer.valueOf(data / 128).byteValue();
                UKeyBytes[i + 1] = Integer.valueOf(data - data / 128 * 128).byteValue();
            }
        }

        // V header
        int VDataLen = VHuffmanTree.getOutput().length() / 8;
        int VDataFillZero = 8 - VHuffmanTree.getOutput().length() + VDataLen * 8;
        if (VDataFillZero == 8) {
            VDataFillZero = 0;
        }
        if (VHuffmanTree.getOutput().length() % 8 > 0) {
            VDataLen++;
        }
        int VTreeLen = VHuffmanTree.getTree().getTreeString().length() / 8;
        int VTreeFillZero = 8 - VHuffmanTree.getTree().getTreeString().length() + VTreeLen * 8;
        if (VTreeFillZero == 8) {
            VTreeFillZero = 0;
        }
        if (VHuffmanTree.getTree().getTreeString().length() % 8 > 0) {
            VTreeLen++;
        }
        int VKeyLen = VHuffmanTree.getTree().dataList().size() * 2;

        // initial byte array as header to tell the data size
        byte[] VDataByte = new byte[3];
        temp = VDataLen / 256 / 256;
        VDataByte[0] = Integer.valueOf(temp - 128).byteValue();
        VDataByte[1] = Integer.valueOf(VDataLen / 256 - temp * 256 - 128).byteValue();
        temp = VDataLen / 256;
        VDataByte[2] = Integer.valueOf(VDataLen - temp * 256 - 128).byteValue();
        byte[] VDataFillZeroByte = new byte[1];
        VDataFillZeroByte[0] = Integer.valueOf(VDataFillZero - 128).byteValue();
        byte[] VTreeByte = new byte[2];
        VTreeByte[0] = Integer.valueOf(VTreeLen / 256 - 128).byteValue();
        VTreeByte[1] = Integer.valueOf(VTreeLen - VTreeLen / 256 * 256 - 128).byteValue();
        byte[] VTreeFillZeroByte = new byte[1];
        VTreeFillZeroByte[0] = Integer.valueOf(VTreeFillZero - 128).byteValue();
        byte[] VKeyByte = new byte[2];
        VKeyByte[0] = Integer.valueOf(VKeyLen / 256 - 128).byteValue();
        VKeyByte[1] = Integer.valueOf(VKeyLen - VKeyLen / 256 * 256 - 128).byteValue();

        // V data
        String VData = VHuffmanTree.getOutput();
        VData = VData + "0".repeat(VDataFillZero);
        String VTree = VHuffmanTree.getTree().getTreeString();
        VTree = VTree + "0".repeat(VTreeFillZero);
        byte[] VDataBytes = new byte[VData.length() / 8];
        byte[] VTreeBytes = new byte[VTree.length() / 8];
        byte[] VKeyBytes = new byte[VKeyLen];
        for (int i = 0; i < VData.length() / 8; i++) {
            String s = VData.substring(i * 8, i * 8 + 8);
            VDataBytes[i] = Integer.valueOf(Integer.parseInt(s, 2) - 128).byteValue();
        }
        for (int i = 0; i < VTree.length() / 8; i++) {
            String s = VTree.substring(i * 8, i * 8 + 8);
            VTreeBytes[i] = Integer.valueOf(Integer.parseInt(s, 2) - 128).byteValue();
        }
        for (int i = 0; i < VKeyLen; i = i + 2) {
            int data = Integer.parseInt(VHuffmanTree.getTree().dataList().get(i / 2));
            if (data < 0) {
                data = -data;
                VKeyBytes[i] = Integer.valueOf(-(data / 128)).byteValue();
                VKeyBytes[i + 1] = Integer.valueOf(-(data - data / 128 * 128)).byteValue();
            } else {
                VKeyBytes[i] = Integer.valueOf(data / 128).byteValue();
                VKeyBytes[i + 1] = Integer.valueOf(data - data / 128 * 128).byteValue();
            }
        }
        // combine
        byte[] total = new byte[2 + YDataByte.length + YDataFillZeroByte.length + YTreeByte.length + YTreeFillZeroByte.length + YKeyByte.length + YDataBytes.length + YTreeBytes.length + YKeyBytes.length +
                UDataByte.length + UDataFillZeroByte.length + UTreeByte.length + UTreeFillZeroByte.length + UKeyByte.length + UDataBytes.length + UTreeBytes.length + UKeyBytes.length +
                VDataByte.length + VDataFillZeroByte.length + VTreeByte.length + VTreeFillZeroByte.length + VKeyByte.length + VDataBytes.length + VTreeBytes.length + VKeyBytes.length];
        int index = 2;
        // header Y
        total[0] = Integer.valueOf((bmpProcessor.getHeight() / 8 - 128)).byteValue();
        total[1] = Integer.valueOf((bmpProcessor.getWidth() / 8 - 128)).byteValue();
        System.arraycopy(YDataByte, 0, total, index, YDataByte.length);
        index += YDataByte.length;
        System.arraycopy(YDataFillZeroByte, 0, total, index, YDataFillZeroByte.length);
        index += YDataFillZeroByte.length;
        System.arraycopy(YTreeByte, 0, total, index, YTreeByte.length);
        index += YTreeByte.length;
        System.arraycopy(YTreeFillZeroByte, 0, total, index, YTreeFillZeroByte.length);
        index += YTreeFillZeroByte.length;
        System.arraycopy(YKeyByte, 0, total, index, YKeyByte.length);
        index += YKeyByte.length;
        // header U
        System.arraycopy(UDataByte, 0, total, index, UDataByte.length);
        index += UDataByte.length;
        System.arraycopy(UDataFillZeroByte, 0, total, index, UDataFillZeroByte.length);
        index += UDataFillZeroByte.length;
        System.arraycopy(UTreeByte, 0, total, index, UTreeByte.length);
        index += UTreeByte.length;
        System.arraycopy(UTreeFillZeroByte, 0, total, index, UTreeFillZeroByte.length);
        index += UTreeFillZeroByte.length;
        System.arraycopy(UKeyByte, 0, total, index, UKeyByte.length);
        index += YKeyByte.length;
        // header V
        System.arraycopy(VDataByte, 0, total, index, VDataByte.length);
        index += VDataByte.length;
        System.arraycopy(VDataFillZeroByte, 0, total, index, VDataFillZeroByte.length);
        index += VDataFillZeroByte.length;
        System.arraycopy(VTreeByte, 0, total, index, VTreeByte.length);
        index += VTreeByte.length;
        System.arraycopy(VTreeFillZeroByte, 0, total, index, VTreeFillZeroByte.length);
        index += VTreeFillZeroByte.length;
        System.arraycopy(VKeyByte, 0, total, index, VKeyByte.length);
        index += VKeyByte.length;
        // data Y
        System.arraycopy(YDataBytes, 0, total, index, YDataBytes.length);
        index += YDataBytes.length;
        System.arraycopy(YTreeBytes, 0, total, index, YTreeBytes.length);
        index += YTreeBytes.length;
        System.arraycopy(YKeyBytes, 0, total, index, YKeyBytes.length);
        index += YKeyBytes.length;
        // data U
        System.arraycopy(UDataBytes, 0, total, index, UDataBytes.length);
        index += UDataBytes.length;
        System.arraycopy(UTreeBytes, 0, total, index, UTreeBytes.length);
        index += UTreeBytes.length;
        System.arraycopy(UKeyBytes, 0, total, index, UKeyBytes.length);
        index += UKeyBytes.length;
        // data V
        System.arraycopy(VDataBytes, 0, total, index, VDataBytes.length);
        index += VDataBytes.length;
        System.arraycopy(VTreeBytes, 0, total, index, VTreeBytes.length);
        index += VTreeBytes.length;
        System.arraycopy(VKeyBytes, 0, total, index, VKeyBytes.length);
        index += VKeyBytes.length;

        File file = new File(name + ".IMG");
        writeByte(total, file);
        size = total.length;
//        Decoder decoder = new Decoder(total);
//        decoder.decode();
//        tempY = decoder.getTempY();
//        tempU = decoder.getTempU();
//        tempV = decoder.getTempV();

    }

    //https://www.geeksforgeeks.org/convert-byte-array-to-file-using-java/#:~:text=To%20convert%20byte%5B%5D%20to,and%20write%20in%20a%20file.
    void writeByte(byte[] bytes, File file) {
        try {
            // Initialize a pointer
            // in file using OutputStream
            OutputStream os = new FileOutputStream(file);

            // Starts writing the bytes in it
            os.write(bytes);
            System.out.println("Successfully" + " byte inserted");

            // Close the file
            os.close();
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
    }

    //https://my.oschina.net/tigerBin/blog/1083549
    double[] ZigZag(double mtx[]) {
        double[] out = new double[64];
        for (int i = 0; i < 64; i++)
            out[i] = mtx[ZigZagTable[i]];
        return out;
    }

    //https://my.oschina.net/tigerBin/blog/1083549
    double[] UnZigZag(double mtx[]) {
        double[] out = new double[64];
        for (int i = 0; i < 64; i++)
            out[i] = mtx[UnZigZagTable[i]];
        return out;
    }

    private HuffmanTree Huffman(int[] data) {
        HuffmanTree huffmanTree = new HuffmanTree(data);
        huffmanTree.oneWordHuffman();
        return huffmanTree;
    }

    private void getRGB() {
        this.rChannel = new int[bmpProcessor.getWidth() * bmpProcessor.getHeight()];
        this.gChannel = new int[bmpProcessor.getWidth() * bmpProcessor.getHeight()];
        this.bChannel = new int[bmpProcessor.getWidth() * bmpProcessor.getHeight()];
        initialWidthAndHeight();
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
            if (bmpProcessor.isStartAtLeft()) {
                if (width >= bmpProcessor.getWidth() - 1) {
                    i = i + bmpProcessor.getSkip();
                }
            } else if (width <= 0) {
                i = i + bmpProcessor.getSkip();
            }
            nextHeightAndWidth();
        }
    }

    private void getYUV() {
        YChannel = new double[this.rChannel.length];
        UChannel = new double[this.rChannel.length];
        VChannel = new double[this.rChannel.length];
        for (int i = 0; i < this.rChannel.length; i++) {
            int R = this.rChannel[i];
            int G = this.gChannel[i];
            int B = this.bChannel[i];
            // on book "4.3.2 YUV Color Model" have formula to convert RGB to YUV, and YUV to RGB
            YChannel[i] = 0.299 * R + 0.587 * G + 0.114 * B;
            UChannel[i] = -0.14713 * R + -0.28886 * G + 0.436 * B;
            VChannel[i] = 0.615 * R + -0.51499 * G + -0.10001 * B;
        }
    }

    //https://www.baeldung.com/java-matrix-multiplication
    double[][] multiplyMatrices(double[][] firstMatrix, double[][] secondMatrix) {
        double[][] result = new double[firstMatrix.length][secondMatrix[0].length];

        for (int row = 0; row < result.length; row++) {
            for (int col = 0; col < result[row].length; col++) {
                result[row][col] = multiplyMatricesCell(firstMatrix, secondMatrix, row, col);
            }
        }
        return result;
    }

    //https://www.baeldung.com/java-matrix-multiplication
    double multiplyMatricesCell(double[][] firstMatrix, double[][] secondMatrix, int row, int col) {
        double cell = 0;
        for (int i = 0; i < secondMatrix.length; i++) {
            cell += firstMatrix[row][i] * secondMatrix[i][col];
        }
        return cell;
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

    public BMPProcessor getBmpProcessor() {
        return bmpProcessor;
    }

    public int getSize() {
        return size;
    }
}
