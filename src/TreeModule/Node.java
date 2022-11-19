package TreeModule;

public class Node implements Comparable<Node>{
    public Node left;
    public Node right;
    public String key;
    public double value;
    public int depth;
    public String code;

    public Node(double value, String key){
        this.value = value;
        this.key = key;
        left = null;
        right = null;
        this.depth = 0;
    }

    public Node(Node left, Node right, double value){
        this.left = left;
        this.right = right;
        this.value = value;
        this.key = "";
        this.depth = 0;
    }

    @Override
    public int compareTo(Node o) {
        if (this.value == o.value){
            return 0;
        }
        else if (this.value >= o.value){
            return -1;
        }
        else {
            return 1;
        }
    }
}
