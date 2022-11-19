package TreeModule;

import java.util.ArrayList;
import java.util.List;

public class Tree {
    public Node head;
    private List<Node> list;
    private int index;
    public Tree(Node head) {
        list = new ArrayList<>();
        this.head = head;
    }

    public void setDepth() {
        depthRecursion(head, 0, "");
    }

    public void depthRecursion(Node node, int depth, String code) {
        node.depth = depth;
        node.code = code;
        if (node.left != null) {
            depthRecursion(node.left, depth + 1, code + "0");
        }
        if (node.right != null) {
            depthRecursion(node.right, depth + 1, code + "1");
        }
    }

    public void print() {
        printNode(head);
    }

    private void printNode(Node node) {
        if (node.left != null && node.right != null) {
            printNode(node.left);
            printNode(node.right);
        } else {
            System.out.println(node.key);
        }
    }

    public String encodeLength(Tree tree, String[] input) {
        addNodeToList(tree.head);
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < input.length; i++) {
            code.append(findInTree(input[i]));
        }
        return code.toString();
    }

    private void addNodeToList(Node node) {
        if (!node.key.equals("")) {
            list.add(node);
        }
        if (node.left != null) {
            addNodeToList(node.left);
        }
        if (node.right != null) {
            addNodeToList(node.right);
        }
    }

    private String findInTree(String string) {
        for (Node node : list) {
            if (string.equals(node.key)) {
                return node.code;
            }
        }
        return "";
    }

    public void printNode() {
        for (Node node : list) {
            if (!node.key.equals("")) {
                System.out.println(node.key + " " + node.value + " " + node.code);
            }
        }
    }

    public String getTreeString(){
        return encodeTree(head);
    }

    private String encodeTree(Node node){
        if (node.left != null && node.right != null){
            return "0" + encodeTree(node.left) + encodeTree(node.right);
        }else{
            return "1";
        }
    }

    public List<String> dataList(){
        List<String> list = new ArrayList<>();
        putDataInList(head, list);
        return list;
    }

    private void putDataInList(Node node, List<String> list) {
        if (node.left != null && node.right != null) {
            putDataInList(node.left, list);
            putDataInList(node.right, list);
        }
        else{
            list.add(node.key);
        }
    }

    public void assignValue(List<Integer> list){
        index = 0;
        assign(head, list);
    }

    private void assign(Node node, List<Integer> list){
        if (node.left != null && node.right != null) {
            assign(node.left, list);
            assign(node.right, list);
        }else
        {
            node.key = String.valueOf(list.get(index));
            index++;
        }
    }
}
