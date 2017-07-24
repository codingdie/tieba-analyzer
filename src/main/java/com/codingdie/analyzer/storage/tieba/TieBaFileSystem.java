package com.codingdie.analyzer.storage.tieba;

import com.codingdie.analyzer.spider.master.tieba.model.tieba.PostDetail;
import com.codingdie.analyzer.spider.master.tieba.model.tieba.PostIndex;
import com.codingdie.analyzer.storage.slave.ContentStorage;
import com.codingdie.analyzer.storage.IndexStorage;
import com.codingdie.analyzer.storage.TaskStorage;
import com.codingdie.analyzer.task.model.Task;

import java.io.File;

/**
 * Created by xupeng on 2017/5/10.
 */
public class TieBaFileSystem {
    public static final int ROLE_MASTER = 0;
    public static final int ROLE_SLAVE = 1;
    private static TieBaFileSystem staveInstance = null;
    private static TieBaFileSystem masterInstance = null;
    private static Object  slaveInstanceLock= new Object();
    private static Object  masterInstanceLock= new Object();

    private IndexStorage<PostIndex> indexStorage;
    private ContentStorage<PostDetail> contentStorage;

    private File root;

    public static TieBaFileSystem getInstance(String name, int role) {
        if (role == ROLE_MASTER) {
            synchronized (slaveInstanceLock){
                 if(masterInstance==null){
                     masterInstance=new TieBaFileSystem(name,role);
                 }
                 return  masterInstance;
            }
        }
        if (role == ROLE_SLAVE) {
            synchronized (slaveInstanceLock){
                if(staveInstance==null){
                    staveInstance=new TieBaFileSystem(name,role);
                }
                return  staveInstance;

            }
        }
        return null;
    }

     TieBaFileSystem(String name, int role) {
        this.root = new File("storage/" + name);
        if (!this.root.exists()) {
            this.root.mkdirs();
        }
        if (role == ROLE_MASTER) {
            File postIndexRootPath = new File(root.getAbsolutePath() + File.separatorChar + "postindex");
            if (!postIndexRootPath.exists()) {
                postIndexRootPath.mkdirs();
            }
            this.indexStorage = new IndexStorage(postIndexRootPath);
        }
        if (role == ROLE_SLAVE) {
            File postContentRootPath = new File(root.getAbsolutePath() + File.separatorChar + "postcontent");
            if (!postContentRootPath.exists()) {
                postContentRootPath.mkdirs();
            }
            this.contentStorage = new ContentStorage(postContentRootPath);
        }

    }

    public <T extends Task> TaskStorage<T> getTaskStorage(Class<T> tClass) {
        File spiderTaskRootPath = new File(root.getAbsolutePath() + File.separatorChar + "task");
        if (!spiderTaskRootPath.exists()) {
            spiderTaskRootPath.mkdirs();
        }
        return new TaskStorage<T>(spiderTaskRootPath, tClass);
    }

    public IndexStorage<PostIndex> getIndexStorage() {
        return indexStorage;
    }

    public ContentStorage getContentStorage() {
        return contentStorage;
    }

    public void clear() {
        this.root.delete();
    }

}
