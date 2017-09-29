package test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class ZookeeperUpload {

	private static String window_spilt = "\\";
	private static String linux_spilt = "/";
	private final static int BUFFER_LEN = 1024;
	private final static int END = -1;

	private static void moveFile2Zookeeper(String sourceAdd,
			String destinationAdd, String zookeeperAdd) {
		ZooKeeper zk = null;
		Stat stat = null;

		try {
			Watcher watcher = new Watcher() {
				public void process(WatchedEvent event) {
					//System.out.println("已经触发了" + event.getType() + "事件！");
				}
			};
			zk = new ZooKeeper(zookeeperAdd, 5000, watcher);

			stat = zk.exists(destinationAdd, watcher);
			if (null == stat) {
				zk.create(destinationAdd, "".getBytes(), Ids.OPEN_ACL_UNSAFE,
						CreateMode.PERSISTENT);
			}
			moveFile(sourceAdd, zk, watcher, destinationAdd);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (KeeperException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			try {
				if (null != zk) {
					zk.close();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

	private static void moveFile(String sourceAdd, ZooKeeper zk,
			Watcher watcher, String destinationAdd) {

		InputStream in = null;
		Stat stat = null;
		String remoteAdd = null;
		try {
			File file = new File(sourceAdd);

			if (!file.isDirectory()) {
				in = new FileInputStream(sourceAdd);
				remoteAdd = destinationAdd + linux_spilt + file.getName();
				stat = zk.exists(remoteAdd, watcher);
				if (null == stat) {
					zk.create(remoteAdd, "".getBytes(), Ids.OPEN_ACL_UNSAFE,
							CreateMode.PERSISTENT);
				}
				StringBuffer sb = new StringBuffer();
				byte[] buffer = new byte[BUFFER_LEN];
				while (true) {
					int byteRead = in.read(buffer);
					if (byteRead == END)
						break;
					sb.append(new String(buffer, 0, byteRead));
				}
				zk.setData(remoteAdd, sb.toString().getBytes(), -1);
			} else {
				String[] filelist = file.list();
				for (int i = 0; i < filelist.length; i++) {
					File readfile = new File(sourceAdd + window_spilt
							+ filelist[i]);
					if (!readfile.isDirectory()) {
						in = new FileInputStream(sourceAdd + window_spilt
								+ filelist[i]);

						stat = zk.exists(destinationAdd, watcher);
						if (null == stat) {
							zk.create(destinationAdd, "".getBytes(),
									Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
						}
						remoteAdd = destinationAdd + linux_spilt
								+ file.getName();
						stat = zk.exists(remoteAdd, watcher);
						if (null == stat) {
							zk.create(remoteAdd, "".getBytes(),
									Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
						}
						remoteAdd = destinationAdd + linux_spilt
								+ file.getName() + linux_spilt
								+ readfile.getName();

						stat = zk.exists(remoteAdd, watcher);
						if (null == stat) {
							zk.create(remoteAdd, "".getBytes(),
									Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
						}
						StringBuffer sb = new StringBuffer();
						byte[] buffer = new byte[BUFFER_LEN];
						while (true) {
							int byteRead = in.read(buffer);
							if (byteRead == END)
								break;
							sb.append(new String(buffer, 0, byteRead));
						}
						zk.setData(remoteAdd, sb.toString().getBytes(), -1); 
						System.out.println("文件" + remoteAdd + " 已处理！");
					} else if (readfile.isDirectory()) {// 这里是按照递归处理的
						String sourceAdd2 = sourceAdd + window_spilt
								+ readfile.getName();
						String destinationAdd2 = destinationAdd + linux_spilt
								+ file.getName(); 
						moveFile(sourceAdd2, zk, watcher, destinationAdd2);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (null != in) {
					in.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void runBatch(String zkhost,String add,String listProj){
		String zkpath = "/searchplatform/config";
		for(String s:listProj.split(",")){
			if(s.length()>1){
				moveFile2Zookeeper(add+s, zkpath, zkhost);
			} 
		}
		
	}
	public static void main(String[] args) {
		String position = "online";//beta  preview  online
		String zkhost=null,add=null;
		boolean batch=false;
		switch (position) {
		case "beta":
			zkhost = "10.202.249.144:2288"; 
			add = "E:\\svn\\config\\beta\\searchplatform4zk\\banned_province";
			break;
		case "preview":
			zkhost = "10.201.168.111:2288";
			add = "E:\\svn\\config\\preview\\searchplatform4zk\\order_commodity04";
			break;
		case "online":
			zkhost = "10.201.131.21:2288";
			add = "E:\\svn\\config\\online\\searchplatform4zk\\fresh_order_item04\\fresh_order_item04.xml";
			break; 
		} 
		if(add!=null && zkhost!=null){
			String zkpath = "/searchplatform/config"; 
			if(position.equals("online")){
				zkpath = "/searchplatform/config2.0/fresh_order_item04"; 
			}
			
			if(batch){
				runBatch(zkhost,add,"product,community,order01,order02,order03,order04,fresh_order01,fresh_order02,fresh_order03,fresh_order04,order_commodity01,order_commodity02,order_commodity03,order_commodity04,cremit_bank_set,huabei_mall,order_user01,order_user02,order_user03,order_user04,huabei,compare_price,order_return01,order_return02,favorite_num,soa_data,banned_province");
			}else{ 
				moveFile2Zookeeper(add, zkpath, zkhost);
			} 
		}
		
	} 
}
