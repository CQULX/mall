package com.lx.gulimall.search.thread;

import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.*;

public class ThreadTest {
    public static ExecutorService executorService = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("main......start......");
//        CompletableFuture.runAsync(()->{
//            System.out.println("当前线程："+Thread.currentThread().getId());
//            int i=10/2;
//            System.out.println("运行结果：" + i);
//        },executorService);
        //方法完成后的感知
//        CompletableFuture<Integer> integerCompletableFuture = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 0;
//            System.out.println("运行结果：" + i);
//            return i;
//        }, executorService).whenCompleteAsync((res,exception)->{
//            //没法修改返回数据
//            System.out.println("异步任务成功,结果是："+res+" 异常是："+exception);
//        }).exceptionally(throwable -> {
//            return 10;
//        });
//        Integer i = integerCompletableFuture.get();
//
//        CompletableFuture<Integer> integerCompletableFuture = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("运行结果：" + i);
//            return i;
//        }, executorService).handle((res,throwable)->{
//            if(res!=null){
//                return res*2;
//            }
//            if(throwable!=null){
//                return 0;
//            }
//            return 0;
//        });

//         CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getId());
//            int i = 10 / 2;
//            System.out.println("运行结果：" + i);
//            return i;
//        }, executorService).thenAcceptAsync((res) -> {
//            System.out.println("任务2启动"+res);
//        }, executorService);


        CompletableFuture<Integer> future1 = CompletableFuture.supplyAsync(() -> {
            System.out.println("任务1线程：" + Thread.currentThread().getId());
            int i = 10 / 2;
            System.out.println("任务1结果：" + i);
            return i;
        }, executorService);
        CompletableFuture<String> future2 = CompletableFuture.supplyAsync(() -> {
            System.out.println("任务2线程：" + Thread.currentThread().getId());
            System.out.println("任务2结束");
            return "Hello";
        }, executorService);

//        future1.runAfterBothAsync(future2,()->{
//            System.out.println("任务3开始");
//        },executorService);
        future1.thenAcceptBothAsync(future2,(f1,f2)->{
            System.out.println("任务3开始之前的结果"+f1+">-----<"+f2);
        },executorService);

        //thenCombine 有任务3的返回值


        System.out.println("main......end......");
    }

    public static void thread(String[] args) throws ExecutionException, InterruptedException {



        /***
         * 线程启动的方式：
         * 1）继承Thread
         *
         *         Thread01 thread=new Thread01();
         *         thread.start();  //启动线程
         *
         * 2）实现Runnable接口
         *  Runnable01 runnable01 = new Runnable01();
         *         new Thread(runnable01).start();
         *
         *  3)实现Callable接口+FutureTask(可以拿到返回结果，可以处理异常)
         *
         *   FutureTask<Integer> futureTask = new FutureTask<>(new Callable01());
         *         new Thread(futureTask).start();
         *         //阻塞等待线程运行完成，获取返回结果
         *         Integer i = futureTask.get();
         *
         * 4)线程池 给线程池提交任务 [ExecutorService]
         * 1、创建
         * 1）、Executors
         * 2)、new ThreadPoolExecutor
         *
         * 七大参数
         * 1 corePoolSize: 核心线程数 线程池准备好以后，就准备就绪的线程数量 就等待来接受异步任务去执行
         * new Thread()
         * 2 maximumPoolSize: 最大线程数量 控制资源
         * 3 keepAliveTime 存活时间 如果当前正在运行的线程数量大于core数量
         *     释放空闲的线程（maximumPoolSize-corePoolSize） 只要线程空闲大于指定的keepAliveTime；
         * 4 unit:时间单位(KeepAliveTime)
         * 5 BlockingQueue<Runnable> workQueue:阻塞队列 如果任务有很多，就将目前多的任务放在阻塞队列里面
         *                                      只要线程空闲，就会去队列里面取出新的任务运行
         * 6 threadFactory 线程的创建工厂
         * 7 RejectedExecutionHandler handler:如果队列满了，按照我们的拒绝策略拒绝执行我们的任务
         *
         */


        System.out.println("main......start......");

//        Thread01 thread=new Thread01();
//        thread.start();  //启动线程



//        Runnable01 runnable01 = new Runnable01();
//        new Thread(runnable01).start();

//        FutureTask<Integer> futureTask = new FutureTask<>(new Callable01());
//        new Thread(futureTask).start();
//        //阻塞等待线程运行完成，获取返回结果
//        Integer i = futureTask.get();

        //系统中池只有一两个，每个异步任务，直接提交给线程池让他自己去执行
//        executorService.execute(new Runnable01());

        ThreadPoolExecutor executor=new ThreadPoolExecutor(5,
                200,
                10,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100000),   //默认是Integer的最大值
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());


        System.out.println("main......end......");


    }



    public static class Thread01 extends Thread{
        @Override
        public void run() {
            System.out.println("当前线程："+Thread.currentThread().getId());
            int i=10/2;
            System.out.println("运行结果：" + i);
        }
    }

    public static class Runnable01 implements Runnable{

        @Override
        public void run() {
            System.out.println("当前线程："+Thread.currentThread().getId());
            int i=10/2;
            System.out.println("运行结果：" + i);
        }
    }

    public static class Callable01 implements Callable<Integer>{

        @Override
        public Integer call() throws Exception {
            System.out.println("当前线程："+Thread.currentThread().getId());
            int i=10/2;
            System.out.println("运行结果：" + i);
            return i;
        }
    }
}
