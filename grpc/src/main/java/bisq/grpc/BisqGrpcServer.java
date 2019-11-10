/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.grpc;

import bisq.core.CoreApi;

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

import bisq.grpc.protobuf.GetBalanceReply;
import bisq.grpc.protobuf.GetBalanceRequest;
import bisq.grpc.protobuf.GetBalanceGrpc;
import bisq.grpc.protobuf.GetVersionReply;
import bisq.grpc.protobuf.GetVersionRequest;
import bisq.grpc.protobuf.GetVersionGrpc;
import bisq.grpc.protobuf.StopServerReply;
import bisq.grpc.protobuf.StopServerRequest;
import bisq.grpc.protobuf.StopServerGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

/**
 * gRPC server. Gets a instance of BisqFacade passed to access data from the running Bisq instance.
 */
@Slf4j
public class BisqGrpcServer {

    private Server server;

    private static BisqGrpcServer instance;
    private static CoreApi coreApi;

    private static BisqGrpcServer getInstance() {
        return instance;
    }

    private static CoreApi getCoreApi() {
        return coreApi;
    }

    public BisqGrpcServer(CoreApi coreApi) {
        instance = this;
        BisqGrpcServer.coreApi = coreApi;

        try {
            start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void start() throws IOException {
        /* The port on which the server should run */
        int port = 50051;
        server = ServerBuilder.forPort(port).addService(new GetVersionImpl()).addService(new GetBalanceImpl()).addService(new StopServerImpl()).build().start();
        log.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Use stderr here since the logger may have been reset by its JVM shutdown hook.
            log.error("*** shutting down gRPC server since JVM is shutting down");
            BisqGrpcServer.this.stop();
            log.error("*** server shut down");
        }));
    }

    public void stop() {
        if (server != null) server.shutdown();
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    // Services
    static class GetVersionImpl extends GetVersionGrpc.GetVersionImplBase {
        @Override
        public void getVersion(GetVersionRequest req, StreamObserver<GetVersionReply> responseObserver) {
            GetVersionReply reply = GetVersionReply.newBuilder().setVersion(getCoreApi().getVersion()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }

    static class GetBalanceImpl extends GetBalanceGrpc.GetBalanceImplBase {
        @Override
        public void getBalance(GetBalanceRequest req, StreamObserver<GetBalanceReply> responseObserver) {
            GetBalanceReply reply = GetBalanceReply.newBuilder().setBalance(getCoreApi().getAvailableBalance()).build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }
    }

    static class StopServerImpl extends StopServerGrpc.StopServerImplBase {
        @Override
        public void stopServer(StopServerRequest req, StreamObserver<StopServerReply> responseObserver) {
            StopServerReply reply = StopServerReply.newBuilder().build();
            responseObserver.onNext(reply);
            responseObserver.onCompleted();

            getInstance().stop();
        }
    }
}
