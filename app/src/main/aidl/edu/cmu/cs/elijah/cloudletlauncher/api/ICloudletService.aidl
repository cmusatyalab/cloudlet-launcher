// ICloudletService.aidl
package edu.cmu.cs.elijah.cloudletlauncher.api;

import edu.cmu.cs.elijah.cloudletlauncher.api.ICloudletServiceCallback;

interface ICloudletService {
    void startOpenVpn();

    void endOpenVpn();

    void findCloudlet(String appId);

    void disconnectCloudlet(String appId);

    void registerCallback(ICloudletServiceCallback callback);

    void unregisterCallback(ICloudletServiceCallback callback);
}
