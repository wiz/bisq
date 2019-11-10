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

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

import bisq.grpc.protobuf.GetBalanceRequest;
import bisq.grpc.protobuf.GetVersionRequest;
import bisq.grpc.protobuf.StopServerRequest;
import bisq.grpc.protobuf.GetBalanceGrpc;
import bisq.grpc.protobuf.GetVersionGrpc;
import bisq.grpc.protobuf.StopServerGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

/**
 * gRPC client.
 */
@Slf4j
public class BisqGrpcClient {
    private final ManagedChannel channel;
    private final GetVersionGrpc.GetVersionBlockingStub getVersionStub;
    private final GetBalanceGrpc.GetBalanceBlockingStub getBalanceStub;
    private final StopServerGrpc.StopServerBlockingStub stopServerStub;

    public static void main(String[] args) throws Exception {
        BisqGrpcClient client = new BisqGrpcClient("localhost", 50051);
        try (Scanner scanner = new Scanner(System.in);) {
            while (true) {
                String input = scanner.nextLine();
                String result = "";
                long startTs = System.currentTimeMillis();
                if (input.equals("getVersion")) {
                    result = client.getVersion();
                } else if (input.equals("getBalance")) {
                    result = String.valueOf(client.getBalance());
                } else if (input.equals("stop")) {
                    result = "Shut down client";
                    client.shutdown();
                } else if (input.equals("stopServer")) {
                    client.stopServer();
                    result = "Server stopped";
                }

                // First response is rather slow (300 ms) but following responses are fast (3-5 ms).
                log.info("Request took: {} ms", System.currentTimeMillis() - startTs);
                System.out.println(result);
            }
        }
    }

    private BisqGrpcClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port)
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext(true).build());
    }

    /**
     * Construct client for accessing server using the existing channel.
     */
    private BisqGrpcClient(ManagedChannel channel) {
        this.channel = channel;
        getVersionStub = GetVersionGrpc.newBlockingStub(channel);
        getBalanceStub = GetBalanceGrpc.newBlockingStub(channel);
        stopServerStub = StopServerGrpc.newBlockingStub(channel);
    }

    private String getVersion() {
        GetVersionRequest request = GetVersionRequest.newBuilder().build();
        try {
            return getVersionStub.getVersion(request).getVersion();
        } catch (StatusRuntimeException e) {
            return "RPC failed: " + e.getStatus();
        }
    }

    private long getBalance() {
        GetBalanceRequest request = GetBalanceRequest.newBuilder().build();
        try {
            return getBalanceStub.getBalance(request).getBalance();
        } catch (StatusRuntimeException e) {
            log.warn("RPC failed: {}", e.getStatus());
            return -1;
        }
    }

    private void stopServer() {
        StopServerRequest request = StopServerRequest.newBuilder().build();
        try {
            stopServerStub.stopServer(request);
        } catch (StatusRuntimeException e) {
            log.warn("RPC failed: {}", e.getStatus());
        }
    }

    private void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
        System.exit(0);
    }
}
