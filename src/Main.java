import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

public class Main {
    static public void main(String[] args) throws IOException {
        FileReader fileReader = new FileReader();
        // have to be bmp format file
        try {
            if (!fileReader.getFormat().toLowerCase().equals("bmp") && !fileReader.getFormat().toLowerCase().equals("img")) {
                System.out.println("Wrong data format");
            } else if (fileReader.getFormat().toLowerCase().equals("bmp")) {
                String[] rawData = fileReader.getOutput();
                BMPProcessor bmpProcessor = new BMPProcessor(rawData);
                // make sure this is BM, no compress, and
                if (bmpProcessor.getBM().equals("BM") && bmpProcessor.isNoCompress() && bmpProcessor.getBitCount() == 24 && bmpProcessor.getColorUsed() == 0) {
                    Instant start = Instant.now();
                    Lossy lossy = new Lossy(bmpProcessor, fileReader.getSize());
                    lossy.compress(fileReader.getName());
                    Instant end = Instant.now();
                    Duration timeElapsed = Duration.between(start, end);
                    double ratio = fileReader.getSize() / (double) lossy.getSize();
                    new Graph(lossy, fileReader.getName() + ".bmp", timeElapsed.toMillis(), ratio);
                }
            } else {
                Instant start = Instant.now();
                Decoder decoder = new Decoder(fileReader.getInput());
                decoder.decode();
                Instant end = Instant.now();
                Duration timeElapsed = Duration.between(start, end);
                new GraphDecode(decoder, fileReader.getName() + ".IMG", timeElapsed.toMillis());
            }
        } catch (NullPointerException e) {
            System.out.println("No file selected");
        }
//        catch (Exception e) {
//            System.out.println("Error in process file");
//        }
    }
}
