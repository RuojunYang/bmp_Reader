import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


public class FileReader {
    private String[] output;
    private String name;
    private String format;
    private long size;
    byte[] input;

    public FileReader() throws IOException {
        // dialog box to choose file
        JFileChooser chooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Images(.bmp, .IMG)", "bmp", "IMG");

        chooser.setFileFilter(filter);

        File selectedFile = null;
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            selectedFile = chooser.getSelectedFile();
            name = selectedFile.getName();
            size = selectedFile.length();
            // get the last string after last dot
            format = selectedFile.getName().split("\\.")[selectedFile.getName().split("\\.").length - 1];
            // read file in byte
            input = Files.readAllBytes(Paths.get(selectedFile.getPath()));
            output = new String[input.length];
            for (int i = 0; i < input.length; i++) {
                // save in hex
                output[i] = Integer.toHexString(Byte.toUnsignedInt(input[i]));
                // keep the length be 2 (consistent)
                if (output[i].length() < 2) {
                    output[i] = 0 + output[i];
                }
            }
        }
    }

    public byte[] getInput() {
        return input;
    }

    public long getSize() {
        return size;
    }

    public String[] getOutput() {
        return output;
    }

    public String getName() {
        int index = name.lastIndexOf(".");
        return name.substring(0, index);
    }

    public String getFormat() {
        return format;
    }
}
