package com.svcl.app;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws Exception {
        AppState appState = new AppState();
        appState.initialize();

        SessionStore sessionStore = new SessionStore();
        HttpServer server = HttpServer.create(new InetSocketAddress(AppConfig.HOST, AppConfig.PORT), 0);
        server.createContext("/", new SvclHttpHandler(appState, sessionStore));
        server.setExecutor(null);
        server.start();

        System.out.println("Servidor Java disponible en http://" + AppConfig.HOST + ":" + AppConfig.PORT);
    }
}
