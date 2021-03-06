package com.digger.analyzer;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.digger.config.AkkaConfigBuilder;
import com.digger.config.TieBaAnalyserConfigFactory;
import com.digger.analyzer.task.TaskReceiverActor;

import java.util.Arrays;

/**
 * Created by xupeng on 2017/4/14.
 */
public class SlaveStarter {
    public static void main(String[] args) throws Exception {
        final ActorSystem system = ActorSystem.create("cluster", new AkkaConfigBuilder().consoleParam(args).roles(Arrays.asList("slave")).build());
        TieBaAnalyserConfigFactory.getInstance();
        ActorRef detailSpiderSlaveActor = system.actorOf(Props.create(TaskReceiverActor.class), TaskReceiverActor.class.getSimpleName());
        System.out.println(detailSpiderSlaveActor.path().toString());
    }


}

