package common.metadata;

import ecs.ServerNode;

import java.security.*;
import java.lang.*;
import java.math.BigInteger;
import java.util.*;

public class Metadata {
    // bst
    //private transient static TreeSet<Node> servers_bst = new TreeSet<Node>(); // Node class has custom compareTo fct
    private static TreeSet<Node> servers_bst = new TreeSet<Node>(); // Node class has custom compareTo fct
    // BigInteger: hash, String: unique identifier for a Server
    private LinkedHashMap<BigInteger, String> HashToServer;
    private transient Node first_node = null;
    private transient Node last_node = null;
    private LinkedList<String> serverList =new LinkedList<String>();  //this must be a cyclic linked list

    public Metadata(){
        HashToServer = new LinkedHashMap<BigInteger, String>();
    }

    public Metadata(Node first_node, LinkedHashMap<BigInteger, String> HashToServer){
        this.first_node = first_node;
        this.HashToServer = HashToServer;
    }

    //primarily for KVClient to search correct server
    /**
     * Purpose: Upon receiving a new Metadata object, by the KVClient
     * it builds a new bst (as the bst is not converted to JSON and thus not sent in the message)
     */

    public void build_bst(){
        System.out.println("building bst...");
        this.removeAll();

        for (Map.Entry<BigInteger,String> entry : HashToServer.entrySet()){
            String serverNodeName = entry.getValue();
            System.out.println("build: " + serverNodeName);
            addToBst(entry.getValue(),entry.getKey());
            
        }
        first_node = servers_bst.first();
        last_node = servers_bst.last();
    }

    public LinkedHashMap<BigInteger, String> getMetadata(){
        return HashToServer;
    }

    /**
     * helper function for build_bst()
     */
    private void addToBst(String serverNodeName, BigInteger hashValue){
        Node s = new Node(serverNodeName,hashValue);
        if(!servers_bst.contains(s)){
            servers_bst.add(s);
        }
    }


    public String findServer(ServerNode n){
        return findServer(n.getNodeHostPort());
    }
    /**
     * findServer( ): this function is called on KVClient side.
     */
    public String findServer(String Key){
        if(servers_bst.isEmpty())
            return null;

        BigInteger keyhash = null;
        try {
            keyhash = getMD5(Key);
        }catch (Exception e){
            e.printStackTrace();
        }
        Node temp = new Node("0.0.0.0", keyhash);
        Node server_node = servers_bst.higher(temp);

        if(server_node == null)
            return first_node.id;
        else{
            return server_node.id;
        }
    }



    /*        ECS only                    */

//    public void init_hash2server(){
//        Iterator i = servers_bst.iterator();
//
//        if(i.hasNext()){
//            first_node = servers_bst.first();
//            last_node = servers_bst.last();
//        }
//
//        while (i.hasNext()) {
//            Node current_node = (Node) i.next();
//            HashToServer.put(current_node.hash, current_node.id);
//        }
//    }

    public void removeServer(ServerNode node){
        removeServer(node.getNodeHostPort());
    }

