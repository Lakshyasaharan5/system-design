import java.util.ArrayList;
import java.util.List;

/*
    it feels like Trie data structure

    example = lakshya/Documents/resume.pdf
    class File {
        name
        size
        permissions maybe
    }
    class Directory {
        Directory[] child;
        File[] files;
        permissions maybe
    }

    even better to create common class or interface for file and directory
    abstract class Node {
        String name;
    }
    File {}
    Directory {List<FileNode>}

    [d1, d2, f1, d3]
     \             \
     [d4, f2]       [f3]
        \
         [f4]

     path = d1/d4/f4

     read(path){
        if (path == file) return file.open();
        find next directory
            return read(path+1)
     }

     Node -> File
        \---> Directory
 */
abstract class Node {
    private String name;
    public Node(String name) {this.name = name;}
    public String getName() {return this.name;}
}
class File extends Node {
    private String content;
    File(String name, String content) {
        super(name);

        this.content = content;
    }
    public String getContent() {return this.content;}
}
class Directory extends Node {
    List<Node> children;
    public Directory(String name) {
        super(name);
        children = new ArrayList<>();
    }
    private int getCurrNodeIndex(String path) {
        int i = 0;
        while (i < path.length() && path.charAt(i) != '/') i++;
        return i;
    }
    /*
         root
        a
        \
         b


         mkdir path = a/b/c
                      01234
                       i
     */
    public void mkdir(String path) {
        int index = getCurrNodeIndex(path);
        for (Node curr : children) {
            if (curr instanceof Directory && curr.getName().equals(path.substring(0,index))) {
                ((Directory) curr).mkdir(path.substring( index + 1));
                return;
            }
        }
        // create a child here
        children.add(new Directory(path));
    }

    /*
            root
            |_d1
            |_d2
            | |_d3
            | |_f1
            |_d4
            | |_d5
            | | |_f2
            | |_d6
            |_d7

            root
                d1
                d2
                    d3
                    f1
                d4
     */
    public void printTree(int tabs) {
        System.out.println(" ".repeat(tabs) + this.getName());
        for (Node child : children) {
            if (child instanceof File) {
                System.out.println(" ".repeat(tabs + 4) + child.getName());
            } else {
                ((Directory) child).printTree(tabs + 4);
            }
        }
    }

    /*
        root
            d1
            d2
                f1

            path = d2/f1
     */
    public void addFile(String path, String content) {
        String[] parts = path.split("/");

        Directory currDir = this;

        // traverse directories only
        for (int i = 0; i < parts.length - 1; i++) {
            String dirName = parts[i];

            Directory nextDir = null;

            for (Node child : currDir.children) {
                if (child instanceof Directory &&
                        child.getName().equals(dirName)) {

                    nextDir = (Directory) child;
                    break;
                }
            }

            if (nextDir == null) {
                throw new IllegalArgumentException(
                        "Directory not found: " + dirName
                );
            }

            currDir = nextDir;
        }

        String fileName = parts[parts.length - 1];
        currDir.children.add(new File(fileName, content));
    }

    /*
     ls a/
      root
       \
        a
        \
         b
     */
    public String ls(String path) {
        Directory currDir = this;

        // walk to the target directory (same traversal as addFile)
        if (!path.isEmpty()) {
            String[] parts = path.split("/");
            for (String dirName : parts) {
                Directory nextDir = null;
                for (Node child : currDir.children) {
                    if (child instanceof Directory && child.getName().equals(dirName)) {
                        nextDir = (Directory) child;
                        break;
                    }
                }
                if (nextDir == null) throw new IllegalArgumentException("Directory not found: " + dirName);
                currDir = nextDir;
            }
        }

        // list immediate children's names (files and dirs, one level only)
        StringBuilder sb = new StringBuilder();
        for (Node child : currDir.children) {
            sb.append(child.getName()).append("  ");
        }
        return sb.toString().trim();
    }
}
public class Main {
    public static void main(String[] args) {
        Directory root = new Directory("root");
        root.mkdir("d1");
        root.mkdir("d2");
        root.mkdir("d1/d3");
        root.mkdir("d1/d3/d4");
        root.mkdir("d1/d3/d5");
        root.mkdir("d6");
        root.mkdir("d2/d7");
        root.addFile("d1/d3/f1", "resume");
        root.addFile("d1/d3/d4/f2", "transcript");
        root.printTree(0);
    }
}

