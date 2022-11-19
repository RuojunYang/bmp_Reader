import TreeModule.Tree;
import TreeModule.Node;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class Decoder {
    byte[] input;
    private int[] UnZigZagTable;
    private double[] tempY;
    private double[] tempU;
    private double[] tempV;
    int height;
    int width;

    public Decoder(byte[] input) {
        this.input = input;
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

    public void decode() {
        int height = input[0] + 128;
        int width = input[1] + 128;
        this.height = height * 8;
        this.width = width * 8;
        int YDataLen = (input[2] + 128) * 256 * 256 + (input[3] + 128) * 256 + (input[4] + 128);
        int YDataFillZero = (input[5] + 128);
        int YTreeLen = (input[6] + 128) * 256 + (input[7] + 128);
        int YTreeFillZero = (input[8] + 128);
        int YKeyLen = (input[9] + 128) * 256 + (input[10] + 128);
        int UDataLen = (input[11] + 128) * 256 * 256 + (input[12] + 128) * 256 + (input[13] + 128);
        int UDataFillZero = (input[14] + 128);
        int UTreeLen = (input[15] + 128) * 256 + (input[16] + 128);
        int UTreeFillZero = (input[17] + 128);
        int UKeyLen = (input[18] + 128) * 256 + (input[19] + 128);
        int VDataLen = (input[20] + 128) * 256 * 256 + (input[21] + 128) * 256 + (input[22] + 128);
        int VDataFillZero = (input[23] + 128);
        int VTreeLen = (input[24] + 128) * 256 + (input[25] + 128);
        int VTreeFillZero = (input[26] + 128);
        int VKeyLen = (input[27] + 128) * 256 + (input[28] + 128);
        int index = 29;

        double[][] oneDYMatrix = getDoubles(YDataLen, YDataFillZero, YTreeLen, YTreeFillZero, YKeyLen, index);
        index = 29 + YDataLen + YTreeLen + YKeyLen;
        double[][] oneDUMatrix = getDoubles(UDataLen, UDataFillZero, UTreeLen, UTreeFillZero, UKeyLen, index);
        index = 29 + YDataLen + YTreeLen + YKeyLen + UDataLen + UTreeLen + UKeyLen;
        double[][] oneDVMatrix = getDoubles(VDataLen, VDataFillZero, VTreeLen, VTreeFillZero, VKeyLen, index);

        double[][] YMatrix = new double[height * 8][width * 8];
        double[][] newYMatrix = new double[height * 8][width * 8];
        double[][] UMatrix = new double[height * 8][width * 8];
        double[][] newUMatrix = new double[height * 8][width * 8];
        double[][] VMatrix = new double[height * 8][width * 8];
        double[][] newVMatrix = new double[height * 8][width * 8];
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
        double[][] transpose = new double[8][8];
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                transpose[i][j] = DCT[j][i];
            }
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
        newYMatrix = new double[this.height][this.width];
        newUMatrix = new double[this.height][this.width];
        newVMatrix = new double[this.height][this.width];
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
    }

    private double[][] getDoubles(int YDataLen, int YDataFillZero, int YTreeLen, int YTreeFillZero, int YKeyLen, int index) {
        StringBuilder YDataString = new StringBuilder();
        StringBuilder YTreeString = new StringBuilder();
        List<Integer> YKey = new ArrayList<>();
        for (int i = index; i < index + YDataLen; i++) {
            YDataString.append(binary(input[i] + 128));
        }
        index += YDataLen;
        for (int i = index; i < index + YTreeLen; i++) {
            YTreeString.append(binary(input[i] + 128));
        }
        index += YTreeLen;

        for (int i = index; i < index + YKeyLen; i = i + 2) {
            int data = (int) input[i] * 128 + (int) input[i + 1];
            YKey.add(data);
        }

        String YActualData = YDataString.substring(0, YDataString.length() - YDataFillZero);

        String YActualTree = YTreeString.substring(0, YTreeString.length() - YTreeFillZero);
        Tree YTree = decodeTree(YActualTree);
        YTree.assignValue(YKey);

        List<String> YDataREL = decodeData(YTree, YActualData);
        List<Double> YDataZIG = fromREL2ZIG(YDataREL);
        double[] YDataZIGArr = new double[YDataZIG.size()];
        for (int i = 0; i < YDataZIG.size(); i++) {
            YDataZIGArr[i] = YDataZIG.get(i);
        }
        double[][] oneDYMatrix = new double[YDataZIGArr.length / 64][64];
        for (int i = 0; i < YDataZIGArr.length / 64; i++) {
            for (int j = 0; j < 64; j++) {
                oneDYMatrix[i][j] = YDataZIGArr[i * 64 + j];
            }
            oneDYMatrix[i] = UnZigZag(oneDYMatrix[i]);
        }
        return oneDYMatrix;
    }

    private List<Double> fromREL2ZIG(List<String> rel) {
        List<Double> zig = new ArrayList<>();
        for (int i = 0; i < rel.size(); i = i + 2) {
            for (int j = 0; j < Double.parseDouble(rel.get(i)); j++) {
                zig.add(0.0);
            }
            zig.add(Double.parseDouble(rel.get(i + 1)));
        }
        if (zig.size() % 64 > 0) {
            int count = 64 - zig.size() % 64;
            for (int i = 0; i < count; i++) {
                zig.add(0.0);
            }
        }
        return zig;
    }

    private List<String> decodeData(Tree tree, String output) {
        Node current = tree.head;
        List<String> data = new ArrayList<>();
        for (int i = 0; i < output.length(); i++) {
            if (output.charAt(i) == '0') {
                if (current.left.key.equals("")) {
                    current = current.left;
                } else {
                    data.add(current.left.key);
                    current = tree.head;
                }
            } else {
                if (current.right.key.equals("")) {
                    current = current.right;
                } else {
                    data.add(current.right.key);
                    current = tree.head;
                }
            }
        }
        return data;
    }

    private Tree decodeTree(String treeStr) {
        List<Node> nodeQueue = new ArrayList<>();
        for (int i = 0; i < treeStr.length(); i++) {
            if (treeStr.charAt(i) == '0') {
                Node node = new Node(0, "");
                if (nodeQueue.size() > 0) {
                    Node parent = nodeQueue.get(nodeQueue.size() - 1);
                    for (int j = nodeQueue.size() - 1; j >= 0; j--) {
                        if (nodeQueue.get(j).value == 0) {
                            parent = nodeQueue.get(j);
                            break;
                        }
                    }
                    if (parent.left == null) {
                        parent.left = node;
                    } else if (parent.right == null) {
                        parent.right = node;
                        parent.value = 1;
                    }
                }
                nodeQueue.add(node);
            } else {
                Node node = new Node(1, "");
                Node parent = nodeQueue.get(nodeQueue.size() - 1);
                for (int j = nodeQueue.size() - 1; j >= 0; j--) {
                    if (nodeQueue.get(j).value == 0) {
                        parent = nodeQueue.get(j);
                        break;
                    }
                }
                if (parent.left == null) {
                    parent.left = node;
                } else if (parent.right == null) {
                    parent.right = node;
                    parent.value = 1;
                }
            }
        }
        Tree tree = new Tree(nodeQueue.get(0));
        return tree;
    }


    private String binary(int i) {
        String s = "";
        int temp = i;
        s = s + temp / 128;
        temp = temp - temp / 128 * 128;
        s = s + temp / 64;
        temp = temp - temp / 64 * 64;
        s = s + temp / 32;
        temp = temp - temp / 32 * 32;
        s = s + temp / 16;
        temp = temp - temp / 16 * 16;
        s = s + temp / 8;
        temp = temp - temp / 8 * 8;
        s = s + temp / 4;
        temp = temp - temp / 4 * 4;
        s = s + temp / 2;
        temp = temp - temp / 2 * 2;
        s = s + temp;
        return s;
    }

    //https://my.oschina.net/tigerBin/blog/1083549
    double[] UnZigZag(double mtx[]) {
        double[] out = new double[64];
        for (int i = 0; i < 64; i++)
            out[i] = mtx[UnZigZagTable[i]];
        return out;
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

    public double[] getTempY() {
        return tempY;
    }

    public double[] getTempU() {
        return tempU;
    }

    public double[] getTempV() {
        return tempV;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }
}