    public void removeServer(String serverIpPort){
        try {
            Node s = new Node(serverIpPort, getMD5(serverIpPort));
            if(servers_bst.contains(s)) {
                servers_bst.remove(s);
                removeServerListNode(serverIpPort);
                if(!servers_bst.isEmpty()) {
                    first_node = servers_bst.first();
                    last_node = servers_bst.last();
                }
                else{
                    first_node = null;
                    last_node = null;
                }
                HashToServer.remove(s.hash);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addServer(ServerNode node) {
        addServer(node.getNodeHostPort());
    }

    public LinkedHashMap<BigInteger, String> getHashToServer() {
        return HashToServer;
    }

    /**
     * Purpose: to add a new Server, this is called on ECS side
     */
    public void addServer(String serverIpPort){
        try {
            Node s = new Node(serverIpPort, getMD5(serverIpPort));
            //System.out.println(serverIpPort + " hash value = " + getMD5(serverIpPort));
            if(!servers_bst.contains(s)) {
                servers_bst.add(s);
                System.out.println("bst add: " + serverIpPort);
                first_node = servers_bst.first();
                last_node = servers_bst.last();
                HashToServer.put(s.hash, s.id);
                addServerListNode(serverIpPort);
                System.out.println(serverIpPort + " is added to the serverList\n");
            }
            else
                System.err.println("Server already exist or Hash collosion!!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeAll(){
        if(!servers_bst.isEmpty()){
            servers_bst.clear();
            serverList.clear();
        }
    }

    public boolean isEmpty(){
        return servers_bst.isEmpty();
    }

    private BigInteger getMD5(String input) throws Exception{
        MessageDigest md=MessageDigest.getInstance("MD5");
        md.update(input.getBytes(),0,input.length());
        String hash_temp = new BigInteger(1,md.digest()).toString(16);
        BigInteger hash = new BigInteger(hash_temp, 16);
        return hash;
    }

    // debugging fct
    public void printData(String s){
        if(s.equals("bst")) {
            Iterator i = servers_bst.iterator();
            System.out.println("Printing BST...");
            while (i.hasNext()) {
                Node temp = (Node) i.next();
                System.out.println(temp.hash + ": " + temp.id);
            }
            System.out.println("--------------------------------------------");
        }

        if(s.equals("h2s")) {
            System.out.println("Printing HashToServer...");
            Iterator i = HashToServer.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry pair = (Map.Entry) i.next();
                System.out.println(pair.getKey() + ":" + pair.getValue());
            }
            System.out.println("--------------------------------------------");
        }
    }

    /**
     * findNode( ): use unique identifier to find the corresponding node stored in BST.
     */
    private Node findNode(String id) {
        Iterator<Node> iterator = servers_bst.iterator();
        while(iterator.hasNext()) {
            Node node = iterator.next();
            if(node.id.equals(id)){ //fixed here
                return node;
            }
        }
        System.out.println("findNode( ): return null");
        return null;
    }

    public String getSuccesor(ServerNode n){
        return getSuccessor(n.getNodeHostPort());
    }

    // public String getSuccessor(String id){
    //     Node n = findNode(id);
    //     if(n == null){
    //         return null;
    //     }
    //     Node successor = servers_bst.higher(n);
    //     if(successor == null){ //meaning that the current node is the highest one, need to return the smallest one.
    //         return null;
    //     } else {
    //         return successor.id;
    //     }
    // }


    private void removeServerListNode(String id) throws Exception{

      
        BigInteger newNodeHash = getMD5(id);
        int listSize = serverList.size();
        for (int i = 0; i < listSize; i++) {
           
           BigInteger hash = getMD5(serverList.get(i));

           int compare_result = newNodeHash.compareTo(hash);
           if(compare_result == 0){

                serverList.remove(i);
                return;
           }
        }
    }

    private void addServerListNode(String id) throws Exception{

        BigInteger newNodeHash = getMD5(id);
        int listSize = serverList.size();
        for (int i = 0; i < listSize; i++) {
           
           BigInteger hash = getMD5(serverList.get(i));
           int compare_result = newNodeHash.compareTo(hash);
           if(compare_result < 0){

                serverList.add(i,id);
                return;
           }
        }
        //when reached here, it means the current id is the biggest one in the list, so simply append to the list
        serverList.add(id);
    }

    public String getPredecessor(String id){

        if(id == null){
            return null;
        }
        Node n = findNode(id);
        if(n == null || servers_bst.size() == 1){//meaning either no such node is found or only node available now
            return null;
        }
        Node predecessor = servers_bst.lower(n);
        if(predecessor == null){ //meaning that the current node is the lowest one, need to return the largest one, only if there are 2 nodes.
            return last_node.getNodeID();
        } else {
            return predecessor.id;
        }
    }

    public String findSuccessor(String id){

        int listSize = serverList.size();
        if(listSize < 2){
            return null;
        }
        if(listSize == 2){

           if(serverList.contains(id)){
                if(serverList.getLast().equals(id)){
                    //System.out.println("findSuccessor( ): no need to report the predecessor back\n");
                    return null; //in this case, the successor is essentially the predecessor, no point to send message to the predecessor itself.
                }
                else{
                    return serverList.getLast();
                }
           }
           else{
            System.out.println("findSuccessor( ): the ring doesn't contain " + id + "\n");
            return null;
           }
        }
        else{ //normal case: >= 3 nodes in the ring

            String successorID = new String();
            if(serverList.contains(id)){

                for (int i = 0; i < listSize; i++) {
                   
                     if(serverList.get(i).equals(id) == true){
                        if(i == (listSize - 1)){ //when the node is the last node
                            successorID = serverList.get(0);
                            
                        }
                        else{ //normal case
                            successorID = serverList.get(i+1);
                        }
                        System.out.println("findSuccessor( ): " + id + "'s successor is found: " + successorID +"\n");
                        return successorID;
                    }
                }
                System.out.println("findSuccessor( ): no successor is found with ring size > 2\n");
                return null;
            }
            else{
                System.out.println("findSuccessor( ): the ring doesn't contain " + id + "\n");
                return null;
            }
        }
    }

    public String getSuccessor(String id){

        if(id == null){
            return null;
        }
        Node n = findNode(id);
        if(n == null || servers_bst.size() == 1){//meaning either no such node is found or only node available now
            return null;
        }

        Node successor = servers_bst.higher(n);
        if(successor == null){ //meaning that the current node is the highest one, need to return the smallest one.
            return first_node.getNodeID();
        } else {
            return successor.id;
        }
    }

    /*
   updateSuccessor( ) works for both adding nodes and removing nodes.
   lower doesn't change, the higher will change for all cases.
     */

    public BigInteger[] getSuccessorRange(ServerNode n){
        return getSuccessorRange(n.getNodeHostPort());
    }

    public BigInteger[] getSuccessorRange(String id){
        BigInteger[] successorRange;
        Node n = findNode(id);
        if(n == null){
            return null;
        }

        Node successor = servers_bst.higher(n);
        if(successor == null){
            return null;
        }
        else {
            successorRange = findHashRange(successor.id);
        }
        return successorRange;
    }
    /*
    findHashRange(): use id(i.e. ip:port) to find the corresponding hash range.
    If only one node in BST, we return low range = high range = server's hash. And this needs to be interpreted
    in KVServer side.
     */
    public BigInteger[] findHashRange(ServerNode n) {
        return findHashRange(n.getNodeHostPort());
    }

    public BigInteger[] findHashRange(String id){

        Node node = null;
        try {
            node = new Node(id, getMD5(id));
        }catch(Exception e){
            e.printStackTrace();
        }
        if(!servers_bst.contains(node)){
            System.out.println("findHashRange: reached here 1\n");
            return null;
        }

        BigInteger[] range = new BigInteger[2];

        if(first_node.id == last_node.id){
            range[0] = first_node.hash;
            range[1] = first_node.hash;
        }
        try {
            Node curr = findNode(id);
            Node pre = servers_bst.lower(curr);

            if(pre == null){
                range[0] = last_node.hash;
                range[1] = curr.hash;
            }
            else{
                range[0] = pre.hash;
                range[1] = curr.hash;
            }

       }catch(Exception e){
            e.printStackTrace();
            return null;
        }
        return range;
    }



    public static void main(String[] args){

        Metadata m = new Metadata();

        //test for add/remove server and update hash2server
        m.addServer("a");
        m.addServer("b");
        m.addServer("c");
        m.addServer("d");
        m.addServer("e");
        m.addServer("f");
        m.removeServer("c");
        m.removeServer("f");
       // m.update_hash2server();
        m.printData("bst");
        m.printData("h2s");


        //test for build bst from hash2server
        m.removeAll();
        if(m.isEmpty())
            System.out.println("bst empty");
        m.build_bst();
        m.printData("bst");


        //test for find server
        BigInteger test_val = new BigInteger("16955237001963240173058271559858726496");
        String server = m.findServer(String.valueOf(test_val));
        System.out.println(server);
    }
}

