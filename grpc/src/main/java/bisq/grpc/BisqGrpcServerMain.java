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
import bisq.core.app.BisqExecutable;
import bisq.core.app.BisqHeadlessAppMain;
import bisq.core.app.BisqSetup;

import bisq.common.UserThread;
import bisq.common.setup.CommonSetup;

/**
 * Main class to start gRPC server with a headless BisqGrpcApp instance.
 */
public class BisqGrpcServerMain extends BisqHeadlessAppMain implements BisqSetup.BisqSetupCompleteListener {
    private static BisqGrpcServer bisqGrpcServer;

    public static void main(String[] args) throws Exception {
        if (BisqExecutable.setupInitialOptionParser(args)) {
            // For some reason the JavaFX launch process results in us losing the thread context class loader: reset it.
            // In order to work around a bug in JavaFX 8u25 and below, you must include the following code as the first line of your realMain method:
            Thread.currentThread().setContextClassLoader(BisqGrpcServerMain.class.getClassLoader());

            new BisqGrpcServerMain().execute(args);
        }
    }

    @Override
    protected void launchApplication() {
        headlessApp = new BisqGrpcApp();
        CommonSetup.setup(BisqGrpcServerMain.this.headlessApp);

        UserThread.execute(this::onApplicationLaunched);
    }

    @Override
    protected void onApplicationStarted() {
        BisqSetup bisqSetup = injector.getInstance(BisqSetup.class);
        bisqSetup.addBisqSetupCompleteListener(this);
        bisqSetup.start();
    }

    @Override
    public void onSetupComplete() {
        final CoreApi coreApi = injector.getInstance(CoreApi.class);
        bisqGrpcServer = new BisqGrpcServer(coreApi);

        // If we start headless we need to keep the main thread busy...
        try {
            bisqGrpcServer.blockUntilShutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
