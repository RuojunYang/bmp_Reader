import java.util.Arrays;

public class BMPProcessor {
    private String BM;
    private int size;
    private int dataOffset;
    private int width;
    private int height;
    private boolean noCompress;
    private int horizontalResolution;
    private int verticalResolution;
    private int colorUsed;
    private int infoHeaderSize;
    private String[] pixelData;
    private int rowSize;
    private int skip;
    private boolean startAtLeft;
    private int bitCount;

    public BMPProcessor(String[] data) {
        String[] BM = Arrays.copyOfRange(data, 0, 2);
        String[] size = Arrays.copyOfRange(data, 2, 6);
        String[] dataOffset = Arrays.copyOfRange(data, 10, 14);
        String[] infoHeaderSize = Arrays.copyOfRange(data, 14, 18);
        String[] width = Arrays.copyOfRange(data, 18, 22);
        String[] height = Arrays.copyOfRange(data, 22, 26);
        String[] bitCount = Arrays.copyOfRange(data, 28, 30);
        String[] compress = Arrays.copyOfRange(data, 30, 34);
        String[] horizontalResolution = Arrays.copyOfRange(data, 38, 42);
        String[] verticalResolution = Arrays.copyOfRange(data, 42, 46);
        String[] colorUsed = Arrays.copyOfRange(data, 46, 50);

        this.BM = "";
        for (String s : BM) {
            this.BM = this.BM + ((char) Integer.parseInt(s, 16));
        }
        int temp = 1;
        this.size = 0;
        for (int i = 0; i < 4; i++) {
            this.size = temp * Integer.parseInt(size[i], 16) + this.size;
            temp = temp * 256;
        }

        this.dataOffset = stringToInt(LERead(dataOffset));

        this.infoHeaderSize = stringToInt(LERead(infoHeaderSize));

        this.width = stringToInt(LERead(width));

        this.height = stringToInt(LERead(height));

        startAtLeft = true;
        if (this.height < 0) {
            startAtLeft = false;
            this.height = -this.height;
        }

        this.noCompress = true;
        for (String s : compress) {
            if (!s.equals("00")) {
                this.noCompress = false;
                break;
            }
        }

        this.horizontalResolution = stringToInt(LERead(horizontalResolution));

        this.verticalResolution = stringToInt(LERead(verticalResolution));

        this.bitCount = stringToInt(LERead(bitCount));

        this.colorUsed = stringToInt(LERead(colorUsed));

        this.rowSize = ((24 * this.width + 31) / 32) * 4;

        this.pixelData = Arrays.copyOfRange(data, this.dataOffset, rowSize * this.height + 54);

        skip = 4 - (this.width * 3 % 4);
        if (this.width * 3 % 4 == 0) {
            skip = 0;
        }
    }

    public int getSkip() {
        return skip;
    }

    public String getBM() {
        return BM;
    }

    public int getSize() {
        return size;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isNoCompress() {
        return noCompress;
    }

    public int getHorizontalResolution() {
        return horizontalResolution;
    }

    public int getVerticalResolution() {
        return verticalResolution;
    }

    public int getColorUsed() {
        return colorUsed;
    }

    public int getDataOffset() {
        return dataOffset;
    }

    public int getInfoHeaderSize() {
        return infoHeaderSize;
    }

    public String[] getPixelData() {
        return pixelData;
    }

    public int getRowSize() {
        return rowSize;
    }

    public boolean isStartAtLeft() {
        return startAtLeft;
    }

    public int getBitCount() {
        return bitCount;
    }

    private int stringToInt(String s) {
        String binaryData = Integer.toBinaryString(Integer.parseInt(s, 16));
        StringBuilder stringBuilder = new StringBuilder(binaryData);
        int twoSValue = Integer.parseInt(String.valueOf(stringBuilder), 2);
        // negative in 2's
        if (stringBuilder.length() == 8 && stringBuilder.charAt(0) == '1') {
            // transfer to positive
            for (int k = 0; k < stringBuilder.length(); k++) {
                if (stringBuilder.charAt(k) == '0') {
                    stringBuilder.setCharAt(k, '1');
                } else {
                    stringBuilder.setCharAt(k, '0');
                }
            }
            // -(positive + 1) is the actual negative number
            twoSValue = -(Integer.parseInt(stringBuilder.toString(), 2) + 1);
        }
        return twoSValue;
    }

    private String LERead(String[] s) {
        StringBuilder temp = new StringBuilder();
        for (int i = 0; i < s.length; i++) {
            temp.insert(0, s[i]);
        }
        return temp.toString();
    }
}
