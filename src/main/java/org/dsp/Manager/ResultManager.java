package org.dsp.Manager;

import org.dsp.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ResultManager {//TODO main thread creates it in the beginning
    private final ConcurrentHashMap<String/*bucket name*/, Pair<Integer/*number of links*/, List<WorkerResult>>> resultByLocalApplication;
    public ResultManager(){
        this.resultByLocalApplication = new ConcurrentHashMap<>();
    }

    public void addLocalBucketKey(String bucketName, int numberOfLinks){
        this.resultByLocalApplication.put(bucketName, new Pair<>(numberOfLinks, new LinkedList<>()));
    }

    public void addResult(String bucketName, WorkerResult workerResult){
        Pair<Integer,List<WorkerResult>> value = this.resultByLocalApplication.get(bucketName);
        value.getSecond().add(workerResult);
        System.out.println("result added"+ workerResult.toString());
    }

    public void deleteLocalBucketEntry(String bucketName){
        this.resultByLocalApplication.remove(bucketName);
    }

    public boolean didFinishRequest(String bucketName){//for workersResultThread
        Pair<Integer,List<WorkerResult>> value = this.resultByLocalApplication.get(bucketName);
        return value.getFirst() == value.getSecond().size();
    }
    public List<WorkerResult> getWorkerResults(String bucketNameOfLocal){
        return this.resultByLocalApplication.get(bucketNameOfLocal).getSecond();
    }

    public boolean isEmpty(){
        return this.resultByLocalApplication.isEmpty();
    }

}
