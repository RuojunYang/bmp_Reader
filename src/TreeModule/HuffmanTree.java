package TreeModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class HuffmanTree {
    private String[] decimalData;
    private Tree tree;
    private String output;

    public HuffmanTree(int[] decimalData) {
        this.decimalData = new String[decimalData.length];
        for (int i = 0; i < decimalData.length; i++) {
            this.decimalData[i] = String.valueOf(decimalData[i]);
        }
    }

    public void oneWordHuffman() {
        List<String> differentNumbers = new ArrayList<>();
        for (String s : decimalData) {
            if (!differentNumbers.contains(s)) {
                differentNumbers.add(s);
            }
        }
        int[] count = new int[differentNumbers.size()];
        for (String s : decimalData) {
            count[differentNumbers.indexOf(s)]++;
        }
        double[] prob = new double[differentNumbers.size()];
        for (int i = 0; i < differentNumbers.size(); i++) {
            prob[i] = count[i] / (double) decimalData.length;
        }
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < differentNumbers.size(); i++) {
            nodes.add(new Node(prob[i], differentNumbers.get(i)));
        }
        tree = buildTree(nodes);
        tree.setDepth();
        output = tree.encodeLength(tree, decimalData);
        // bit to byte
    }

    private Tree buildTree(List<Node> nodes) {
        // do it until it only have one node left
        while (nodes.size() > 1) {
            // sort it
            Collections.sort(nodes);
            // choose the least two
            Node last = nodes.get(nodes.size() - 1);
            Node secondLast = nodes.get(nodes.size() - 2);
            // combine the last two
            Node CombineLastTwo = new Node(last, secondLast, last.value + secondLast.value);
            // delete the original two
            nodes.remove(last);
            nodes.remove(secondLast);
            // add new one
            nodes.add(CombineLastTwo);
        }
        Collections.sort(nodes);
        return new Tree(nodes.get(0));
    }

    public Tree getTree() {
        return tree;
    }

    public String getOutput() {
        return output;
    }
}
