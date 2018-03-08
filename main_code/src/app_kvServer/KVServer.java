package app_kvServer;

import common.cache.StorageManager;
import common.metadata.Metadata;
import common.zookeeper.ZookeeperWatcher;
import ecs.ServerNode;
import logger.LogSetup;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.net.ServerSocket;

// IN PROGRESS
public class KVServer implements IKVServer {
    //log info
    private static final String PROMPT = "KVSERVER>";
    private static final Logger LOGGER = Logger.getLogger(KVServer.class);

    //connection info
    private int port;
    private String hostname = null;
    private ServerSocket serverSocket;
    private boolean running = false;
    private boolean stop = false;

    private static int numConnectedClients = 0; // this variable should be in KVClientConnection

	private static StorageManager storage;

	/* This needs to be passed into ClientConnections & ZookeeperWatcher thread */
	private static ServerNode serverNode;

	private static UpcomingStatusQueue upcomingStatusQueue = new UpcomingStatusQueue();

	/**
	 * Start KV Server at given port
	 * @param port given port for storage server to operate
	 * @param cacheSize specifies how many key-value pairs the server is allowed
	 *           to keep in-memory
	 * @param strategy specifies the cache replacement strategy in case the cache
	 *           is full and there is a GET- or PUT-request on a key that is
	 *           currently not contained in the cache. Options are "FIFO", "LRU",
	 *           and "LFU".
	 */

	static {
		try {
			new LogSetup("logs/server.log", Level.DEBUG);
		} catch (IOException e) {
			System.out.println("Error! Unable to initialize logger!");
			e.printStackTrace();
			System.exit(1);
		}
	}

	// in progress
	public KVServer(String name, String zkHostname, int zkPort) { // m2 interface
		ZookeeperWatcher zookeeperWatcher = null;
		try {
			String zookeeperHost = zkHostname + ":" + Integer.toString(zkPort);
			zookeeperWatcher = new ZookeeperWatcher(zookeeperHost,100000,name, upcomingStatusQueue);
		} catch (IOException | InterruptedException e) {
			LOGGER.error("Failed to connect to zookeeper server");
			System.exit(-1);
		}

		try { // get serverNode
			serverNode = zookeeperWatcher.initServerNode();
			zookeeperWatcher.setServerNode(serverNode); // zookeeperWatcher may change this when receive data updates
			ServerStatus ss = new ServerStatus(ServerStatusType.INITIALIZE);
			serverNode.setServerStatus(ss);
		} catch (KeeperException | InterruptedException e){
			LOGGER.error("Failed to get data from zNode ",e);
			System.exit(-1);
		}
		storage = new StorageManager(serverNode.getCacheSize(), serverNode.getCacheStrategy());
		zookeeperWatcher.run(); // NOW IT SETS THE WATCH AND WAITS FOR DATA CHANGES

		// TODO: HERE IS WHERE YOU INITIALIZE THE KVCLIENCONNECTION
		// & then run it
	}

//    public boolean isRunning() {
//        return this.running;
//    }

	private CacheStrategy string_to_enum_cache_strategy(String str) {
		switch (str.toLowerCase()){
			case "LRU":
				return CacheStrategy.LRU;
			case "LFU":
				return CacheStrategy.LFU;
			case "FIFO":
				return CacheStrategy.FIFO;
			default:
				return CacheStrategy.None;
		}
	}

	/*
		For update & initKVServer, confirm argument type & return type when the
		following question is answered:
		https://piazza.com/class/jc6l5ut99r35yl?cid=270
	 */
//	public void initKVServer(byte[] metadata, int cacheSize, String replacementStrategy) {
//
//	}
//
//	public void update(byte[] metadata) {
//
//	}

	@Override
	public int getPort(){
		// TODO Auto-generated method stub
//		LOGGER.info(">Server port: " + this.port);
		return port;
	}

	@Override
    public String getHostname(){
		// TODO Auto-generated method stubc
//		LOGGER.info("Server hostname: " + hostname);
		return hostname;
	}

	@Override
    public CacheStrategy getCacheStrategy(){
		// TODO Auto-generated method stub
        //LOGGER.info("Server ("+hostname+","+port+") : CacheManager Strategy is "+ cacheStrategy);
		return string_to_enum_cache_strategy(serverNode.getCacheStrategy());
	}

	@Override
    public int getCacheSize(){
		// TODO Auto-generated method stub
		return serverNode.getCacheSize();
	}

    @Override
    public boolean inStorage(String key){
		// TODO Auto-generated method stub
        if(key != null && !(key.isEmpty()) && !(key.equals("")) && !(key.contains(" ")) && !(key.length() > 20)) {
            return storage.inDatabase(key);
        }
        else{
            return false;
        }
	}

	@Override
    public boolean inCache(String key){
		// TODO Auto-generated method stub
		return storage.inCache(key);
	}

	@Override
    public String getKV(String key) throws Exception{
		// TODO Auto-generated method stub
        return storage.getKV(key);
	}

	@Override
    public void putKV(String key, String value) throws Exception{
		// TODO Auto-generated method stub
        if(storage.putKV(key, value)){
            LOGGER.info("Server ("+hostname+","+port+") : Success in putKV");
        }
        else{
            LOGGER.info("Server ("+hostname+","+port+") : Error in putKV");
        }
	}

	@Override
    public void clearCache(){
		// TODO Auto-generated method stub
		storage.clearCache();
		return;
	}

	@Override
    public void clearStorage(){
		// TODO Auto-generated method stub
		storage.clearAll();
	}

	public void run(){
		// TODO Auto-generated method stub
		while(true){

		}
	}
//
//	private boolean initializeServer() {
//		LOGGER.info("Initialize server ...");
//		try {
//			this.serverSocket = new ServerSocket(this.port);
//          this.hostname = serverSocket.getInetAddress().getHostName();
//			this.port = this.serverSocket.getLocalPort();
//			LOGGER.info("Server listening on port: " + this.serverSocket.getLocalPort());
//			this.serverSocket.setSoTimeout(1000); // 1 s
//			return true;
//		} catch (IOException e) {
//			LOGGER.error("Error! Cannot open server socket:");
//			if(e instanceof BindException){
//				LOGGER.error("Port " + port + " is already bound!");
//			}
//			return false;
//		}
//	}

	@Override
    public void kill(){ //here kill( ) will be same as close( ) as we are using write-through cache. For now, leave it as the same as close()
		// TODO
        try {
            serverSocket.close();
        } catch (IOException e) {
            LOGGER.error("Error! " + "Unable to close socket on port: " + port, e);
        }
        running = false;
		stop = true;
	}

	@Override
    public void close(){
		// TODO
		try {
			serverSocket.close();
		} catch (IOException e) {
			LOGGER.error("Error! " + "Unable to close socket on port: " + port, e);
		}
		running = false;
		stop = true;
	}

	//TODO
	@Override
	public void start() {

	}

	@Override
	public void stop() {
	}


	@Override
	public void lockWrite() {

	}

	@Override
	public void unlockWrite() {

	}

	@Override
	public boolean moveData(String[] hashRange, String targetName) throws Exception {
		return false;
	}

	public static void main(String[] args){
		//TODO read from cmdline the arguments needed to start KVServer
//			KVServer server = new KVServer(50000,10,"LRU"); // these should be from cmdline
//			server.run();
	}
}
