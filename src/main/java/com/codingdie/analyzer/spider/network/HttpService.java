package com.codingdie.analyzer.spider.network;

import com.codingdie.analyzer.spider.config.SpiderConfigFactory;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.log4j.Logger;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BinaryOperator;

/**
 * Created by xupeng on 2017/5/5.
 */
public class HttpService {

    private static HttpService httpService = null;
    private  boolean proxy_flag = false;

    public synchronized static HttpService getInstance() {
        if (httpService == null) {
            httpService = new HttpService();
        }
        return httpService;
    }

    private OkHttpClient okHttpClient = null;

    private String proxyServer = "proxy.abuyun.com";
    private int proxyPort = 9020;
    private int count = 0;

    private Logger netlogger =Logger.getLogger("network");
    private Logger cookielogger =Logger.getLogger("cookie");

    private Timer timer = new Timer();
    private LinkedBlockingQueue<Integer> linkedBlockingQueue=new LinkedBlockingQueue();

    public HttpService() {
        okHttpClient = this.buildClient();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    if(linkedBlockingQueue.size()==0){
                        for(int i = 0; i< SpiderConfigFactory.getInstance().workConfig.max_http_request_per_second; i++){
                            linkedBlockingQueue.put(new Integer(count++));
                        }
                    }
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        },0L,1000L);
    }


    public  String  excute(Request request) {

        request = request.newBuilder().header("Cookie", SpiderConfigFactory.getInstance().workConfig.cookie)
//                .header("Proxy-Authorization", "Basic " + Base64.getEncoder().encodeToString(SpiderConfigFactory.getInstance().masterConfig.key.getBytes()))
                .build();

        String html = null;
        int n = 0;
        long begin = System.currentTimeMillis();
        while (html == null || html.length() == 0) {
            try {
                if (n > 5) {
                    netlogger.info("尝试" + n + "次失败,耗时" + (System.currentTimeMillis() - begin));
                    return  null;
                }
                n++;
                Response response = getResponse(request);

                if (response.code() != 200) {
                    netlogger.info(request.url()+":failed:"+n);

                    String location = response.header("Location");
                    netlogger.info("Location:" + location + " code" + response.code() + " url:" + request.url());
                    newCookie(response.headers("Set-Cookie"));

                    response.close();

                    if (location != null && !location.equals(request.url())) {
                        request = request.newBuilder().url(location).build();
                    }
                } else {
                    html = response.body().string();
                    netlogger.info(request.url()+":success");

                    newCookie(response.headers("Set-Cookie"));
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        return html;
    }

    private Response getResponse(Request request) {
        Response result = null;
        try {
            Integer integer= linkedBlockingQueue.take();
            result = okHttpClient.newCall(request).execute();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return result;
    }

    private void newCookie(List<String> setCookies) {
        setCookies.iterator().forEachRemaining(str -> {
            cookielogger.info(str);

            String[] strs = str.split(";")[0].split("=");
            String newCokkie = Arrays.stream(SpiderConfigFactory.getInstance().workConfig.cookie.split(";")).filter(s -> {
                return !s.contains(strs[0]);
            }).reduce(new BinaryOperator<String>() {
                @Override
                public String apply(String s, String s2) {

                    return s + ";" + s2;
                }
            }).get() + ";" + strs[0] + "=" + strs[1];

            SpiderConfigFactory.getInstance().workConfig.cookie = newCokkie;
            cookielogger.info(newCokkie);

        });
    }


    private OkHttpClient buildClient() {
        OkHttpClient.Builder builder= new OkHttpClient.Builder().followRedirects(false)

                .readTimeout(30, TimeUnit.SECONDS);
        if(proxy_flag){
            builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyServer, proxyPort)));
        }
        return builder.build();
    }

}