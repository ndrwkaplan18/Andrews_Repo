package Interface.Impl;

import Interface.Trie;

import java.util.*;

public class TrieImpl<Value> implements Trie<Value>{
    protected static final int alphabetSize = 256; // extended ASCII
    protected Node root; // root of trie
    protected Comparator<Value> comparator;

    public static class Node<Value>
    {
        protected HashSet<Value> val = new HashSet<>();
        protected Node[] links = new Node[alphabetSize];
    }


    /**
     * get all matches for the given key, sorted in descending order.
     * Search is CASE INSENSITIVE.
     *
     * @param key
     * @return
     */
    @Override
    public List<Value> getAllSorted(String key) {
        key = key.toLowerCase();
        Node x = this.get(this.root, key, 0);
        if (x == null)
        {
            return null;
        }
        ArrayList<Value> result = new ArrayList<>();
        Collections.addAll(result,(Value[])x.val.toArray());
        result.sort(comparator);
        return result;
    }

    /**
     * A char in java has an int value.
     * see http://docs.oracle.com/javase/8/docs/api/java/lang/Character.html#getNumericValue-char-
     * see http://docs.oracle.com/javase/specs/jls/se7/html/jls-5.html#jls-5.1.2
     */
    private Node get(Node x, String key, int d)
    {
        //link was null - return null, indicating a miss
        if (x == null)
        {
            return null;
        }
        //we've reached the last node in the key,
        //return the node
        if (d == key.length())
        {
            return x;
        }
        //proceed to the next node in the chain of nodes that
        //forms the desired key
        char c = key.charAt(d);
        return this.get(x.links[c], key, d + 1);
    }

    /**
     * add the given value at the given key
     *
     * @param key
     * @param val
     */
    @Override
    public void put(String key, Value val) {
        if(val == null){
            deleteAll(key);
        }
        else {
            this.root = put(this.root, key, val, 0);
        }
    }

    private Node put(Node node, String key, Value val, int d){
        if(node == null){
            node = new Node();
        }

        if(d == key.length()){
            node.val.add(val);
            return node;
        }

        char c = key.charAt(d);
        node.links[c] = this.put(node.links[c],key,val,d+1);
        return node;
    }

    /**
     * delete ALL matches for the given key
     *
     * @param key
     */
    @Override
    public void deleteAll(String key) {
        this.root = deleteAll(this.root, key, 0);
    }

    protected Node deleteAll(Node x, String key, int d){
        if (x == null)
        {
            return null;
        }
        //we're at the node to del - set the val to null
        if (d == key.length())
        {
            x.val.clear();
        }
        //continue down the trie to the target node
        else
        {
            char c = key.charAt(d);
            x.links[c] = this.deleteAll(x.links[c], key, d + 1);
        }
        //this node has a val – do nothing, return the node
        if (x.val.size() != 0)
        {
            return x;
        }
        //remove subtrie rooted at x if it is completely empty
        for (int c = 0; c <alphabetSize; c++)
        {
            if (x.links[c] != null)
            {
                return x; //not empty
            }
        }
        //empty - set this link to null in the parent
        return null;
    }

    /**
     * delete ONLY the given value from the given key. Leave all other values.
     *
     * @param key
     * @param val
     */
    @Override
    public void delete(String key, Value val) {
        this.root = delete(this.root, key, val, 0);
    }

    protected Node delete(Node x, String key, Value val, int d){
        //We're at node with val to delete
        if(d == key.length()){
            if(x.val.contains(val)) {//verify target value exists
                for (Object v : x.val) { //For-each go through ArrayList at Node x
                    if (v.equals(val)) {
                        if(x.val.size() > 1){
                            x.val.remove(v);
                            break;
                        }
                        else{ //val was last in list, deleteAll() will delete null nodes above this one
                            deleteAll(key);
                            break;
                        }
                    }
                }
            }
            else{//target value doesn't exist. No action
                return x;
            }
        }
        else{
            char c = key.charAt(d);
            x.links[c] = this.delete(x.links[c], key, val, d + 1);
        }
        return x;
    }
}
