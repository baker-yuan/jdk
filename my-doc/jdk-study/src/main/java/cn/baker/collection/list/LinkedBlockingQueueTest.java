package cn.baker.collection.list;

import java.util.concurrent.LinkedBlockingQueue;

public class LinkedBlockingQueueTest {
    public static void main(String[] args) throws InterruptedException {
        LinkedBlockingQueue<String> linkedBlockingQueue = new LinkedBlockingQueue<>();
        linkedBlockingQueue.offer("");

        String take = linkedBlockingQueue.take();



        System.out.println();

    }
}
